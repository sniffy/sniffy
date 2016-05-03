package io.sniffy;

import io.sniffy.test.Count;
import io.sniffy.test.junit.SniffyRule;

import java.lang.annotation.*;

/**
 * Alias for {@code @Expectation(count = @Count(0), threads = Threads.CURRENT)}
 * @see Expectation
 * @see SniffyRule
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
@Expectation(count = @Count(0), threads = Threads.CURRENT)
public @interface NoQueriesAllowed {
}
