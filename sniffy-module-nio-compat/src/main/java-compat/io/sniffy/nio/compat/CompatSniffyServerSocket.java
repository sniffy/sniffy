package io.sniffy.nio.compat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;

public class CompatSniffyServerSocket extends CompatSniffyServerSocketAdapter {

    private final CompatSniffyServerSocketChannel serverSocketChannel;

    public CompatSniffyServerSocket(ServerSocket delegate, CompatSniffyServerSocketChannel serverSocketChannel) throws IOException {
        super(delegate);
        this.serverSocketChannel = serverSocketChannel;
    }

    @Override
    public ServerSocketChannel getChannel() {
        return null != serverSocketChannel ? serverSocketChannel : super.getChannel();
    }

    @Override
    public Socket accept() throws IOException {
        return new CompatSniffySocketChannel(serverSocketChannel.provider(), super.accept().getChannel()).socket(); // TODO: it should be better
    }

}
