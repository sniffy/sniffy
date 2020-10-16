package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

public abstract class SocketChannelDelegate extends SocketChannel implements SelChImpl {

    private final SelChImpl delegate;

    public SocketChannelDelegate(SelectorProvider provider, Object delegate) {
        super(provider);
        this.delegate = (SelChImpl) delegate;
    }

    @Override
    public FileDescriptor getFD() {
        return delegate.getFD();
    }

    @Override
    public int getFDVal() {
        return delegate.getFDVal();
    }

    @Override
    public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl sk) {
        return delegate.translateAndUpdateReadyOps(ops, sk);
    }

    @Override
    public boolean translateAndSetReadyOps(int ops, SelectionKeyImpl sk) {
        return delegate.translateAndSetReadyOps(ops, sk);
    }

    @Override
    public void translateAndSetInterestOps(int ops, SelectionKeyImpl sk) {
        delegate.translateAndSetInterestOps(ops, sk);
    }

    @Override
    public void kill() throws IOException {
        delegate.kill();
    }

}
