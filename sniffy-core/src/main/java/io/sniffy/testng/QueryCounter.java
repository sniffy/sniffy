package io.sniffy.testng;

import io.sniffy.sql.NoSql;
import io.sniffy.sql.SqlExpectation;
import io.sniffy.sql.SqlExpectations;
import io.sniffy.test.testng.SniffyTestNgListener;

/**
 * Provides integration with TestNG. Add {@code QueryCounter} as a listener to your TestNG test:
 * <pre>
 * <code>
 * {@literal @}Listeners(QueryCounter.class)
 * public class SampleTestNgTestSuite {
 *     // ... here goes some test methods
 * }
 * </code>
 * </pre>
 * @see SqlExpectations
 * @see SqlExpectation
 * @see NoSql
 * @since 2.1
 */
@Deprecated
public class QueryCounter extends SniffyTestNgListener {
}
