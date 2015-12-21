package io.sniffy.socket;

import io.sniffy.Sniffer;
import io.sniffy.Spy;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Rule;
import org.junit.Test;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class SnifferSocketImplFactoryTest {

    @Rule
    public EchoServerRule echoServerRule = new EchoServerRule();

    @Test
    public void testInstall() throws Exception {

        SnifferSocketImplFactory.install();

        try (Spy<?> s = Sniffer.spy()) {

            Socket socket = new Socket(InetAddress.getByName(null), echoServerRule.getBoundPort());

            assertTrue(socket.isConnected());
            echoServerRule.getCountDownLatch().await();

            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(new byte[]{1, 2, 3, 4});
            outputStream.flush();
            outputStream.close();

            echoServerRule.joinThreads();

            assertThat(s.getSocketOperations().entrySet(), new IsCollectionContaining<>(new BaseMatcher<Entry<String, AtomicLong>>() {
                @Override
                public boolean matches(Object item) {
                    Entry<String, AtomicLong> entry = (Entry<String, AtomicLong>) item;
                    return entry.getKey().contains("localhost");
                }

                @Override
                public void describeTo(Description description) {
                    description.appendText("a map with string key containing 'localhost'");
                }
            }));

        }

    }

    @Test
    public void testUninstall() throws Exception {

        SnifferSocketImplFactory.install();
        SnifferSocketImplFactory.uninstall();

        try (Spy<?> s = Sniffer.spy()) {

            Socket socket = new Socket(InetAddress.getByName(null), echoServerRule.getBoundPort());

            assertTrue(socket.isConnected());
            echoServerRule.getCountDownLatch().await();

            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(new byte[]{1, 2, 3, 4});
            outputStream.flush();
            outputStream.close();

            echoServerRule.joinThreads();

            assertTrue(s.getSocketOperations().isEmpty());

        }

    }

}