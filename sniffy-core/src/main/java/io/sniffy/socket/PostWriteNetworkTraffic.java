package io.sniffy.socket;

/** Receives bytes after a socket stream has successfully written them. */
public interface PostWriteNetworkTraffic {

    void onBytesWritten(byte[] bytes, int offset, int length);

}
