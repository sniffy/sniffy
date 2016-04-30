package io.sniffy;

import io.sniffy.junit.QueryCounter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Alias for {@code @Expectation(0)}
 * @see Expectation
 * @see QueryCounter
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Expectation(count = @Count(0), threads = Threads.ANY)
public @interface NoQueriesAllowed {
}
