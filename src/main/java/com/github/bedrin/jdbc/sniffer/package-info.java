/**
 * JDBC Sniffer allows you to validate the number of SQL queries executed by a given block of code
 * Example usage:
 * <pre>
 * {@code
 * @Test
 * public void testFunctionalApi() throws SQLException {
 *     final Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
 *     // Sniffer.execute() method executes the lambda expression and returns an instance of RecordedQueries
 *     // this class provides methods for validating the number of executed queries
 *     Sniffer.execute(() -> connection.createStatement().execute("SELECT 1 FROM DUAL")).verifyNotMoreThanOne();
 * }
 * }
 * </pre>
 * @see com.github.bedrin.jdbc.sniffer.Sniffer for the detailed API description
 */
package com.github.bedrin.jdbc.sniffer;