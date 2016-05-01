package io.sniffy.testng;

import io.sniffy.Count;
import io.sniffy.SniffyAssertionError;
import io.sniffy.socket.BaseSocketTest;
import io.sniffy.socket.SocketExpectation;
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

    @Test(expectedExceptions = SniffyAssertionError.class)
    @SocketExpectation(connections = @Count(2))
    @MustFail(SniffyAssertionError.class)
    public void testAllowedOneQuery_Fails() {
        performSocketOperation();
        performSocketOperation();
        performSocketOperation();
    }


}
