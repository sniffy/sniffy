package io.sniffy.nio;

import java.nio.channels.spi.AbstractSelectableChannel;

public interface SelectableChannelWrapper<T extends AbstractSelectableChannel> {

    T getDelegate();

}
