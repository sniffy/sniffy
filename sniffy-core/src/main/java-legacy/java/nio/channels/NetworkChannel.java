package java.nio.channels;

import java.net.SocketOption;
import java.net.SocketAddress;
import java.util.Set;
import java.io.IOException;

public interface NetworkChannel extends Channel {

    NetworkChannel bind(SocketAddress local) throws IOException;

    SocketAddress getLocalAddress() throws IOException;

    <T> NetworkChannel setOption(SocketOption<T> name, T value) throws IOException;

    <T> T getOption(SocketOption<T> name) throws IOException;

    Set<SocketOption<?>> supportedOptions();
}

