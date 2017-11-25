package io.sniffy.scd;

import io.sniffy.sql.JdbcInvocationHandler;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @since 3.1.6
 */
public class SharedConnection extends JdbcInvocationHandler<Connection> {

    private Set<SlaveConnection> slaveConnections = new HashSet<SlaveConnection>();

    private boolean master;

    public SharedConnection(Connection delegate) {
        super(null, delegate);
    }

    protected synchronized void addSlaveConnection(SlaveConnection slaveConnection) {
        slaveConnections.add(slaveConnection);
    }

    public Connection createProxy() {
        return Connection.class.cast(Proxy.newProxyInstance(
                SharedConnection.class.getClassLoader(),
                new Class[]{Connection.class},
                this
        ));
    }

    public Connection createMasterProxy() {
        markAsMaster();
        return Connection.class.cast(Proxy.newProxyInstance(
                SharedConnection.class.getClassLoader(),
                new Class[]{Connection.class},
                this
        ));
    }

    public Connection createSlaveProxy() {
        return Connection.class.cast(Proxy.newProxyInstance(
                SharedConnection.class.getClassLoader(),
                new Class[]{Connection.class},
                new SlaveConnection(this)
        ));
    }

    protected Connection getDelegate() {
        return delegate;
    }

    public void markAsMaster() {
        master = false;
    }

    public synchronized void waitForAllSlavesToFinish() throws InterruptedException {
        for (SlaveConnection slaveConnection : slaveConnections) {
            slaveConnection.waitForClose();
        }
    }

}
