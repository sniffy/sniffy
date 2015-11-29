package io.sniffy.socket;

import org.junit.rules.ExternalResource;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bedrin on 22.11.2015.
 */
public class EchoServerRule extends ExternalResource implements Runnable {

    private int boundPort = 10000;
    private ServerSocket serverSocket;

    private Thread thread;

    private List<Thread> socketThreads = new ArrayList<>();
    private List<Socket> sockets = new ArrayList<>();

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

        (thread = new Thread(this)).start();

    }

    @Override
    public void run() {

        try {
            while (!Thread.interrupted()) {
                Socket socket = serverSocket.accept();
                PipedOutputStream pipedOutputStream = new PipedOutputStream();

                sockets.add(socket);

                Thread socketInputStreamReaderThread = new Thread(
                        new SocketInputStreamReader(socket, pipedOutputStream)
                );
                Thread socketOutputStreamWriterThread = new Thread (
                        new SocketOutputStreamWriter(socket, pipedOutputStream)
                );

                socketThreads.add(socketInputStreamReaderThread);
                socketThreads.add(socketOutputStreamWriterThread);

                socketInputStreamReaderThread.start();
                socketOutputStreamWriterThread.start();
            }
        } catch (SocketException e) {
            if (!"socket closed".equalsIgnoreCase(e.getMessage())) {
                e.printStackTrace();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void after() {

        socketThreads.forEach(Thread::interrupt);

        sockets.forEach((socket) -> {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        socketThreads.forEach((thread) -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
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

    private static class SocketInputStreamReader implements Runnable {

        private final Socket socket;
        private final PipedOutputStream pipedOutputStream;

        public SocketInputStreamReader(Socket socket, PipedOutputStream pipedOutputStream) {
            this.socket = socket;
            this.pipedOutputStream = pipedOutputStream;
        }

        @Override
        public void run() {
            try {
                InputStream inputStream = socket.getInputStream();
                byte[] buff = new byte[1024];
                int read;
                while ((read = inputStream.read(buff)) != -1) {
                    pipedOutputStream.write(buff, 0, read);
                }
                pipedOutputStream.flush();
                pipedOutputStream.close();

                System.out.println("PipedOutputStream flushed");
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }

    private static class SocketOutputStreamWriter implements Runnable {

        private final Socket socket;
        private final PipedOutputStream pipedOutputStream;

        public SocketOutputStreamWriter(Socket socket, PipedOutputStream pipedOutputStream) {
            this.socket = socket;
            this.pipedOutputStream = pipedOutputStream;
        }

        @Override
        public void run() {

            try {
                PipedInputStream pipedInputStream = new PipedInputStream(pipedOutputStream);
                OutputStream outputStream = socket.getOutputStream();
                byte[] buff = new byte[1024];
                int read;
                while ((read = pipedInputStream.read(buff)) != -1) {
                    if (read > 0) {
                        outputStream.write(buff, 0, read);
                    }
                }
                outputStream.flush();
                outputStream.close();

                System.out.println("EchoOutputStream flushed");
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }



}
