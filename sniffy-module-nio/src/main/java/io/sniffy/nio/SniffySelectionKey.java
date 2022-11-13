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
import java.nio.channels.spi.AbstractSelectableChannel;

import static io.sniffy.util.ReflectionUtil.invokeMethod;

/**
 * properties:
 * attachment
 * methods:
 * attach() and attachment() are final
 *
 * @since 3.1.7
 */
public class SniffySelectionKey extends SelectionKey implements ObjectWrapper<SelectionKey> {

    private static final Polyglog LOG = PolyglogFactory.log(SniffySelectionKey.class);

    private final WeakReference<SelectionKey> delegateReference;
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

    @Override
    public SelectionKey getDelegate() {
        return delegateReference.get();
    }

    @Override
    public SelectableChannel channel() {
        return sniffyChannel;
    }

    @Override
    public Selector selector() {
        if (!isValid() &&
                StackTraceExtractor.hasClassAndMethodInStackTrace("java.nio.channels.spi.AbstractSelectableChannel", "findKey") &&
                sniffyChannel instanceof SocketChannel
        ) {
            return NoOpSelector.INSTANCE; // TODO: cleanup this and other collections
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
            if (sniffyChannel instanceof SelectableChannelWrapper) {
                //noinspection unchecked
                ((SelectableChannelWrapper<AbstractSelectableChannel>) sniffyChannel).keyCancelled();
            }
            // TODO: seems that code below is safe to be removed on Java 17; is it the same on older Java?
            /*synchronized (this) {
                delegate.cancel();
                try {
                    // TODO: reevaluate copying other fields across NIO stack
                    ReflectionUtil.invokeMethod(AbstractSelector.class, sniffySelector, "cancel", SelectionKey.class, this);
                } catch (Exception e) {
                    throw ExceptionUtil.processException(e);
                }
            }*/
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
    @SuppressWarnings("Since15")
    public int interestOpsOr(int ops) {
        // TODO: handle NPE
        try {
            return invokeMethod(SelectionKey.class, delegateReference.get(), "interestOpsOr", Integer.TYPE, ops, Integer.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    // No @Override annotation here because this method is available in Java 11+ only
    //@Override
    @SuppressWarnings("Since15")
    public int interestOpsAnd(int ops) {
        // TODO: handle NPE
        try {
            return invokeMethod(SelectionKey.class, delegateReference.get(), "interestOpsAnd", Integer.TYPE, ops, Integer.TYPE);
        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

}
