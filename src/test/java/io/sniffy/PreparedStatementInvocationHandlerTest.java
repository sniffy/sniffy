package io.sniffy;

import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;

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
    public void testExecuteEmptyBatch() throws Exception {
        try (Connection connection = openConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "INSERT INTO PUBLIC.PROJECT (ID, NAME) VALUES (SEQ_PROJECT.NEXTVAL, ?)"
             )) {
            int[] result = preparedStatement.executeBatch();
            assertEquals(0, result.length);
        }
    }

}