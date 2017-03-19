package io.sniffy.sql;

import io.sniffy.Sniffer;
import io.sniffy.Sniffy;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import static io.sniffy.util.StackTraceExtractor.getTraceForProxiedMethod;
import static io.sniffy.util.StackTraceExtractor.printStackTrace;

/**
 * @since 3.1
 */
class StatementInvocationHandler extends SniffyInvocationHandler<Object> {

    private Map<String, Integer> batchedSql;

    final Object sniffyConnectionProxy;

    StatementMetaData lastStatementMetaData;

    StatementInvocationHandler(Object delegate, Object sniffyConnectionProxy, String url, String userName) {
        super(delegate, url, userName);
        this.sniffyConnectionProxy = sniffyConnectionProxy;
    }

    protected enum StatementMethodType {
        EXECUTE_SQL,
        EXECUTE_UPDATE,
        ADD_BATCH,
        CLEAR_BATCH,
        EXECUTE_BATCH,
        GET_CONNECTION,
        OTHER;

        static StatementMethodType parse(String methodName) {
            if ("execute".equals(methodName) || "executeQuery".equals(methodName)) {
                return EXECUTE_SQL;
            } else if ("executeUpdate".equals(methodName) ||  "executeLargeUpdate".equals(methodName)) {
                return EXECUTE_UPDATE;
            } else if ("addBatch".equals(methodName)) {
                return ADD_BATCH;
            } else if ("clearBatch".equals(methodName)) {
                return CLEAR_BATCH;
            } else if ("executeBatch".equals(methodName) || "executeLargeBatch".equals(methodName)) {
                return EXECUTE_BATCH;
            } else if ("getConnection".equals(methodName)) {
                return GET_CONNECTION;
            } else {
                return OTHER;
            }
        }
    }

    // TODO: wrap complex parameters and results like streams and blobs
    // TODO: support methods fro Object class such as equals here as well as in other invocation handlers
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        checkConnectionAllowed();

        Object result;

        switch (StatementMethodType.parse(method.getName())) {
            case ADD_BATCH:
                addBatch(String.class.cast(args[0]));
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
                result = invokeTargetAndRecord(method, args, null != args && args.length > 0 ? String.class.cast(args[0]) : null, true);
                break;
            case EXECUTE_SQL:
                result = invokeTargetAndRecord(method, args, null != args && args.length > 0 ? String.class.cast(args[0]) : null, false);
                break;
            case GET_CONNECTION:
                result = sniffyConnectionProxy;
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
                    new ResultSetInvocationHandler(result, url, userName, lastStatementMetaData)
            );
        }

        return result;

    }

    protected Object invokeTargetAndRecord(Method method, Object[] args, String sql, boolean isUpdateQuery) throws Throwable {
        long start = System.currentTimeMillis();
        int rowsUpdated = 0;
        try {
            Sniffy.enterJdbcMethod();
            Object result = invokeTargetImpl(method, args);
            if (isUpdateQuery) {
                if (result instanceof Number) {
                    rowsUpdated = ((Number) result).intValue();
                }
                if (result instanceof int[]) {
                    int[] updatedRows = (int[]) result;
                    for (int i : updatedRows) {
                        if (-1 != i) rowsUpdated += i;
                    }
                }
                if (result instanceof long[]) {
                    long[] updatedRows = (long[]) result;
                    for (long i : updatedRows) {
                        if (-1 != i) rowsUpdated += i;
                    }
                }
            }
            return result;
        } finally {
            long elapsedTime = System.currentTimeMillis() - start;
            Sniffy.logSqlTime(sql, elapsedTime);
            if (Sniffy.hasSpies()) {
                String stackTrace = printStackTrace(getTraceForProxiedMethod(method));
                lastStatementMetaData = Sniffy.executeStatement(sql, elapsedTime, stackTrace, rowsUpdated);
            } else {
                Sniffer.executedStatementsGlobalCounter.incrementAndGet();
            }
        }
    }

    protected synchronized void addBatch(String sql) {

        if (null == sql) return;

        if (null == batchedSql) batchedSql = new HashMap<String, Integer>();

        Integer count = batchedSql.get(sql);
        if (null != count) {
            batchedSql.put(sql, count + 1);
        } else {
            batchedSql.put(sql, 1);
        }
    }

    protected synchronized void clearBatch() {
        batchedSql = null;
    }

    protected synchronized String getBatchedSql() {
        if (null == batchedSql || batchedSql.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : batchedSql.entrySet()) {
            if (0 != sb.length()) sb.append("; ");
            Integer times = entry.getValue();
            sb.append(entry.getKey());
            if (times > 1) sb.append(" /*").append(times).append(" times*/");
        }
        return sb.toString();
    }

}
