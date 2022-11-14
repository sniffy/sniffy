package io.sniffy.nio.compat;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.nio.NoOpSelector;
import io.sniffy.util.JVMUtil;
import io.sniffy.util.ObjectWrapper;
import io.sniffy.util.StackTraceExtractor;

import java.lang.ref.WeakReference;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * @since 3.1.14
 */
public class CompatSniffySelectionKey extends SelectionKey implements ObjectWrapper<SelectionKey> {

    private static final Polyglog LOG = PolyglogFactory.log(CompatSniffySelectionKey.class);

    private final WeakReference<SelectionKey> delegateReference;
    private final CompatSniffySelector sniffySelector;
    private final SelectableChannel sniffyChannel;

    protected CompatSniffySelectionKey(SelectionKey delegate, CompatSniffySelector sniffySelector, SelectableChannel sniffyChannel) {
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

}
