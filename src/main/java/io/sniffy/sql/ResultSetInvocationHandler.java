package io.sniffy.sql;

import io.sniffy.Sniffer;

import java.lang.reflect.Method;

public class ResultSetInvocationHandler extends SniffyInvocationHandler<Object> {

    private final StatementMetaData statementMetaData;

    public ResultSetInvocationHandler(Object delegate, StatementMetaData statementMetaData) {
        super(delegate);
        this.statementMetaData = statementMetaData;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

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
        long start = System.currentTimeMillis();
        try {
            Sniffer.enterJdbcMethod();
            Object result = invokeTargetImpl(method, args);
            if (Boolean.TRUE.equals(result)) {
                Sniffer.readDatabaseRow(method, System.currentTimeMillis() - start, statementMetaData);
            }
            return result;
        } finally {
            Sniffer.exitJdbcMethod(method, System.currentTimeMillis() - start);
        }
    }
}
