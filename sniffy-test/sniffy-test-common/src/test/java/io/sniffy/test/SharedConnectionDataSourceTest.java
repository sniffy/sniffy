package io.sniffy.test;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SharedConnectionDataSourceTest {

    private DataSource targetDataSource;

    @Mock
    private DataSource mockDataSouce;

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

    @Test
    public void testStatementGetConnectionReturnsProxy() throws SQLException, InterruptedException {

        SharedConnectionDataSource sharedConnectionDataSource = new SharedConnectionDataSource(targetDataSource);
        sharedConnectionDataSource.setCurrentThreadAsMaster();

        try (Connection masterConnection = sharedConnectionDataSource.getConnection();
             PreparedStatement preparedStatement = masterConnection.prepareStatement("SELECT 1 FROM DUAL")) {

            assertEquals(masterConnection, preparedStatement.getConnection());

        } finally {
            sharedConnectionDataSource.resetMasterConnection();
        }

    }

    @Test
    public void testGetLogWriterCallsTarget() throws SQLException {

        SharedConnectionDataSource sharedConnectionDataSource = new SharedConnectionDataSource(mockDataSouce);

        PrintWriter printWriter = new PrintWriter(new ByteArrayOutputStream());

        when(mockDataSouce.getLogWriter()).thenReturn(printWriter);

        assertEquals(printWriter, sharedConnectionDataSource.getLogWriter());

        verify(mockDataSouce).getLogWriter();
        verifyNoMoreInteractions(mockDataSouce);

    }

    @Test
    public void testSetLogWriterCallsTarget() throws SQLException {

        SharedConnectionDataSource sharedConnectionDataSource = new SharedConnectionDataSource(mockDataSouce);

        AtomicReference<PrintWriter> captured = new AtomicReference<>();

        doAnswer(invocation -> {
            captured.set(invocation.getArgumentAt(0, PrintWriter.class)); return null;
        }).when(mockDataSouce).setLogWriter(any(PrintWriter.class));

        PrintWriter printWriter = new PrintWriter(new ByteArrayOutputStream());
        sharedConnectionDataSource.setLogWriter(printWriter);

        assertEquals(printWriter, captured.get());

        verify(mockDataSouce).setLogWriter(any(PrintWriter.class));
        verifyNoMoreInteractions(mockDataSouce);

    }

}