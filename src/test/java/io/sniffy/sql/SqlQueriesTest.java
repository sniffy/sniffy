package io.sniffy.sql;

import io.sniffy.BaseTest;
import io.sniffy.Sniffer;
import io.sniffy.Spy;
import io.sniffy.WrongNumberOfQueriesError;
import org.junit.Test;

import static io.sniffy.Query.*;

public class SqlQueriesTest extends BaseTest {

    @Test
    public void testMinMaxQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.min(2).max(3))) {
            executeStatement();
            executeStatement();
            executeStatement();
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testMaxMinQueries_Exception() {
        try (Spy $= Sniffer.expect(SqlQueries.max(3).min(2))) {
            executeStatement();
        }
    }

    @Test
    public void testEqualMaxMinQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.max(2).min(2))) {
            executeStatement();
            executeStatement();
        }
    }

    @Test
    public void testExactTwoSelectQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.exact(2).select())) {
            executeStatement(SELECT);
            executeStatement(SELECT);
            executeStatement(INSERT);
        }
    }

    @Test
    public void testExactTwoInsertQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.exact(2).insert())) {
            executeStatement(INSERT);
            executeStatement(INSERT);
            executeStatement(SELECT);
        }
    }

    @Test
    public void testExactTwoUpdateQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.exact(2).update())) {
            executeStatement(UPDATE);
            executeStatement(UPDATE);
            executeStatement(SELECT);
        }
    }

    @Test
    public void testExactTwoDeleteQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.exact(2).delete())) {
            executeStatement(DELETE);
            executeStatement(DELETE);
            executeStatement(SELECT);
        }
    }

    @Test
    public void testExactTwoMergeQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.exact(2).merge())) {
            executeStatement(MERGE);
            executeStatement(MERGE);
            executeStatement(SELECT);
        }
    }

    @Test
    public void testExactTwoOtherQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.exact(2).other())) {
            executeStatement(OTHER);
            executeStatement(OTHER);
            executeStatement(SELECT);
        }
    }

    @Test
    public void testExactTwoCurrentThreadQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.exact(2).currentThread())) {
            executeStatement();
            executeStatement();
            executeStatementInOtherThread();
        }
    }

    @Test
    public void testExactTwoOtherThreadsQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.exact(2).otherThreads())) {
            executeStatementInOtherThread();
            executeStatementInOtherThread();
            executeStatement();
        }
    }

    @Test
    public void testExactTwoAnyThreadsQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.exact(2).anyThreads())) {
            executeStatementInOtherThread();
            executeStatement();
        }
    }

    @Test
    public void testExactTwoSelectCurrentThreadQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.exact(2).select().currentThread())) {
            executeStatement();
            executeStatement();
            executeStatementInOtherThread();
        }
    }

    @Test
    public void testExactTwoSelectOtherThreadsQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.exact(2).select().otherThreads())) {
            executeStatementInOtherThread();
            executeStatementInOtherThread();
            executeStatement();
        }
    }

    @Test
    public void testExactTwoSelectAnyThreadsQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.exact(2).select().anyThreads())) {
            executeStatementInOtherThread();
            executeStatement();
        }
    }

    @Test
    public void testExactTwoAnyThreadsSelectQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.exact(2).anyThreads().select())) {
            executeStatement(SELECT);
            executeStatementInOtherThread(SELECT);
        }
    }

    @Test
    public void testExactTwoAnyThreadsInsertQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.exact(2).anyThreads().insert())) {
            executeStatement(INSERT);
            executeStatementInOtherThread(INSERT);
        }
    }

    @Test
    public void testExactTwoAnyThreadsUpdateQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.exact(2).anyThreads().update())) {
            executeStatement(UPDATE);
            executeStatementInOtherThread(UPDATE);
        }
    }

    @Test
    public void testExactTwoAnyThreadsDeleteQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.exact(2).anyThreads().delete())) {
            executeStatement(DELETE);
            executeStatementInOtherThread(DELETE);
        }
    }

    @Test
    public void testExactTwoAnyThreadsMergeQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.exact(2).anyThreads().merge())) {
            executeStatement(MERGE);
            executeStatementInOtherThread(MERGE);
        }
    }

    @Test
    public void testExactTwoAnyThreadsOtherQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.exact(2).anyThreads().other())) {
            executeStatement(OTHER);
            executeStatementInOtherThread(OTHER);
        }
    }

}
