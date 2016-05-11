package io.sniffy.testng;

import io.sniffy.Expectation;
import io.sniffy.Expectations;
import io.sniffy.NoQueriesAllowed;
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
 * @see Expectations
 * @see Expectation
 * @see NoQueriesAllowed
 * @since 2.1
 */
@Deprecated
public class QueryCounter extends SniffyTestNgListener {
}
