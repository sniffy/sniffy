/**
 * Sniffy allows you to validate the number of SQL queries executed by a given block of code
 * Example usage:
 * <pre>
 * <code>
 *     {@literal @}Test
 *     public void testVerifyApi() throws SQLException {
 *         // Just add sniffer: in front of your JDBC connection URL in order to enable sniffer
 *         Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:mem:", "sa", "sa");
 *         // Spy holds the amount of queries executed till the given amount of time
 *         // It acts as a base for further assertions
 *         Spy spy = Sniffer.spy();
 *         // You do not need to modify your JDBC code
 *         connection.createStatement().execute("SELECT 1 FROM DUAL");
 *         assertEquals(1, spy.executedStatements());
 *         // Sniffer.verifyAtMostOnce() throws an AssertionError if more than one query was executed;
 *         spy.verifyAtMostOnce();
 *         // Sniffer.verifyNever(Threads.OTHERS) throws an AssertionError if at least one query was executed
 *         // by the thread other than then current one
 *         spy.verifyNever(Threads.OTHERS);
 *     }
 *
 *     {@literal @}Test
 *     public void testFunctionalApi() throws SQLException {
 *         // Just add sniffer: in front of your JDBC connection URL in order to enable sniffer
 *         final Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:mem:", "sa", "sa");
 *         // Sniffer.execute() method executes the lambda expression and returns an instance of Spy
 *         // which provides methods for validating the number of executed queries in given lambda
 *         Sniffer.execute(() -{@literal >} connection.createStatement().execute("SELECT 1 FROM DUAL")).verifyAtMostOnce();
 *     }
 *
 *     {@literal @}Test
 *     public void testResourceApi() throws SQLException {
 *         // Just add sniffer: in front of your JDBC connection URL in order to enable sniffer
 *         final Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:mem:", "sa", "sa");
 *         // You can use Sniffer in a try-with-resource block using expect methods instead of verify
 *         // When the try-with-resource block is completed, Sniffy will verify all the expectations defined
 *         try ( {@literal @}SuppressWarnings("unused") Spy s = Sniffer.expectAtMostOnce().expectNever(Threads.OTHERS);
 *         Statement statement = connection.createStatement()) {
 *             statement.execute("SELECT 1 FROM DUAL");
 *         }
 *     }
 * </code>
 * </pre>
 * @see io.sniffy.Sniffer for the detailed API description
 */
package io.sniffy;