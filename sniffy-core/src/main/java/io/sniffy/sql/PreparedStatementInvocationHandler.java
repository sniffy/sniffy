package io.sniffy.sql;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

class PreparedStatementInvocationHandler<T extends PreparedStatement> extends StatementInvocationHandler<T> {

    private final String sql;

    PreparedStatementInvocationHandler(T delegate, Connection sniffyConnectionProxy, String url, String userName, String sql) {
        super(delegate, sniffyConnectionProxy, url, userName);
        this.sql = sql;
    }

    @Override
    public Object invokeImpl(T proxy, String methodName, Method method, Object[] args) throws Throwable {

        checkConnectionAllowed();

        Object result;

        switch (StatementMethodType.parse(method.getName())) {
            case ADD_BATCH:
                addBatch(sql);
                result = invokeTarget(method, args);
                break;
            case CLEAR_BATCH:
                clearBatch();
                result = invokeTarget(method, args);
                break;
            case EXECUTE_BATCH:
                result = invokeTargetAndRecord(method, args, getBatchedSql(), true);
                break;
            case EXECUTE_UPDATE:
                result = invokeTargetAndRecord(method, args, null != args && args.length > 0 ? String.class.cast(args[0]) : sql, true);
                break;
            case EXECUTE_SQL:
                result =  invokeTargetAndRecord(method, args, null != args && args.length > 0 ? String.class.cast(args[0]) : sql, false);
                break;
            case OTHER:
            default:
                result = invokeTarget(method, args);
                break;
        }

        return proxyResultSet(result);

    }

    @Override
    protected synchronized String getBatchedSql() {
        String batchedSql = super.getBatchedSql();
        return null == batchedSql ? sql : batchedSql;
    }

}
