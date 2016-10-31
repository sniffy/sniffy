package io.sniffy;

import io.sniffy.sql.SqlQueries;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.OptionalInt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PreparedStatementInvocationHandlerTest extends BaseTest {

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