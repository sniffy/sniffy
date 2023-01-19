package io.sniffy.nio;

import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

public abstract class SocketChannelWrapper extends SocketChannel implements SelectableChannelWrapper<SocketChannel> {

    public SocketChannelWrapper(SelectorProvider provider) {
        super(provider);
    }

}
