package io.sniffy.sql;

import io.sniffy.Sniffy;

import java.lang.reflect.Method;

class ResultSetInvocationHandler extends SniffyInvocationHandler<Object> {

    private final StatementMetaData statementMetaData;

    ResultSetInvocationHandler(Object delegate, String url, String userName, StatementMetaData statementMetaData) {
        super(delegate, url, userName);
        this.statementMetaData = statementMetaData;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        checkConnectionAllowed();

        String methodName = method.getName();

        // TODO: consider using fetch size for exact calculations
        // TODO: getStatement should return statement proxy
        if ("next".equals(methodName) || "previous".equals(methodName) ||
                "first".equals(methodName) || "last".equals(methodName) ||
                "absolute".equals(methodName) || "relative".equals(methodName)) {
            return invokeTargetAndRecord(method, args);
        } else {
            return invokeTarget(method, args);
        }

    }

    protected Object invokeTargetAndRecord(Method method, Object[] args) throws Throwable {

        if (Sniffy.hasSpies()) {
            long start = System.currentTimeMillis();
            try {
                Sniffy.enterJdbcMethod();
                Object result = invokeTargetImpl(method, args);
                if (Boolean.TRUE.equals(result)) {
                    Sniffy.readDatabaseRow(method, System.currentTimeMillis() - start, statementMetaData);
                }
                return result;
            } finally {
                Sniffy.exitJdbcMethod(method, System.currentTimeMillis() - start);
            }
        } else {
            return invokeTargetImpl(method, args);
        }

    }
}
