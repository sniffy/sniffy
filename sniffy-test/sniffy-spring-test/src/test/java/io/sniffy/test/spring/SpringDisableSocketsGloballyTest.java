package io.sniffy.test.spring;

import io.sniffy.socket.BaseSocketTest;
import io.sniffy.socket.DisableSockets;
import io.sniffy.socket.EchoServerRule;
import net.bytebuddy.implementation.bytecode.Throw;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.qatools.allure.annotations.Features;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;

import static org.junit.Assert.*;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringDisableSocketsGloballyTest.class)
@TestExecutionListeners(value = SniffySpringTestListener.class, mergeMode = MERGE_WITH_DEFAULTS)
@DisableSocketsGlobally
public class SpringDisableSocketsGloballyTest /*extends BaseSocketTest*/ {

    private final static EchoServerRule echoServerRule = new EchoServerRule(new byte[]{9,8,7,6,5,4,3,2});

    static {
        try {
            echoServerRule.before();
            Thread.sleep(1000);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Bean
    public SomeBeanUsingNetwork someBeanUsingNetwork() {
        return new SomeBeanUsingNetwork(echoServerRule.getBoundPort());
    }

    @Autowired
    private SomeBeanUsingNetwork someBeanUsingNetwork;

    public static class SomeBeanUsingNetwork {

        private final int serverPort;

        protected ConnectException connectException;

        public SomeBeanUsingNetwork(int serverPort) {
            this.serverPort = serverPort;
        }

        @PreDestroy
        public void stop() throws Exception {
            echoServerRule.after();
        }

        @PostConstruct
        public void connect() throws Exception {
            Socket socket = null;
            try {
                socket = new Socket(InetAddress.getByName(null), serverPort);
                fail("Connection should have been refused by Sniffy");
            } catch (ConnectException e) {
                connectException = e;
            } finally {
                if (null != socket) {
                    socket.close();
                }
            }
        }

    }

    @DisableSockets
    @Test
    @Features("issues/490")
    public void testAllConnectionsDisabled() {

        assertNotNull(someBeanUsingNetwork.connectException);
        assertTrue(someBeanUsingNetwork.connectException.getMessage().contains("refused by Sniffy"));


    }

}
