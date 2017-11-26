package io.sniffy.test;

import io.sniffy.sql.JdbcInvocationHandler;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

/**
 * @since 3.1.6
 */
public class SlaveConnection extends JdbcInvocationHandler<Connection> {

    private final Set<String> IGNORED_METHOD_NAMES = new HashSet<String>(Arrays.asList(
            "setAutoCommit", "commit", "rollback", "close", "setReadOnly", "setTransactionIsolation",
            "setTypeMap", "setHoldability", "setSavepoint", "releaseSavepoint", "abort", "setNetworkTimeout"
    ));

    @SuppressWarnings("unused")
    private final SharedConnection master;

    private volatile boolean closed;
    private final Semaphore closeSemaphore = new Semaphore(1);

    public SlaveConnection(SharedConnection master) {
        super(null, master.getDelegate());
        this.master = master;
        closeSemaphore.acquireUninterruptibly();
        master.addSlaveConnection(this);
    }

    protected void waitForClose() throws InterruptedException {
        while (!closed) {
            closeSemaphore.acquire();
        }
    }

    private void close() {
        closed = true;
        closeSemaphore.release();
    }

    @Override
    protected Object invokeImpl(Connection proxy, String methodName, Method method, Object[] args) throws Throwable {
        if ("close".equals(methodName)) {
            close();
            return null;
        } else if (IGNORED_METHOD_NAMES.contains(methodName)) {
            return null;
        } else {
            return super.invokeImpl(proxy, methodName, method, args);
        }
    }

}
