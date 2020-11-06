package io.sniffy.socket;

import java.net.InetSocketAddress;

public interface SniffyNetworkConnection {

    InetSocketAddress getInetSocketAddress();

    void setConnectionStatus(Integer connectionStatus);

}
