package io.sniffy.sql;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;

class PreparedStatementInvocationHandler extends StatementInvocationHandler {

    private final String sql;

    public PreparedStatementInvocationHandler(Object delegate, String sql) {
        super(delegate);
        this.sql = sql;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

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
                result = invokeTargetAndRecord(method, args, getBatchedSql(), false);
                // TODO process int[] response of executeBatch methods to calculate number fo affected rows!
                break;
            case EXECUTE_UPDATE:
                result = invokeTargetAndRecord(method, args, null != args && args.length > 0 ? String.class.cast(args[0]) : null, true);
                break;
            case EXECUTE_SQL:
                result =  invokeTargetAndRecord(method, args, null != args && args.length > 0 ? String.class.cast(args[0]) : sql, false);
                break;
            case OTHER:
            default:
                result = invokeTarget(method, args);
                break;
        }

        if (result instanceof ResultSet) {
            return Proxy.newProxyInstance(
                    ResultSetInvocationHandler.class.getClassLoader(),
                    new Class[]{ResultSet.class},
                    new ResultSetInvocationHandler(result, lastStatementMetaData)
            );
        }

        return result;

    }

    @Override
    protected synchronized String getBatchedSql() {
        String batchedSql = super.getBatchedSql();
        return null == batchedSql ? sql : batchedSql;
    }

}
