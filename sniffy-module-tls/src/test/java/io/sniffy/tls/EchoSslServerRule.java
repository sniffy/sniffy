package io.sniffy.tls;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class EchoSslServerRule extends ExternalResource implements Runnable {

    private final TemporaryFolder tempFolder;

    private final Thread thread = new Thread(this);

    private final List<Thread> socketThreads = new ArrayList<>();
    private final List<Socket> sockets = new ArrayList<>();

    private final AtomicInteger bytesReceivedCounter = new AtomicInteger();
    private final int expectedBytes;

    private int boundPort = 10600;
    private ServerSocket serverSocket;

    private final byte[] dataToBeSent;
    private final Queue<ByteArrayOutputStream> receivedData = new ConcurrentLinkedQueue<>();

    public EchoSslServerRule(TemporaryFolder tempFolder, byte[] dataToBeSent, int expectedBytes) {
        this.tempFolder = tempFolder;
        this.dataToBeSent = dataToBeSent;
        this.expectedBytes = expectedBytes;
    }

    public int getBoundPort() {
        return boundPort;
    }

    public int getBytesReceived() {
        return bytesReceivedCounter.get();
    }

    private String serverKeyStorePath;
    private String serverKeyStorePassword;
    private String serverTrustStorePath;
    private String serverTrustStorePassword;

    private String clientKeyStorePath;
    private String clientKeyStorePassword;
    private String clientTrustStorePath;
    private String clientTrustStorePassword;

    private static final BouncyCastleProvider bcProvider;

    static {
        bcProvider = new BouncyCastleProvider();
        Security.addProvider(bcProvider);
    }

    public void generateCertificates() throws Exception {

        serverKeyStorePath = tempFolder.newFile("serverKeyStore.jks").getAbsolutePath();
        serverKeyStorePassword = "serverKeyStorePassword";
        serverTrustStorePath = tempFolder.newFile("serverTrustStore.jks").getAbsolutePath();
        serverTrustStorePassword = "serverTrustStore";

        clientKeyStorePath = tempFolder.newFile("clientKeyStore.jks").getAbsolutePath();
        clientKeyStorePassword = "clientKeyStorePassword";
        clientTrustStorePath = tempFolder.newFile("clientTrustStore.jks").getAbsolutePath();
        clientTrustStorePassword = "clientTrustStore";

        {
            KeyPair caKeyPair = generateKeyPair();
            KeyPair certificateKeyPair = generateKeyPair();

            X509Certificate selfSignedCertificate = selfSign(caKeyPair.getPrivate(), caKeyPair.getPublic(), "CN=myserverca.test.sniffy.io");
            X509Certificate serverCertificate = issueCertificate(caKeyPair.getPrivate(), certificateKeyPair.getPublic(), "CN=myserver.test.sniffy.io", selfSignedCertificate);

            {
                KeyStore keyStore = KeyStore.getInstance("jks");
                keyStore.load(null, "changeit".toCharArray());
                keyStore.setKeyEntry("key", certificateKeyPair.getPrivate(), serverKeyStorePassword.toCharArray(), new Certificate[]{serverCertificate, selfSignedCertificate});
                FileOutputStream fos = new FileOutputStream(serverKeyStorePath);
                keyStore.store(fos, serverKeyStorePassword.toCharArray());
                fos.close();
            }
            {
                KeyStore keyStore = KeyStore.getInstance("jks");
                keyStore.load(null, "changeit".toCharArray());
                keyStore.setCertificateEntry("serverca", selfSignedCertificate);
                FileOutputStream fos = new FileOutputStream(clientTrustStorePath);
                keyStore.store(fos, clientTrustStorePassword.toCharArray());
                fos.close();
            }

        }

        {
            KeyPair caKeyPair = generateKeyPair();
            KeyPair certificateKeyPair = generateKeyPair();

            X509Certificate selfSignedCertificate = selfSign(caKeyPair.getPrivate(), caKeyPair.getPublic(), "CN=myclientca.test.sniffy.io");
            X509Certificate serverCertificate = issueCertificate(caKeyPair.getPrivate(), certificateKeyPair.getPublic(), "CN=myclient.test.sniffy.io", selfSignedCertificate);

            {
                KeyStore keyStore = KeyStore.getInstance("jks");
                keyStore.load(null, "changeit".toCharArray());
                keyStore.setKeyEntry("key", certificateKeyPair.getPrivate(), clientKeyStorePassword.toCharArray(), new Certificate[]{serverCertificate, selfSignedCertificate});
                FileOutputStream fos = new FileOutputStream(clientKeyStorePath);
                keyStore.store(fos, clientKeyStorePassword.toCharArray());
                fos.close();
            }
            {
                KeyStore keyStore = KeyStore.getInstance("jks");
                keyStore.load(null, "changeit".toCharArray());
                keyStore.setCertificateEntry("clientca", selfSignedCertificate);
                FileOutputStream fos = new FileOutputStream(serverTrustStorePath);
                keyStore.store(fos, serverTrustStorePassword.toCharArray());
                fos.close();
            }

        }

    }

    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    private static X509Certificate selfSign(PrivateKey privateKey, PublicKey publicKey, String subject) throws OperatorCreationException, SocketException, CertIOException, CertificateException {

        long now = System.currentTimeMillis();
        Date from = new Date(now);
        X500Name x500Name = new X500Name(subject);
        BigInteger certSerialNumber = new BigInteger(Long.toString(now));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(from);
        calendar.add(1, Calendar.YEAR);
        Date to = calendar.getTime();

        String signatureAlgorythm = "SHA256WithRSA";
        ContentSigner contentSigner = (new JcaContentSignerBuilder(signatureAlgorythm)).build(privateKey);
        JcaX509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(x500Name, certSerialNumber, from, to, x500Name, publicKey);

        certificateBuilder.addExtension(Extension.subjectAlternativeName, false, alternativeNames());

        BasicConstraints basicConstraints = new BasicConstraints(true);
        certificateBuilder.addExtension(Extension.basicConstraints, true, basicConstraints);

        return (new JcaX509CertificateConverter()).setProvider(bcProvider).getCertificate(certificateBuilder.build(contentSigner));

    }

    private static X509Certificate issueCertificate(PrivateKey privateKey, PublicKey publicKey, String subject, X509Certificate issuer) throws OperatorCreationException, SocketException, CertIOException, CertificateException {

        long now = System.currentTimeMillis();
        Date from = new Date(now);
        X500Name x500Name = new X500Name(subject);
        BigInteger certSerialNumber = new BigInteger(Long.toString(now));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(from);
        calendar.add(Calendar.YEAR, 1);
        Date to = calendar.getTime();

        String signatureAlgorythm = "SHA256WithRSA";
        ContentSigner contentSigner = (new JcaContentSignerBuilder(signatureAlgorythm)).build(privateKey);
        JcaX509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(issuer, certSerialNumber, from, to, x500Name, publicKey);

        certificateBuilder.addExtension(Extension.subjectAlternativeName, false, alternativeNames());

        BasicConstraints basicConstraints = new BasicConstraints(true);
        certificateBuilder.addExtension(Extension.basicConstraints, true, basicConstraints);

        return (new JcaX509CertificateConverter()).setProvider(bcProvider).getCertificate(certificateBuilder.build(contentSigner));

    }

    @SuppressWarnings("CommentedOutCode")
    private static GeneralNames alternativeNames() throws SocketException {

        List<GeneralName> generalNames = new ArrayList<>();

        generalNames.add(new GeneralName(GeneralName.dNSName, "localhost"));
        generalNames.add(new GeneralName(GeneralName.iPAddress, "127.0.0.1"));
        generalNames.add(new GeneralName(GeneralName.iPAddress, "0:0:0:0:0:0:0:1"));

        // Commented out since it slows down CI/CD a lot
        /*
        Collections.list(NetworkInterface.getNetworkInterfaces()).stream().filter(it -> {
            try {
                return it.isUp();
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }
        }).flatMap(it -> Collections.list(it.getInetAddresses()).stream()).forEach(it -> {
            generalNames.add(new GeneralName(GeneralName.dNSName, it.getHostName()));
            generalNames.add(new GeneralName(GeneralName.dNSName, it.getCanonicalHostName()));
            generalNames.add(new GeneralName(GeneralName.iPAddress, !it.getHostAddress().contains("%") ? it.getHostAddress() : it.getHostAddress().substring(0, it.getHostAddress().indexOf("%"))));
        });
        */

        return new GeneralNames(generalNames.toArray(new GeneralName[0]));

    }

    public SSLContext getClientSSLContext() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream tstore = new FileInputStream(clientTrustStorePath);
        trustStore.load(tstore, clientTrustStorePassword.toCharArray());
        tstore.close();

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream kstore = new FileInputStream(clientKeyStorePath);
        keyStore.load(kstore, clientKeyStorePassword.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, clientKeyStorePassword.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        return sslContext;
    }

    @Override
    public void before() throws Throwable {

        generateCertificates();

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream tstore = new FileInputStream(serverTrustStorePath);
        trustStore.load(tstore, serverTrustStorePassword.toCharArray());
        tstore.close();

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream kstore = new FileInputStream(serverKeyStorePath);
        keyStore.load(kstore, serverKeyStorePassword.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, serverKeyStorePassword.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        bytesReceivedCounter.set(0);

        for (int i = 0; i < 10; i++, boundPort++) {
            try {
                serverSocket = sslContext.getServerSocketFactory().createServerSocket(boundPort, 50, InetAddress.getByName(null));
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
                //socket.setOOBInline(true); // OOBInline is not supported for SSLSocket
                socket.setTcpNoDelay(true);

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

        if (bytesReceivedCounter.get() < expectedBytes) {
            synchronized (this) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        socketThreads.forEach((thread) -> {
            try {
                if (thread.isAlive()) {
                    thread.interrupt();
                }
            } catch (Exception e) {
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
            synchronized (EchoSslServerRule.this) {
                try {

                    int read;
                    int bytesRead = 0;

                    while (bytesRead < expectedBytes && (read = inputStream.read()) != -1) {
                        bytesReceivedCounter.incrementAndGet();
                        baos.write(read);
                        bytesRead++;
                    }

                } catch (SocketException e) {
                    if (!"socket closed".equalsIgnoreCase(e.getMessage())) {
                        e.printStackTrace();
                    }
                } catch (SSLException e) {
                    Throwable t = e;
                    if (null != e.getCause()) {
                        t = e.getCause();
                    }
                    if (!"socket closed".equalsIgnoreCase(t.getMessage())) {
                        t.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    EchoSslServerRule.this.notifyAll(); // TODO: move to joinServerThreads method
                }
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

            } catch (SocketException e) {
                if (!"socket closed".equalsIgnoreCase(e.getMessage())) {
                    e.printStackTrace();
                }
            } catch (SSLException e) {
                Throwable t = e;
                if (null != e.getCause()) {
                    t = e.getCause();
                }
                if (!"socket closed".equalsIgnoreCase(t.getMessage())) {
                    t.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }


}
