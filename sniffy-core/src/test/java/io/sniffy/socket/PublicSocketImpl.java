package io.sniffy.socket;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Set;

public abstract class PublicSocketImpl extends SocketImpl {

    @Override
    public void create(boolean stream) throws IOException {
        
    }

    @Override
    public void connect(String host, int port) throws IOException {

    }

    @Override
    public void connect(InetAddress address, int port) throws IOException {

    }

    @Override
    public void connect(SocketAddress address, int timeout) throws IOException {

    }

    @Override
    public void bind(InetAddress host, int port) throws IOException {

    }

    @Override
    public void listen(int backlog) throws IOException {

    }

    @Override
    public void accept(SocketImpl s) throws IOException {

    }

    @Override
    public InputStream getInputStream() throws IOException {
        return null;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return null;
    }

    @Override
    public int available() throws IOException {
        return 0;
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void sendUrgentData(int data) throws IOException {

    }

    @Override
    public void setOption(int optID, Object value) throws SocketException {

    }

    @Override
    public Object getOption(int optID) throws SocketException {
        return null;
    }

    @Override
    public void shutdownInput() throws IOException {
        super.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        super.shutdownOutput();
    }

    @Override
    public FileDescriptor getFileDescriptor() {
        return super.getFileDescriptor();
    }

    @Override
    public InetAddress getInetAddress() {
        return super.getInetAddress();
    }

    @Override
    public int getPort() {
        return super.getPort();
    }

    @Override
    public boolean supportsUrgentData() {
        return super.supportsUrgentData();
    }

    @Override
    public int getLocalPort() {
        return super.getLocalPort();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        super.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    // TODO: test Java9+ only code below
    /*@Override
    public <T> void setOption(SocketOption<T> name, T value) throws IOException {
        super.setOption(name, value);
    }

    @Override
    public <T> T getOption(SocketOption<T> name) throws IOException {
        return super.getOption(name);
    }

    @Override
    public Set<SocketOption<?>> supportedOptions() {
        return super.supportedOptions();
    }*/
}
