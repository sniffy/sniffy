package io.sniffy.sql;

import io.sniffy.*;
import org.junit.Test;

public class SqlQueries_Rows_Test extends BaseTest {

    @Test
    public void testNoneRows() {
        executeStatement(Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.noneRows())) {
            executeStatement(Query.DELETE);
        }
    }

    @Test
    public void testOneQueryNoRows() {
        executeStatement(Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.atMostOneQuery().delete().noneRows())) {
            executeStatement(Query.DELETE);
        }
    }

    @Test
    public void testOneMergeRow() {
        executeStatement(Query.DELETE);
        executeStatement(Query.INSERT);
        try (Spy $= Sniffer.expect(SqlQueries.atMostOneRow().currentThread().merge())) {
            executeStatement(Query.MERGE);
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testOneOtherRow_Exception() {
        executeStatement(Query.DELETE);
        executeStatement(Query.INSERT);
        try (Spy $= Sniffer.expect(SqlQueries.exactRows(1).other())) {
            executeStatement(Query.OTHER);
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testTwoQueryMinTwoRows_Exception() {
        executeStatement(Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.exactQueries(2).minRows(2).delete().anyThreads())) {
            executeStatement(Query.DELETE);
            executeStatement(Query.DELETE);
        }
    }

    @Test
    public void testTwoQueryMaxTwoMergeRowsOtherThreads() {
        executeStatement(Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.exactQueries(2).maxRows(2).otherThreads().merge())) {
            executeStatementsInOtherThread(2, Query.MERGE);
        }
    }

    @Test
    public void testMinMaxRows() {
        try (Spy $= Sniffer.expect(SqlQueries.minRows(2).maxRows(3))) {
            executeStatement();
            executeStatement();
            executeStatement();
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testMaxMinRows_Exception() {
        try (Spy $= Sniffer.expect(SqlQueries.maxRows(5).minRows(4))) {
            executeStatement();
            executeStatement();
            executeStatement();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaxMinRows_IllegalArgumentException() {
        try (Spy $= Sniffer.expect(SqlQueries.maxRows(5).minRows(6))) {
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

    @Test
    public void testMaxRowsInserted() {
        try (Spy $= Sniffer.expect(SqlQueries.maxRows(3).insert())) {
            executeStatements(4, Query.SELECT);
            executeStatements(2, Query.INSERT);
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testAtMostOneRowUpdated_Exception() {
        try (Spy $= Sniffer.expect(SqlQueries.atMostOneRow().update())) {
            executeStatements(2, Query.INSERT);
            executeStatements(2, Query.UPDATE);
            executeStatements(1, Query.DELETE);
        }
    }

    @Test
    public void testExactThreeRowsDeleteOtherThreads() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.exactRows(3).delete().otherThreads())) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatementsInOtherThread(1, Query.DELETE);
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testBetweenNineAndElevenRowSelectedAllQueries_Exception() {
        try (Spy $= Sniffer.expect(SqlQueries.rowsBetween(9,11).select().anyThreads())) {
            executeStatements(2, Query.INSERT);
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatements(1, Query.SELECT);
            executeStatementsInOtherThread(1, Query.SELECT);
        }
    }

    @Test
    public void testExactThreeRowsDeleteOtherThreadsMinMaxQueries() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.exactRows(3).delete().otherThreads().minQueries(5).maxQueries(7))) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatementsInOtherThread(6, Query.DELETE);
            executeStatements(2, Query.DELETE);
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testExactThreeRowsDeleteOtherThreadsMaxMinQueries_Exception() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.exactRows(3).otherThreads().delete().minQueries(5).maxQueries(7))) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatementsInOtherThread(4, Query.DELETE);
            executeStatements(2, Query.DELETE);
        }
    }

    @Test
    public void testExactQueriesAnyThreadsDeleteMaxMinRows() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.exactQueries(8).anyThreads().delete().maxRows(4).minRows(2))) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatementsInOtherThread(6, Query.DELETE);
            executeStatements(2, Query.DELETE);
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testExactQueriesAnyThreadsDeleteMinMaxRows_Exception() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.exactQueries(8).anyThreads().delete().minRows(1).maxRows(2))) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatementsInOtherThread(6, Query.DELETE);
            executeStatements(2, Query.DELETE);
        }
    }

    @Test
    public void testExactQueriesExactRows() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.exactQueries(2).exactRows(3))) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatements(2, Query.DELETE);
        }
    }

    @Test
    public void testExactQueriesBetweenRowsAnyThreads() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.exactQueries(5).exactRows(6).anyThreads())) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatements(2, Query.DELETE);
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testExactQueriesMaxMinRows_Exception() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.exactQueries(2).maxRows(10).minRows(9))) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatements(2, Query.DELETE);
        }
    }

    @Test
    public void testExactQueriesAtMostOneRow() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.exactQueries(2).atMostOneRow().otherThreads())) {
            executeStatements(3, Query.INSERT);
            executeStatements(2, Query.DELETE);
            executeStatementsInOtherThread(2, Query.DELETE);
        }
    }

    @Test
    public void testExactQueriesNoRows_Exception() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (Spy $= Sniffer.expect(SqlQueries.exactQueries(2).noneRows())) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatementsInOtherThread(2, Query.DELETE);
            executeStatements(2, Query.DELETE);
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testOneRowAnyThreads_Exception() {
        try (Spy $= Sniffer.expect(SqlQueries.atMostOneRow().anyThreads())) {
            executeStatement(Query.INSERT);
            executeStatementInOtherThread(Query.INSERT);
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testOneRowNoneQueries_Exception() {
        try (Spy $= Sniffer.expect(SqlQueries.atMostOneRow().noneQueries().otherThreads().insert())) {
            executeStatement(Query.INSERT);
            executeStatementInOtherThread(Query.INSERT);
        }
    }

    @Test
    public void testOneRowOneQueryInsertOtherThreads() {
        try (Spy $= Sniffer.expect(SqlQueries.atMostOneRow().atMostOneQuery().insert().otherThreads())) {
            executeStatement(Query.INSERT);
            executeStatementInOtherThread(Query.INSERT);
        }
    }

    @Test
    public void testTwoRowsMinMaxQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.rowsBetween(2,2).minQueries(1).maxQueries(2).anyThreads().insert())) {
            executeStatement(Query.INSERT);
            executeStatementInOtherThread(Query.INSERT);
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testTwoRowsMaxMinQueries_Exception() {
        try (Spy $= Sniffer.expect(SqlQueries.rowsBetween(2,2).maxQueries(4).minQueries(3).insert().anyThreads())) {
            executeStatement(Query.INSERT);
            executeStatementInOtherThread(Query.INSERT);
        }
    }

}
