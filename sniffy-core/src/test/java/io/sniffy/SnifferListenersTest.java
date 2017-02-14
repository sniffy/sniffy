package io.sniffy;

import io.sniffy.sql.SqlStatement;
import io.sniffy.sql.StatementMetaData;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Iterator;

import static org.junit.Assert.*;

public class SnifferListenersTest extends BaseTest {

    @Test
    public void testSpyRemovedOnClose() throws Exception {
        Spy spy = Sniffy.spy();
        spy.close();

        Sniffy.registeredSpies.stream().
                filter(spyReference -> spyReference.get() == spy).
                forEach(spyReference -> fail("Spy was not removed from Sniffer observers"));

    }

    @Test
    public void testStatementBatchIsLogged() throws Exception {
        try (Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:mem:", "sa", "sa")) {
            connection.createStatement().execute("CREATE TEMPORARY TABLE TEMPORARY_TABLE (BAZ VARCHAR(255))");
            try (@SuppressWarnings("unused") Spy spy = Sniffer.expectNever();
                 Statement statement = connection.createStatement()) {
                statement.addBatch("INSERT INTO TEMPORARY_TABLE (BAZ) VALUES ('foo')");
                statement.addBatch("INSERT INTO TEMPORARY_TABLE (BAZ) VALUES (LOWER('bar'))");
                statement.executeBatch();
            }
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
            assertEquals(0, e.getMinimumQueries());
            assertEquals(0, e.getMaximumQueries());
            assertEquals(1, e.getNumQueries());
            assertEquals(1, e.getExecutedStatements().size());
            assertEquals(1, e.getExecutedSqls().size());
            assertEquals(Threads.CURRENT, e.getThreadMatcher());
            assertTrue(e.getMessage().contains("INSERT INTO TEMPORARY_TABLE (BAZ) VALUES ('foo')"));
            assertTrue(e.getMessage().contains("INSERT INTO TEMPORARY_TABLE (BAZ) VALUES (LOWER('bar'))"));
        }

    }

    @Test
    public void testQueryType() throws Exception {
        try (Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:mem:", "sa", "sa")) {
            connection.createStatement().execute("CREATE TEMPORARY TABLE TEMPORARY_TABLE (BAZ VARCHAR(255))");
            try (@SuppressWarnings("unused") Spy spy = Sniffer.expectAtMostOnce(Query.INSERT)) {
                connection.createStatement().execute("INSERT INTO TEMPORARY_TABLE (BAZ) VALUES ('foo')");
                connection.createStatement().execute("INSERT INTO TEMPORARY_TABLE (BAZ) VALUES (LOWER('bar'))");
                connection.createStatement().execute("UPDATE TEMPORARY_TABLE SET BAZ = UPPER(BAZ)");
            }
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
            assertEquals(0, e.getMinimumQueries());
            assertEquals(1, e.getMaximumQueries());
            assertEquals(2, e.getNumQueries());
            assertEquals(3, e.getExecutedStatements().size());
            assertEquals(3, e.getExecutedSqls().size());
            assertEquals(Threads.CURRENT, e.getThreadMatcher());
            assertEquals(SqlStatement.INSERT, e.getQuery());
            assertTrue(e.getMessage().contains("INSERT INTO TEMPORARY_TABLE (BAZ) VALUES ('foo')"));
            assertTrue(e.getMessage().contains("INSERT INTO TEMPORARY_TABLE (BAZ) VALUES (LOWER('bar'))"));
            assertFalse(e.getMessage().contains("UPDATE TEMPORARY_TABLE SET BAZ = UPPER(BAZ)"));
        }

    }

    @Test
    public void testPreparedStatementBatchIsLogged() throws Exception {
        try (Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:mem:", "sa", "sa")) {
            connection.createStatement().execute("CREATE TEMPORARY TABLE TEMPORARY_TABLE (BAZ VARCHAR(255))");
            try (@SuppressWarnings("unused") Spy spy = Sniffer.expectNever();
                 PreparedStatement preparedStatement = connection.prepareStatement(
                         "INSERT INTO TEMPORARY_TABLE (BAZ) VALUES (?)")) {
                preparedStatement.setString(1, "foo");
                preparedStatement.addBatch();
                preparedStatement.setString(1, "bar");
                preparedStatement.addBatch();
                preparedStatement.clearBatch();
                preparedStatement.setString(1, "foo");
                preparedStatement.addBatch();
                preparedStatement.setString(1, "bar");
                preparedStatement.addBatch();
                preparedStatement.executeBatch();
            }
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
            assertEquals(0, e.getMinimumQueries());
            assertEquals(0, e.getMaximumQueries());
            assertEquals(1, e.getNumQueries());
            assertEquals(1, e.getExecutedStatements().size());
            assertEquals(1, e.getExecutedSqls().size());
            Iterator<StatementMetaData> statementsIt = e.getExecutedStatements().iterator();
            assertEquals("INSERT INTO TEMPORARY_TABLE (BAZ) VALUES (?) /*2 times*/", statementsIt.next().sql);
            assertFalse(statementsIt.hasNext());
            assertEquals(Threads.CURRENT, e.getThreadMatcher());
            assertEquals(SqlStatement.ANY, e.getQuery());
            assertTrue(e.getMessage().contains("INSERT INTO TEMPORARY_TABLE (BAZ) VALUES (?) /*2 times*/"));
        }

    }

    @Test
    public void testQueryFromOtherThread() {
        try (@SuppressWarnings("unused") Spy spy = Sniffer.expect(1)) {
            executeStatement();
            executeStatement(Query.INSERT);
            executeStatementInOtherThread();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
            assertEquals(1, e.getMinimumQueries());
            assertEquals(1, e.getMaximumQueries());
            assertEquals(2, e.getNumQueries());
            assertEquals(2, e.getExecutedStatements().size());
            assertEquals(2, e.getExecutedSqls().size());
        }
    }

}