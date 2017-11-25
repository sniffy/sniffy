package io.sniffy.scd;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * @since 3.1.6
 */
public class SharedConnectionDataSource implements DataSource {

    private final ThreadLocal<Connection> lastConnectionThreadLocal = new ThreadLocal<Connection>();

    private final DataSource targetDataSource;

    private Thread masterConnectionThread;

    private Connection masterConnection;

    public SharedConnectionDataSource(DataSource targetDataSource) {
        this.targetDataSource = targetDataSource;
    }

    public synchronized void setCurrentThreadAsMaster() {
        masterConnectionThread = Thread.currentThread();
        masterConnection = lastConnectionThreadLocal.get();
    }

    public synchronized void resetMasterConnection() {
        masterConnectionThread = null;
        masterConnection = null;
    }

    @Override
    public synchronized Connection getConnection() throws SQLException {
        return getConnection(null, null);
    }

    @Override
    public synchronized Connection getConnection(String username, String password) throws SQLException {

        if (null == masterConnectionThread) {
            // Shared connection mode not started yet
            return obtainConnection(username, password);
        } else if (null == masterConnection) {
            if (Thread.currentThread() == masterConnectionThread) {
                // Obtaining master connection
                masterConnection = obtainConnection(username, password);
                return SharedConnectionInvocationHandler.proxy(masterConnection, true);
            } else {
                // No master connection yet; returning target connection
                return obtainConnection(username, password);
            }
        } else {
            // Returning shared connection
            return SharedConnectionInvocationHandler.proxy(masterConnection, false);
        }

    }

    private Connection obtainConnection(String username, String password) throws SQLException {
        Connection targetConnection = (null == username && null == password) ?
                targetDataSource.getConnection() :
                targetDataSource.getConnection(username, password);
        lastConnectionThreadLocal.set(targetConnection);
        return targetConnection;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return targetDataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        targetDataSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        targetDataSource.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return targetDataSource.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return targetDataSource.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return targetDataSource.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return targetDataSource.isWrapperFor(iface);
    }

}
