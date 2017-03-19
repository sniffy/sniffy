package io.sniffy;

import com.codahale.metrics.Timer;
import io.sniffy.configuration.SniffyConfiguration;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

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

    @Test
    @Features("issues/292")
    public void testGlobalSqlStatsDisabled() throws Exception {
        int topSqlCapacity = SniffyConfiguration.INSTANCE.getTopSqlCapacity();
        try {
            Sniffy.getGlobalSqlStats().clear();
            SniffyConfiguration.INSTANCE.setTopSqlCapacity(0);
            executeStatements(3);
            assertTrue(Sniffy.getGlobalSqlStats().isEmpty());
        } finally {
            SniffyConfiguration.INSTANCE.setTopSqlCapacity(topSqlCapacity);
        }
    }

    @Test
    @Features("issues/292")
    public void testGetGlobalSqlStats() throws Exception {
        Sniffy.getGlobalSqlStats().clear();
        executeStatements(3);
        ConcurrentMap<String, Timer> globalSqlStats = Sniffy.getGlobalSqlStats();
        assertEquals(1, globalSqlStats.size());
        Map.Entry<String, Timer> entry = globalSqlStats.entrySet().iterator().next();
        assertEquals("SELECT 1 FROM DUAL", entry.getKey());
        assertEquals(3, entry.getValue().getCount());
    }

    @Test
    @Features("issues/292")
    public void testLruGlobalSqlStats() throws Exception {
        Sniffy.getGlobalSqlStats().clear();
        for (int i = 0; i < Sniffy.TOP_SQL_CAPACITY; i++) {
            executeSelectStatements(i, 2 + i % 100);
        }
        assertEquals(Sniffy.TOP_SQL_CAPACITY, Sniffy.getGlobalSqlStats().size());

        assertNotNull(Sniffy.getGlobalSqlStats().get(String.format("SELECT %d FROM DUAL", 0)));

        executeSelectStatements(Sniffy.TOP_SQL_CAPACITY + 1000, 1);
        assertEquals(Sniffy.TOP_SQL_CAPACITY, Sniffy.getGlobalSqlStats().size());

        assertTrue(Sniffy.getGlobalSqlStats().containsKey(String.format("SELECT %d FROM DUAL", Sniffy.TOP_SQL_CAPACITY + 1000)));
        assertTrue(Sniffy.getGlobalSqlStats().containsKey(String.format("SELECT %d FROM DUAL", 0)));
        assertFalse(Sniffy.getGlobalSqlStats().containsKey(String.format("SELECT %d FROM DUAL", 1)));
    }

    private void executeSelectStatements(int index, int count) throws SQLException {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            for (int i = 0; i < count; i++) {
                statement.execute(String.format("SELECT %d FROM DUAL", index));
            }
        }
    }

}