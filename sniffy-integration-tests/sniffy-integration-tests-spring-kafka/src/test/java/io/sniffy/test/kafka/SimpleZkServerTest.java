package io.sniffy.test.kafka;

import io.sniffy.socket.DisableSockets;
import io.sniffy.test.junit.SniffyRule;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerMain;
import org.junit.Rule;
import org.junit.Test;

public class SimpleZkServerTest {

    @Rule
    public final SniffyRule sniffy = new SniffyRule();

    @Test
    public void testZookeperServer() throws Exception {

        System.setProperty("log4j.configuration","file:/home/bedrin/opt/apache-zookeeper-3.6.2-bin/conf/log4j.properties");

        ZooKeeperServerMain.main(new String[]{"/home/bedrin/opt/apache-zookeeper-3.6.2-bin/conf/zoo.cfg"});

    }

}
