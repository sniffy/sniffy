package io.sniffy.test.spring;

import io.sniffy.socket.BaseSocketTest;
import io.sniffy.socket.SocketExpectation;
import io.sniffy.socket.TcpConnectionsExpectationError;
import io.sniffy.spring.QueryCounterListener;
import io.sniffy.test.Count;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringIntegrationSocketTest.class)
@TestExecutionListeners(QueryCounterListener.class)
public class SpringIntegrationSocketTest extends BaseSocketTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();


    @Test
    @SocketExpectation(connections = @Count(2))
    public void testExactConnections() {
        performSocketOperation();
        performSocketOperation();
    }

    @Test
    @SocketExpectation(connections = @Count(3))
    public void testExactConnections_Failed() {
        performSocketOperation();
        performSocketOperation();
        thrown.expect(TcpConnectionsExpectationError.class);
    }

}