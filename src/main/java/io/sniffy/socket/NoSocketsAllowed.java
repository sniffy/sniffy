package io.sniffy.socket;

import io.sniffy.test.Count;
import io.sniffy.junit.QueryCounter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Alias for {@code @SocketExpectation(connections = @Count(0))}
 * @see SocketExpectation
 * @see QueryCounter
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@SocketExpectation(connections = @Count(0))
public @interface NoSocketsAllowed {
}
