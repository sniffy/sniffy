package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.util.*;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;
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
@SuppressWarnings({"Convert2Diamond", "Convert2Lambda", "TryWithIdenticalCatches"})
public class SniffySelector extends AbstractSelector implements ObjectWrapper<AbstractSelector> {

    private static final Polyglog LOG = PolyglogFactory.log(SniffySelector.class);

    private final AbstractSelector delegate;

    private volatile Set<SelectionKey> keysWrapper = null;
    private volatile Set<SelectionKey> selectedKeysWrapper = null;

    /*// TODO: check that values do not have strong references to keys
    // TODO: clear sniffySelectionKeyCache when channel, key or selector are closed or cancelled

    protected final WrapperWeakHashMap<SelectableChannel, SelectableChannelWrapper<? extends AbstractSelectableChannel>> sniffyChannelCache =
            new WrapperWeakHashMap<SelectableChannel, SelectableChannelWrapper<? extends AbstractSelectableChannel>>();

    protected final WrapperWeakHashMap<SelectionKey, SniffySelectionKey> sniffySelectionKeyCache =
            new WrapperWeakHashMap<SelectionKey, SniffySelectionKey>();*/

    public SniffySelector(SelectorProvider provider, AbstractSelector delegate) {
        super(provider);
        this.delegate = delegate;
        LOG.trace("Created new SniffySelector(" + provider + ", " + delegate + ") = " + this);
        // install some assertions when testing Sniffy
        if (JVMUtil.isTestingSniffy()) {
            ReflectionUtil.setField(AbstractSelector.class, this, "cancelledKeys", null); // trigger NPE in case it is used (it shouldn't be)
        }
    }

    /*private SniffySelectionKey wrap(SelectionKey delegate, final SniffySelector sniffySelector, final SelectableChannel sniffySocketChannel) {
        return sniffySelectionKeyCache.getOrWrap(
                delegate, new WrapperFactory<SelectionKey, SniffySelectionKey>() {
                    @Override
                    public SniffySelectionKey wrap(SelectionKey delegate) {
                        return new SniffySelectionKey(delegate, sniffySelector, sniffySocketChannel);
                    }
                }
        );
    }*/

    @Override
    public AbstractSelector getDelegate() {
        return delegate;
    }

    /**
     * close() method is final - hence we need to do similar work in implCloseSelector() method
     * Specifically set the closed (or selectorOpen depending on JDK version) flag
     */
    @SuppressWarnings("RedundantThrows")
    @Override
    protected void implCloseSelector() throws IOException {
        try {
            if (!setField(AbstractSelector.class, delegate, "closed", true)) {
                AtomicBoolean delegateSelectorOpen = getField(AbstractSelector.class, delegate, "selectorOpen");
                if (null != delegateSelectorOpen) {
                    delegateSelectorOpen.set(false);
                } else {
                    LOG.trace("Neither AbstractSelector.closed nor AbstractSelector.selectorOpen fields found");
                }
            }
            invokeMethod(AbstractSelector.class, delegate, "implCloseSelector", Void.class);
            updateKeysFromDelegate();
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
                return (SniffySelectionKey) delegate.attachment();
                /*SelectableChannelWrapper<? extends AbstractSelectableChannel> sniffySocketChannel = sniffyChannelCache.get(delegate.channel());
                return SniffySelector.this.wrap(delegate, SniffySelector.this, null == sniffySocketChannel ? null : sniffySocketChannel.asSelectableChannel());*/
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
//            delegate.accept(wrap(selectionKey, SniffySelector.this, sniffyChannel));
            delegate.accept((SniffySelectionKey) selectionKey.attachment());
        }

    }

