package io.sniffy;

import io.sniffy.sql.SqlQueries;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class PreparedStatementInvocationHandlerTest extends BaseTest {

    @Test
    public void testExecuteBatch() throws Exception {
        try (Connection connection = openConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "INSERT INTO PUBLIC.PROJECT (ID, NAME) VALUES (SEQ_PROJECT.NEXTVAL, ?)"
             )) {
            preparedStatement.setString(1, "foo");
            preparedStatement.addBatch();
            int[] result = preparedStatement.executeBatch();
            assertEquals(1, result.length);
        }
    }

    @Test
    public void testExecuteBatchCountUpdatedRows() throws Exception {
        try (Spy $= Sniffer.expect(SqlQueries.exactRows(2));
             Connection connection = openConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "INSERT INTO PUBLIC.PROJECT (ID, NAME) VALUES (SEQ_PROJECT.NEXTVAL, ?)"
             )) {
            preparedStatement.setString(1, "foo");
            preparedStatement.addBatch();
            preparedStatement.setString(1, "bar");
            preparedStatement.addBatch();
            int[] result = preparedStatement.executeBatch();
            assertEquals(2, Arrays.stream(result).filter(i -> i != -1).reduce((a,b) -> a + b).getAsInt());
        }
    }

    @Test
    public void testExecuteEmptyBatch() throws Exception {
        try (Connection connection = openConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "INSERT INTO PUBLIC.PROJECT (ID, NAME) VALUES (SEQ_PROJECT.NEXTVAL, ?)"
             )) {
            int[] result = preparedStatement.executeBatch();
            assertEquals(0, result.length);
        }
    }

    @Test
    public void testExecuteInsertPreparedStatement() throws Exception {
        try (Connection connection = openConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "INSERT INTO PUBLIC.PROJECT (ID, NAME) VALUES (SEQ_PROJECT.NEXTVAL, ?)"
             )) {
            preparedStatement.setString(1, "foo");
            int result = preparedStatement.executeUpdate();
            assertEquals(1, result);
        }
    }

}