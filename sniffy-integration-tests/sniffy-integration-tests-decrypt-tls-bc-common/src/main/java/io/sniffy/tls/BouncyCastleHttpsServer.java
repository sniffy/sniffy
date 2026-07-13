package io.sniffy.tls;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;

/**
 * Single-request loopback HTTPS server used by the TLS integration tests.
 */
public final class BouncyCastleHttpsServer implements AutoCloseable {

    public static final String HOST = "127.0.0.1";
    public static final String PATH = "/bcjsse-fixture";
    public static final String RESPONSE_BODY = "sniffy-bcjsse-response";

    private static final String READY_PREFIX = "SNIFFY_HTTPS_READY ";
    private static final String REQUEST_PREFIX = "SNIFFY_HTTPS_REQUEST ";
    private static final Charset US_ASCII = Charset.forName("US-ASCII");
    private static final char[] KEY_PASSWORD = "sniffy-test".toCharArray();

    private final Process process;
    private final BufferedReader processOutput;
    private final int port;
    private final X509Certificate certificate;

    private boolean outputConsumed;
    private boolean closed;
    private String request;

    private BouncyCastleHttpsServer(Process process, BufferedReader processOutput,
                                    int port, X509Certificate certificate) {
        this.process = process;
        this.processOutput = processOutput;
        this.port = port;
        this.certificate = certificate;
    }

    public static BouncyCastleHttpsServer start() throws Exception {
        Process process = new ProcessBuilder(
                System.getProperty("java.home") + "/bin/java",
                "-classpath", System.getProperty("java.class.path"),
                ServerProcess.class.getName())
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
        BufferedReader processOutput = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));

        try {
            String line;
            while (null != (line = processOutput.readLine())) {
                if (line.startsWith(READY_PREFIX)) {
                    String ready = line.substring(READY_PREFIX.length());
                    int separator = ready.indexOf(' ');
                    int port = Integer.parseInt(ready.substring(0, separator));
                    byte[] encodedCertificate = Base64.getDecoder().decode(ready.substring(separator + 1));
                    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                    X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(
                            new ByteArrayInputStream(encodedCertificate));
                    return new BouncyCastleHttpsServer(process, processOutput, port, certificate);
                }
                System.out.println(line);
            }
            throw new IllegalStateException("HTTPS fixture process exited before reporting its port with exit code " +
                    process.waitFor());
        } catch (Exception e) {
            process.destroy();
            process.waitFor();
            try {
                processOutput.close();
            } catch (IOException closeFailure) {
                e.addSuppressed(closeFailure);
            }
            throw e;
        }
    }

    public int getPort() {
        return port;
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
        consumeProcessOutput();
        if (null == request) {
            throw new AssertionError("The local HTTPS fixture did not receive a request");
        }
        return request;
    }

    private void consumeProcessOutput() throws Exception {
        if (outputConsumed) {
            return;
        }

        String line;
        while (null != (line = processOutput.readLine())) {
            if (line.startsWith(REQUEST_PREFIX)) {
                byte[] encodedRequest = Base64.getDecoder().decode(line.substring(REQUEST_PREFIX.length()));
                request = new String(encodedRequest, US_ASCII);
            } else {
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        outputConsumed = true;
        if (0 != exitCode) {
            throw new IllegalStateException("HTTPS fixture process exited with code " + exitCode);
        }
    }

    private static ServerFixture createServerFixture() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        X509Certificate certificate = selfSignedCertificate(keyPair);

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        keyStore.setKeyEntry("server", keyPair.getPrivate(), KEY_PASSWORD,
                new Certificate[]{certificate});

        BouncyCastleJsseProvider jsseProvider = new BouncyCastleJsseProvider();
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X.509", jsseProvider);
        keyManagerFactory.init(keyStore, KEY_PASSWORD);

        SSLContext sslContext = SSLContext.getInstance("TLSv1.2", jsseProvider);
        sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());
        SSLServerSocket serverSocket = (SSLServerSocket) sslContext.getServerSocketFactory()
                .createServerSocket(0, 1, InetAddress.getByName(HOST));
        serverSocket.setEnabledProtocols(new String[]{"TLSv1.2"});
        return new ServerFixture(serverSocket, certificate);
    }

    private static String serve(SSLServerSocket serverSocket) throws Exception {
        Socket acceptedSocket = null;
        try {
            acceptedSocket = serverSocket.accept();
            String request = readHeaders(acceptedSocket.getInputStream());
            byte[] body = RESPONSE_BODY.getBytes(US_ASCII);
            byte[] response = ("HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Length: " + body.length + "\r\n" +
                    "Connection: close\r\n\r\n" + RESPONSE_BODY).getBytes(US_ASCII);
            OutputStream outputStream = acceptedSocket.getOutputStream();
            outputStream.write(response);
            outputStream.flush();
            return request;
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
        if (closed) {
            return;
        }
        closed = true;

        try {
            if (!outputConsumed) {
                if (process.isAlive()) {
                    process.destroy();
                    process.waitFor();
                    outputConsumed = true;
                } else {
                    consumeProcessOutput();
                }
            }
        } finally {
            processOutput.close();
        }
    }

    private static class ServerFixture {

        private final SSLServerSocket serverSocket;
        private final X509Certificate certificate;

        private ServerFixture(SSLServerSocket serverSocket, X509Certificate certificate) {
            this.serverSocket = serverSocket;
            this.certificate = certificate;
        }

    }

    public static class ServerProcess {

        public static void main(String[] args) throws Exception {
            ServerFixture fixture = createServerFixture();
            System.out.println(READY_PREFIX + fixture.serverSocket.getLocalPort() + " " +
                    Base64.getEncoder().encodeToString(fixture.certificate.getEncoded()));
            System.out.flush();

            String request = serve(fixture.serverSocket);
            System.out.println(REQUEST_PREFIX + Base64.getEncoder().encodeToString(request.getBytes(US_ASCII)));
            System.out.flush();
        }

    }

}
