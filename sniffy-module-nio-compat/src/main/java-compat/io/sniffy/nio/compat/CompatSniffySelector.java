package io.sniffy.nio.compat;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.ReflectionUtil;
import io.sniffy.util.SetWrapper;
import io.sniffy.util.WrapperFactory;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import static io.sniffy.util.ReflectionUtil.invokeMethod;
import static io.sniffy.util.ReflectionUtil.setField;

/**
 * @since 3.1.7
 */
public class CompatSniffySelector extends AbstractSelector {

    private static final Polyglog LOG = PolyglogFactory.log(CompatSniffySelector.class);

    private final AbstractSelector delegate;

    private volatile Set<SelectionKey> keysWrapper = null;
    private volatile Set<SelectionKey> selectedKeysWrapper = null;

    private final Map<AbstractSelectableChannel, CompatSniffySocketChannel> channelToSniffyChannelMap =
            new WeakHashMap<AbstractSelectableChannel, CompatSniffySocketChannel>();

    private final Map<SelectionKey, CompatSniffySelectionKey<CompatSniffySocketChannel>> sniffySelectionKeyCache =
            new WeakHashMap<SelectionKey, CompatSniffySelectionKey<CompatSniffySocketChannel>>();

    public CompatSniffySelector(SelectorProvider provider, AbstractSelector delegate) {
        super(provider);
        this.delegate = delegate;
        LOG.trace("Created new SniffySelector(" + provider + ", " + delegate + ") = " + this);
    }

    public CompatSniffySelectionKey<CompatSniffySocketChannel> wrap(SelectionKey delegate, CompatSniffySelector sniffySelector, CompatSniffySocketChannel sniffySocketChannel) {
        CompatSniffySelectionKey<CompatSniffySocketChannel> sniffySelectionKey = sniffySelectionKeyCache.get(delegate);
        if (null == sniffySelectionKey) {
            synchronized (sniffySelectionKeyCache) {
                sniffySelectionKey = sniffySelectionKeyCache.get(delegate);
                if (null == sniffySelectionKey) {
                    sniffySelectionKey = new CompatSniffySelectionKey<CompatSniffySocketChannel>(delegate, sniffySelector, sniffySocketChannel);
                    sniffySelectionKeyCache.put(delegate, sniffySelectionKey);
                }
            }
        }
        return sniffySelectionKey;
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
                    keysWrapper = createSelectionKeysWrapper(delegates);
                }
            }
        }
        return keysWrapper;
    }

    private Set<SelectionKey> wrapSelectedKeys(final Set<SelectionKey> delegates) {
        if (null == selectedKeysWrapper) {
            synchronized (this) {
                if (null == selectedKeysWrapper && null != delegates) {
                    selectedKeysWrapper = createSelectionKeysWrapper(delegates);
                }
            }
        }
        return selectedKeysWrapper;
    }

    private SetWrapper<CompatSniffySelectionKey, SelectionKey> createSelectionKeysWrapper(Set<SelectionKey> delegates) {
        return new SetWrapper<CompatSniffySelectionKey, SelectionKey>(delegates, new WrapperFactory<SelectionKey, CompatSniffySelectionKey>() {
            @Override
            public CompatSniffySelectionKey wrap(SelectionKey delegate) {
                //noinspection SuspiciousMethodCalls
                return CompatSniffySelector.this.wrap(delegate, CompatSniffySelector.this, channelToSniffyChannelMap.get(delegate.channel()));
            }
        });
    }

    private class SelectionKeyConsumerWrapper implements Consumer<SelectionKey> {

        private final Consumer<SelectionKey> delegate;
        private final CompatSniffySocketChannel sniffyChannel;

        public SelectionKeyConsumerWrapper(Consumer<SelectionKey> delegate) {
            this(delegate, null);
        }

        public SelectionKeyConsumerWrapper(Consumer<SelectionKey> delegate, CompatSniffySocketChannel sniffyChannel) {
            this.delegate = delegate;
            this.sniffyChannel = sniffyChannel;
        }

        @Override
        public void accept(SelectionKey selectionKey) {
            delegate.accept(wrap(selectionKey, CompatSniffySelector.this, sniffyChannel));
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
            CompatSniffySocketChannel sniffySocketChannel = null;


            if (ch instanceof CompatSniffySocketChannel) {
                sniffySocketChannel = (CompatSniffySocketChannel) ch;
                chDelegate = sniffySocketChannel.getDelegate();
                channelToSniffyChannelMap.put(chDelegate, sniffySocketChannel);
            }

            SelectionKey selectionKeyDelegate = invokeMethod(AbstractSelector.class, delegate, "register",
                    AbstractSelectableChannel.class, chDelegate,
                    Integer.TYPE, ops,
                    Object.class, att,
                    SelectionKey.class
            );

            Object keyLock = ReflectionUtil.getField(AbstractSelectableChannel.class, chDelegate, "keyLock");
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (keyLock) {
                invokeMethod(AbstractSelectableChannel.class, chDelegate, "addKey", SelectionKey.class, selectionKeyDelegate, Void.class);
            }

            if (null != sniffySocketChannel) {
                return wrap(selectionKeyDelegate, this, sniffySocketChannel);
            } else {
                return selectionKeyDelegate;
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
