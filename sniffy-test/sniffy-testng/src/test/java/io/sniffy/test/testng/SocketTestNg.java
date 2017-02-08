package io.sniffy.test.testng;

import io.sniffy.socket.BaseSocketTest;
import io.sniffy.socket.SocketExpectation;
import io.sniffy.socket.TcpConnectionsExpectationError;
import io.sniffy.test.Count;
import io.sniffy.testng.QueryCounter;
import org.testng.annotations.*;

import java.net.UnknownHostException;

@Listeners({QueryCounter.class,MustFailListener.class})
public class SocketTestNg extends BaseSocketTest {

    @BeforeSuite
    public void beforeSuite() throws UnknownHostException {
        BaseSocketTest.resolveLocalhost();
    }

    @BeforeTest
    public void beforeTest() throws Throwable {
        echoServerRule.before();
    }

    @AfterTest
    public void afterTest() throws Throwable {
        echoServerRule.after();
    }


    @Test
    @SocketExpectation(connections = @Count(2))
    public void testAllowedOneQuery() {
        performSocketOperation();
        performSocketOperation();
    }

    @Test
    @SocketExpectation(connections = @Count(2))
    @MustFail(TcpConnectionsExpectationError.class)
    public void testAllowedOneQuery_Fails() {
        performSocketOperation();
        performSocketOperation();
        performSocketOperation();
    }


}
