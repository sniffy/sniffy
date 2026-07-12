package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.util.*;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.function.Consumer;

import static io.sniffy.util.ReflectionUtil.invokeMethod;

/**
 * @since 3.1.7
 */
public class SniffySelector extends AbstractSelector {

    private static final Polyglog LOG = PolyglogFactory.log(SniffySelector.class);

    private final AbstractSelector delegate;

    /*
     * Ownership and locking:
     * - every delegate key owns its SelectionKeyLink attachment; this selector only tracks active links for cleanup;
     * - registration is entered with wrapper channel regLock/keyLock and may then take delegate channel locks;
     * - selection/cancellation cleanup runs after delegate select releases delegate locks, then takes wrapper keyLock;
     * - no path may hold a delegate channel/selector lock while acquiring a wrapper channel lock.
     */
    private final Set<SelectionKeyLink> activeLinks = Collections.newSetFromMap(
            new java.util.concurrent.ConcurrentHashMap<SelectionKeyLink, Boolean>());

    private volatile Set<SelectionKey> keysWrapper = null;
    private volatile Set<SelectionKey> selectedKeysWrapper = null;

    public SniffySelector(SelectorProvider provider, AbstractSelector delegate) {
        super(provider);
        this.delegate = delegate;
        LOG.trace("Created new SniffySelector(" + provider + ", " + delegate + ") = " + this);
    }

    public SniffySelectionKey wrap(SelectionKey delegate, SniffySelector sniffySelector, SelectableChannel sniffySocketChannel) {
        if (sniffySelector != this) {
            throw new IllegalArgumentException("SelectionKey cannot be wrapped by a different SniffySelector");
        }
        return requireLinkedKey(delegate);
    }

