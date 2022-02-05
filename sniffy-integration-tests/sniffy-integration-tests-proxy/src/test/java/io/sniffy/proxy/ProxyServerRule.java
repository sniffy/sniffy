package io.sniffy.proxy;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.littleshoot.proxy.Launcher;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class ProxyServerRule implements TestRule {

    @Override
    public Statement apply(Statement base, Description description) {
        return new ProxyServerStatement(base);
    }

    public static class ProxyServerStatement extends Statement {

        private final Statement delegate;
        private Process process;

        public ProxyServerStatement(Statement delegate) {
            this.delegate = delegate;
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                startProxy();
                waitForProxy();
                System.out.println("Proxy server ready; running test");
                delegate.evaluate();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                stopProxy();
            }
        }

        private void waitForProxy() {

            boolean connected = false;

            for (int i = 0; i < 1000; i++) {
                try {
                    new Socket("localhost", 8080);
                    connected = true;
                    break;
                } catch (Exception e) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                }
            }

            if (!connected) {
                throw new RuntimeException("Failed to connect to proxy server");
            }

        }

        private void startProxy() {

            try {
                process = new ProcessBuilder(
                        System.getProperty("java.home") + "/bin/java",
                        "-classpath", System.getProperty("java.class.path"),
                        Launcher.class.getName())
                        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .start();
                System.out.println("Started proxy server process");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void stopProxy() throws InterruptedException {
            System.out.println("Destroying proxy server process");
            process.destroy();
            process.waitFor(1, TimeUnit.MINUTES);
            System.out.println("Proxy server process joined");
        }

    }

}
