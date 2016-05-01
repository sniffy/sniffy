package io.sniffy.socket;

import io.sniffy.*;
import io.sniffy.junit.QueryCounter;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

/**
 * Created by bedrin on 01.05.2016.
 */
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
        thrown.expect(SniffyAssertionError.class);
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
        thrown.expect(SniffyAssertionError.class);
    }


}
