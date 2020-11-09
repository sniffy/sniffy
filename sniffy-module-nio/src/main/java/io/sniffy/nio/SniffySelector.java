package io.sniffy.nio;

import io.sniffy.util.*;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static io.sniffy.util.ReflectionUtil.invokeMethod;
import static io.sniffy.util.ReflectionUtil.setField;

public class SniffySelector extends AbstractSelector {

    private final AbstractSelector delegate;

    private Map<AbstractSelectableChannel, AbstractSelectableChannel>
            channelToSniffyChannelMap = new ConcurrentHashMap<AbstractSelectableChannel, AbstractSelectableChannel>();  // TODO: fix memory leak

    public SniffySelector(SelectorProvider provider, AbstractSelector delegate) {
        super(provider);
        this.delegate = delegate;
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void implCloseSelector() throws IOException {
        try {
            setField(AbstractSelector.class, delegate, "closed", true);
            invokeMethod(AbstractSelector.class, delegate, "implCloseSelector", Void.class);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    private SniffySelectionKey wrapSelectionKey(SelectionKey delegate, SelectableChannel ch) {
        return SniffySelectionKey.wrap(delegate, this, ch);
    }

    private Set<SelectionKey> wrapSelectionKeys(final Set<SelectionKey> delegates) {
        if (null == delegates) {
            return null;
        } else if (delegates.isEmpty()) {
            return Collections.<SelectionKey>emptySet();
        } else {
            return new SetWrapper<SniffySelectionKey, SelectionKey>(delegates, new WrapperFactory<SelectionKey, SniffySelectionKey>() {

                @Override
                public SniffySelectionKey wrap(SelectionKey delegate) {
                    //noinspection SuspiciousMethodCalls
                    return wrapSelectionKey(delegate, channelToSniffyChannelMap.get(delegate.channel()));
                }

            });
        }
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
            delegate.accept(wrapSelectionKey(selectionKey, sniffyChannel));
        }

    }

    /**
     * This method adds a selection key to provided AbstractSelectableChannel, hence we're doing the same here manually
     */
    @Override
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

            return wrapSelectionKey(selectionKeyDelegate, ch);


        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @Override
    public Set<SelectionKey> keys() {
        return wrapSelectionKeys(delegate.keys());
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        return wrapSelectionKeys(delegate.selectedKeys());
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
            updateSelectionKeysFromDelegate();
        }
    }

    /**
     * select method can remove cancelled selection keys from delegate so we need to update them in sniffy channels as well
     */
    private void updateSelectionKeysFromDelegate() {
        for (Map.Entry<AbstractSelectableChannel, AbstractSelectableChannel> entry : channelToSniffyChannelMap.entrySet()) {
            AbstractSelectableChannel sniffyChannel = entry.getValue();

            if (sniffyChannel instanceof SelectableChannelWrapper) {
                //((SniffySocketChannelAdapter) sniffyChannel).updateSelectionKeysFromDelegate(this);
                AbstractSelectableChannel delegate = ((SelectableChannelWrapper<?>) sniffyChannel).getDelegate();
                //noinspection TryWithIdenticalCatches
                try {

                    Object keyLock = ReflectionUtil.getField(AbstractSelectableChannel.class, sniffyChannel, "keyLock");
                    Object delegateKeyLock = ReflectionUtil.getField(AbstractSelectableChannel.class, delegate, "keyLock");

                    //noinspection SynchronizationOnLocalVariableOrMethodParameter
                    synchronized (delegateKeyLock) {
                        //noinspection SynchronizationOnLocalVariableOrMethodParameter
                        synchronized (keyLock) {

                            SelectionKey[] delegateKeys = ReflectionUtil.getField(AbstractSelectableChannel.class, delegate, "keys");
                            List<SelectionKey> sniffyKeys = new ArrayList<SelectionKey>(delegateKeys.length);
                            for (SelectionKey delegateKey : delegateKeys) {
                                sniffyKeys.add(null == delegateKey ? null : SniffySelectionKey.wrap(delegateKey, this, sniffyChannel));
                            }
                            ReflectionUtil.setField(AbstractSelectableChannel.class, sniffyChannel, "keys", sniffyKeys.toArray(new SelectionKey[0]));

                            ReflectionUtil.setField(AbstractInterruptibleChannel.class, delegate, "keyCount"
                                    , ReflectionUtil.getField(AbstractInterruptibleChannel.class, sniffyChannel, "keyCount"));

                        }
                    }

                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
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
