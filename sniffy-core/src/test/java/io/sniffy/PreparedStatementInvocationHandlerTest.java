package io.sniffy;

import io.sniffy.sql.SqlQueries;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.OptionalInt;

import static org.junit.Assert.*;

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
        try (@SuppressWarnings("unused") Spy $= Sniffer.expect(SqlQueries.exactRows(2));
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

}