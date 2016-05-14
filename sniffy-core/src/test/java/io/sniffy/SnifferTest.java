package io.sniffy;

import io.sniffy.sql.StatementMetaData;
import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.*;

public class SnifferTest extends BaseTest {

    @Test
    public void testExecutedStatements() throws Exception {
        int actual = Sniffer.executedStatements();
        executeStatement();
        executeStatementInOtherThread();
        assertEquals(2, Sniffer.executedStatements() - actual);
    }

    @Test
    public void testSpyExecutedStatements() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatement();
        assertEquals(1, spy.executedStatements(Threads.ANY));
    }

    @Test
    public void testSpyExecutedStatements_StackTraceTracked() throws Exception {
        Spy<?> spy = Sniffy.spy();
        executeStatement();
        Collection<StatementMetaData> statements = spy.getExecutedStatements(Threads.ANY, false).keySet();
        Iterator<StatementMetaData> statementsIt = statements.iterator();
        StatementMetaData statementMetaData = statementsIt.next();
        assertNotNull(statementMetaData.stackTrace);
        assertTrue(statementMetaData.stackTrace.contains("testSpyExecutedStatements_StackTraceTracked"));
        assertFalse(statementsIt.hasNext());
    }

    @Test
    public void testSpyExecutedStatementsCurrentThread() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatement();
        assertEquals(1, spy.executedStatements());
    }

    @Test
    public void testSpyExecutedStatementsOtherThreads() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatementInOtherThread();
        assertEquals(1, spy.executedStatements(Threads.OTHERS));
    }

    @Test
    public void testSpyExecutedDeleteStatements() throws Exception {
        Spy spy = Sniffy.spy();
        executeStatement();
        executeStatement(Query.DELETE);
        assertEquals(1, spy.executedStatements(Threads.ANY, Query.DELETE));
    }

    @Test
    public void testVerifyExact() throws Exception {
        // test positive
        Spy spy = Sniffy.spy();
        executeStatement();
        spy.verify(1);

        // test negative case 1
        spy = Sniffy.spy();
        try {
            spy.verify(1);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }

        // test negative case 2
        spy = Sniffy.spy();
        executeStatements(2);
        try {
            spy.verify(1);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testVerifyExactUpdate() throws Exception {
        // test positive
        Spy spy = Sniffy.spy();
        executeStatement(Query.SELECT);
        executeStatement(Query.INSERT);
        executeStatement(Query.UPDATE);
        executeStatement(Query.DELETE);
        executeStatement(Query.MERGE);
        executeStatement(Query.OTHER);
        spy.verify(1, Query.UPDATE);
        spy.verify(6, Query.ANY);

        // test negative case 1
        spy = Sniffy.spy();
        try {
            spy.verify(1);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }

        // test negative case 2
        spy = Sniffy.spy();
        executeStatements(2, Query.UPDATE);
        try {
            spy.verify(1, Query.UPDATE);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testVerifyMultipleFailures() throws Exception {
        try {
            Sniffer.expectAtLeast(1, Threads.CURRENT).expectAtLeast(1, Threads.OTHERS).verify();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
            assertNotNull(e.getCause());
            assertTrue(WrongNumberOfQueriesError.class.isAssignableFrom(e.getCause().getClass()));
            assertNull(e.getCause().getCause());
        }
    }

    @Test
    public void testExecuteThrowsException() throws Exception {
        try {
            Sniffer.expect(1).execute(() -> {
                throw new RuntimeException();
            });
        } catch (RuntimeException e) {
            assertNotNull(e);
            assertNull(e.getCause());
            assertEquals(1, e.getSuppressed().length);
            assertTrue(WrongNumberOfQueriesError.class.isAssignableFrom(e.getSuppressed()[0].getClass()));
        }
    }

    @Test
    public void testRunThrowsException() throws Exception {
        try {
            Sniffer.expect(1).run(() -> {
                throw new RuntimeException();
            });
        } catch (RuntimeException e) {
            assertNotNull(e);
            assertNull(e.getCause());
            assertEquals(1, e.getSuppressed().length);
            assertTrue(WrongNumberOfQueriesError.class.isAssignableFrom(e.getSuppressed()[0].getClass()));
        }
    }

    @Test
    public void testCallThrowsException() throws Exception {
        try {
            Sniffer.expect(1).call(() -> {
                throw new RuntimeException();
            });
        } catch (RuntimeException e) {
            assertNotNull(e);
            assertNull(e.getCause());
            assertEquals(1, e.getSuppressed().length);
            assertTrue(WrongNumberOfQueriesError.class.isAssignableFrom(e.getSuppressed()[0].getClass()));
        }
    }

    @Test
    public void testRecordQueriesPositive() throws Exception {
        Sniffer.run(BaseTest::executeStatement).verifyAtMostOnce();
    }

    @Test
    public void testRecordQueriesNegative() throws Exception {
        try {
            Sniffer.run(BaseTest::executeStatement).verifyNever();
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRecordQueriesNegativeQueryType() throws Exception {
        try {
            Sniffer.run(BaseTest::executeStatement).verify(1, Query.INSERT);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRecordQueriesThreadLocalPositive() throws Exception {
        Sniffer.execute(() -> {
            executeStatement();
            Thread thread = new Thread(BaseTest::executeStatement);
            thread.start();
            thread.join();
        }).verifyAtMostOnce(Threads.CURRENT);
    }

    @Test
    public void testRecordQueriesThreadLocalNegative() throws Exception {
        try {
            Sniffer.run(BaseTest::executeStatement).verifyNever(Threads.CURRENT);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRecordQueriesOtherThreadsPositive() throws Exception {
        Sniffer.execute(() -> {
            executeStatement();
            Thread thread = new Thread(BaseTest::executeStatement);
            thread.start();
            thread.join();
        }).verifyAtMostOnce(Threads.OTHERS);
    }

    @Test
    public void testRecordQueriesOtherThreadsNegative() throws Exception {
        try {
            Sniffer.execute(() -> {
                executeStatement();
                Thread thread = new Thread(BaseTest::executeStatement);
                thread.start();
                thread.join();
            }).verifyNever(Threads.OTHERS);
            fail();
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testRecordQueriesWithValue() throws Exception {
        assertEquals("test", Sniffer.call(() -> {
            executeStatement();
            return "test";
        }).verifyAtMostOnce().getValue());
    }

    @Test
    public void testTryWithResourceApi_Never() throws Exception {
        try {
            try (Spy ignored = Sniffer.expectNever()) {
                executeStatement();
                throw new RuntimeException("This is a test exception");
            }
        } catch (RuntimeException e) {
            assertEquals("This is a test exception", e.getMessage());
            assertNotNull(e.getSuppressed());
            assertEquals(1, e.getSuppressed().length);
            assertTrue(WrongNumberOfQueriesError.class.isAssignableFrom(e.getSuppressed()[0].getClass()));
        }
    }

    @Test
    public void testTryWithResourceApi_NeverOtherThread() throws Exception {
        try {
            try (Spy ignored = Sniffer.expectNever(Threads.OTHERS)) {
                executeStatementInOtherThread();
                throw new RuntimeException("This is a test exception");
            }
        } catch (Exception e) {
            assertEquals("This is a test exception", e.getMessage());
            assertNotNull(e.getSuppressed());
            assertEquals(1, e.getSuppressed().length);
            assertTrue(WrongNumberOfQueriesError.class.isAssignableFrom(e.getSuppressed()[0].getClass()));
        }
    }

    @Test
    public void testTryWithResourceApi_Exact() throws Exception {
        try (Spy ignored = Sniffer.expect(2)) {
            executeStatements(2);
        }
    }

    @Test
    public void testTryWithResourceApi_ExactCurrentThread() throws Exception {
        try (Spy ignored = Sniffer.expect(2, Threads.CURRENT)) {
            executeStatements(2);
        }
    }

    @Test
    public void testTryWithResourceApi_AtLeast() throws Exception {
        try (Spy ignored = Sniffer.expectAtLeast(2)) {
            executeStatements(5);
        }
    }

    @Test
    public void testTryWithResourceApi_AtLeastCurrentThread() throws Exception {
        try (Spy ignored = Sniffer.expectAtLeast(2, Threads.CURRENT)) {
            executeStatements(5);
            executeStatementsInOtherThread(1);
        }
    }

    @Test
    public void testTryWithResourceApi_Between() throws Exception {
        try (Spy ignored = Sniffer.expectBetween(2, 10)) {
            executeStatements(7);
        }
    }

    @Test
    public void testTryWithResourceApi_BetweenOtherThreads() throws Exception {
        try (Spy ignored = Sniffer.expectBetween(2, 10, Threads.OTHERS)) {
            executeStatements(7);
            executeStatementsInOtherThread(7);
        }
    }

    @Test
    public void testExpectNotMoreThanOne() {
        // positive
        try (Spy ignored = Sniffer.expectAtMostOnce()) {
            executeStatement();
        }
        // negative
        try {
            try (Spy ignored = Sniffer.expectAtMostOnce()) {
                executeStatements(2);
            }
        } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }
        // positive thread local
        try (Spy ignored = Sniffer.expectAtMostOnce(Threads.CURRENT)) {
            executeStatement();
            executeStatementInOtherThread();
        }
    }

    @Test
    public void testSpyClosed() throws Exception {
        Spy spy = Sniffer.execute(() -> {
            executeStatement();
            Thread thread = new Thread(BaseTest::executeStatement);
            thread.start();
            thread.join();
        }).expectAtMostOnce(Threads.OTHERS);
        spy.close();
        try {
            spy.verify(1);
        } catch (Spy.SpyClosedException e) {
            assertNotNull(e);
            assertNotNull(e.getCloseStackTrace());
            assertNotNull(e.getCloseStackTrace()[1]);
            StackTraceElement stackTraceElement = e.getCloseStackTrace()[1];
            assertEquals("io.sniffy.SnifferTest", stackTraceElement.getClassName());
            assertEquals("testSpyClosed", stackTraceElement.getMethodName());
        }
    }

}