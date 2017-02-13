package io.sniffy.sql;

import io.sniffy.Threads;
import io.sniffy.test.Count;

import java.lang.annotation.*;

/**
 * Alias for {@code @SqlExpectation(count = @Count(0), threads = Threads.CURRENT)}
 * @see SqlExpectation
 * @since 3.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
@SqlExpectation(count = @Count(0), threads = Threads.CURRENT)
public @interface NoSql {
}
