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
public class SharedConnectionInvocationHandler extends JdbcInvocationHandler<Connection> {

    private final Set<String> IGNORED_METHOD_NAMES = new HashSet<String>(Arrays.asList(
            "setAutoCommit", "commit", "rollback", "close", "setReadOnly", "setTransactionIsolation",
            "setTypeMap", "setHoldability", "setSavepoint", "releaseSavepoint", "abort", "setNetworkTimeout"
    ));

    private final boolean master;

    public SharedConnectionInvocationHandler(Connection delegate, boolean master) {
        super(null, delegate);
        this.master = master;
    }

    public static Connection proxy(Connection delegate, boolean master) {
        return Connection.class.cast(Proxy.newProxyInstance(
                SharedConnectionInvocationHandler.class.getClassLoader(),
                new Class[]{Connection.class},
                new SharedConnectionInvocationHandler(delegate, master)
        ));
    }

    @Override
    protected Object invokeImpl(Connection proxy, String methodName, Method method, Object[] args) throws Throwable {
        if (IGNORED_METHOD_NAMES.contains(methodName)) {
            return null;
        } else {
            return super.invokeImpl(proxy, methodName, method, args);
        }
    }

}
