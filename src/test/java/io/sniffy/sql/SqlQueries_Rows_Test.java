package io.sniffy.sql;

import io.sniffy.*;
import org.junit.Test;

public class SqlQueries_Rows_Test extends BaseTest {

    @Test
    public void testMinMaxRows() {
        try (Spy $= Sniffer.expect(SqlQueries.minRows(2).maxRows(3))) {
            executeStatement();
            executeStatement();
            executeStatement();
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testMinMaxRows_Exception() {
        try (Spy $= Sniffer.expect(SqlQueries.minRows(2).maxRows(3))) {
            executeStatement();
            executeStatement();
            executeStatement();
            executeStatement();
        }
    }

    @Test
    public void testMinMaxRowsOtherThreads() {
        try (Spy $= Sniffer.expect(SqlQueries.maxRows(3).otherThreads())) {
            executeStatements(4);
            executeStatementsInOtherThread(2);
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testMinMaxRowsOtherThreads_Exception() {
        try (Spy $= Sniffer.expect(SqlQueries.minRows(5).otherThreads())) {
            executeStatements(6);
            executeStatementsInOtherThread(4);
        }
    }

}
