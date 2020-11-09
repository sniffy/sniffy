package io.sniffy.nio;

import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.ReflectionCopier;
import io.sniffy.util.ReflectionUtil;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.function.Consumer;

public class SniffySelector extends AbstractSelector {

    private final AbstractSelector delegate;

    private static final ReflectionCopier<AbstractSelector> abstractSelectorFieldsCopier = new ReflectionCopier<AbstractSelector>(AbstractSelector.class, "provider");

    private void copyToDelegate() {
        abstractSelectorFieldsCopier.copy(this, delegate);
    }

    private void copyFromDelegate() {
        abstractSelectorFieldsCopier.copy(delegate, this);
    }


    public SniffySelector(SelectorProvider provider, AbstractSelector delegate) {
        super(provider);
        this.delegate = delegate;
    }

    @Override
    protected void implCloseSelector() throws IOException {
        try {
            copyToDelegate();
            ReflectionUtil.invokeMethod(AbstractSelector.class, delegate, "implCloseSelector", Void.class);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    private SelectionKey wrapSelectionKey(SelectionKey delegate, AbstractSelectableChannel ch) {
        return new SniffySelectionKey(delegate, this, ch);
    }

    private Set<SelectionKey> wrapSelectionKeys(Set<SelectionKey> delegates) {
        return wrapSelectionKeys(delegates, null);
    }

    private Set<SelectionKey> wrapSelectionKeys(final Set<SelectionKey> delegates, final AbstractSelectableChannel ch) {
        if (null == delegates) {
            return null;
        } else if (delegates.isEmpty()) {
            return Collections.<SelectionKey>emptySet();
        } else {
            Set<SelectionKey> sniffySelectionKeys = new HashSet<SelectionKey>(delegates.size()) {

                // TODO: should we override removeAll and others (?)
                // TODO: should we override iterator and its remve and others (?)

                // TODO: wrap in runtime instead of copying selection keys

                @Override
                public Iterator<SelectionKey> iterator() {
                    final Iterator<SelectionKey> delegate = delegates.iterator();
                    final Iterator<SelectionKey> parent = super.iterator();
                    return new Iterator<SelectionKey>() {
                        @Override
                        public boolean hasNext() {
                            delegate.hasNext();
                            return parent.hasNext();
                        }



                        @Override
                        public SelectionKey next() {
                            delegate.next();
                            return parent.next();
                        }

                        @Override
                        public void remove() {
                            delegate.remove();
                            parent.remove();
                        }
                    };
                }

                @Override
                public void clear() {
                    delegates.clear();
                    super.clear();
                }

                @Override
                public boolean removeAll(Collection<?> c) {
                    for (Object o : c) {
                        if (o instanceof SniffySelectionKey) {
                            delegates.remove(((SniffySelectionKey) o).getDelegate());
                        }
                    }
                    return super.removeAll(c);
                }

                @Override
                public boolean remove(Object o) {
                    boolean remove = super.remove(o);
                    if (remove) {
                        if (o instanceof SniffySelectionKey) {
                            delegates.remove(((SniffySelectionKey) o).getDelegate());
                        }
                    }
                    return remove;
                }

            };
            for (SelectionKey delegate : delegates) {
                sniffySelectionKeys.add(wrapSelectionKey(delegate, ch));
            }
            return sniffySelectionKeys;
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

    @Override
    protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
        try {
            copyToDelegate();

            AbstractSelectableChannel chDelegate = ch;

            if (ch instanceof SniffySocketChannelAdapter) {
                ((SniffySocketChannelAdapter) ch).copyToDelegate();
                chDelegate = ((SniffySocketChannelAdapter) ch).delegate;
            } else if (ch instanceof SniffyServerSocketChannel) {
                ch = ((SniffyServerSocketChannel) ch).delegate;
            }

            SelectionKey selectionKeyDelegate = ReflectionUtil.invokeMethod(AbstractSelector.class, delegate, "register",
                    AbstractSelectableChannel.class, chDelegate,
                    Integer.TYPE, ops,
                    Object.class, att,
                    SelectionKey.class
            );

            Object keyLock = ReflectionUtil.getField(AbstractSelectableChannel.class, ch, "keyLock");
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (keyLock) {
                ReflectionUtil.invokeMethod(AbstractSelectableChannel.class, ch, "addKey", SelectionKey.class, selectionKeyDelegate, Void.class);
            }

            return wrapSelectionKey(selectionKeyDelegate, ch);


        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
            if (ch instanceof SniffySocketChannelAdapter) {
                ((SniffySocketChannelAdapter) ch).copyFromDelegate(); // TODO: wrap selection keys
            }
        }
    }

    @Override
    public Set<SelectionKey> keys() {
        try {
            copyToDelegate();
            return wrapSelectionKeys(delegate.keys());
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        try {
            copyToDelegate();
            // TODO: wrapper Set should support remove operation
            return wrapSelectionKeys(delegate.selectedKeys());
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public int selectNow() throws IOException {
        try {
            copyToDelegate();
            return delegate.selectNow();
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public int select(long timeout) throws IOException {
        try {
            copyToDelegate();
            return delegate.select(timeout);
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public int select() throws IOException {
        try {
            copyToDelegate();
            return delegate.select();
        } finally {
            copyFromDelegate();
        }
    }

    @Override
    public Selector wakeup() {
        try {
            copyToDelegate();
            delegate.wakeup();
            return this;
        } finally {
            copyFromDelegate();
        }
    }



    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    public int select(Consumer<SelectionKey> action, long timeout) throws IOException {
        try {
            copyToDelegate();
            return ReflectionUtil.invokeMethod(Selector.class, delegate, "select",
                    Consumer.class, new SelectionKeyConsumerWrapper(action),
                    Long.TYPE, timeout,
                    Integer.TYPE
            );
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    public int select(Consumer<SelectionKey> action) throws IOException {
        try {
            copyToDelegate();
            return ReflectionUtil.invokeMethod(Selector.class, delegate, "select",
                    Consumer.class, new SelectionKeyConsumerWrapper(action),
                    Integer.TYPE
            );
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    public int selectNow(Consumer<SelectionKey> action) throws IOException {
        try {
            copyToDelegate();
            return ReflectionUtil.invokeMethod(Selector.class, delegate, "selectNow",
                    Consumer.class, new SelectionKeyConsumerWrapper(action),
                    Integer.TYPE
            );
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        } finally {
            copyFromDelegate();
        }
    }
}
