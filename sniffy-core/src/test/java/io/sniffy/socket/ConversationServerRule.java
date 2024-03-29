package io.sniffy.socket;

import org.junit.rules.ExternalResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConversationServerRule extends ExternalResource implements Runnable {

    private final Thread thread = new Thread(this);

    private final List<Thread> socketThreads = new ArrayList<>();
    private final List<Socket> sockets = new ArrayList<>();

    private final AtomicInteger bytesReceivedCounter = new AtomicInteger();

    private int boundPort = 10300;
    private ServerSocket serverSocket;

    private final List<byte[]> dataToBeReceived;
    private final List<byte[]> dataToBeSent;

    public ConversationServerRule(List<byte[]> dataToBeReceived, List<byte[]> dataToBeSent) {
        this.dataToBeReceived = dataToBeReceived;
        this.dataToBeSent = dataToBeSent;
    }

    public int getBoundPort() {
        return boundPort;
    }

    public int getBytesReceived() {
        return bytesReceivedCounter.get();
    }

    @Override
    public void before() throws Throwable {

        bytesReceivedCounter.set(0);

        for (int i = 0; i < 10; i++, boundPort++) {
            try {
                serverSocket = new ServerSocket(boundPort, 50, InetAddress.getByName(null));
                serverSocket.setReuseAddress(true);
                break;
            } catch (IOException e) {
                try {
                    serverSocket.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        if (null == serverSocket) {
            throw new IOException("Failed to find an available port");
        }

        thread.start();

    }

    @Override
    public void after() {

        socketThreads.forEach(Thread::interrupt);

        joinThreads();

        thread.interrupt();

        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {

        try {
            while (!Thread.interrupted()) {

                Socket socket = serverSocket.accept();
                socket.setReuseAddress(true);
                socket.setOOBInline(true);
                socket.setTcpNoDelay(true);

                sockets.add(socket);

                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream();

                Conversation conversation = new Conversation();

                Thread socketInputStreamReaderThread = new Thread(new SocketInputStreamReader(socket, inputStream, dataToBeReceived, conversation));
                Thread socketOutputStreamWriterThread = new Thread(new SocketOutputStreamWriter(socket, outputStream, dataToBeSent, conversation));

                socketThreads.add(socketInputStreamReaderThread);
                socketThreads.add(socketOutputStreamWriterThread);

                socketInputStreamReaderThread.start();
                socketOutputStreamWriterThread.start();
            }
        } catch (SocketException e) {
            if (null == e.getMessage() || !e.getMessage().toLowerCase().matches("socket.*closed")) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void joinThreads() {

        socketThreads.forEach((thread) -> {
            try {
                thread.join(10000);
                if (thread.isAlive()) {
                    thread.interrupt();
                    thread.join(1000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        sockets.forEach((socket) -> {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    private class Conversation {

        private volatile boolean receiving = true;

        private synchronized void awaitSending() throws InterruptedException {
            while (receiving) {
                wait();
            }
        }

        private synchronized void awaitReceiving() throws InterruptedException {
            while (!receiving) {
                wait();
            }
        }

        private synchronized void sendingFinished() {
            receiving = true;
            notifyAll();
        }

        private synchronized void receivingFinished() {
            receiving = false;
            notifyAll();
        }

    }

    private class SocketInputStreamReader implements Runnable {

        private final Socket socket;
        private final InputStream inputStream;

        private final List<byte[]> dataToBeReceived;

        private final Conversation conversation;

        public SocketInputStreamReader(Socket socket, InputStream inputStream, List<byte[]> dataToBeReceived, Conversation conversation) {
            this.socket = socket;
            this.inputStream = inputStream;
            this.dataToBeReceived = dataToBeReceived;
            this.conversation = conversation;
        }

        @Override
        public void run() {
            try {

                for (byte[] dataToBeReceived : dataToBeReceived) {
                    conversation.awaitReceiving();
                    for (byte b : dataToBeReceived) {
                        if (((byte) inputStream.read()) == b) {
                            bytesReceivedCounter.incrementAndGet();
                        } else {
                            throw new RuntimeException("Incomplete request");
                        }
                    }
                    conversation.receivingFinished();
                }

                socket.shutdownInput();
            } catch (SocketException e) {
                if (!"socket closed".equalsIgnoreCase(e.getMessage())) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    private class SocketOutputStreamWriter implements Runnable {

        private final Socket socket;
        private final OutputStream outputStream;

        private final List<byte[]> dataToBeSent;

        private final Conversation conversation;

        private SocketOutputStreamWriter(Socket socket, OutputStream outputStream, List<byte[]> dataToBeSent, Conversation conversation) {
            this.socket = socket;
            this.outputStream = outputStream;

            this.dataToBeSent = dataToBeSent;
            this.conversation = conversation;

        }

        @Override
        public void run() {

            try {

                for (byte[] dataToBeSent : dataToBeSent) {
                    conversation.awaitSending();
                    outputStream.write(dataToBeSent);
                    outputStream.flush();
                    conversation.sendingFinished();
                }

                socket.shutdownOutput();
            } catch (SocketException e) {
                if (!"socket closed".equalsIgnoreCase(e.getMessage())) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }



}
