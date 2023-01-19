package io.sniffy.nio;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;

public class SniffyServerSocket extends SniffyServerSocketAdapter {

    private final SniffyServerSocketChannel serverSocketChannel;

    public SniffyServerSocket(ServerSocket delegate, SniffyServerSocketChannel serverSocketChannel) throws IOException {
        super(delegate);
        this.serverSocketChannel = serverSocketChannel;
    }

    @Override
    public ServerSocketChannel getChannel() {
        return null != serverSocketChannel ? serverSocketChannel : super.getChannel();
    }

    @Override
    public Socket accept() throws IOException {
        return new SniffySocketChannel(serverSocketChannel.provider(), super.accept().getChannel()).socket(); // TODO: it should be better
    }

}
