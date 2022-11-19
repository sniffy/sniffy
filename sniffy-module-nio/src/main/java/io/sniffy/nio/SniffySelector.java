package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.reflection.FieldRef;
import io.sniffy.reflection.UnsafeException;
import io.sniffy.util.*;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static io.sniffy.reflection.Unsafe.$;
import static io.sniffy.util.ReflectionUtil.invokeMethod;

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
            LOG.trace("Closing SniffySelector(" + provider() + ", " + delegate + ") = " + this);

            Class<? extends AbstractSelector> delegateClass = delegate.getClass();
            FieldRef<? super AbstractSelector, ? extends Set<? extends SelectionKey>> publicKeysFieldRef = $(delegateClass, "publicKeys", true);

            synchronized (delegate) {

                Object secondLock = delegate;

                if (publicKeysFieldRef.isResolved()) {
                    Set<? extends SelectionKey> delegatePublicKeys = publicKeysFieldRef.getValue(delegate);
                    if (null != delegatePublicKeys) {
                        secondLock = delegatePublicKeys;
                    }
                }

                //noinspection ReassignedVariable,SynchronizationOnLocalVariableOrMethodParameter
                synchronized (secondLock) {

                    boolean changed = isSelectorClosing();

                    if (changed) {

                        Set<SelectionKey> delegateSelectionKeys = getPublicKeysFromDelegate(publicKeysFieldRef);

                        invokeMethod(AbstractSelector.class, delegate, "implCloseSelector", Void.class);

                        removeSniffyInvalidSelectionKEysForGivenDelegates(delegateSelectionKeys);

                    }

                }
            }

        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    private boolean isSelectorClosing() throws UnsafeException {
        boolean changed = false;
        FieldRef<? super AbstractSelector, Object> closedFieldRef = $(AbstractSelector.class, "closed");
        if (closedFieldRef.isResolved()) {
            changed = closedFieldRef.compareAndSet(delegate, false, true);
        } else {
            FieldRef<? super AbstractSelector, AtomicBoolean> selectorOpenFieldRef = $(AbstractSelector.class, "selectorOpen");
            if (selectorOpenFieldRef.isResolved()) {
                AtomicBoolean selectorOpen = selectorOpenFieldRef.getValue(delegate);
                if (null != selectorOpen) {
                    changed = selectorOpen.getAndSet(false);
                } else {
                    LOG.error("AbstractSelector.selectorOpen is null");
                }
            } else {
                LOG.error("Neither AbstractSelector.closed nor AbstractSelector.selectorOpen fields found");
            }
        }
        return changed;
    }

    private Set<SelectionKey> getPublicKeysFromDelegate(FieldRef<? super AbstractSelector, ? extends Set<? extends SelectionKey>> publicKeysFieldRef) throws UnsafeException {
        Set<SelectionKey> delegateSelectionKeys = null;

        if (publicKeysFieldRef.isResolved()) {
            Set<? extends SelectionKey> delegatePublicKeys = publicKeysFieldRef.getValue(delegate);
            if (null != delegatePublicKeys) {
                LOG.trace("Public keys before closing = " + delegatePublicKeys.size());
                delegateSelectionKeys = new HashSet<SelectionKey>(delegatePublicKeys);
            }
        }
        return delegateSelectionKeys;
    }

    private static void removeSniffyInvalidSelectionKEysForGivenDelegates(Set<SelectionKey> delegateSelectionKeys) throws UnsafeException {
        if (null != delegateSelectionKeys) {
            for (SelectionKey delegateSelectionKey : delegateSelectionKeys) {
                SelectableChannel delegateChannel = delegateSelectionKey.channel();

                Object keyLock = null;

                FieldRef<Object, Object> keyLockFieldRef = $(AbstractSelectableChannel.class, "keyLock");
                if (keyLockFieldRef.isResolved()) {
                    keyLock = keyLockFieldRef.getValue(delegateChannel);
                }

                if (null == keyLock) {
                    keyLock = delegateChannel;
                }

                //noinspection ReassignedVariable,SynchronizationOnLocalVariableOrMethodParameter
                synchronized (keyLock) {
                    Object attachment = delegateSelectionKey.attachment();
                    if (attachment instanceof SniffySelectionKey) {
                        SniffySelectionKey sniffySelectionKey = (SniffySelectionKey) attachment;
                        SelectableChannel sniffyChannel = sniffySelectionKey.channel();
                        //invokeMethod(AbstractSelectableChannel.class, sniffyChannel, "removeKey", SelectionKey.class, sniffySelectionKey, Void.class);

                        FieldRef<SelectableChannel, Integer> keyCountFieldRef = $(AbstractSelectableChannel.class, "keyCount");
                        FieldRef<SelectableChannel, SelectionKey[]> keysFieldRef = $(AbstractSelectableChannel.class, "keys");

                        if (keyCountFieldRef.isResolved() && keysFieldRef.isResolved()) {
                            int keyCount = keyCountFieldRef.getValue(sniffyChannel);
                            SelectionKey[] sniffyKeys = keysFieldRef.getValue(sniffyChannel);

                            for (int i = 0; i < sniffyKeys.length; i++) {

                                SelectionKey sk = sniffyKeys[i];

                                if (null != sk && !sk.isValid()) {
                                    keyCount--;
                                    sniffyKeys[i] = null;
                                }

                            }

                            keyCountFieldRef.setValue(sniffyChannel, keyCount);
                        }

                    }
                }
            }
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

            Object regLock = ReflectionUtil.getField(AbstractSelectableChannel.class, delegateChannel, "regLock");
            Object keyLock = ReflectionUtil.getField(AbstractSelectableChannel.class, delegateChannel, "keyLock");

            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (regLock) {
                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (keyLock) {

                    SniffySelectionKey sniffySelectionKey = new SniffySelectionKey(this, sniffyChannel, att);

                    SelectionKey selectionKeyDelegate = invokeMethod(AbstractSelector.class, delegate, "register",
                            AbstractSelectableChannel.class, delegateChannel,
                            Integer.TYPE, ops,
                            Object.class, sniffySelectionKey,
                            SelectionKey.class
                    );

                    sniffySelectionKey.setDelegate(selectionKeyDelegate);

                    selectionKeyDelegate.attach(sniffySelectionKey);

                    invokeMethod(AbstractSelectableChannel.class, delegateChannel, "addKey", SelectionKey.class, selectionKeyDelegate, Void.class);

                    return sniffySelectionKey;

                }
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
        try {
            return delegate.selectNow();
        } finally {
            // update keys on related channels
            updateKeysFromDelegate();
        }
    }

    private final Queue<SelectionKey> cancelledKeys = new LinkedBlockingQueue<SelectionKey>();

    protected void addCancelledKey(SelectionKey selectionKey) {
        cancelledKeys.add(selectionKey);
    }

    protected void processCancelledQueue() {

    }

    protected void updateKeysFromDelegate() {

        try {

            // TODO: synchronize on delegate and delegate.publicSelectedKeys or rather switch to cancelledkeys queue

            if (!isOpen()) return;

            for (SelectionKey key : delegate.keys()) { // throws ClosedSelectorException: null
                Object attachment = key.attachment();
                if (null == attachment) {
                    LOG.error("Couldn't determine SniffySelectionKey counterpart for key " + key);
                    if (JVMUtil.isTestingSniffy()) {
                        throw new NullPointerException();
                    }
                }
                SniffySelectionKey sniffySelectionKey = (SniffySelectionKey) attachment;
                AbstractSelectableChannel sniffyChannel = (AbstractSelectableChannel) sniffySelectionKey.channel();

                Object sniffyKeyLock = ReflectionUtil.getField(AbstractSelectableChannel.class, sniffyChannel, "keyLock");
                Object delegateKeyLock = ReflectionUtil.getField(
                        AbstractSelectableChannel.class,
                        ((SelectableChannelWrapper<? extends AbstractSelectableChannel>) sniffyChannel).getDelegate(),
                        "keyLock"
                );

                //noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (sniffyKeyLock) {
                    //noinspection SynchronizationOnLocalVariableOrMethodParameter
                    synchronized (delegateKeyLock) {
                        // without this lock we can get a SniffySelectionKey without actual selection key delegate
                        // TODO: if we stay with this method - we need to refactor it

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
