package io.sniffy.nio.compat;

import io.sniffy.nio.SelectableChannelWrapper;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.OSUtil;
import io.sniffy.util.ReflectionUtil;
import io.sniffy.util.StackTraceExtractor;
import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;
import sun.nio.ch.ServerSocketChannelDelegate;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;

import static io.sniffy.util.ReflectionUtil.invokeMethod;
import static io.sniffy.util.ReflectionUtil.setField;

/**
 * @since 3.1.7
 */
// TODO: test properly and come up with a strategy for server sockets and server channels
public class CompatSniffyServerSocketChannel extends ServerSocketChannelDelegate implements SelectableChannelWrapper<ServerSocketChannel> {

    private final ServerSocketChannel delegate;

    public CompatSniffyServerSocketChannel(SelectorProvider provider, ServerSocketChannel delegate) {
        super(provider, delegate);
        this.delegate = delegate;
    }

    @Override
    public ServerSocketChannel getDelegate() {
        return delegate;
    }

    @Override
    public AbstractSelectableChannel asSelectableChannel() {
        return this;
    }

    @Override
    @IgnoreJRERequirement
    public ServerSocketChannel bind(SocketAddress local, int backlog) throws IOException {
        delegate.bind(local, backlog);
        return this;
    }

    @Override
    @IgnoreJRERequirement
    public <T> ServerSocketChannel setOption(SocketOption<T> name, T value) throws IOException {
        delegate.setOption(name, value);
        return this;
    }

    @Override
    public ServerSocket socket() {
        return delegate.socket();
    }

    @Override
    public SocketChannel accept() throws IOException {

        SocketChannel socketChannel = delegate.accept();

        if (null == socketChannel) {
            return null;
        }

        // Windows Selector is implemented using a pair of sockets which are explicitly cast and do not work with Sniffy
        return OSUtil.isWindows() && StackTraceExtractor.hasClassInStackTrace("sun.nio.ch.Pipe") ?
                socketChannel :
                new CompatSniffySocketChannelAdapter(provider(), socketChannel);
    }

    @Override
    @IgnoreJRERequirement
    public SocketAddress getLocalAddress() throws IOException {
        return delegate.getLocalAddress();
    }

    @Override
    public void implCloseSelectableChannel() {
        try {

            Object delegateCloseLock = ReflectionUtil.getField(AbstractInterruptibleChannel.class, delegate, "closeLock");

            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (delegateCloseLock) {
                setField(AbstractInterruptibleChannel.class, delegate, "closed", true);
                invokeMethod(AbstractSelectableChannel.class, delegate, "implCloseSelectableChannel", Void.class);
            }

        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @Override
    public void implConfigureBlocking(boolean block) {
        try {

            Object delegateRegLock = ReflectionUtil.getField(AbstractSelectableChannel.class, delegate, "regLock");

            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (delegateRegLock) {
                invokeMethod(AbstractSelectableChannel.class, delegate, "implConfigureBlocking", Boolean.TYPE, block, Void.class);
                if (!setField(AbstractSelectableChannel.class, delegate, "nonBlocking", !block)) {
                    setField(AbstractSelectableChannel.class, delegate, "blocking", block); // Java 10 had blocking field instead of nonBlocking
                }
            }

        } catch (Exception e) {
            throw ExceptionUtil.processException(e);
        }
    }

    @Override
    @IgnoreJRERequirement
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return delegate.getOption(name);
    }

    @Override
    @IgnoreJRERequirement
    public Set<SocketOption<?>> supportedOptions() {
        return delegate.supportedOptions();
    }

}