    private SniffySelectionKey requireLinkedKey(SelectionKey delegate) {
        Object association = delegate.attachment();
        if (!(association instanceof SelectionKeyLink)) {
            String message = "Delegate SelectionKey attachment no longer contains Sniffy's internal link: " + association;
            LOG.error(message);
            throw new IllegalStateException(message);
        }
        SelectionKeyLink link = (SelectionKeyLink) association;
        if (!link.belongsTo(this)) {
            String message = "Delegate SelectionKey is linked to a different Sniffy selector";
            LOG.error(message);
            throw new IllegalStateException(message);
        }
        return link.wrapper(delegate);
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void implCloseSelector() throws IOException {
        IOException failure = null;
        try {
            delegate.close();
        } catch (IOException e) {
            failure = e;
        }
        try {
            cleanupLinks(true);
        } catch (IOException e) {
            if (failure == null) {
                failure = e;
            } else {
                failure.addSuppressed(e);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private Set<SelectionKey> wrapKeys(final Set<SelectionKey> delegates) {
        if (null == keysWrapper) {
            synchronized (this) {
                if (null == keysWrapper && null != delegates) {
                    keysWrapper = new SelectionKeySetView(delegates, false);
                }
            }
        }
        return keysWrapper;
    }

    private Set<SelectionKey> wrapSelectedKeys(final Set<SelectionKey> delegates) {
        if (null == selectedKeysWrapper) {
            synchronized (this) {
                if (null == selectedKeysWrapper && null != delegates) {
                    selectedKeysWrapper = new SelectionKeySetView(delegates, true);
                }
            }
        }
        return selectedKeysWrapper;
    }

    private final class SelectionKeySetView extends AbstractSet<SelectionKey> {

        private final Set<SelectionKey> delegates;
        private final boolean removalAllowed;

        private SelectionKeySetView(Set<SelectionKey> delegates, boolean removalAllowed) {
            this.delegates = delegates;
            this.removalAllowed = removalAllowed;
        }

        @Override
        public Iterator<SelectionKey> iterator() {
            final Iterator<SelectionKey> iterator = delegates.iterator();
            return new Iterator<SelectionKey>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public SelectionKey next() {
                    return requireLinkedKey(iterator.next());
                }

                @Override
                public void remove() {
                    ensureRemovalAllowed();
                    iterator.remove();
                }
            };
        }

        @Override
        public int size() {
            return delegates.size();
        }

        @Override
        public boolean contains(Object candidate) {
            SelectionKey delegateKey = unwrap(candidate);
            return delegateKey != null && delegates.contains(delegateKey) && requireLinkedKey(delegateKey) == candidate;
        }

        @Override
        public boolean remove(Object candidate) {
            ensureRemovalAllowed();
            SelectionKey delegateKey = unwrap(candidate);
            return delegateKey != null && delegates.remove(delegateKey);
        }

        @Override
        public void clear() {
            ensureRemovalAllowed();
            delegates.clear();
        }

        private SelectionKey unwrap(Object candidate) {
            if (!(candidate instanceof SniffySelectionKey)) {
                return null;
            }
            SniffySelectionKey wrapper = (SniffySelectionKey) candidate;
            return wrapper.selector() == SniffySelector.this ? wrapper.getDelegate() : null;
        }

        private void ensureRemovalAllowed() {
            if (!removalAllowed) {
                throw new UnsupportedOperationException("Selector.keys() does not support removal");
            }
        }
    }

    private class SelectionKeyConsumerWrapper implements Consumer<SelectionKey> {

        private final Consumer<SelectionKey> delegate;
        public SelectionKeyConsumerWrapper(Consumer<SelectionKey> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void accept(SelectionKey selectionKey) {
            SniffySelectorProvider.exitDelegateSelectorConstruction();
            try {
                delegate.accept(requireLinkedKey(selectionKey));
            } finally {
                SniffySelectorProvider.enterDelegateSelectorConstruction();
            }
        }

    }

    /** Registers the delegate channel while publishing the wrapper key through its attachment link. */
    @Override
    protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
        try {

            AbstractSelectableChannel chDelegate = ch;

            if (ch instanceof SelectableChannelWrapper) {
                chDelegate = ((SelectableChannelWrapper<?>) ch).getDelegate();
            }

            SelectionKeyLink link = new SelectionKeyLink(this, ch, att);
            activeLinks.add(link);
            try {
                SelectionKey selectionKeyDelegate = chDelegate.register(delegate, ops, link);
                return link.wrapper(selectionKeyDelegate);
            } catch (RuntimeException e) {
                activeLinks.remove(link);
                throw e;
            }

        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @Override
    public Set<SelectionKey> keys() {
        return wrapKeys(delegate.keys());
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        return wrapSelectedKeys(delegate.selectedKeys());
    }

    /**
     * This methods processes de-register queue (filled-in using selectionKey.cancel() method)
     * As a result it modifies the cancelledKeys field and also removes selectionKeys from associated channels
     */
    @Override
    public int selectNow() throws IOException {
        propagateWrapperCancellations();
        int selected = selectNowDelegate();
        cleanupLinks(false);
        return selected;
    }

    /**
     * This methods processes de-register queue (filled-in using selectionKey.cancel() method)
     * As a result it modifies the cancelledKeys field and also removes selectionKeys from associated channels
     */
    @Override
    public int select(long timeout) throws IOException {
        propagateWrapperCancellations();
        int selected = selectDelegate(timeout);
        cleanupLinks(false);
        return selected;
    }

    /**
     * This methods processes de-register queue (filled-in using selectionKey.cancel() method)
     * As a result it modifies the cancelledKeys field and also removes selectionKeys from associated channels
     */
    @Override
    public int select() throws IOException {
        propagateWrapperCancellations();
        int selected = selectDelegate();
        cleanupLinks(false);
        return selected;
    }

    @Override
    public Selector wakeup() {
        SniffySelectorProvider.enterDelegateSelectorConstruction();
        try {
            delegate.wakeup();
        } finally {
            SniffySelectorProvider.exitDelegateSelectorConstruction();
        }
        return this;
    }

    private int selectNowDelegate() throws IOException {
        SniffySelectorProvider.enterDelegateSelectorConstruction();
        try {
            return delegate.selectNow();
        } finally {
            SniffySelectorProvider.exitDelegateSelectorConstruction();
        }
    }

    private int selectDelegate(long timeout) throws IOException {
        SniffySelectorProvider.enterDelegateSelectorConstruction();
        try {
            return delegate.select(timeout);
        } finally {
            SniffySelectorProvider.exitDelegateSelectorConstruction();
        }
    }

    private int selectDelegate() throws IOException {
        SniffySelectorProvider.enterDelegateSelectorConstruction();
        try {
            return delegate.select();
        } finally {
            SniffySelectorProvider.exitDelegateSelectorConstruction();
        }
    }

    private void cleanupLinks(boolean all) throws IOException {
        IOException failure = null;
        for (SelectionKeyLink link : activeLinks) {
            SelectionKey delegateKey = link.delegate();
            if (!all && (delegateKey == null || delegateKey.isValid())) {
                continue;
            }
            try {
                SniffySelectionKey wrapper = link.existingWrapper();
                AbstractSelectableChannel channel = link.channel();
                if (wrapper != null && channel != null && channel.keyFor(this) == wrapper) {
                    JdkNioAccess.resolve().removeChannelKey(channel, wrapper);
                }
                activeLinks.remove(link); // remove only after wrapper cleanup succeeds
            } catch (Exception e) {
                LOG.error("Failed to reconcile a deregistered NIO selection key; cleanup will be retried", e);
                IOException ioe = new IOException("Failed to reconcile a deregistered NIO selection key", e);
                if (failure == null) {
                    failure = ioe;
                } else {
                    failure.addSuppressed(ioe);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private void propagateWrapperCancellations() {
        for (SelectionKeyLink link : activeLinks) {
            SniffySelectionKey wrapper = link.existingWrapper();
            SelectionKey delegateKey = link.delegate();
            if (wrapper != null && !wrapper.isValid() && delegateKey != null && delegateKey.isValid()) {
                delegateKey.cancel();
            }
        }
    }

    int activeLinkCount() {
        return activeLinks.size();
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    @SuppressWarnings({"RedundantThrows", "Since15"})
    public int select(Consumer<SelectionKey> action, long timeout) throws IOException {
        try {
            propagateWrapperCancellations();
            int selected = selectDelegate(action, timeout);
            cleanupLinks(false);
            return selected;
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    @SuppressWarnings({"RedundantThrows", "Since15"})
    public int select(Consumer<SelectionKey> action) throws IOException {
        try {
            propagateWrapperCancellations();
            int selected = selectDelegate(action);
            cleanupLinks(false);
            return selected;
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    @SuppressWarnings({"RedundantThrows", "Since15"})
    public int selectNow(Consumer<SelectionKey> action) throws IOException {
        try {
            propagateWrapperCancellations();
            int selected = selectNowDelegate(action);
            cleanupLinks(false);
            return selected;
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    private int selectDelegate(Consumer<SelectionKey> action, long timeout) throws Exception {
        SniffySelectorProvider.enterDelegateSelectorConstruction();
        try {
            return invokeMethod(Selector.class, delegate, "select",
                    Consumer.class, new SelectionKeyConsumerWrapper(action),
                    Long.TYPE, timeout,
                    Integer.TYPE
            );
        } finally {
            SniffySelectorProvider.exitDelegateSelectorConstruction();
        }
    }

    private int selectDelegate(Consumer<SelectionKey> action) throws Exception {
        SniffySelectorProvider.enterDelegateSelectorConstruction();
        try {
            return invokeMethod(Selector.class, delegate, "select",
                    Consumer.class, new SelectionKeyConsumerWrapper(action),
                    Integer.TYPE
            );
        } finally {
            SniffySelectorProvider.exitDelegateSelectorConstruction();
        }
    }

    private int selectNowDelegate(Consumer<SelectionKey> action) throws Exception {
        SniffySelectorProvider.enterDelegateSelectorConstruction();
        try {
            return invokeMethod(Selector.class, delegate, "selectNow",
                    Consumer.class, new SelectionKeyConsumerWrapper(action),
                    Integer.TYPE
            );
        } finally {
            SniffySelectorProvider.exitDelegateSelectorConstruction();
        }
    }

}
