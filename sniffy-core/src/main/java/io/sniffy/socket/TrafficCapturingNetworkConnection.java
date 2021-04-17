package io.sniffy.socket;

public interface TrafficCapturingNetworkConnection {

    void logTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len);

}
