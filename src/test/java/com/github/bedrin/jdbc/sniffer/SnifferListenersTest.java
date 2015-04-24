package com.github.bedrin.jdbc.sniffer;

import org.junit.Ignore;
import org.junit.Test;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import static org.junit.Assert.*;

public class SnifferListenersTest extends BaseTest {

    @Test
    public void testSpyRemovedOnClose() throws Exception {
        Spy spy = Sniffer.spy();
        spy.close();

        for (WeakReference<Spy> spyReference : Sniffer.registeredSpies()) {
            if (spyReference.get() == spy) fail("Spy was not removed from Sniffer observers");
        }

    }

    @Test
    @Ignore("H2 doesn't seem to support batch queries against DUAL table")
    public void testBatchIsLogged() throws Exception {
        try { try (Spy spy = Sniffer.expectNever();
             Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
             PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1 FROM DUAL WHERE 1 = ?")) {
            preparedStatement.setLong(1, 1);
            preparedStatement.addBatch();
            preparedStatement.setLong(1, 2);
            preparedStatement.addBatch();
            preparedStatement.executeBatch();
        } } catch (WrongNumberOfQueriesError e) {
            assertNotNull(e);
        }

    }

}