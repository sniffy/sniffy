package io.sniffy;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SniffyTest extends BaseTest {

    @Before
    public void clearSpies() {
        Sniffy.registeredSpies.clear();
        Sniffy.currentThreadSpies.clear();
    }

    @Test
    public void hasSpiesFromOtherThreads() throws Exception {
        try (@SuppressWarnings("unused") Spy spy = Sniffy.spy()) {
            AtomicBoolean hasSpies = new AtomicBoolean();
            Thread thread = new Thread(() -> hasSpies.set(Sniffy.hasSpies()));
            thread.start();
            thread.join();
            assertTrue(hasSpies.get());
        }
    }

    @Test
    public void hasNotSpiesFromOtherThreads() throws Exception {
        try (@SuppressWarnings("unused") CurrentThreadSpy spy = Sniffy.spyCurrentThread()) {
            AtomicBoolean hasSpies = new AtomicBoolean();
            Thread thread = new Thread(() -> hasSpies.set(Sniffy.hasSpies()));
            thread.start();
            thread.join();
            assertFalse(hasSpies.get());
        }
    }

    @Test
    public void testCurrentThreadSpy() throws Exception {
        CurrentThreadSpy spy = Sniffy.spyCurrentThread();
        executeStatements(2);
        executeStatementsInOtherThread(3);
        assertEquals(2, spy.executedStatements());
        assertEquals(1, spy.getExecutedStatements().size());
        assertEquals(2, spy.getExecutedStatements().values().iterator().next().queries.get());
    }

}