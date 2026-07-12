package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.util.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
        if (delegate instanceof SniffySelector) {
            throw new IllegalArgumentException("SniffySelector requires an original selector delegate");
        }
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
        Throwable failure = null;
        try {
            delegate.close();
        } catch (Throwable e) {
            failure = e;
        }
        try {
            cleanupLinks(true);
        } catch (Throwable e) {
            if (failure == null) {
                failure = e;
            } else {
                failure.addSuppressed(e);
            }
        }
        if (failure != null) {
            rethrowSelectionFailure(failure);
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
            return delegateKey != null
                    && delegates.contains(delegateKey)
                    && requireLinkedKey(delegateKey) == candidate
                    && delegates.remove(delegateKey);
        }

        @Override
        public boolean add(SelectionKey selectionKey) {
            throw new UnsupportedOperationException("Selector key views do not support addition");
        }

        @Override
        public boolean addAll(Collection<? extends SelectionKey> collection) {
            throw new UnsupportedOperationException("Selector key views do not support addition");
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            ensureRemovalAllowed();
            Objects.requireNonNull(collection, "collection");
            boolean modified = false;
            Iterator<SelectionKey> iterator = iterator();
            while (iterator.hasNext()) {
                if (collection.contains(iterator.next())) {
                    iterator.remove();
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            ensureRemovalAllowed();
            Objects.requireNonNull(collection, "collection");
            boolean modified = false;
            Iterator<SelectionKey> iterator = iterator();
            while (iterator.hasNext()) {
                if (!collection.contains(iterator.next())) {
                    iterator.remove();
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public boolean removeIf(Predicate<? super SelectionKey> filter) {
            ensureRemovalAllowed();
            Objects.requireNonNull(filter, "filter");
            boolean modified = false;
            Iterator<SelectionKey> iterator = iterator();
            while (iterator.hasNext()) {
                if (filter.test(iterator.next())) {
                    iterator.remove();
                    modified = true;
                }
            }
            return modified;
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
        SelectionKeyLink link = new SelectionKeyLink(this, ch, att);
        boolean success = false;
        Throwable failure = null;
        try {
            AbstractSelectableChannel chDelegate = ch;
            if (ch instanceof SelectableChannelWrapper) {
                chDelegate = ((SelectableChannelWrapper<?>) ch).getDelegate();
            }
            activeLinks.add(link);
            if (ch instanceof SelectableChannelWrapper) {
                ((SelectableChannelWrapper<?>) ch).registerKeyLink(link);
            }
            SelectionKey selectionKeyDelegate = chDelegate.register(delegate, ops, link);
            SniffySelectionKey wrapper = link.wrapper(selectionKeyDelegate);
            success = true;
            return wrapper;
        } catch (Throwable e) {
            failure = e;
            throw ExceptionUtil.processException(e);
        } finally {
            if (!success) {
                try {
                    rollbackRegistration(link, ch);
                } catch (Throwable cleanupFailure) {
                    if (failure != null) {
                        failure.addSuppressed(cleanupFailure);
                    } else {
                        throw ExceptionUtil.processException(cleanupFailure);
                    }
                }
            }
        }
    }

    private void rollbackRegistration(SelectionKeyLink link, AbstractSelectableChannel channel) {
        Throwable failure = null;
        SelectionKey delegateKey = link.delegate();
        if (delegateKey != null) {
            try {
                delegateKey.cancel();
            } catch (Throwable e) {
                failure = e;
            }
        }
        if (channel instanceof SelectableChannelWrapper) {
            try {
                ((SelectableChannelWrapper<?>) channel).unregisterKeyLink(link);
            } catch (Throwable e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        activeLinks.remove(link);
        if (failure != null) {
            throw ExceptionUtil.throwException(failure);
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

    @Override
    public int selectNow() throws IOException {
        return selectWithCleanup(new SelectionOperation() {
            @Override
            public int execute() throws Exception {
                return selectNowDelegate();
            }
        });
    }

    @Override
    public int select(final long timeout) throws IOException {
        return selectWithCleanup(new SelectionOperation() {
            @Override
            public int execute() throws Exception {
                return selectDelegate(timeout);
            }
        });
    }

    @Override
    public int select() throws IOException {
        return selectWithCleanup(new SelectionOperation() {
            @Override
            public int execute() throws Exception {
                return selectDelegate();
            }
        });
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
            SniffySelectionKey wrapper = link.existingWrapper();
            if (!all && (delegateKey == null || (wrapper == null ? delegateKey.isValid() : wrapper.isValid()))) {
                continue;
            }
            try {
                AbstractSelectableChannel channel = link.channel();
                if (wrapper != null && channel != null && channel.keyFor(this) == wrapper) {
                    JdkNioAccess.resolve().removeChannelKey(channel, wrapper);
                }
                if (channel instanceof SelectableChannelWrapper) {
                    ((SelectableChannelWrapper<?>) channel).unregisterKeyLink(link);
                }
                activeLinks.remove(link); // remove only after wrapper cleanup succeeds
            } catch (Throwable e) {
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

    int activeLinkCount() {
        return activeLinks.size();
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    @SuppressWarnings({"RedundantThrows", "Since15"})
    public int select(final Consumer<SelectionKey> action, final long timeout) throws IOException {
        return selectWithCleanup(new SelectionOperation() {
            @Override
            public int execute() throws Exception {
                return selectDelegate(action, timeout);
            }
        });
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    @SuppressWarnings({"RedundantThrows", "Since15"})
    public int select(final Consumer<SelectionKey> action) throws IOException {
        return selectWithCleanup(new SelectionOperation() {
            @Override
            public int execute() throws Exception {
                return selectDelegate(action);
            }
        });
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    @SuppressWarnings({"RedundantThrows", "Since15"})
    public int selectNow(final Consumer<SelectionKey> action) throws IOException {
        return selectWithCleanup(new SelectionOperation() {
            @Override
            public int execute() throws Exception {
                return selectNowDelegate(action);
            }
        });
    }

    private int selectWithCleanup(SelectionOperation operation) throws IOException {
        Throwable primaryFailure = null;
        try {
            return operation.execute();
        } catch (Throwable e) {
            primaryFailure = unwrapInvocationFailure(e);
            rethrowSelectionFailure(primaryFailure);
            return 0;
        } finally {
            try {
                cleanupLinks(false);
            } catch (Throwable cleanupFailure) {
                if (primaryFailure != null) {
                    primaryFailure.addSuppressed(cleanupFailure);
                } else {
                    rethrowSelectionFailure(cleanupFailure);
                }
            }
        }
    }

    private static Throwable unwrapInvocationFailure(Throwable failure) {
        if (failure instanceof InvocationTargetException
                && ((InvocationTargetException) failure).getTargetException() != null) {
            return ((InvocationTargetException) failure).getTargetException();
        }
        return failure;
    }

    private static void rethrowSelectionFailure(Throwable failure) throws IOException {
        if (failure instanceof IOException) {
            throw (IOException) failure;
        }
        throw ExceptionUtil.throwException(failure);
    }

    private interface SelectionOperation {
        int execute() throws Exception;
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
