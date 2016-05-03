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
    public void testExactTwoCurrentThreadSelectQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.exact(2).select().currentThread())) {
            executeStatement();
            executeStatement();
            executeStatementInOtherThread();
        }
    }

    @Test
    public void testExactTwoOtherThreadsSelectQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.exact(2).select().otherThreads())) {
            executeStatementInOtherThread();
            executeStatementInOtherThread();
            executeStatement();
        }
    }

    @Test
    public void testExactTwoAnyThreadsSelectQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.exact(2).select().anyThreads())) {
            executeStatementInOtherThread();
            executeStatement();
        }
    }

}