    /**
     * This method adds a selection key to provided AbstractSelectableChannel, hence we're doing the same here manually
     */
    @Override
    // TODO: document
    protected SelectionKey register(AbstractSelectableChannel sniffyChannel, int ops, Object att) {
        try {

            AbstractSelectableChannel delegateChannel = null;

            if (sniffyChannel instanceof SelectableChannelWrapper) {
                //sniffyChannelCache.put((SelectableChannelWrapper<? extends AbstractSelectableChannel>) sniffyChannel);
                delegateChannel = ((SelectableChannelWrapper<? extends AbstractSelectableChannel>) sniffyChannel).getDelegate();
            } else {
                if (JVMUtil.isTestingSniffy()) {
                    /*
                    //noinspection ConstantConditions
                    sniffyChannelCache.put((SelectableChannelWrapper<? extends AbstractSelectableChannel>) sniffyChannel);
                    */

                    //noinspection ConstantConditions
                    delegateChannel = ((SelectableChannelWrapper<? extends AbstractSelectableChannel>) sniffyChannel).getDelegate();

                } else {
                    LOG.error("Suspicious channel " + sniffyChannel + " is passed to SniffySelector.register() method");
                }
            }

            SelectionKey selectionKeyDelegate = invokeMethod(AbstractSelector.class, delegate, "register",
                    AbstractSelectableChannel.class, delegateChannel,
                    Integer.TYPE, ops,
                    Object.class, att,
                    SelectionKey.class
            );

            SniffySelectionKey sniffySelectionKey = new SniffySelectionKey(selectionKeyDelegate, this, sniffyChannel);

            selectionKeyDelegate.attach(sniffySelectionKey);

            Object regLock = ReflectionUtil.getField(AbstractSelectableChannel.class, sniffyChannel, "regLock");
            Object keyLock = ReflectionUtil.getField(AbstractSelectableChannel.class, sniffyChannel, "keyLock");
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (regLock) {
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (keyLock) {
                    invokeMethod(AbstractSelectableChannel.class, delegateChannel, "addKey", SelectionKey.class, selectionKeyDelegate, Void.class);
                }
            }

            //return wrap(selectionKeyDelegate, this, sniffyChannel);

            return sniffySelectionKey;

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

    protected void updateKeysFromDelegate() {

        //delegate.keys(); // TODO: process these keys; sniffy selection key is available as attachment

        // this is not impacting functionality - just the cleanup

        try {


            for (SelectionKey key : delegate.keys()) { // throws ClosedSelectorException: null
                AbstractSelectableChannel delegateChannel = (AbstractSelectableChannel) key.channel();
                AbstractSelectableChannel sniffyChannel = (AbstractSelectableChannel) ((SelectionKey) (key.attachment())).channel();

                int delegateCount = ReflectionUtil.getField(AbstractSelectableChannel.class, delegateChannel, "keyCount");
                int sniffyCount = ReflectionUtil.getField(AbstractSelectableChannel.class, sniffyChannel, "keyCount");

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

                        ReflectionUtil.setField(AbstractSelectableChannel.class, sniffyChannel, "keyCount", sniffyCount);

                    }
                }



            /*}
            for (SelectableChannelWrapper sniffyChannelWrapper : sniffyChannelCache.values()) {
                AbstractSelectableChannel delegateChannel = sniffyChannelWrapper.getDelegate();
                AbstractSelectableChannel sniffyChannel = sniffyChannelWrapper.asSelectableChannel();*/

                /*int delegateCount = ReflectionUtil.getField(AbstractSelectableChannel.class, delegateChannel, "keyCount");
                int sniffyCount = ReflectionUtil.getField(AbstractSelectableChannel.class, sniffyChannel, "keyCount");

                if (true || delegateCount != sniffyCount) {

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

                            if (sniffyCount != delegateCount) {
                                if (JVMUtil.isTestingSniffy()) {
                                    throw new IllegalStateException("Count of keys in Sniffy and delegate channel are different");
                                } else {
                                    LOG.error("Count of keys in Sniffy and delegate channel are different");
                                }
                            }

                            ReflectionUtil.setField(AbstractSelectableChannel.class, sniffyChannel, "keyCount", sniffyCount);

                        }
                    }

                }*/

            }
        } catch (NoSuchFieldException e) {
            LOG.error(e);
        } catch (IllegalAccessException e) {
            LOG.error(e);
        } catch (ClosedSelectorException e) {
            LOG.error(e);
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
    @SuppressWarnings({"RedundantThrows", "Since15", "RedundantSuppression"})
    public int select(Consumer<SelectionKey> action, long timeout) throws IOException {
        try {
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
    @SuppressWarnings({"RedundantThrows", "Since15", "RedundantSuppression"})
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
    @SuppressWarnings({"RedundantThrows", "Since15", "RedundantSuppression", "unused"})
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
