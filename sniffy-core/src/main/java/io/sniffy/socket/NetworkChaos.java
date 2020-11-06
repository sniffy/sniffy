package io.sniffy.socket;

/**
 * Allows adding latency to network connections using a lot of assumptions and heuristics
 *
 * This class tries to emulate TCP Windows and TCP Window Scaling and add a latency for each TCP Window sent or received
 * By default it assumes that starting window size is 64K and i can grow up to 8M
 *
 * Another functionality provided is autodetect of request-response protocols where each write followed by read causes
 * a delay to be injected regardless from amount of bytes sent previously
 *
 * TODO: parse net.ipv4.tcp_rmem and similar parameters on Linux to get OS settings for TCP Windows
 * TODO: find a similar way for Windows and MacOS X as lower priority
 *
 * TODO: support Nagle algorithm
 * TODO: support other options like late ack, TCP FAST START etc.
 * TODO: support throttling bandwidth
 * TODO: can we do something about Jitter as well (?)
 * TODO: shall we look at SocketOptions.SO_RCVBUF and similar options? They do not seem to correlate with TCP Window size
 */
public class NetworkChaos {

    private static int defaultTcpWindowSize = 1 << 16;
    private static int maximumTcpScale = 7;

    // TODO: implement

}
