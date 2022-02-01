package io.sniffy.sql;

import io.qameta.allure.Issue;
import io.sniffy.BaseTest;
import io.sniffy.CurrentThreadSpy;
import io.sniffy.Sniffy;
import io.sniffy.Spy;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PreparedStatementInvocationHandlerTest extends BaseTest {

    @Test
    @Issue("issues/337")
    public void testExecuteBatchSuccessNoInfo() throws SQLException, IOException {

        PreparedStatement target = mock(PreparedStatement.class);
        when(target.executeBatch()).thenReturn(new int[]{Statement.SUCCESS_NO_INFO, Statement.EXECUTE_FAILED});

        PreparedStatement sniffyPreparedStatement = (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatementInvocationHandlerTest.class.getClassLoader(),
                new Class[]{PreparedStatement.class},
                new PreparedStatementInvocationHandler(target, null, "jdbc:test:connection:url", "sa", "UPDATE TAB SET FOO = ?")
        );

        try (CurrentThreadSpy spy = Sniffy.spyCurrentThread()) {

            sniffyPreparedStatement.executeBatch();

            List<SqlStats> sqlStatsList = new ArrayList<>(spy.getExecutedStatements().values());
            assertEquals(1, sqlStatsList.size());

            SqlStats sqlStats = sqlStatsList.get(0);

            assertEquals(1, sqlStats.queries.intValue());
            assertEquals(0, sqlStats.rows.intValue());

        }

    }

    @Test
    @Issue("issues/337")
    public void testExecuteLongBatchSuccessNoInfo() throws SQLException, IOException {

        PreparedStatement target = mock(PreparedStatement.class);
        when(target.executeLargeBatch()).thenReturn(new long[]{Statement.SUCCESS_NO_INFO, Statement.EXECUTE_FAILED});

        PreparedStatement sniffyPreparedStatement = (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatementInvocationHandlerTest.class.getClassLoader(),
                new Class[]{PreparedStatement.class},
                new PreparedStatementInvocationHandler(target, null, "jdbc:test:connection:url", "sa", "UPDATE TAB SET FOO = ?")
        );

        try (CurrentThreadSpy spy = Sniffy.spyCurrentThread()) {

            sniffyPreparedStatement.executeLargeBatch();

            List<SqlStats> sqlStatsList = new ArrayList<>(spy.getExecutedStatements().values());
            assertEquals(1, sqlStatsList.size());

            SqlStats sqlStats = sqlStatsList.get(0);

            assertEquals(1, sqlStats.queries.intValue());
            assertEquals(0, sqlStats.rows.intValue());

        }

    }

    @Test
    public void testExecuteBatch() throws Exception {
        try (Connection connection = openConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(INSERT_PREPARED_STATEMENT)) {
            preparedStatement.setString(1, "foo");
            preparedStatement.addBatch();
            int[] result = preparedStatement.executeBatch();
            assertEquals(1, result.length);
        }
    }

    @Test
    public void testExecuteBatchCountUpdatedRows() throws Exception {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactRows(2));
             Connection connection = openConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(INSERT_PREPARED_STATEMENT)) {
            preparedStatement.setString(1, "foo");
            preparedStatement.addBatch();
            preparedStatement.setString(1, "bar");
            preparedStatement.addBatch();
            int[] result = preparedStatement.executeBatch();
            OptionalInt rowsAffected = Arrays.stream(result).filter(i -> i != -1).reduce((a, b) -> a + b);
            assertTrue(rowsAffected.isPresent());
            assertEquals(2, rowsAffected.getAsInt());
        }
    }

    @Test
    public void testExecuteEmptyBatch() throws Exception {
        try (Connection connection = openConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(INSERT_PREPARED_STATEMENT)) {
            int[] result = preparedStatement.executeBatch();
            assertEquals(0, result.length);
        }
    }

    @Test
    public void testExecuteInsertPreparedStatement() throws Exception {
        try (Connection connection = openConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(INSERT_PREPARED_STATEMENT)) {
            preparedStatement.setString(1, "foo");
            int result = preparedStatement.executeUpdate();
            assertEquals(1, result);
        }
    }

    @Test
    public void getConnectionFromPreparedStatement() throws SQLException {
        try (Connection connection = DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa");
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM DUAL")) {
            assertEquals(connection, statement.getConnection());
        }
    }

}