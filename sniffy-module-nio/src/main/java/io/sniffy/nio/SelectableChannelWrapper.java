package io.sniffy.nio;

import java.nio.channels.InterruptibleChannel;
import java.nio.channels.spi.AbstractSelectableChannel;

/**
 * @since 3.1.7
 */
public interface SelectableChannelWrapper<T extends AbstractSelectableChannel> extends InterruptibleChannel {

    T getDelegate();

}
