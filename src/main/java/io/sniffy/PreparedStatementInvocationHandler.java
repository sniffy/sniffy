package io.sniffy;

import java.lang.reflect.Method;

class PreparedStatementInvocationHandler extends StatementInvocationHandler {

    private final String sql;

    public PreparedStatementInvocationHandler(Object delegate, String sql) {
        super(delegate);
        this.sql = sql;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        switch (StatementMethodType.parse(method.getName())) {
            case ADD_BATCH:
                addBatch(sql);
                break;
            case CLEAR_BATCH:
                clearBatch();
                break;
            case EXECUTE_BATCH:
                return invokeTargetAndRecord(method, args, getBatchedSql());
            case EXECUTE_SQL:
                return invokeTargetAndRecord(method, args, null != args && args.length > 0 ? String.class.cast(args[0]) : sql);
        }

        return invokeTarget(method, args);
    }

    @Override
    protected synchronized String getBatchedSql() {
        String batchedSql = super.getBatchedSql();
        return null == batchedSql ? sql : batchedSql;
    }

}
