package io.sniffy.socket;

import java.net.InetSocketAddress;

public interface SniffySocket {

    InetSocketAddress getInetSocketAddress();

    void setConnectionStatus(Integer connectionStatus);

}
