package io.sniffy.test.spring;

import io.sniffy.socket.BaseSocketTest;
import io.sniffy.socket.DisableSockets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.qatools.allure.annotations.Features;

import java.net.ConnectException;
import java.net.Socket;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringDisableSocketsTest.class)
@TestExecutionListeners(SniffySpringTestListener.class)
public class SpringDisableSocketsTest extends BaseSocketTest {

    @DisableSockets
    @Test
    @Features("issues/224")
    public void testAllConnectionsDisabled() {

        try {
            new Socket(localhost, echoServerRule.getBoundPort());
            fail("Connection should have been refused by Sniffy");
        } catch (ConnectException e) {
            assertTrue(e.getMessage().contains("refused by Sniffy"));
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            echoServerRule.joinThreads();
        }

    }

}
