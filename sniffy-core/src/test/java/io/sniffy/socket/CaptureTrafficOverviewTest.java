package io.sniffy.socket;

import io.sniffy.*;
import io.sniffy.configuration.SniffyConfiguration;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class CaptureTrafficOverviewTest extends BaseSocketTest {

    static {
        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
    }

    // tag::CaptureTrafficOverview[]
    @Test
    public void testCaptureTraffic() throws Exception {

        try (Spy<?> spy = Sniffy.spy(
                SpyConfiguration.builder().captureNetworkTraffic(true).build()) // <1>
        ) {

            performSocketOperation(); // <2>

            Map<SocketMetaData, List<NetworkPacket>> networkTraffic = spy.getNetworkTraffic( // <3>
                    Threads.ANY, // <4>
                    AddressMatchers.anyAddressMatcher(), // <5>
                    GroupingOptions.builder().
                            groupByThread(false). // <6>
                            groupByStackTrace(false). // <7>
                            groupByConnection(false). // <8>
                            build()
            );

            assertEquals(1, networkTraffic.size());

            for (Map.Entry<SocketMetaData, List<NetworkPacket>> entry : networkTraffic.entrySet()) {

                SocketMetaData socketMetaData = entry.getKey(); // <9>

                Protocol protocol = socketMetaData.getProtocol(); // say TCP
                String hostName = socketMetaData.getAddress().getHostName(); // say "hostname.acme.com"
                int port = socketMetaData.getAddress().getPort(); // say 443

                List<NetworkPacket> networkPackets = entry.getValue(); // <10>

                assertArrayEquals(REQUEST, networkPackets.get(0).getBytes());
                assertTrue(networkPackets.get(0).isSent());

                assertArrayEquals(RESPONSE, networkPackets.get(1).getBytes());
                assertFalse(networkPackets.get(1).isSent());

            }

        }

    }
    // end::CaptureTrafficOverview[]
}
