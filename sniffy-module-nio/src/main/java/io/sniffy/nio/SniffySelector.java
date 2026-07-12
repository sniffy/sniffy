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
import static io.sniffy.util.ReflectionUtil.setField;

/**
 * @since 3.1.7
 */
public class SniffySelector extends AbstractSelector {

    private static final Polyglog LOG = PolyglogFactory.log(SniffySelector.class);

    private final AbstractSelector delegate;

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
        try {
            // TODO: document
            setField(AbstractSelector.class, delegate, "closed", true);
            invokeMethod(AbstractSelector.class, delegate, "implCloseSelector", Void.class);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
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
            delegate.accept(requireLinkedKey(selectionKey));
        }

    }

    /**
     * This method adds a selection key to provided AbstractSelectableChannel, hence we're doing the same here manually
     */
    @Override
    // TODO: document
    protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
        try {

            AbstractSelectableChannel chDelegate = ch;

            if (ch instanceof SelectableChannelWrapper) {
                chDelegate = ((SelectableChannelWrapper<?>) ch).getDelegate();
            }

            SelectionKeyLink link = new SelectionKeyLink(this, ch, att);

            SelectionKey selectionKeyDelegate = invokeMethod(AbstractSelector.class, delegate, "register",
                    AbstractSelectableChannel.class, chDelegate,
                    Integer.TYPE, ops,
                    Object.class, link,
                    SelectionKey.class
            );

            Object regLock = ReflectionUtil.getField(AbstractSelectableChannel.class, chDelegate, "regLock");
            Object keyLock = ReflectionUtil.getField(AbstractSelectableChannel.class, chDelegate, "keyLock");
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (regLock) {
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (keyLock) {
                    invokeMethod(AbstractSelectableChannel.class, chDelegate, "addKey", SelectionKey.class, selectionKeyDelegate, Void.class);
                }
            }

            return link.wrapper(selectionKeyDelegate);

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

    private void copyKeysFromDelegate() {

        // TODO: copy keys from delegate channels to sniffy channels
        // TODO: consider doing in background?

    }

    /**
     * This methods processes de-register queue (filled-in using selectionKey.cancel() method)
     * As a result it modifies the cancelledKeys field and also removes selectionKeys from associated channels
     */
    @Override
    public int selectNow() throws IOException {
        return delegate.selectNow();
    }

    /**
     * This methods processes de-register queue (filled-in using selectionKey.cancel() method)
     * As a result it modifies the cancelledKeys field and also removes selectionKeys from associated channels
     */
    @Override
    public int select(long timeout) throws IOException {
        return delegate.select(timeout);
    }

    /**
     * This methods processes de-register queue (filled-in using selectionKey.cancel() method)
     * As a result it modifies the cancelledKeys field and also removes selectionKeys from associated channels
     */
    @Override
    public int select() throws IOException {
        return delegate.select();
    }

    @Override
    public Selector wakeup() {
        delegate.wakeup();
        return this;
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    @SuppressWarnings({"RedundantThrows", "Since15"})
    public int select(Consumer<SelectionKey> action, long timeout) throws IOException {
        try {
            // TODO: call delegate.processDeregisterQueue and update selection keys from delegate
            return invokeMethod(Selector.class, delegate, "select",
                    Consumer.class, new SelectionKeyConsumerWrapper(action),
                    Long.TYPE, timeout,
                    Integer.TYPE
            );
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    @SuppressWarnings({"RedundantThrows", "Since15"})
    public int select(Consumer<SelectionKey> action) throws IOException {
        try {
            return invokeMethod(Selector.class, delegate, "select",
                    Consumer.class, new SelectionKeyConsumerWrapper(action),
                    Integer.TYPE
            );
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    @SuppressWarnings({"RedundantThrows", "Since15"})
    public int selectNow(Consumer<SelectionKey> action) throws IOException {
        try {
            return invokeMethod(Selector.class, delegate, "selectNow",
                    Consumer.class, new SelectionKeyConsumerWrapper(action),
                    Integer.TYPE
            );
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

}
