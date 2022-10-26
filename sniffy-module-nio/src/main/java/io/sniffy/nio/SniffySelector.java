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

    private final Map<AbstractSelectableChannel, AbstractSelectableChannel> channelToSniffyChannelMap =
            Collections.synchronizedMap(new WeakHashMap<AbstractSelectableChannel, AbstractSelectableChannel>());

    private final Map<SelectionKey, SniffySelectionKey> sniffySelectionKeyCache =
            new WeakHashMap<SelectionKey, SniffySelectionKey>();

    public SniffySelector(SelectorProvider provider, AbstractSelector delegate) {
        super(provider);
        this.delegate = delegate;
        LOG.trace("Created new SniffySelector(" + provider + ", " + delegate + ") = " + this);
    }

    public SniffySelectionKey wrap(SelectionKey delegate, SniffySelector sniffySelector, SelectableChannel sniffyChannel) {
        SniffySelectionKey sniffySelectionKey = sniffySelectionKeyCache.get(delegate);
        if (null == sniffySelectionKey) {
            synchronized (sniffySelectionKeyCache) {
                sniffySelectionKey = sniffySelectionKeyCache.get(delegate);
                if (null == sniffySelectionKey) {
                    sniffySelectionKey = new SniffySelectionKey(delegate, sniffySelector, sniffyChannel);
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
            // TODO: copy keys for related channels
            updateSelectionKeysFromDelegate();
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
        private final AbstractSelectableChannel sniffyChannel;

        public SelectionKeyConsumerWrapper(Consumer<SelectionKey> delegate) {
            this(delegate, null);
        }

        public SelectionKeyConsumerWrapper(Consumer<SelectionKey> delegate, AbstractSelectableChannel sniffyChannel) {
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

            Object keyLock = ReflectionUtil.getField(AbstractSelectableChannel.class, chDelegate, "keyLock");
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (keyLock) {
                invokeMethod(AbstractSelectableChannel.class, chDelegate, "addKey", SelectionKey.class, selectionKeyDelegate, Void.class);
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
            updateCancelledKeysToDelegate();
            // TODO: call delegate.processDeregisterQueue and update selection keys from delegate first
            return delegate.selectNow();
        } finally {
            updateSelectionKeysFromDelegate();
        }
    }

    /**
     * select method can remove cancelled selection keys from delegate so we need to update them in sniffy channels as well
     *
     * copies "keys" from delegate channels bound to this selector to sniffy wrappers
     */
    private void updateSelectionKeysFromDelegate() {


        // TODO: evaluate condition below
        if (JVMUtil.getVersion() < 14 && !Boolean.getBoolean("io.sniffy.forceJava14Compatibility")) {
            return; // Before Java 14 is updating attachment in delegate from SniffySelectionKey
        }

        Map<AbstractSelectableChannel, AbstractSelectableChannel> channelToSniffyChannelMap;
        synchronized (this.channelToSniffyChannelMap) {
            channelToSniffyChannelMap = new HashMap<AbstractSelectableChannel, AbstractSelectableChannel>(this.channelToSniffyChannelMap);
        }

        for (Map.Entry<AbstractSelectableChannel, AbstractSelectableChannel> entry : channelToSniffyChannelMap.entrySet()) {
            AbstractSelectableChannel sniffyChannel = entry.getValue();

            if (sniffyChannel instanceof SelectableChannelWrapper) {
                AbstractSelectableChannel delegate = ((SelectableChannelWrapper<?>) sniffyChannel).getDelegate();
                try {

                    Object keyLock = ReflectionUtil.getField(AbstractSelectableChannel.class, sniffyChannel, "keyLock");
                    Object delegateKeyLock = ReflectionUtil.getField(AbstractSelectableChannel.class, delegate, "keyLock");

                    // We're always obtaining monitor of wrapper (SniffySelector) first in order to avoid dead locks

                    //noinspection SynchronizationOnLocalVariableOrMethodParameter
                    synchronized (keyLock) {
                        //noinspection SynchronizationOnLocalVariableOrMethodParameter
                        synchronized (delegateKeyLock) {

                            SelectionKey[] delegateKeys = ReflectionUtil.getField(AbstractSelectableChannel.class, delegate, "keys");
                            List<SelectionKey> sniffyKeys = new ArrayList<SelectionKey>(delegateKeys.length);
                            for (SelectionKey delegateKey : delegateKeys) {
                                sniffyKeys.add(null == delegateKey ? null : wrap(delegateKey, this, sniffyChannel));
                            }
                            ReflectionUtil.setField(AbstractSelectableChannel.class, sniffyChannel, "keys", sniffyKeys.toArray(new SelectionKey[0]));

                            ReflectionUtil.setField(AbstractSelectableChannel.class, delegate, "keyCount"
                                    ,ReflectionUtil.getField(AbstractSelectableChannel.class, sniffyChannel, "keyCount"));

                            // TODO: shall we copy other fields as well?

                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     *copy canncelled keys from sniffy to delegate
     */
    private void updateCancelledKeysToDelegate() {

/*


        // TODO: evaluate condition below
        if (JVMUtil.getVersion() < 14 && !Boolean.getBoolean("io.sniffy.forceJava14Compatibility")) {
            return; // Before Java 14 is updating attachment in delegate from SniffySelectionKey
        }

        Map<AbstractSelectableChannel, AbstractSelectableChannel> channelToSniffyChannelMap;
        synchronized (this.channelToSniffyChannelMap) {
            channelToSniffyChannelMap = new HashMap<AbstractSelectableChannel, AbstractSelectableChannel>(this.channelToSniffyChannelMap);
        }

        for (Map.Entry<AbstractSelectableChannel, AbstractSelectableChannel> entry : channelToSniffyChannelMap.entrySet()) {
            AbstractSelectableChannel sniffyChannel = entry.getValue();

            if (sniffyChannel instanceof SelectableChannelWrapper) {
                AbstractSelectableChannel delegate = ((SelectableChannelWrapper<?>) sniffyChannel).getDelegate();
                try {

                    Object keyLock = ReflectionUtil.getField(AbstractSelectableChannel.class, sniffyChannel, "keyLock");
                    Object delegateKeyLock = ReflectionUtil.getField(AbstractSelectableChannel.class, delegate, "keyLock");

                    // We're always obtaining monitor of wrapper (SniffySelector) first in order to avoid dead locks

                    //noinspection SynchronizationOnLocalVariableOrMethodParameter
                    synchronized (keyLock) {
                        //noinspection SynchronizationOnLocalVariableOrMethodParameter
                        synchronized (delegateKeyLock) {

                            SelectionKey[] delegateKeys = ReflectionUtil.getField(AbstractSelectableChannel.class, delegate, "keys");
                            List<SelectionKey> sniffyKeys = new ArrayList<SelectionKey>(delegateKeys.length);
                            for (SelectionKey delegateKey : delegateKeys) {
                                sniffyKeys.add(null == delegateKey ? null : wrap(delegateKey, this, sniffyChannel));
                            }
                            ReflectionUtil.setField(AbstractSelectableChannel.class, sniffyChannel, "keys", sniffyKeys.toArray(new SelectionKey[0]));

                            ReflectionUtil.setField(AbstractSelectableChannel.class, delegate, "keyCount"
                                    ,ReflectionUtil.getField(AbstractSelectableChannel.class, sniffyChannel, "keyCount"));

                            // TODO: shall we copy other fields as well?

                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
*/
    }

    /**
     * This methods processes de-register queue (filled-in using selectionKey.cancel() method)
     * As a result it modifies the cancelledKeys field and also removes selectionKeys from associated channels
     */
    @Override
    public int select(long timeout) throws IOException {
        try {
            // TODO: call delegate.processDeregisterQueue and update selection keys from delegate
            return delegate.select(timeout);
        } finally {
            updateSelectionKeysFromDelegate();
        }
    }

    /**
     * This methods processes de-register queue (filled-in using selectionKey.cancel() method)
     * As a result it modifies the cancelledKeys field and also removes selectionKeys from associated channels
     */
    @Override
    public int select() throws IOException {
        try {
            // TODO: call delegate.processDeregisterQueue and update selection keys from delegate
            return delegate.select();
        } finally {
            updateSelectionKeysFromDelegate();
        }
    }

    @Override
    public Selector wakeup() {
        delegate.wakeup();
        return this;
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    @SuppressWarnings("RedundantThrows")
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
            updateSelectionKeysFromDelegate();
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    @SuppressWarnings("RedundantThrows")
    public int select(Consumer<SelectionKey> action) throws IOException {
        try {
            // TODO: call delegate.processDeregisterQueue and update selection keys from delegate
            return invokeMethod(Selector.class, delegate, "select",
                    Consumer.class, new SelectionKeyConsumerWrapper(action),
                    Integer.TYPE
            );
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            updateSelectionKeysFromDelegate();
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    @SuppressWarnings("RedundantThrows")
    public int selectNow(Consumer<SelectionKey> action) throws IOException {
        try {
            // TODO: call delegate.processDeregisterQueue and update selection keys from delegate
            return invokeMethod(Selector.class, delegate, "selectNow",
                    Consumer.class, new SelectionKeyConsumerWrapper(action),
                    Integer.TYPE
            );
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            updateSelectionKeysFromDelegate();
        }
    }

}
