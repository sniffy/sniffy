package io.sniffy.sql;

import io.sniffy.BaseTest;
import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.Threads;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.TcpConnections;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;

import javax.sql.*;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class SniffyDataSourceTest extends BaseTest {

    @Test
    public void testWrap() {
        JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:");

        XADataSource wrap = SniffyDataSource.wrap(h2DataSource);

        assertNotNull(wrap);
    }

    @Test
    public void testUnWrap() throws SQLException {
        JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:");

        DataSource wrap = SniffyDataSource.wrap(h2DataSource);
        assertNotNull(wrap);

        assertTrue(wrap.isWrapperFor(JdbcDataSource.class));
        assertNotNull(wrap.unwrap(JdbcDataSource.class));

        {
            Exception exception = null;
            Boolean isWrapper = null;
            try {
                isWrapper = wrap.isWrapperFor(Test.class);
            } catch (Exception e) {
                exception = e;
            }

            assertTrue(null != exception || !isWrapper);
        }

        {
            Exception exception = null;
            Object unwrapped = wrap;
            try {
                unwrapped = wrap.unwrap(Test.class);
            } catch (Exception e) {
                exception = e;
            }

            assertTrue(null != exception || null == unwrapped);
        }

    }

    @Test
    public void testGetConnection() throws Exception {
        JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:");

        DataSource sniffyDataSource = new SniffyDataSource(h2DataSource);

        try (Connection connection = sniffyDataSource.getConnection()) {
            assertNotNull(connection);
            assertTrue(Proxy.isProxyClass(connection.getClass()));
            try (Spy spy = Sniffy.expect(SqlQueries.exactQueries(1));
                 Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1 FROM DUAL");
                statement.getResultSet().next();
            }
        }
    }

    @Test
    public void testGetConnectionWithSocketOperation() throws Exception {
        JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:");

        CommonDataSource targetDataSource = (CommonDataSource) Proxy.newProxyInstance(
                JdbcDataSource.class.getClassLoader(),
                new Class[]{ DataSource.class, XADataSource.class, ConnectionPoolDataSource.class, CommonDataSource.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getConnection")) {
                        Sniffy.logSocket(1, new InetSocketAddress(InetAddress.getLoopbackAddress(), 9876), 2, 3, 4);
                    }
                    return method.invoke(h2DataSource, args);
                }
        );

        SniffyDataSource sniffyDataSource = new SniffyDataSource(targetDataSource);

        try (Spy spy = Sniffy.expect(SqlQueries.exactQueries(1)).expect(TcpConnections.none());
             Connection connection = sniffyDataSource.getConnection()) {
            assertNotNull(connection);
            assertTrue(Proxy.isProxyClass(connection.getClass()));
            try (Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1 FROM DUAL");
                statement.getResultSet().next();
            }
            assertTrue(spy.getSocketOperations(Threads.CURRENT, false).isEmpty());
        }
    }

    @Test
    public void testGetConnectionWithCredentialsWithSocketOperation() throws Exception {
        JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:");

        CommonDataSource targetDataSource = (CommonDataSource) Proxy.newProxyInstance(
                JdbcDataSource.class.getClassLoader(),
                new Class[]{ DataSource.class, XADataSource.class, ConnectionPoolDataSource.class, CommonDataSource.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getConnection")) {
                        Sniffy.logSocket(1, new InetSocketAddress(InetAddress.getLoopbackAddress(), 9876), 2, 3, 4);
                    }
                    return method.invoke(h2DataSource, args);
                }
        );

        SniffyDataSource sniffyDataSource = new SniffyDataSource(targetDataSource);

        try (Spy spy = Sniffy.expect(SqlQueries.exactQueries(1)).expect(TcpConnections.none());
             Connection connection = sniffyDataSource.getConnection("sa","sa")) {
            assertNotNull(connection);
            assertTrue(Proxy.isProxyClass(connection.getClass()));
            try (Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1 FROM DUAL");
                statement.getResultSet().next();
            }
            assertTrue(spy.getSocketOperations(Threads.CURRENT, false).isEmpty());
        }
    }

    @Test
    public void testGetXAConnectionWithSocketOperation() throws Exception {
        JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:");

        CommonDataSource targetDataSource = (CommonDataSource) Proxy.newProxyInstance(
                JdbcDataSource.class.getClassLoader(),
                new Class[]{ DataSource.class, XADataSource.class, ConnectionPoolDataSource.class, CommonDataSource.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getXAConnection")) {
                        Sniffy.logSocket(1, new InetSocketAddress(InetAddress.getLoopbackAddress(), 9876), 2, 3, 4);
                    }
                    return method.invoke(h2DataSource, args);
                }
        );

        SniffyDataSource sniffyDataSource = new SniffyDataSource(targetDataSource);

        try (Spy spy = Sniffy.expect(SqlQueries.exactQueries(1)).expect(TcpConnections.none());
             Connection connection = sniffyDataSource.getXAConnection().getConnection()) {
            assertNotNull(connection);
            assertTrue(Proxy.isProxyClass(connection.getClass()));
            try (Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1 FROM DUAL");
                statement.getResultSet().next();
            }
            assertTrue(spy.getSocketOperations(Threads.CURRENT, false).isEmpty());
        }
    }

    @Test
    public void testGetXAConnectionWithCredentialsWithSocketOperation() throws Exception {
        JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:");

        CommonDataSource targetDataSource = (CommonDataSource) Proxy.newProxyInstance(
                JdbcDataSource.class.getClassLoader(),
                new Class[]{ DataSource.class, XADataSource.class, ConnectionPoolDataSource.class, CommonDataSource.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getXAConnection")) {
                        Sniffy.logSocket(1, new InetSocketAddress(InetAddress.getLoopbackAddress(), 9876), 2, 3, 4);
                    }
                    return method.invoke(h2DataSource, args);
                }
        );

        SniffyDataSource sniffyDataSource = new SniffyDataSource(targetDataSource);

        try (Spy spy = Sniffy.expect(SqlQueries.exactQueries(1)).expect(TcpConnections.none());
             Connection connection = sniffyDataSource.getXAConnection("sa","sa").getConnection()) {
            assertNotNull(connection);
            assertTrue(Proxy.isProxyClass(connection.getClass()));
            try (Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1 FROM DUAL");
                statement.getResultSet().next();
            }
            assertTrue(spy.getSocketOperations(Threads.CURRENT, false).isEmpty());
        }
    }

    @Test
    public void testGetPooledConnectionWithSocketOperation() throws Exception {
        JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:");

        CommonDataSource targetDataSource = (CommonDataSource) Proxy.newProxyInstance(
                JdbcDataSource.class.getClassLoader(),
                new Class[]{ DataSource.class, XADataSource.class, ConnectionPoolDataSource.class, CommonDataSource.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getConnection")) {
                        Sniffy.logSocket(1, new InetSocketAddress(InetAddress.getLoopbackAddress(), 9876), 2, 3, 4);
                    }
                    return method.invoke(h2DataSource, args);
                }
        );

        SniffyDataSource sniffyDataSource = new SniffyDataSource(targetDataSource);

        try (Spy spy = Sniffy.expect(SqlQueries.exactQueries(1)).expect(TcpConnections.none());
             Connection connection = sniffyDataSource.getPooledConnection().getConnection()) {
            assertNotNull(connection);
            assertTrue(Proxy.isProxyClass(connection.getClass()));
            try (Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1 FROM DUAL");
                statement.getResultSet().next();
            }
            assertTrue(spy.getSocketOperations(Threads.CURRENT, false).isEmpty());
        }
    }

    @Test
    public void testGetPooledConnectionClosedIfRejected() throws Exception {
        JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:");

        AtomicReference<Connection> targetConnectionReference = new AtomicReference<>();

        CommonDataSource targetDataSource = (CommonDataSource) Proxy.newProxyInstance(
                JdbcDataSource.class.getClassLoader(),
                new Class[]{ DataSource.class, XADataSource.class, ConnectionPoolDataSource.class, CommonDataSource.class },
                (proxy, method, args) -> {
                    Object result = method.invoke(h2DataSource, args);
                    if (method.getName().equals("getPooledConnection")) {
                        PooledConnection pc = (PooledConnection) result;
                        return (PooledConnection) Proxy.newProxyInstance(
                                JdbcDataSource.class.getClassLoader(),
                                new Class[] { PooledConnection.class },
                                (proxy1, method1, args1) -> {
                                    Object result1 = method1.invoke(pc, args1);
                                    if (method1.getName().equals("getConnection")) {
                                        targetConnectionReference.set((Connection) result1);
                                    }
                                    return result1;
                                }
                        );
                    }
                    return result;
                }
        );

        SniffyDataSource sniffyDataSource = new SniffyDataSource(targetDataSource);

        PooledConnection sniffyPooledConnection = sniffyDataSource.getPooledConnection();

        ConnectionsRegistry.INSTANCE.setDataSourceStatus("jdbc:h2:mem:", "", -1);

        try {
            sniffyPooledConnection.getConnection();
            fail();
        } catch (SQLException e) {
            assertNotNull(e);
        } finally {
            ConnectionsRegistry.INSTANCE.clear();
        }

        assertTrue(targetConnectionReference.get().isClosed());

    }

    @Test
    public void testGetPooledConnectionWithCredentialsWithSocketOperation() throws Exception {
        JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:");

        CommonDataSource targetDataSource = (CommonDataSource) Proxy.newProxyInstance(
                JdbcDataSource.class.getClassLoader(),
                new Class[]{ DataSource.class, XADataSource.class, ConnectionPoolDataSource.class, CommonDataSource.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getConnection")) {
                        Sniffy.logSocket(1, new InetSocketAddress(InetAddress.getLoopbackAddress(), 9876), 2, 3, 4);
                    }
                    return method.invoke(h2DataSource, args);
                }
        );

        SniffyDataSource sniffyDataSource = new SniffyDataSource(targetDataSource);

        try (Spy spy = Sniffy.expect(SqlQueries.exactQueries(1)).expect(TcpConnections.none());
             Connection connection = sniffyDataSource.getPooledConnection("sa","sa").getConnection()) {
            assertNotNull(connection);
            assertTrue(Proxy.isProxyClass(connection.getClass()));
            try (Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1 FROM DUAL");
                statement.getResultSet().next();
            }
            assertTrue(spy.getSocketOperations(Threads.CURRENT, false).isEmpty());
        }
    }

}
