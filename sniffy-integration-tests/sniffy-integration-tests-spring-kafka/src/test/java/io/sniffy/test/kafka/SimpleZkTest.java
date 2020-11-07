package io.sniffy.test.kafka;

import io.sniffy.socket.DisableSockets;
import io.sniffy.test.junit.SniffyRule;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Rule;
import org.junit.Test;

public class SimpleZkTest {

    @Rule
    public final SniffyRule sniffy = new SniffyRule();

    @Test
    //@DisableSockets
    public void testZookeperClient() throws Exception {

        ZooKeeper zk = new ZooKeeper("localhost:2181", 3000, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                System.out.println(event);
            }
        });

        System.out.println(zk.exists("/node1", true));
        zk.close();

    }

}
