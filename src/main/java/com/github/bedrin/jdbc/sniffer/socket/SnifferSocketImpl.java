package com.github.bedrin.jdbc.sniffer.socket;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;

public class SnifferSocketImpl extends SocketImpl {

    private final SocketImpl delegate;

    public SnifferSocketImpl(SocketImpl delegate) {
        this.delegate = delegate;
    }

    private static Method method(String methodName, Class<?>... argumentTypes) throws NoSuchMethodException {
        Method method = SocketImpl.class.getDeclaredMethod(methodName, argumentTypes);
        method.setAccessible(true);
        return method;
    }

    @Override
    protected void sendUrgentData(int data) throws IOException {
        try {
            method("sendUrgentData", int.class).invoke(delegate, data);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void shutdownInput() throws IOException {
        try {
            method("shutdownInput").invoke(delegate);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void shutdownOutput() throws IOException {
        super.shutdownOutput();
    }

    @Override
    protected FileDescriptor getFileDescriptor() {
        return super.getFileDescriptor();
    }

    @Override
    protected InetAddress getInetAddress() {
        return super.getInetAddress();
    }

    @Override
    protected int getPort() {
        return super.getPort();
    }

    @Override
    protected boolean supportsUrgentData() {
        return super.supportsUrgentData();
    }

    @Override
    protected int getLocalPort() {
        return super.getLocalPort();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    protected void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        super.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    @Override
    protected void create(boolean stream) throws IOException {

    }

    @Override
    protected void connect(String host, int port) throws IOException {

    }

    @Override
    protected void connect(InetAddress address, int port) throws IOException {

    }

    @Override
    protected void connect(SocketAddress address, int timeout) throws IOException {

    }

    @Override
    protected void bind(InetAddress host, int port) throws IOException {

    }

    @Override
    protected void listen(int backlog) throws IOException {

    }

    @Override
    protected void accept(SocketImpl s) throws IOException {

    }

    @Override
    protected InputStream getInputStream() throws IOException {
        return null;
    }

    @Override
    protected OutputStream getOutputStream() throws IOException {
        return null;
    }

    @Override
    protected int available() throws IOException {
        return 0;
    }

    @Override
    protected void close() throws IOException {

    }

    // interface

    @Override
    public void setOption(int optID, Object value) throws SocketException {

    }

    @Override
    public Object getOption(int optID) throws SocketException {
        return null;
    }
}
