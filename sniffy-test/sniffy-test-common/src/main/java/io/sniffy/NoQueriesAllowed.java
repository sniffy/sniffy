package io.sniffy;

import io.sniffy.sql.NoSql;

import java.lang.annotation.*;

/**
 * @see NoSql
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
@Expectation(value = 0, threads = Threads.CURRENT)
@NoSql
@Deprecated
public @interface NoQueriesAllowed {}
