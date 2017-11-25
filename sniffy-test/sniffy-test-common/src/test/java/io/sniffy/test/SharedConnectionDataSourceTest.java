package io.sniffy.test;

import io.sniffy.test.SharedConnectionDataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

public class SharedConnectionDataSourceTest {

    private DataSource targetDataSource;

    @Before
    public void setupTargetDataSource() {
        JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:");
        targetDataSource = h2DataSource;
    }

    @Test
    public void testSameConnectionReturnedForAllThreads() throws SQLException, ExecutionException, InterruptedException {

        SharedConnectionDataSource sharedConnectionDataSource = new SharedConnectionDataSource(targetDataSource);

        {
            Connection masterConnection = sharedConnectionDataSource.getConnection();
            Connection slaveConnection = newSingleThreadExecutor().submit((Callable<Connection>) sharedConnectionDataSource::getConnection).get();

            assertNotEquals(masterConnection, slaveConnection);
        }

        sharedConnectionDataSource.setCurrentThreadAsMaster();

        try (Connection masterConnection = sharedConnectionDataSource.getConnection();
             Connection slaveConnection = newSingleThreadExecutor().submit(
                     (Callable<Connection>) sharedConnectionDataSource::getConnection).get()
        ) {
            assertEquals(masterConnection, slaveConnection);
        } finally {
            sharedConnectionDataSource.resetMasterConnection();
        }

        {
            Connection masterConnection = sharedConnectionDataSource.getConnection();
            Connection slaveConnection = newSingleThreadExecutor().submit((Callable<Connection>) sharedConnectionDataSource::getConnection).get();

            assertNotEquals(masterConnection, slaveConnection);
        }

    }

    @Test
    public void testSlaveConnectionCloseIgnored() throws SQLException, ExecutionException, InterruptedException {

        SharedConnectionDataSource sharedConnectionDataSource = new SharedConnectionDataSource(targetDataSource);

        sharedConnectionDataSource.setCurrentThreadAsMaster();

        try (Connection masterConnection = sharedConnectionDataSource.getConnection();
             Connection slaveConnection = newSingleThreadExecutor().submit(
                     (Callable<Connection>) sharedConnectionDataSource::getConnection).get()
        ) {
            assertEquals(masterConnection, slaveConnection);

            slaveConnection.close();

            assertFalse(slaveConnection.isClosed());

        } finally {
            sharedConnectionDataSource.resetMasterConnection();
        }

    }

}