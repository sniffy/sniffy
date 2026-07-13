package io.sniffy.tls;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Single-request loopback HTTPS server used by the Bouncy Castle integration tests.
 */
public final class BouncyCastleHttpsServer implements AutoCloseable, Runnable {

    public static final String HOST = "127.0.0.1";
    public static final String PATH = "/bcjsse-fixture";
    public static final String RESPONSE_BODY = "sniffy-bcjsse-response";

    private static final Charset US_ASCII = Charset.forName("US-ASCII");
    private static final char[] KEY_PASSWORD = "sniffy-test".toCharArray();

    private final SSLServerSocket serverSocket;
    private final X509Certificate certificate;
    private final Thread serverThread;

    private volatile Socket acceptedSocket;
    private volatile Throwable failure;
    private volatile String request;

    private BouncyCastleHttpsServer(SSLServerSocket serverSocket, X509Certificate certificate) {
        this.serverSocket = serverSocket;
        this.certificate = certificate;
        this.serverThread = new Thread(this, "sniffy-bcjsse-https-server");
        this.serverThread.setDaemon(true);
        this.serverThread.start();
    }

    public static BouncyCastleHttpsServer start() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        X509Certificate certificate = selfSignedCertificate(keyPair);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        keyStore.setKeyEntry("server", keyPair.getPrivate(), KEY_PASSWORD,
                new Certificate[]{certificate});

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, KEY_PASSWORD);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());
        SSLServerSocket serverSocket = (SSLServerSocket) sslContext.getServerSocketFactory()
                .createServerSocket(0, 1, InetAddress.getByName(HOST));
        serverSocket.setEnabledProtocols(new String[]{"TLSv1.2"});
        return new BouncyCastleHttpsServer(serverSocket, certificate);
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

    public String getAddress() {
        return HOST + ":" + getPort();
    }

    public TrustManager[] createTrustManagers() throws Exception {
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("server", certificate);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        return trustManagerFactory.getTrustManagers();
    }

    public String awaitRequest() throws Exception {
        serverThread.join();
        rethrowFailure();
        if (null == request) {
            throw new AssertionError("The local HTTPS fixture did not receive a request");
        }
        return request;
    }

    @Override
    public void run() {
        try {
            acceptedSocket = serverSocket.accept();
            request = readHeaders(acceptedSocket.getInputStream());
            byte[] body = RESPONSE_BODY.getBytes(US_ASCII);
            byte[] response = ("HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Length: " + body.length + "\r\n" +
                    "Connection: close\r\n\r\n" + RESPONSE_BODY).getBytes(US_ASCII);
            OutputStream outputStream = acceptedSocket.getOutputStream();
            outputStream.write(response);
            outputStream.flush();
        } catch (Throwable e) {
            if (!serverSocket.isClosed()) {
                failure = e;
            }
        } finally {
            closeQuietly(acceptedSocket);
            closeQuietly(serverSocket);
        }
    }

    private static String readHeaders(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int matched = 0;
        int value;
        while ((value = inputStream.read()) != -1) {
            outputStream.write(value);
            if (((matched == 0 || matched == 2) && value == '\r') ||
                    ((matched == 1 || matched == 3) && value == '\n')) {
                matched++;
                if (matched == 4) {
                    break;
                }
            } else {
                matched = value == '\r' ? 1 : 0;
            }
        }
        return new String(outputStream.toByteArray(), US_ASCII);
    }

    private static X509Certificate selfSignedCertificate(KeyPair keyPair) throws Exception {
        BouncyCastleProvider provider = new BouncyCastleProvider();
        X500Name subject = new X500Name("CN=" + HOST);
        long now = System.currentTimeMillis();
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                subject,
                BigInteger.valueOf(now),
                new Date(now - 60000L),
                new Date(now + 86400000L),
                subject,
                keyPair.getPublic());
        builder.addExtension(Extension.subjectAlternativeName, false,
                new GeneralNames(new GeneralName(GeneralName.iPAddress, HOST)));
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                .setProvider(provider)
                .build(keyPair.getPrivate());
        return new JcaX509CertificateConverter().setProvider(provider)
                .getCertificate(builder.build(signer));
    }

    private void rethrowFailure() throws Exception {
        if (failure instanceof Exception) {
            throw (Exception) failure;
        }
        if (null != failure) {
            throw new AssertionError("Local HTTPS fixture failed", failure);
        }
    }

    private static void closeQuietly(Socket socket) {
        if (null != socket) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // Best-effort fixture cleanup.
            }
        }
    }

    private static void closeQuietly(ServerSocket socket) {
        if (null != socket) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // Best-effort fixture cleanup.
            }
        }
    }

    @Override
    public void close() throws Exception {
        closeQuietly(acceptedSocket);
        closeQuietly(serverSocket);
        serverThread.join();
        rethrowFailure();
    }
}
