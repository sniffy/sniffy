package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.JVMUtil;
import io.sniffy.util.ObjectWrapper;
import io.sniffy.util.StackTraceExtractor;

import java.lang.ref.WeakReference;
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
@SuppressWarnings("Convert2Diamond")
public class SniffySelectionKey extends SelectionKey implements ObjectWrapper<SelectionKey> {

    private static final Polyglog LOG = PolyglogFactory.log(SniffySelectionKey.class);

    private final WeakReference<? extends SelectionKey> delegateReference;
    private final SniffySelector sniffySelector;
    private final SelectableChannel sniffyChannel;

    protected SniffySelectionKey(SelectionKey delegate, SniffySelector sniffySelector, SelectableChannel sniffyChannel) {
        this.delegateReference = new WeakReference<SelectionKey>(delegate);

        if (null != delegate) {
            attach(delegate.attachment());
        }

        this.sniffySelector = sniffySelector;
        this.sniffyChannel = sniffyChannel;
    }

    protected SniffySelectionKey(WeakReference<? extends SelectionKey> delegateReference, SniffySelector sniffySelector, SelectableChannel sniffyChannel) {
        this.delegateReference = delegateReference;

        SelectionKey delegate = delegateReference.get();
        if (null != delegate) {
            attach(delegate.attachment());
        }

        this.sniffySelector = sniffySelector;
        this.sniffyChannel = sniffyChannel;
    }

    @Override
    public SelectionKey getDelegate() {
        return delegateReference.get();
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
        SelectionKey delegate = delegateReference.get();
        return null != delegate && delegate.isValid();
    }

    @Override
    public void cancel() {
        SelectionKey delegateKey = delegateReference.get();
        if (null == delegateKey) {
            LOG.error("Trying to invoke SniffySelectionKey.cancel() on null delegate");
            if (JVMUtil.isTestingSniffy()) {
                throw new NullPointerException();
            }
        } else {
            delegateKey.cancel();
        }
    }

    @Override
    public int interestOps() {
        SelectionKey delegateKey = delegateReference.get();
        if (null == delegateKey) {
            LOG.error("Trying to invoke SniffySelectionKey.interestOps() on null delegate");
            if (JVMUtil.isTestingSniffy()) {
                throw new NullPointerException();
            }
            return 0;
        } else {
            return delegateKey.interestOps();
        }
    }

    @Override
    public SelectionKey interestOps(int ops) {
        SelectionKey delegateKey = delegateReference.get();
        if (null == delegateKey) {
            LOG.error("Trying to invoke SniffySelectionKey.interestOps(int ops) on null delegate");
            if (JVMUtil.isTestingSniffy()) {
                throw new NullPointerException();
            }
        } else {
            delegateKey.interestOps(ops);
        }
        return this;
    }

    @Override
    public int readyOps() {
        SelectionKey delegateKey = delegateReference.get();
        if (null == delegateKey) {
            LOG.error("Trying to invoke SniffySelectionKey.readyOps() on null delegate");
            if (JVMUtil.isTestingSniffy()) {
                throw new NullPointerException();
            }
            return 0;
        } else {
            return delegateKey.readyOps();
        }
    }

    // No @Override annotation here because this method is available in Java 11+ only
    //@Override
    @SuppressWarnings({"Since15", "RedundantSuppression", "unused"})
    public int interestOpsOr(int ops) {
        SelectionKey delegateKey = delegateReference.get();
        if (null == delegateKey) {
            LOG.error("Trying to invoke SniffySelectionKey.interestOpsOr(int ops) on null delegate");
            if (JVMUtil.isTestingSniffy()) {
                throw new NullPointerException();
            }
            return 0;
        } else {
            try {
                return invokeMethod(delegateKey.getClass(), delegateReference.get(), "interestOpsOr", Integer.TYPE, ops, Integer.TYPE);
            } catch (Exception e) {
                throw ExceptionUtil.processException(e);
            }
        }
    }

    // No @Override annotation here because this method is available in Java 11+ only
    //@Override
    @SuppressWarnings({"Since15", "RedundantSuppression", "unused"})
    public int interestOpsAnd(int ops) {
        SelectionKey delegateKey = delegateReference.get();
        if (null == delegateKey) {
            LOG.error("Trying to invoke SniffySelectionKey.interestOpsAnd(int ops) on null delegate");
            if (JVMUtil.isTestingSniffy()) {
                throw new NullPointerException();
            }
            return 0;
        } else {
            try {
                return invokeMethod(delegateKey.getClass(), delegateReference.get(), "interestOpsAnd", Integer.TYPE, ops, Integer.TYPE);
            } catch (Exception e) {
                throw ExceptionUtil.processException(e);
            }
        }
    }

}
