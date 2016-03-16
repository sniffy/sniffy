package io.sniffy.socket;

import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class EchoServerRule extends ExternalResource implements Runnable {

    private final Thread thread = new Thread(this);

    private final List<Thread> socketThreads = new ArrayList<>();
    private final List<Socket> sockets = new ArrayList<>();

    private int boundPort = 10000;
    private ServerSocket serverSocket;

    public int getBoundPort() {
        return boundPort;
    }

    @Override
    protected void before() throws Throwable {

        for (int i = 0; i < 10; i++, boundPort++) {
            try {
                serverSocket = new ServerSocket(boundPort, 50, InetAddress.getByName(null));
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

    private CountDownLatch countDownLatch = new CountDownLatch(3);

    public CountDownLatch getCountDownLatch() {
        return countDownLatch;
    }

    @Override
    public void run() {

        try {
            while (!Thread.interrupted()) { // TODO in fact it doesn't support multiple connections

                Socket socket = serverSocket.accept();

                sockets.add(socket);

                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream();

                Thread socketInputStreamReaderThread = new Thread(new SocketInputStreamReader(inputStream));
                Thread socketOutputStreamWriterThread = new Thread(new SocketOutputStreamWriter(outputStream));

                socketThreads.add(socketInputStreamReaderThread);
                socketThreads.add(socketOutputStreamWriterThread);

                socketInputStreamReaderThread.start();
                socketOutputStreamWriterThread.start();
            }
        } catch (SocketException e) {
            if (!"socket closed".equalsIgnoreCase(e.getMessage())) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void after() {

        socketThreads.forEach(Thread::interrupt);

        joinThreads();

    }

    public void joinThreads() {

        socketThreads.forEach((thread) -> {
            try {
                thread.join();
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

    private class SocketInputStreamReader implements Runnable {

        private final InputStream inputStream;

        public SocketInputStreamReader(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try {

                countDownLatch.countDown();
                countDownLatch.await();

                int totalRead = 0, read = 0;

                while ((read = inputStream.read()) != -1) {
                    totalRead++;
                }

                inputStream.close();
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

        private final OutputStream outputStream;

        public SocketOutputStreamWriter(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void run() {

            try {

                countDownLatch.countDown();
                countDownLatch.await();

                outputStream.write(new byte[]{9,8,7,6,5,4,3,2});
                outputStream.flush();
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }



}
