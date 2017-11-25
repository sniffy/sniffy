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

    private final ThreadLocal<SharedConnection> lastConnectionThreadLocal = new ThreadLocal<SharedConnection>();

    private final DataSource targetDataSource;

    private Thread masterConnectionThread;

    private SharedConnection masterConnection;

    public SharedConnectionDataSource(DataSource targetDataSource) {
        this.targetDataSource = targetDataSource;
    }

    public synchronized void setCurrentThreadAsMaster() {
        masterConnectionThread = Thread.currentThread();
        masterConnection = lastConnectionThreadLocal.get();
        if (null != masterConnection) {
            masterConnection.markAsMaster();
        }
    }

    public synchronized void resetMasterConnection() throws InterruptedException {
        masterConnectionThread = null;

        if (null != masterConnection) {
            masterConnection.waitForAllSlavesToFinish();
            masterConnection = null;
        }
    }

    @Override
    public synchronized Connection getConnection() throws SQLException {
        return getConnection(null, null);
    }

    @Override
    public synchronized Connection getConnection(String username, String password) throws SQLException {

        if (null == masterConnectionThread) {
            // Shared connection mode not started yet
            return obtainConnection(username, password).createProxy();
        } else if (null == masterConnection) {
            if (Thread.currentThread() == masterConnectionThread) {
                // Obtaining master connection
                masterConnection = obtainConnection(username, password);
                return masterConnection.createMasterProxy();
            } else {
                // No master connection yet; returning target connection
                return obtainConnection(username, password).createProxy();
            }
        } else {
            // Returning shared connection
            return masterConnection.createSlaveProxy();
        }

    }

    private SharedConnection obtainConnection(String username, String password) throws SQLException {
        Connection targetConnection = (null == username && null == password) ?
                targetDataSource.getConnection() :
                targetDataSource.getConnection(username, password);
        SharedConnection sharedConnection = new SharedConnection(targetConnection);
        lastConnectionThreadLocal.set(sharedConnection);
        return sharedConnection;
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
