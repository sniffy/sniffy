package io.sniffy.junit;

import io.sniffy.socket.NoSocketsAllowed;
import io.sniffy.socket.SocketExpectation;
import io.sniffy.socket.SocketExpectations;
import io.sniffy.test.junit.SniffyRule;

/**
 * Provides integration with JUnit. Add following field to your test class:
 * <pre>
 * <code>
 * {@literal @}Rule
 * public final QueryCounter queryCounter = new QueryCounter();
 * }
 * </code>
 * </pre>
 * @see SocketExpectations
 * @see SocketExpectation
 * @see NoSocketsAllowed
 * @since 1.3
 */
@Deprecated
public class QueryCounter extends SniffyRule {

}
