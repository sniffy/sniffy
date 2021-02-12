package io.sniffy;


import io.sniffy.socket.AddressMatchers;
import io.sniffy.socket.NetworkPacket;
import io.sniffy.socket.Protocol;
import io.sniffy.socket.SocketMetaData;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Issue;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SpyNetworkTrafficTest {

    @Test
    @Issue("issues/424")
    public void testGetNetworkTraffic() throws Exception {

        Spy<?> spy = new Spy<>();

        @SuppressWarnings("InstantiatingAThreadWithDefaultRunMethod") Thread thread1 = new Thread();
        @SuppressWarnings("InstantiatingAThreadWithDefaultRunMethod") Thread thread2 = new Thread();

        for (byte i = 0; i < 100; i += 2) {

            spy.addNetworkTraffic(
                    new SocketMetaData(Protocol.TCP, InetSocketAddress.createUnresolved("host", 1234), 1),
                    true,
                    100,
                    null,
                    ThreadMetaData.create(thread1),
                    new byte[]{i}, 0, 1
            );

            spy.addNetworkTraffic(
                    new SocketMetaData(Protocol.TCP, InetSocketAddress.createUnresolved("host", 1234), 1),
                    false,
                    100,
                    null,
                    ThreadMetaData.create(thread2),
                    new byte[]{(byte) (i + 1)}, 0, 1
            );

        }

        Map<SocketMetaData, List<NetworkPacket>> networkTraffic = spy.getNetworkTraffic(
                Threads.ANY,
                AddressMatchers.exactAddressMatcher("host")
        );

        List<NetworkPacket> networkPackets = networkTraffic.values().iterator().next();
        assertEquals(100, networkPackets.size());

    }

}
