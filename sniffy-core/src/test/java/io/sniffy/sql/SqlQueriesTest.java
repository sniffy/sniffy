package io.sniffy.sql;

import io.sniffy.BaseTest;
import io.sniffy.Sniffy;
import io.sniffy.Spy;
import org.junit.Test;

import static io.sniffy.Query.*;

public class SqlQueriesTest extends BaseTest {

    @Test
    public void testMinMaxQueries() {
        try (Spy $= Sniffy.expect(SqlQueries.minQueries(2).maxQueries(3))) {
            executeStatement();
            executeStatement();
            executeStatement();
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testMaxMinQueries_Exception() {
        try (Spy $= Sniffy.expect(SqlQueries.maxQueries(3).minQueries(2))) {
            executeStatement();
        }
    }

    @Test
    public void testEqualMaxMinQueries() {
        try (Spy $= Sniffy.expect(SqlQueries.maxQueries(2).minQueries(2))) {
            executeStatement();
            executeStatement();
        }
    }

    @Test
    public void testExactTwoSelectQueries() {
        try (Spy $= Sniffy.expect(SqlQueries.exactQueries(2).select())) {
            executeStatement(SELECT);
            executeStatement(SELECT);
            executeStatement(INSERT);
        }
    }

    @Test
    public void testExactTwoInsertQueries() {
        try (Spy $= Sniffy.expect(SqlQueries.exactQueries(2).insert())) {
            executeStatement(INSERT);
            executeStatement(INSERT);
            executeStatement(SELECT);
        }
    }

    @Test
    public void testExactTwoUpdateQueries() {
        try (Spy $= Sniffy.expect(SqlQueries.exactQueries(2).update())) {
            executeStatement(UPDATE);
            executeStatement(UPDATE);
            executeStatement(SELECT);
        }
    }

    @Test
    public void testExactTwoDeleteQueries() {
        try (Spy $= Sniffy.expect(SqlQueries.exactQueries(2).delete())) {
            executeStatement(DELETE);
            executeStatement(DELETE);
            executeStatement(SELECT);
        }
    }

    @Test
    public void testExactTwoMergeQueries() {
        try (Spy $= Sniffy.expect(SqlQueries.exactQueries(2).merge())) {
            executeStatement(MERGE);
            executeStatement(MERGE);
            executeStatement(SELECT);
        }
    }

    @Test
    public void testExactTwoOtherQueries() {
        try (Spy $= Sniffy.expect(SqlQueries.exactQueries(2).other())) {
            executeStatement(OTHER);
            executeStatement(OTHER);
            executeStatement(SELECT);
        }
    }

    @Test
    public void testExactTwoCurrentThreadQueries() {
        try (Spy $= Sniffy.expect(SqlQueries.exactQueries(2).currentThread())) {
            executeStatement();
            executeStatement();
            executeStatementInOtherThread();
        }
    }

    @Test
    public void testExactTwoOtherThreadsQueries() {
        try (Spy $= Sniffy.expect(SqlQueries.exactQueries(2).otherThreads())) {
            executeStatementInOtherThread();
            executeStatementInOtherThread();
            executeStatement();
        }
    }

    @Test
    public void testExactTwoAnyThreadsQueries() {
        try (Spy $= Sniffy.expect(SqlQueries.exactQueries(2).anyThreads())) {
            executeStatementInOtherThread();
            executeStatement();
        }
    }

    @Test
    public void testExactTwoSelectCurrentThreadQueries() {
        try (Spy $= Sniffy.expect(SqlQueries.exactQueries(2).select().currentThread())) {
            executeStatement();
            executeStatement();
            executeStatementInOtherThread();
        }
    }

    @Test
    public void testExactTwoSelectOtherThreadsQueries() {
        try (Spy $= Sniffy.expect(SqlQueries.exactQueries(2).select().otherThreads())) {
            executeStatementInOtherThread();
            executeStatementInOtherThread();
            executeStatement();
        }
    }

    @Test
    public void testExactTwoSelectAnyThreadsQueries() {
        try (Spy $= Sniffy.expect(SqlQueries.exactQueries(2).select().anyThreads())) {
            executeStatementInOtherThread();
            executeStatement();
        }
    }

    @Test
    public void testExactTwoAnyThreadsSelectQueries() {
        try (Spy $= Sniffy.expect(SqlQueries.exactQueries(2).anyThreads().select())) {
            executeStatement(SELECT);
            executeStatementInOtherThread(SELECT);
        }
    }

    @Test
    public void testExactTwoAnyThreadsInsertQueries() {
        try (Spy $= Sniffy.expect(SqlQueries.exactQueries(2).anyThreads().insert())) {
            executeStatement(INSERT);
            executeStatementInOtherThread(INSERT);
        }
    }

    @Test
    public void testExactTwoAnyThreadsUpdateQueries() {
        try (Spy $= Sniffy.expect(SqlQueries.exactQueries(2).anyThreads().update())) {
            executeStatement(UPDATE);
            executeStatementInOtherThread(UPDATE);
        }
    }

    @Test
    public void testExactTwoAnyThreadsDeleteQueries() {
        try (Spy $= Sniffy.expect(SqlQueries.exactQueries(2).anyThreads().delete())) {
            executeStatement(DELETE);
            executeStatementInOtherThread(DELETE);
        }
    }

    @Test
    public void testExactTwoAnyThreadsMergeQueries() {
        try (Spy $= Sniffy.expect(SqlQueries.exactQueries(2).anyThreads().merge())) {
            executeStatement(MERGE);
            executeStatementInOtherThread(MERGE);
        }
    }

    @Test
    public void testExactTwoAnyThreadsOtherQueries() {
        try (Spy $= Sniffy.expect(SqlQueries.exactQueries(2).anyThreads().other())) {
            executeStatement(OTHER);
            executeStatementInOtherThread(OTHER);
        }
    }

}
