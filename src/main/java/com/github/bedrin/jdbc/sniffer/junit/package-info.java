/**
 * JDBC Sniffer has a convenient integration with JUnit
 * Consider this example:
 * <pre>
 * <code>
 * public class UsageTest {
 *     // Integrate JDBC Sniffer to your test using @Rule annotation and a QueryCounter field
 *     {@literal @}Rule
 *     public final QueryCounter queryCounter = new QueryCounter();
 *
 *     // Now just add @Expectation or @Expectations annotations to define number of queries allowed for given method
 *     {@literal @}Test
 *     {@literal @}Expectation(1)
 *     public void testJUnitIntegration() throws SQLException {
 *         // Just add sniffer: in front of your JDBC connection URL in order to enable sniffer
 *         final Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
 *         // Do not make any changes in your code - just add the @Rule QueryCounter and put annotations on your test method
 *         connection.createStatement().execute("SELECT 1 FROM DUAL");
 *     }
 * }
 * }
 * </code>
 * </pre>
 * @see com.github.bedrin.jdbc.sniffer.junit.QueryCounter
 * @see com.github.bedrin.jdbc.sniffer.junit.Expectations
 * @see com.github.bedrin.jdbc.sniffer.junit.Expectation
 * @see com.github.bedrin.jdbc.sniffer.junit.NoQueriesAllowed
 */
package com.github.bedrin.jdbc.sniffer.junit;