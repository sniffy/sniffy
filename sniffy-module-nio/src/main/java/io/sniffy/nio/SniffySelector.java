package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.util.*;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
 *
 * @since 3.1.7
 */
@SuppressWarnings({"Convert2Diamond", "Convert2Lambda", "TryWithIdenticalCatches"})
public class SniffySelector extends AbstractSelector implements ObjectWrapper<AbstractSelector> {

    private static final Polyglog LOG = PolyglogFactory.log(SniffySelector.class);

    private final AbstractSelector delegate;

    private volatile Set<SelectionKey> keysWrapper = null;
    private volatile Set<SelectionKey> selectedKeysWrapper = null;

    public SniffySelector(SelectorProvider provider, AbstractSelector delegate) {
        super(provider);
        this.delegate = delegate;
        LOG.trace("Created new SniffySelector(" + provider + ", " + delegate + ") = " + this);
        // install some assertions when testing Sniffy
        if (JVMUtil.isTestingSniffy()) {
            ReflectionUtil.setField(AbstractSelector.class, this, "cancelledKeys", null); // trigger NPE in case it is used (it shouldn't be)
        }
    }

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
            }
        });
    }

    private static class SelectionKeyConsumerWrapper implements Consumer<SelectionKey> {

        private final Consumer<SelectionKey> delegate;

        public SelectionKeyConsumerWrapper(Consumer<SelectionKey> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void accept(SelectionKey selectionKey) {
            delegate.accept((SniffySelectionKey) selectionKey.attachment());
        }

    }

    private static Map<SelectionKey, Exception> selectionKeyMap = new ConcurrentHashMap<SelectionKey, Exception>();

    /**
     * This method adds a selection key to provided AbstractSelectableChannel, hence we're doing the same here manually
     */
    @Override
    // TODO: document
    protected SelectionKey register(AbstractSelectableChannel sniffyChannel, int ops, Object att) {
        try {

            AbstractSelectableChannel delegateChannel = null;

            if (sniffyChannel instanceof SelectableChannelWrapper) {
                delegateChannel = ((SelectableChannelWrapper<? extends AbstractSelectableChannel>) sniffyChannel).getDelegate();
            } else {
                if (JVMUtil.isTestingSniffy()) {
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

            selectionKeyMap.put(selectionKeyDelegate, new Exception());

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

        try {

            if (!isOpen()) return;

            for (SelectionKey key : delegate.keys()) { // throws ClosedSelectorException: null
                Object attachment = key.attachment();
                if (null == attachment) {
                    LOG.error("Couldn't determine SniffySelectionKey counterpart for key " + key);
                    Exception e = selectionKeyMap.get(key);
                    if (e != null) {
                        LOG.error(e);
                    }
                }
                SniffySelectionKey sniffySelectionKey = (SniffySelectionKey) attachment;
                AbstractSelectableChannel sniffyChannel = (AbstractSelectableChannel) sniffySelectionKey.channel();

                Object sniffyKeyLock = ReflectionUtil.getField(AbstractSelectableChannel.class, sniffyChannel, "keyLock");

                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (sniffyKeyLock) {

                    int sniffyCount = ReflectionUtil.getField(AbstractSelectableChannel.class, sniffyChannel, "keyCount");

                    SelectionKey[] sniffyKeys = ReflectionUtil.getField(AbstractSelectableChannel.class, sniffyChannel, "keys");

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
