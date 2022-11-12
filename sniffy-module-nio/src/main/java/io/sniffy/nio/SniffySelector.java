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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static io.sniffy.util.ReflectionUtil.*;

/**
 * parent class AbstractSelector contains following properties:
 * <pre>
 * {@code
 * cancelledKeys - not used; it's filled in delegate selector only
 * interruptor - this is used in delegate only
 * provider - immutable, set in constructor
 * selectorOpen - called "closed" on some JDKs; default is true, set to false in final close method; handled inside implCloseSelector method
 *
 * following methods cannot be delegated since they're final or due to modifiers:
 *
 * void cancel(SelectionKey k)
 * - adds key to cancelledKeys
 * - invoked by AbstractSelectionKey cancel in delegate; not used in SniffySelector
 *
 * protected final void deregister(AbstractSelectionKey key)
 * protected final Set<SelectionKey> cancelledKeys()
 * public final void close()
 * void cancel(SelectionKey k)
 * public final boolean isOpen()
 *
 * }
 * </pre>
 * @since 3.1.7
 */
public class SniffySelector extends AbstractSelector implements ObjectWrapper<AbstractSelector> {

    private static final Polyglog LOG = PolyglogFactory.log(SniffySelector.class);

    private final AbstractSelector delegate;

    private volatile Set<SelectionKey> keysWrapper = null;
    private volatile Set<SelectionKey> selectedKeysWrapper = null;

    private final Map<AbstractSelectableChannel, AbstractSelectableChannel> channelToSniffyChannelMap =
            new WeakHashMap<AbstractSelectableChannel, AbstractSelectableChannel>();

    private final Map<SelectionKey, SniffySelectionKey> sniffySelectionKeyCache =
            new WeakHashMap<SelectionKey, SniffySelectionKey>();

    public SniffySelector(SelectorProvider provider, AbstractSelector delegate) {
        super(provider);
        this.delegate = delegate;
        LOG.trace("Created new SniffySelector(" + provider + ", " + delegate + ") = " + this);
        // install some assertions
        if (JVMUtil.hasJUnitOnClassPath()) {
            ReflectionUtil.setField(AbstractSelector.class, this, "cancelledKeys", null); // trigger NPE in case it is used (it shouldn't be)
        }
    }

    public SniffySelectionKey wrap(SelectionKey delegate, SniffySelector sniffySelector, SelectableChannel sniffySocketChannel) {
        SniffySelectionKey sniffySelectionKey = sniffySelectionKeyCache.get(delegate);
        if (null == sniffySelectionKey) {
            synchronized (sniffySelectionKeyCache) {
                sniffySelectionKey = sniffySelectionKeyCache.get(delegate);
                if (null == sniffySelectionKey) {
                    sniffySelectionKey = new SniffySelectionKey(delegate, sniffySelector, sniffySocketChannel);
                    sniffySelectionKeyCache.put(delegate, sniffySelectionKey);
                }
            }
        }
        return sniffySelectionKey;
    }

