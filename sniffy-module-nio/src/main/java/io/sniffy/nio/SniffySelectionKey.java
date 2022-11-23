package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.reflection.clazz.ClassRef;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.ObjectWrapper;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import static io.sniffy.reflection.Unsafe.$;

/**
 * delegate field is not final, since it's set *after* SniffySelectionKey creation in SniffySelector.register() method
 * At first we're creating SniffySelectionKey without delegate, passing it as an attachment to delegate SelectionKey
 * And setting up the back reference link SniffySelectionKey.delegate after wards
 * These operations are done with locks on regLock and keyLock in relevant delegate Channel
 * @since 3.1.7
 */
@SuppressWarnings({"Convert2Diamond", "RedundantSuppression"})
public class SniffySelectionKey extends SelectionKey implements ObjectWrapper<SelectionKey> {
    private static final Polyglog LOG = PolyglogFactory.log(SniffySelectionKey.class);

    private SelectionKey delegate;
    private ClassRef<SelectionKey> classRef;
    private final SniffySelector sniffySelector;
    private final SelectableChannel sniffyChannel;

    protected SniffySelectionKey(SniffySelector sniffySelector, SelectableChannel sniffyChannel, Object attachment) {
        if (null != attachment) {
            attach(attachment);
        }

        this.sniffySelector = sniffySelector;
        this.sniffyChannel = sniffyChannel;

        LOG.trace("Created new SniffySelectionKey(" + delegate + ", " + sniffySelector + ", " + sniffyChannel + ") = " + this);

    }

    public void setDelegate(SelectionKey delegate) {
        assert null == this.delegate;

        this.delegate = delegate;
        this.classRef = $(delegate.getClass(), SelectionKey.class);
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
     * 'void deregister(AbstractSelectionKey key)' as a result of key.cancel() followed by selector.select*() calls
     * Workaround is to copy the 'keys' array from delegate to sniffy wrapper after 'select*' operations are invoked
     * deregister method is invoked from Selector implementation classes during select operations as well as in on close
     * 'void implCloseSelector() throws IOException'
     *
     * @return sniffy selector wrapper for given key or hardcoded NoOpSelector in case of invalid (or null) delegate key
     */
    @SuppressWarnings("CommentedOutCode")
    @Override
    public Selector selector() {
        return sniffySelector;
        // Workaround below shouldn't be required but can be handy in case AbstractSelectableChannel.register() gets an invalid key
        /*
        // TODO: code below shouldn't be required if everything else is configured properly (key removal from channels)
        if (!isValid() &&
                StackTraceExtractor.hasClassAndMethodInStackTrace("java.nio.channels.spi.AbstractSelectableChannel", "findKey") &&
                sniffyChannel instanceof SocketChannel
        ) {
            return NoOpSelector.INSTANCE;
        } else {
            return sniffySelector;
        }
        */
    }

    @Override
    public boolean isValid() {
        try {
            return delegate.isValid();
        } catch (Exception e) {
            LOG.trace("Error when trying to call isValid() on SniffySelectionKey(" + delegate + ", " + sniffySelector + ", " + sniffyChannel + ") = " + this);
            throw ExceptionUtil.throwException(e);
        }
    }

    /**
     * Delegate SelectionKey.cancel() removed this key from channel upon next select operation
     */
    @Override
    public void cancel() {
        boolean wasValid = false;
        try {
            wasValid = delegate.isValid();
            delegate.cancel();
        } catch (Exception e) {
            LOG.trace("Error when trying to call cancel() on SniffySelectionKey(" + delegate + ", " + sniffySelector + ", " + sniffyChannel + ") = " + this);
            throw ExceptionUtil.throwException(e);
        } finally {
            if (wasValid) {
                sniffySelector.addCancelledKey(this);
            }
        }
    }

    @Override
    public int interestOps() {
        try {
            return delegate.interestOps();
        } catch (Exception e) {
            LOG.trace("Error when trying to call interestOps() on SniffySelectionKey(" + delegate + ", " + sniffySelector + ", " + sniffyChannel + ") = " + this);
            throw ExceptionUtil.throwException(e);
        }
    }

    @Override
    public SelectionKey interestOps(int ops) {
        try {
            return delegate.interestOps(ops);
        } catch (Exception e) {
            LOG.trace("Error when trying to call interestOps(int) on SniffySelectionKey(" + delegate + ", " + sniffySelector + ", " + sniffyChannel + ") = " + this);
            throw ExceptionUtil.throwException(e);
        }
    }

    @Override
    public int readyOps() {
        try {
            return delegate.readyOps();
        } catch (Exception e) {
            LOG.trace("Error when trying to call readyOps() on SniffySelectionKey(" + delegate + ", " + sniffySelector + ", " + sniffyChannel + ") = " + this);
            throw ExceptionUtil.throwException(e);
        }
    }

    // No @Override annotation here because this method is available in Java 11+ only
    //@Override
    @SuppressWarnings({"Since15", "RedundantSuppression", "unused"})
    public int interestOpsOr(int ops) {
        try {
            return classRef.getNonStaticMethod(Integer.TYPE, "interestOpsOr", Integer.TYPE).invoke(delegate, ops);
        } catch (Exception e) {
            LOG.trace("Error when trying to call interestOpsOr(int) on SniffySelectionKey(" + delegate + ", " + sniffySelector + ", " + sniffyChannel + ") = " + this);
            throw ExceptionUtil.processException(e);
        }
    }

    // No @Override annotation here because this method is available in Java 11+ only
    //@Override
    @SuppressWarnings({"Since15", "RedundantSuppression", "unused"})
    public int interestOpsAnd(int ops) {
        try {
            return classRef.getNonStaticMethod(Integer.TYPE, "interestOpsAnd", Integer.TYPE).invoke(delegate, ops);
        } catch (Exception e) {
            LOG.trace("Error when trying to call interestOpsAnd(int) on SniffySelectionKey(" + delegate + ", " + sniffySelector + ", " + sniffyChannel + ") = " + this);
            throw ExceptionUtil.processException(e);
        }
    }

}
