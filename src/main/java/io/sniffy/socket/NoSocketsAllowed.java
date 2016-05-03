package io.sniffy.socket;

import io.sniffy.test.Count;
import io.sniffy.test.junit.SniffyRule;

import java.lang.annotation.*;

/**
 * Alias for {@code @SocketExpectation(connections = @Count(0))}
 * @see SocketExpectation
 * @see SniffyRule
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
@SocketExpectation(connections = @Count(0))
public @interface NoSocketsAllowed {
}
