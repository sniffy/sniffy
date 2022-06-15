package io.sniffy.test;

import io.qameta.allure.Feature;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.*;
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
    @Feature("issue/344")
    public void usage() throws Exception {
        // tag::sharedConnectionDataSourceUsage[]
        SharedConnectionDataSource sharedConnectionDataSource = new SharedConnectionDataSource(targetDataSource); // <1>

        sharedConnectionDataSource.setCurrentThreadAsMaster(); // <2>

        try (Connection masterConnection = sharedConnectionDataSource.getConnection(); // <3>
             Connection slaveConnection = newSingleThreadExecutor().submit(
                     (Callable<Connection>) sharedConnectionDataSource::getConnection).get() // <4>
        ) {
            assertEquals(masterConnection, slaveConnection); // <5>
        } finally {
            sharedConnectionDataSource.resetMasterConnection(); // <6>
        }
        // end::sharedConnectionDataSourceUsage[]
    }

    @Test
    @Feature("issue/344")
    public void testSameConnectionReturnedForAllThreads() throws SQLException, ExecutionException, InterruptedException {

        SharedConnectionDataSource sharedConnectionDataSource = new SharedConnectionDataSource(targetDataSource);

        try (Connection masterConnection = sharedConnectionDataSource.getConnection();
            Connection slaveConnection = newSingleThreadExecutor().submit((Callable<Connection>) sharedConnectionDataSource::getConnection).get()) {

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

        try (Connection masterConnection = sharedConnectionDataSource.getConnection();
             Connection slaveConnection = newSingleThreadExecutor().submit((Callable<Connection>) sharedConnectionDataSource::getConnection).get()) {

            assertNotEquals(masterConnection, slaveConnection);
        }

    }

    @Test
    @Feature("issue/344")
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
    @Feature("issue/344")
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
    @Feature("issue/344")
    public void testGetLogWriterCallsTarget() throws SQLException {

        SharedConnectionDataSource sharedConnectionDataSource = new SharedConnectionDataSource(mockDataSouce);

        PrintWriter printWriter = new PrintWriter(new ByteArrayOutputStream());

        when(mockDataSouce.getLogWriter()).thenReturn(printWriter);

        assertEquals(printWriter, sharedConnectionDataSource.getLogWriter());

        verify(mockDataSouce).getLogWriter();
        verifyNoMoreInteractions(mockDataSouce);

    }

    @Test
    @Feature("issue/344")
    public void testSetLogWriterCallsTarget() throws SQLException {

        SharedConnectionDataSource sharedConnectionDataSource = new SharedConnectionDataSource(mockDataSouce);

        AtomicReference<PrintWriter> captured = new AtomicReference<>();

        doAnswer(invocation -> {
            captured.set(invocation.getArgument(0, PrintWriter.class)); return null;
        }).when(mockDataSouce).setLogWriter(any(PrintWriter.class));

        PrintWriter printWriter = new PrintWriter(new ByteArrayOutputStream());
        sharedConnectionDataSource.setLogWriter(printWriter);

        assertEquals(printWriter, captured.get());

        verify(mockDataSouce).setLogWriter(any(PrintWriter.class));
        verifyNoMoreInteractions(mockDataSouce);

    }

    @Test
    @Feature("issue/344")
    public void testGetLoginTimeoutCallsTarget() throws SQLException {

        SharedConnectionDataSource sharedConnectionDataSource = new SharedConnectionDataSource(mockDataSouce);

        when(mockDataSouce.getLoginTimeout()).thenReturn(42);

        assertEquals(42, sharedConnectionDataSource.getLoginTimeout());

        verify(mockDataSouce).getLoginTimeout();
        verifyNoMoreInteractions(mockDataSouce);

    }

    @Test
    @Feature("issue/344")
    public void testSetLoginTimeoutCallsTarget() throws SQLException {

        SharedConnectionDataSource sharedConnectionDataSource = new SharedConnectionDataSource(mockDataSouce);

        AtomicInteger captured = new AtomicInteger();

        doAnswer(invocation -> {
            captured.set(invocation.getArgument(0, Integer.class)); return null;
        }).when(mockDataSouce).setLoginTimeout(anyInt());

        sharedConnectionDataSource.setLoginTimeout(42);

        assertEquals(42, captured.get());

        verify(mockDataSouce).setLoginTimeout(anyInt());
        verifyNoMoreInteractions(mockDataSouce);

    }

    @Test
    @Feature("issue/344")
    public void testGetParentLoggerCallsTarget() throws SQLException {

        SharedConnectionDataSource sharedConnectionDataSource = new SharedConnectionDataSource(mockDataSouce);

        Logger parentLoggerMock = mock(Logger.class);

        when(mockDataSouce.getParentLogger()).thenReturn(parentLoggerMock);

        assertEquals(parentLoggerMock, sharedConnectionDataSource.getParentLogger());

        verify(mockDataSouce).getParentLogger();
        verifyNoMoreInteractions(mockDataSouce);

    }

    @Test
    @Feature("issue/344")
    public void testUnwrapCallsTarget() throws SQLException {

        SharedConnectionDataSource sharedConnectionDataSource = new SharedConnectionDataSource(mockDataSouce);

        AtomicReference<Class> capturedArg = new AtomicReference<>();
        DataSource result = mock(SharedConnectionDataSource.class);

        doAnswer(invocation -> {
            capturedArg.set(invocation.getArgument(0, Class.class)); return result;
        }).when(mockDataSouce).unwrap(eq(SharedConnectionDataSource.class));

        assertEquals(result, sharedConnectionDataSource.unwrap(SharedConnectionDataSource.class));
        assertEquals(SharedConnectionDataSource.class, capturedArg.get());

        verify(mockDataSouce).unwrap(eq(SharedConnectionDataSource.class));
        verifyNoMoreInteractions(mockDataSouce);

    }

    @Test
    @Feature("issue/344")
    public void testIsWrapperForCallsTarget() throws SQLException {

        SharedConnectionDataSource sharedConnectionDataSource = new SharedConnectionDataSource(mockDataSouce);

        AtomicReference<Class> capturedArg = new AtomicReference<>();

        doAnswer(invocation -> {
            capturedArg.set(invocation.getArgument(0, Class.class)); return true;
        }).when(mockDataSouce).isWrapperFor(eq(SharedConnectionDataSource.class));

        assertEquals(true, sharedConnectionDataSource.isWrapperFor(SharedConnectionDataSource.class));
        assertEquals(SharedConnectionDataSource.class, capturedArg.get());

        verify(mockDataSouce).isWrapperFor(eq(SharedConnectionDataSource.class));
        verifyNoMoreInteractions(mockDataSouce);

    }

}