    @Override
    public AbstractSelector getDelegate() {
        return delegate;
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void implCloseSelector() throws IOException {
        try {
            // TODO: document
            if (!setField(AbstractSelector.class, delegate, "closed", true)) {
                AtomicBoolean delegateSelectorOpen = getField(AbstractSelector.class, delegate, "selectorOpen");
                if (null != delegateSelectorOpen) {
                    delegateSelectorOpen.set(false);
                } else {
                    // TODO: log warning?
                }
            } // TODO: it's called 'selectorOpen' in Java 1.8
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

    private SetWrapper<SniffySelectionKey, SelectionKey> createSelectionKeysWrapper(Set<SelectionKey> delegates) {
        return new SetWrapper<SniffySelectionKey, SelectionKey>(delegates, new WrapperFactory<SelectionKey, SniffySelectionKey>() {
            @Override
            public SniffySelectionKey wrap(SelectionKey delegate) {
                //noinspection SuspiciousMethodCalls
                return SniffySelector.this.wrap(delegate, SniffySelector.this, channelToSniffyChannelMap.get(delegate.channel()));
            }
        });
    }

    private class SelectionKeyConsumerWrapper implements Consumer<SelectionKey> {

        private final Consumer<SelectionKey> delegate;
        private final SniffySocketChannel sniffyChannel;

        public SelectionKeyConsumerWrapper(Consumer<SelectionKey> delegate) {
            this(delegate, null);
        }

        public SelectionKeyConsumerWrapper(Consumer<SelectionKey> delegate, SniffySocketChannel sniffyChannel) {
            this.delegate = delegate;
            this.sniffyChannel = sniffyChannel;
        }

        @Override
        public void accept(SelectionKey selectionKey) {
            delegate.accept(wrap(selectionKey, SniffySelector.this, sniffyChannel));
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
                channelToSniffyChannelMap.put(chDelegate, ch);
            }

            SelectionKey selectionKeyDelegate = invokeMethod(AbstractSelector.class, delegate, "register",
                    AbstractSelectableChannel.class, chDelegate,
                    Integer.TYPE, ops,
                    Object.class, att,
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

            return wrap(selectionKeyDelegate, this, ch);

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
        try {
            return delegate.selectNow();
        } finally {
            // update keys on related channels
            updateKeysFromDelegate();
        }
    }

    private void updateKeysFromDelegate() {

        //if (true) return;

        try {
            for (Map.Entry<AbstractSelectableChannel, AbstractSelectableChannel> entry : channelToSniffyChannelMap.entrySet()) {
                AbstractSelectableChannel delegateChannel = entry.getKey();
                AbstractSelectableChannel sniffyChannel = entry.getValue();

                int delegateCount = ReflectionUtil.getField(AbstractSelectableChannel.class, delegateChannel, "keyCount");
                int sniffyCount = ReflectionUtil.getField(AbstractSelectableChannel.class, sniffyChannel, "keyCount");

                if (delegateCount != sniffyCount) {

                    Object delegateKeyLock  = ReflectionUtil.getField(AbstractSelectableChannel.class, delegateChannel, "keyLock");
                    Object sniffyKeyLock  = ReflectionUtil.getField(AbstractSelectableChannel.class, sniffyChannel, "keyLock");

                    //noinspection SynchronizationOnLocalVariableOrMethodParameter
                    synchronized (sniffyKeyLock) {
                        //noinspection SynchronizationOnLocalVariableOrMethodParameter
                        synchronized (delegateKeyLock) {

                            delegateCount = ReflectionUtil.getField(AbstractSelectableChannel.class, delegateChannel, "keyCount");
                            sniffyCount = ReflectionUtil.getField(AbstractSelectableChannel.class, sniffyChannel, "keyCount");

                            SelectionKey[] sniffyKeys = ReflectionUtil.getField(AbstractSelectableChannel.class, sniffyChannel, "keys");
                            SelectionKey[] delegateKeys = ReflectionUtil.getField(AbstractSelectableChannel.class, delegateChannel, "keys");

                            for (int i = 0; i < sniffyKeys.length; i++) {

                                SelectionKey sk = sniffyKeys[i];

                                if (null != sk && !sk.isValid()) {
                                    sniffyCount--;
                                    sniffyKeys[i] = null;
                                    //assert null == delegateKeys[i]; // doesn't always work due to defragmentation
                                }

                            }

                            assert sniffyCount == delegateCount; // TODO: do not asert always but only in sniffy own tests

                            ReflectionUtil.setField(AbstractSelectableChannel.class, sniffyChannel, "keyCount", sniffyCount);

                        }
                    }

                }

            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            // TODO: log exception
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            // TODO: log exception
        }

    }


    /**
     * This methods processes de-register queue (filled-in using selectionKey.cancel() method)
     * As a result it modifies the cancelledKeys field and also removes selectionKeys from associated channels
     */
    @Override
    public int select(long timeout) throws IOException {
        try {
            return delegate.select(timeout);
        } finally {
            // update keys on related channels
            updateKeysFromDelegate();
        }
    }

    /**
     * This methods processes de-register queue (filled-in using selectionKey.cancel() method)
     * As a result it modifies the cancelledKeys field and also removes selectionKeys from associated channels
     */
    @Override
    public int select() throws IOException {
        try {
            return delegate.select();
        } finally {
            // update keys on related channels
            updateKeysFromDelegate();
        }
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
        } finally {
            updateKeysFromDelegate();
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
        } finally {
            updateKeysFromDelegate();
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
        } finally {
            updateKeysFromDelegate();
        }
    }

}
