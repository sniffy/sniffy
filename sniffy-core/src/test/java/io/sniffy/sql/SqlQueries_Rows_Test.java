package io.sniffy.sql;

import io.sniffy.BaseTest;
import io.sniffy.Query;
import io.sniffy.Sniffy;
import io.sniffy.Spy;
import org.junit.Test;

public class SqlQueries_Rows_Test extends BaseTest {

    @Test
    public void testNoneRows() {
        executeStatement(Query.DELETE);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.noneRows())) {
            executeStatement(Query.DELETE);
        }
    }

    @Test
    public void testOneQueryNoRows() {
        executeStatement(Query.DELETE);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.atMostOneQuery().delete().noneRows())) {
            executeStatement(Query.DELETE);
        }
    }

    @Test
    public void testOneMergeRow() {
        executeStatement(Query.DELETE);
        executeStatement(Query.INSERT);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.atMostOneRow().currentThread().merge())) {
            executeStatement(Query.MERGE);
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testOneOtherRow_Exception() {
        executeStatement(Query.DELETE);
        executeStatement(Query.INSERT);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactRows(1).other())) {
            executeStatement(Query.OTHER);
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testTwoQueryMinTwoRows_Exception() {
        executeStatement(Query.DELETE);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactQueries(2).minRows(2).delete().anyThreads())) {
            executeStatement(Query.DELETE);
            executeStatement(Query.DELETE);
        }
    }

    @Test
    public void testTwoQueryMaxTwoMergeRowsOtherThreads() {
        executeStatement(Query.DELETE);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactQueries(2).maxRows(2).otherThreads().merge())) {
            executeStatementsInOtherThread(2, Query.MERGE);
        }
    }

    @Test
    public void testMinMaxRows() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.minRows(2).maxRows(3))) {
            executeStatement();
            executeStatement();
            executeStatement();
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testMaxMinRows_Exception() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.maxRows(5).minRows(4))) {
            executeStatement();
            executeStatement();
            executeStatement();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaxMinRows_IllegalArgumentException() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.maxRows(5).minRows(6))) {
            executeStatement();
            executeStatement();
            executeStatement();
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testMinMaxRows_Exception() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.minRows(2).maxRows(3))) {
            executeStatement();
            executeStatement();
            executeStatement();
            executeStatement();
        }
    }

    @Test
    public void testMinMaxRowsOtherThreads() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.maxRows(3).otherThreads())) {
            executeStatements(4);
            executeStatementsInOtherThread(2);
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testMinMaxRowsOtherThreads_Exception() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.minRows(5).otherThreads())) {
            executeStatements(6);
            executeStatementsInOtherThread(4);
        }
    }

    @Test
    public void testMaxRowsInserted() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.maxRows(3).insert())) {
            executeStatements(4, Query.SELECT);
            executeStatements(2, Query.INSERT);
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testAtMostOneRowUpdated_Exception() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.atMostOneRow().update())) {
            executeStatements(2, Query.INSERT);
            executeStatements(2, Query.UPDATE);
            executeStatements(1, Query.DELETE);
        }
    }

    @Test
    public void testExactThreeRowsDeleteOtherThreads() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactRows(3).delete().otherThreads())) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatementsInOtherThread(1, Query.DELETE);
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testBetweenNineAndElevenRowSelectedAllQueries_Exception() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.rowsBetween(9,11).select().anyThreads())) {
            executeStatements(2, Query.INSERT);
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatements(1, Query.SELECT);
            executeStatementsInOtherThread(1, Query.SELECT);
        }
    }

    @Test
    public void testExactThreeRowsDeleteOtherThreadsMinMaxQueries() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactRows(3).delete().otherThreads().minQueries(5).maxQueries(7))) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatementsInOtherThread(6, Query.DELETE);
            executeStatements(2, Query.DELETE);
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testExactThreeRowsDeleteOtherThreadsMaxMinQueries_Exception() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactRows(3).otherThreads().delete().minQueries(5).maxQueries(7))) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatementsInOtherThread(4, Query.DELETE);
            executeStatements(2, Query.DELETE);
        }
    }

    @Test
    public void testExactQueriesAnyThreadsDeleteMaxMinRows() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactQueries(8).anyThreads().delete().maxRows(4).minRows(2))) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatementsInOtherThread(6, Query.DELETE);
            executeStatements(2, Query.DELETE);
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testExactQueriesAnyThreadsDeleteMinMaxRows_Exception() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactQueries(8).anyThreads().delete().minRows(1).maxRows(2))) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatementsInOtherThread(6, Query.DELETE);
            executeStatements(2, Query.DELETE);
        }
    }

    @Test
    public void testExactQueriesExactRows() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactQueries(2).exactRows(3))) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatements(2, Query.DELETE);
        }
    }

    @Test
    public void testExactQueriesBetweenRowsAnyThreads() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactQueries(5).exactRows(6).anyThreads())) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatements(2, Query.DELETE);
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testExactQueriesMaxMinRows_Exception() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactQueries(2).maxRows(10).minRows(9))) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatements(2, Query.DELETE);
        }
    }

    @Test
    public void testExactQueriesAtMostOneRow() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactQueries(2).atMostOneRow().otherThreads())) {
            executeStatements(3, Query.INSERT);
            executeStatements(2, Query.DELETE);
            executeStatementsInOtherThread(2, Query.DELETE);
        }
    }

    @Test
    public void testExactQueriesNoRows_Exception() {
        executeStatementsInOtherThread(1, Query.DELETE);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactQueries(2).noneRows())) {
            executeStatementsInOtherThread(3, Query.INSERT);
            executeStatementsInOtherThread(2, Query.DELETE);
            executeStatements(2, Query.DELETE);
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testOneRowAnyThreads_Exception() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.atMostOneRow().anyThreads())) {
            executeStatement(Query.INSERT);
            executeStatementInOtherThread(Query.INSERT);
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testOneRowNoneQueries_Exception() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.atMostOneRow().noneQueries().otherThreads().insert())) {
            executeStatement(Query.INSERT);
            executeStatementInOtherThread(Query.INSERT);
        }
    }

    @Test
    public void testOneRowOneQueryInsertOtherThreads() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.atMostOneRow().atMostOneQuery().insert().otherThreads())) {
            executeStatement(Query.INSERT);
            executeStatementInOtherThread(Query.INSERT);
        }
    }

    @Test
    public void testTwoRowsMinMaxQueries() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.rowsBetween(2,2).minQueries(1).maxQueries(2).anyThreads().insert())) {
            executeStatement(Query.INSERT);
            executeStatementInOtherThread(Query.INSERT);
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testTwoRowsMaxMinQueries_Exception() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.rowsBetween(2,2).maxQueries(4).minQueries(3).insert().anyThreads())) {
            executeStatement(Query.INSERT);
            executeStatementInOtherThread(Query.INSERT);
        }
    }

    @Test
    public void testOneRowMerge() {
        executeStatement(Query.DELETE);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.atMostOneRow().merge().currentThread())) {
            executeStatement(Query.MERGE);
        }
    }

    @Test
    public void testOneRowOneQuerySelectOtherThread() {
        executeStatement(Query.DELETE);
        executeStatement(Query.INSERT);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.atMostOneRow().atMostOneQuery().select().otherThreads())) {
            executeStatementInOtherThread(Query.SELECT);
        }
    }

    @Test
    public void testTwoRowsTwoQueriesUpdateAnyThreads() {
        executeStatement(Query.DELETE);
        executeStatement(Query.INSERT);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.queriesBetween(2,2).exactRows(2).update().anyThreads())) {
            executeStatement(Query.UPDATE);
            executeStatementInOtherThread(Query.UPDATE);
        }
    }

    @Test
    public void testTwoRowsTwoQueriesMergeAnyThreads() {
        executeStatement(Query.DELETE);
        executeStatement(Query.INSERT);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.maxQueries(2).minQueries(2).minRows(2).maxRows(2).merge().anyThreads())) {
            executeStatement(Query.MERGE);
            executeStatementInOtherThread(Query.MERGE);
        }
    }

    @Test
    public void testOneRowOneQueryOtherCurrentThread() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.atMostOneQuery().atMostOneRow().other().currentThread())) {
            executeStatement(Query.OTHER);
        }
    }

    @Test
    public void testOneRowOneQueryCurrentThreadOther() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.atMostOneQuery().atMostOneRow().currentThread().other())) {
            executeStatement(Query.OTHER);
        }
    }

    @Test
    public void testAtMostOneSelectQueryReturnsAtMostOneRow() {
        executeStatement(Query.DELETE);
        executeStatement(Query.INSERT);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.atMostOneQuery().select().atMostOneRow())) {
            executeStatement(Query.SELECT);
        }
    }

    @Test
    public void testAtMostOneUpdateQueryReturnsValidMinMaxRows() {
        executeStatement(Query.DELETE);
        executeStatements(3, Query.INSERT);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.atMostOneQuery().merge().minRows(1).maxRows(1).currentThread())) {
            executeStatement(Query.MERGE);
        }
    }

    @Test
    public void testAtMostOneDeleteQueryReturnsValidMaxMinRows() {
        executeStatement(Query.DELETE);
        executeStatements(3, Query.INSERT);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.atMostOneQuery().delete().maxRows(4).minRows(2).otherThreads())) {
            executeStatementInOtherThread(Query.DELETE);
        }
    }

    @Test
    public void testOneRowOneSelectQuery() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.atMostOneRow().select().atMostOneQuery().currentThread())) {
            executeStatement(Query.SELECT);
        }
    }

    @Test
    public void testNoneRowsSelectNoneQueries() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.noneRows().select().noneQueries().otherThreads())) {
            executeStatementInOtherThread(Query.OTHER);
        }
    }

    @Test
    public void testNoneRowsOtherQueriesBetweenTwoAndThree() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.noneRows().other().queriesBetween(2,3).anyThreads())) {
            executeStatement(Query.OTHER);
            executeStatementInOtherThread(Query.OTHER);
        }
    }

    @Test
    public void testThreeRowsDeleteQueriesMinMax() {
        executeStatement(Query.DELETE);
        executeStatements(3, Query.INSERT);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactRows(3).delete().minQueries(1).maxQueries(2))) {
            executeStatement(Query.DELETE);
            executeStatement(Query.DELETE);
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testThreeRowsDeleteQueriesMaxMin_Exception() {
        executeStatement(Query.DELETE);
        executeStatements(3, Query.INSERT);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactRows(3).delete().maxQueries(4).minQueries(3))) {
            executeStatement(Query.DELETE);
            executeStatement(Query.DELETE);
        }
    }

    @Test
    public void testOneSelectQueryOtherThreadOneRow() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.atMostOneQuery().otherThreads().atMostOneRow())) {
            executeStatement(Query.SELECT);
        }
    }

    @Test
    public void testOneDeleteQueryAnyThreadNoneRows() {
        executeStatement(Query.DELETE);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.atMostOneQuery().anyThreads().noneRows())) {
            executeStatement(Query.DELETE);
        }
    }

    @Test
    public void testOneDeleteQueryCurrentThreadBetweenTwoAndThreeRows() {
        executeStatement(Query.DELETE);
        executeStatements(2, Query.INSERT);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.atMostOneQuery().currentThread().rowsBetween(2,3))) {
            executeStatement(Query.DELETE);
        }
    }

    @Test
    public void testOneDeleteQueryCurrentThreadBetweenMinMaxRows() {
        executeStatement(Query.DELETE);
        executeStatements(2, Query.INSERT);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.atMostOneQuery().currentThread().minRows(2).maxRows(3))) {
            executeStatement(Query.DELETE);
        }
    }

    @Test(expected = WrongNumberOfRowsError.class)
    public void testOneDeleteQueryCurrentThreadBetweenMaxMinRows_Exception() {
        executeStatement(Query.DELETE);
        executeStatements(2, Query.INSERT);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.atMostOneQuery().currentThread().maxRows(5).minRows(4))) {
            executeStatement(Query.DELETE);
        }
    }

    @Test
    public void testOneRowCurrentThreadSelectMaxMinQueries() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.atMostOneRow().currentThread().select().maxQueries(2).minQueries(1))) {
            executeStatement(Query.SELECT);
        }
    }

    @Test
    public void testOneRowOtherThreadInsertMinMaxQueries() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.atMostOneRow().otherThreads().insert().minQueries(1).maxQueries(2))) {
            executeStatementInOtherThread(Query.INSERT);
        }
    }

    @Test
    public void testTwoRowsAnyThreadUpdateMinMaxQueries() {
        executeStatement(Query.DELETE);
        executeStatements(2, Query.INSERT);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.maxRows(2).minRows(2).anyThreads().update().atMostOneQuery())) {
            executeStatementInOtherThread(Query.UPDATE);
        }
    }

    @Test
    public void testNoneRowsCurrentThreadOtherQueriesMaxMin() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.noneRows().currentThread().other().maxQueries(2).minQueries(1))) {
            executeStatement(Query.OTHER);
        }
    }

    @Test
    public void testNoneRowsCurrentThreadNoneQueriesOfTypeSelect() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.noneRows().currentThread().noneQueries().type(SqlStatement.SELECT))) {
            executeStatement(Query.OTHER);
        }
    }

    @Test
    public void testNoneRowsAnyThreadAtMostOneQueryOther() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.noneRows().anyThreads().atMostOneQuery().other())) {
            executeStatement(Query.OTHER);
        }
    }

    @Test
    public void testOneRowOtherThreadMaxMinQueriesDelete() {
        executeStatement(Query.DELETE);
        executeStatement(Query.INSERT);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactRows(1).otherThreads().maxQueries(2).minQueries(2).delete())) {
            executeStatementsInOtherThread(2, Query.DELETE);
        }
    }

    @Test
    public void testOneRowOtherThreadMinMaxQueriesDelete() {
        executeStatement(Query.DELETE);
        executeStatement(Query.INSERT);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactRows(1).otherThreads().minQueries(2).maxQueries(2).delete())) {
            executeStatementsInOtherThread(2, Query.DELETE);
        }
    }

    @Test
    public void testNoneRowsOtherThreadMinMaxQueriesOther() {
        executeStatement(Query.DELETE);
        executeStatement(Query.INSERT);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.noneRows().otherThreads().other().minQueries(2).maxQueries(2))) {
            executeStatementsInOtherThread(2, Query.OTHER);
        }
    }

    @Test
    public void testOneQueryOneRowCurrentThreadSelect() {
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactQueries(1).exactRows(1).currentThread().select())) {
            executeStatement(Query.SELECT);
        }
    }

    @Test
    public void testOneQueryOneRowCurrentThreadUpdate() {
        executeStatement(Query.DELETE);
        executeStatement(Query.INSERT);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactQueries(1).exactRows(1).currentThread().update())) {
            executeStatement(Query.UPDATE);
        }
    }

    @Test
    public void testOneQueryCurrentThreadDeleteNoneRows() {
        executeStatement(Query.DELETE);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactQueries(1).currentThread().delete().noneRows())) {
            executeStatement(Query.DELETE);
        }
    }

    @Test
    public void testOneQueryAnyThreadsDeleteAtMostOneRow() {
        executeStatement(Query.DELETE);
        executeStatement(Query.INSERT);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactQueries(1).anyThreads().delete().atMostOneRow())) {
            executeStatement(Query.DELETE);
        }
    }

    @Test
    public void testOneQueryOtherThreadsDeleteExactTwoRows() {
        executeStatement(Query.DELETE);
        executeStatements(2, Query.INSERT);
        try (@SuppressWarnings("unused") Spy $= Sniffy.expect(SqlQueries.exactQueries(1).otherThreads().delete().exactRows(2))) {
            executeStatementInOtherThread(Query.DELETE);
        }
    }

}
