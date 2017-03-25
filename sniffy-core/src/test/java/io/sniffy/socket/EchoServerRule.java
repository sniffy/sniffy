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

public class EchoServerRule extends ExternalResource implements Runnable {

    private final Thread thread = new Thread(this);

    private final List<Thread> socketThreads = new ArrayList<>();
    private final List<Socket> sockets = new ArrayList<>();

    private int boundPort = 10000;
    private ServerSocket serverSocket;

    private final byte[] dataToBeSent;
    private final Queue<ByteArrayOutputStream> receivedData = new ConcurrentLinkedQueue<>();

    public EchoServerRule(byte[] dataToBeSent) {
        this.dataToBeSent = dataToBeSent;
    }

    public int getBoundPort() {
        return boundPort;
    }

    @Override
    public void before() throws Throwable {

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

    public byte[] pollReceivedData() {
        return receivedData.poll().toByteArray();
    }

    @Override
    public void run() {

        try {
            while (!Thread.interrupted()) {

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                receivedData.add(baos);

                Socket socket = serverSocket.accept();
                socket.setReuseAddress(true);

                sockets.add(socket);

                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream();

                Thread socketInputStreamReaderThread = new Thread(new SocketInputStreamReader(socket, inputStream, baos));
                Thread socketOutputStreamWriterThread = new Thread(new SocketOutputStreamWriter(socket, outputStream));

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

    private class SocketInputStreamReader implements Runnable {

        private final Socket socket;
        private final InputStream inputStream;
        private final ByteArrayOutputStream baos;

        public SocketInputStreamReader(Socket socket, InputStream inputStream, ByteArrayOutputStream baos) {
            this.socket = socket;
            this.inputStream = inputStream;
            this.baos = baos;
        }

        @Override
        public void run() {
            try {

                int read;

                while ((read = inputStream.read()) != -1) {
                    baos.write(read);
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

        private SocketOutputStreamWriter(Socket socket, OutputStream outputStream) {
            this.socket = socket;
            this.outputStream = outputStream;
        }

        @Override
        public void run() {

            try {

                outputStream.write(dataToBeSent);
                outputStream.flush();

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
