package io.sniffy.sql;

import io.sniffy.BaseTest;
import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.socket.TcpConnections;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SniffyDataSourceTest extends BaseTest {

    @Test
    public void testGetConnection() throws Exception {
        JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:");

        SniffyDataSource sniffyDataSource = new SniffyDataSource(h2DataSource);

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

        JdbcDataSource targetDataSource = Mockito.spy(h2DataSource);

        Mockito.when(targetDataSource.getConnection()).then(invocation -> {
            Sniffy.logSocket("stackTrace", 1, new InetSocketAddress(InetAddress.getLoopbackAddress(), 9876), 2, 3, 4);
            return invocation.callRealMethod();
        });

        SniffyDataSource sniffyDataSource = new SniffyDataSource(targetDataSource);

        try (Spy spy = Sniffy.expect(SqlQueries.exactQueries(1)).expect(TcpConnections.none());
             Connection connection = sniffyDataSource.getConnection()) {
            assertNotNull(connection);
            assertTrue(Proxy.isProxyClass(connection.getClass()));
            try (Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1 FROM DUAL");
                statement.getResultSet().next();
            }
        }
    }

}
