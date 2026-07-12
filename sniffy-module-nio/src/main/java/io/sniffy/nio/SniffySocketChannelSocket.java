package io.sniffy.nio;

import io.sniffy.socket.PostWriteNetworkTraffic;
import io.sniffy.socket.Protocol;
import io.sniffy.socket.SniffySocket;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

/** Socket view whose monitoring state and accounting are owned by its channel. */
final class SniffySocketChannelSocket extends SniffySocket implements PostWriteNetworkTraffic {

    private final SniffySocketChannel channel;

    SniffySocketChannelSocket(Socket delegate, SniffySocketChannel channel, int connectionId) throws SocketException {
        super(delegate, channel, connectionId, channel.getInetSocketAddress());
        this.channel = channel;
    }

    @Override
    public SocketChannel getChannel() {
        return channel;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }

    @Override
    public void shutdownInput() throws IOException {
        channel.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        channel.shutdownOutput();
    }

    @Override
    public void onBytesWritten(byte[] bytes, int offset, int length) {
        byte[] copy = new byte[length];
        System.arraycopy(bytes, offset, copy, 0, length);
        channel.processOutboundBytes(copy, io.sniffy.Sniffy.getEffectiveSpyConfiguration().isCaptureNetworkTraffic());
    }

    @Override
    public void logTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len) {
        if (sent) {
            onBytesWritten(traffic, off, len);
        } else {
            channel.logTraffic(false, protocol, traffic, off, len);
        }
    }

    @Override
    public void logDecryptedTraffic(boolean sent, Protocol protocol, byte[] traffic, int off, int len) {
        channel.logDecryptedTraffic(sent, protocol, traffic, off, len);
    }

    @Override public InetSocketAddress getInetSocketAddress() { return channel.getInetSocketAddress(); }
    @Override public void setConnectionStatus(Integer value) { channel.setConnectionStatus(value); }
    @Override public void setProxiedInetSocketAddress(InetSocketAddress value) { channel.setProxiedInetSocketAddress(value); }
    @Override public InetSocketAddress getProxiedInetSocketAddress() { return channel.getProxiedInetSocketAddress(); }
    @Override public void setFirstPacketSent(boolean value) { channel.setFirstPacketSent(value); }
    @Override public boolean isFirstPacketSent() { return channel.isFirstPacketSent(); }
    @Override public int getPotentiallyBufferedInputBytes() { return channel.getPotentiallyBufferedInputBytes(); }
    @Override public void setPotentiallyBufferedInputBytes(int value) { channel.setPotentiallyBufferedInputBytes(value); }
    @Override public int getPotentiallyBufferedOutputBytes() { return channel.getPotentiallyBufferedOutputBytes(); }
    @Override public void setPotentiallyBufferedOutputBytes(int value) { channel.setPotentiallyBufferedOutputBytes(value); }
    @Override public long getLastReadThreadId() { return channel.getLastReadThreadId(); }
    @Override public void setLastReadThreadId(long value) { channel.setLastReadThreadId(value); }
    @Override public long getLastWriteThreadId() { return channel.getLastWriteThreadId(); }
    @Override public void setLastWriteThreadId(long value) { channel.setLastWriteThreadId(value); }
    @Override public void logSocket(long millis) { channel.logSocket(millis); }
    @Override public void logSocket(long millis, int down, int up) { channel.logSocket(millis, down, up); }
    @Override public void checkConnectionAllowed() throws ConnectException { channel.checkConnectionAllowed(); }
    @Override public void checkConnectionAllowed(int cycles) throws ConnectException { channel.checkConnectionAllowed(cycles); }
    @Override public void checkConnectionAllowed(InetSocketAddress address) throws ConnectException { channel.checkConnectionAllowed(address); }
    @Override public void checkConnectionAllowed(InetSocketAddress address, int cycles) throws ConnectException {
        channel.checkConnectionAllowed(address, cycles);
    }
}
