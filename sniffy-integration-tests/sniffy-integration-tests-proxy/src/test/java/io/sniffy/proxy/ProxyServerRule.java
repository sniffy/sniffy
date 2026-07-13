package io.sniffy.proxy;

import net.bytebuddy.utility.privilege.GetSystemPropertyAction;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;

import static java.security.AccessController.doPrivileged;

public class ProxyServerRule implements TestRule {

    private static final String PORT_PREFIX = "SNIFFY_PROXY_PORT=";

    private int port;

    @Override
    public Statement apply(Statement base, Description description) {
        return new ProxyServerStatement(base);
    }

    public int getPortNumber() {
        return port;
    }

    private class ProxyServerStatement extends Statement {

        private final Statement delegate;
        private Process process;
        private Thread outputDrainer;

        public ProxyServerStatement(Statement delegate) {
            this.delegate = delegate;
        }

        @Override
        public void evaluate() throws Throwable {
            Throwable failure = null;
            try {
                startProxy();
                delegate.evaluate();
            } catch (Throwable e) {
                failure = e;
                throw e;
            } finally {
                try {
                    stopProxy();
                } catch (Throwable cleanupFailure) {
                    if (null != failure) {
                        failure.addSuppressed(cleanupFailure);
                    } else {
                        throw cleanupFailure;
                    }
                }
            }
        }

        private void startProxy() throws Exception {
            process = new ProcessBuilder(
                    doPrivileged(new GetSystemPropertyAction("java.home")) + "/bin/java",
                    "-classpath", System.getProperty("java.class.path"),
                    ProxyServerProcess.class.getName())
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start();

            BufferedReader processOutput = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String line;
            while (null != (line = processOutput.readLine())) {
                if (line.startsWith(PORT_PREFIX)) {
                    port = Integer.parseInt(line.substring(PORT_PREFIX.length()));
                    startOutputDrainer(processOutput);
                    return;
                }
                System.out.println(line);
            }

            throw new IllegalStateException("Proxy process exited before reporting its port with exit code " +
                    process.waitFor());
        }

        private void startOutputDrainer(final BufferedReader processOutput) {
            outputDrainer = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String line;
                        while (null != (line = processOutput.readLine())) {
                            System.out.println(line);
                        }
                    } catch (IOException e) {
                        if (process.isAlive()) {
                            e.printStackTrace();
                        }
                    }
                }
            }, "sniffy-proxy-output");
            outputDrainer.setDaemon(true);
            outputDrainer.start();
        }

        private void stopProxy() throws Exception {
            if (null == process) {
                return;
            }

            Exception failure = null;
            try {
                process.getOutputStream().close();
            } catch (Exception e) {
                failure = e;
            }

            try {
                int exitCode = process.waitFor();
                if (0 != exitCode) {
                    throw new IllegalStateException("Proxy process exited with code " + exitCode);
                }
            } catch (Exception e) {
                failure = suppress(failure, e);
            }

            if (null != outputDrainer) {
                try {
                    outputDrainer.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failure = suppress(failure, e);
                }
            }

            if (null != failure) {
                throw failure;
            }
        }

        private Exception suppress(Exception first, Exception later) {
            if (null == first) {
                return later;
            }
            first.addSuppressed(later);
            return first;
        }

    }

    public static class ProxyServerProcess {

        public static void main(String[] args) throws Exception {
            HttpProxyServer proxyServer = null;
            try {
                proxyServer = DefaultHttpProxyServer.bootstrap()
                        .withAddress(new InetSocketAddress("127.0.0.1", 0))
                        .start();
                System.out.println(PORT_PREFIX + proxyServer.getListenAddress().getPort());
                System.out.flush();
                System.in.read();
            } finally {
                if (null != proxyServer) {
                    proxyServer.stop();
                }
            }
        }

    }

}
