package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.JVMUtil;
import io.sniffy.util.ObjectWrapper;
import io.sniffy.util.StackTraceExtractor;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import static io.sniffy.util.ReflectionUtil.invokeMethod;

/**
 * Following properties and methods are local to Sniffy wrapper and do not affect delegate:
 * attachment
 * attach() and attachment()
 * Following final methods might not be handled correctly:
 * public final boolean AbstractSelectableChannel.isRegistered()
 * Following final methods are taken care of using some hacks:
 * public final SelectionKey AbstractSelectableChannel.register(Selector sel, int ops, Object att)
 *
 * @since 3.1.7
 */
@SuppressWarnings({"Convert2Diamond", "RedundantSuppression"})
public class SniffySelectionKey extends SelectionKey implements ObjectWrapper<SelectionKey> {
    private static final Polyglog LOG = PolyglogFactory.log(SniffySelectionKey.class);

    private final SelectionKey delegate;
    private final SniffySelector sniffySelector;
    private final SelectableChannel sniffyChannel;

    protected SniffySelectionKey(SelectionKey delegate, SniffySelector sniffySelector, SelectableChannel sniffyChannel) {
        this.delegate = delegate;

        if (null == delegate) {
            LOG.error("Trying to create SniffySelectionKey with null delegate");
            if (JVMUtil.isTestingSniffy()) {
                throw new NullPointerException();
            }
        } else {
            attach(delegate.attachment());
        }

        this.sniffySelector = sniffySelector;
        this.sniffyChannel = sniffyChannel;
    }
    @Override
    public SelectionKey getDelegate() {
        return delegate;
    }

    @Override
    public SelectableChannel channel() {
        return sniffyChannel;
    }

    /**
     * AbstractSelectableChannel holds a collection of keys which are added inside
     * 'SelectionKey register(Selector sel, int ops, Object att)' method
     * and removed using removeKey() method which is called only in delegate from following method
     * 'void deregister(AbstractSelectionKey key)'
     * Workaround is to copy the 'keys' array from delegate to sniffy wrapper after 'select*' operations are invoked
     * deregister method is invoked from Selector implementation classes during select operations as well as in onclose
     * 'void implCloseSelector() throws IOException'
     *
     * @return sniffy selector wrapper for given key or hardcoded NoOpSelector in case of invalid (or null) delegate key
     */
    @Override
    public Selector selector() {
        if (!isValid() &&
                StackTraceExtractor.hasClassAndMethodInStackTrace("java.nio.channels.spi.AbstractSelectableChannel", "findKey") &&
                sniffyChannel instanceof SocketChannel
        ) {
            return NoOpSelector.INSTANCE;
        } else {
            return sniffySelector;
        }
    }

    @Override
    public boolean isValid() {
        return delegate.isValid();
    }

    @Override
    public void cancel() {
        delegate.cancel();
        sniffySelector.updateKeysFromDelegate();
    }

    @Override
    public int interestOps() {
        return delegate.interestOps();
    }

    @Override
    public SelectionKey interestOps(int ops) {
        return delegate.interestOps(ops);
    }

    @Override
    public int readyOps() {
        return delegate.readyOps();
    }

    // No @Override annotation here because this method is available in Java 11+ only
    //@Override
    @SuppressWarnings({"Since15", "RedundantSuppression", "unused"})
    public int interestOpsOr(int ops) {
        try {
            return invokeMethod(delegate.getClass(), delegate, "interestOpsOr", Integer.TYPE, ops, Integer.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    // No @Override annotation here because this method is available in Java 11+ only
    //@Override
    @SuppressWarnings({"Since15", "RedundantSuppression", "unused"})
    public int interestOpsAnd(int ops) {
        try {
            return invokeMethod(delegate.getClass(), delegate, "interestOpsAnd", Integer.TYPE, ops, Integer.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

}
