package io.sniffy.socket;

import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.SpyConfiguration;
import io.sniffy.ThreadMetaData;
import io.sniffy.configuration.SniffyConfiguration;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class CaptureTrafficTest extends BaseSocketTest {

    @Test
    @Stories({"issues/400", "issues/401"})
    public void testTrafficCapture() throws Exception {

        SniffyConfiguration.INSTANCE.setMonitorSocket(true);

        long prevTimeStamp = System.currentTimeMillis();

        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).build())) {

            performSocketOperation();

            Thread thread = new Thread(this::performSocketOperation);
            thread.start();
            thread.join();

            Map<SocketMetaData, List<NetworkPacket>> networkTraffic = spy.getNetworkTraffic();

            for (Map.Entry<SocketMetaData, List<NetworkPacket>> entry : networkTraffic.entrySet()) {

                SocketMetaData socketMetaData = entry.getKey();

                Protocol protocol = socketMetaData.getProtocol(); // say TCP
                String hostName = socketMetaData.getAddress().getHostName(); // say "hostname.acme.com"
                int port = socketMetaData.getAddress().getPort(); // say 443

                assertEquals(Protocol.TCP, protocol);
                assertEquals(localhost.getHostName(), hostName);
                assertEquals(echoServerRule.getBoundPort(), port);

                String stackTrace = socketMetaData.getStackTrace();// optional stacktrace for operation as a String
                ThreadMetaData threadMetaData = socketMetaData.getThreadMetaData();// information about thread which performed the operation

                assertNull(stackTrace);
                assertNull(threadMetaData);

                boolean nextPacketMustBeSent = true;

                List<NetworkPacket> networkPackets = entry.getValue();

                assertEquals(4, networkPackets.size());

                for (NetworkPacket networkPacket : networkPackets) {

                    long timestamp = networkPacket.getTimestamp(); // timestamp of operation
                    byte[] data = networkPacket.getBytes(); // captured traffic

                    assertTrue(timestamp >= prevTimeStamp);
                    prevTimeStamp = timestamp;

                    assertEquals(nextPacketMustBeSent, networkPacket.isSent());

                    if (nextPacketMustBeSent) {
                        assertArrayEquals(REQUEST, data);
                    } else {
                        assertArrayEquals(RESPONSE, data);
                    }

                    nextPacketMustBeSent = !nextPacketMustBeSent;

                }

            }

        }

    }

}
