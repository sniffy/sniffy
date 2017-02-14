package io.sniffy.test.junit;

import io.sniffy.junit.QueryCounter;
import io.sniffy.socket.BaseSocketTest;
import io.sniffy.socket.SocketExpectation;
import io.sniffy.socket.TcpConnectionsExpectationError;
import io.sniffy.test.Count;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class QueryCounterSocketExpectationsTest extends BaseSocketTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Rule
    public final QueryCounter queryCounter = new QueryCounter();

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

    @Test
    @SocketExpectation(connections = @Count(2))
    public void testMinConnections() {
        performSocketOperation();
        performSocketOperation();
    }

    @Test
    @SocketExpectation(connections = @Count(3))
    public void testMinConnections_Failed() {
        performSocketOperation();
        performSocketOperation();
        thrown.expect(TcpConnectionsExpectationError.class);
    }


}
