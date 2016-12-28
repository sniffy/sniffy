package io.sniffy;

import io.sniffy.sql.NoSql;

import java.lang.annotation.*;

/**
 * Alias for {@code @Expectation(count = @Count(0), threads = Threads.CURRENT)}
 * @see Expectation
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
@Expectation(value = 0, threads = Threads.CURRENT)
@NoSql
@Deprecated
public @interface NoQueriesAllowed {}
