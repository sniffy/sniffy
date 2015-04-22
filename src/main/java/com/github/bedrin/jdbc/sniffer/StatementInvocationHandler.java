package com.github.bedrin.jdbc.sniffer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

class StatementInvocationHandler implements InvocationHandler {

    protected final Object delegate;

    private Map<String, Integer> batchedSql;

    public StatementInvocationHandler(Object delegate) {
        this.delegate = delegate;
    }

    protected enum StatementMethodType {
        EXECUTE_SQL,
        ADD_BATCH,
        CLEAR_BATCH,
        EXECUTE_BATCH,
        OTHER;

        static StatementMethodType parse(String methodName) {
            if ("execute".equals(methodName) || "executeQuery".equals(methodName)
                    || "executeUpdate".equals(methodName) ||  "executeLargeUpdate".equals(methodName)) {
                return EXECUTE_SQL;
            } else if ("addBatch".equals(methodName)) {
                return ADD_BATCH;
            } else if ("clearBatch".equals(methodName)) {
                return CLEAR_BATCH;
            } else if ("executeBatch".equals(methodName) || "executeLargeBatch".equals(methodName)) {
                return EXECUTE_BATCH;
            } else {
                return OTHER;
            }
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        switch (StatementMethodType.parse(method.getName())) {
            case ADD_BATCH:
                addBatch(String.class.cast(args[0]));
                break;
            case CLEAR_BATCH:
                clearBatch();
                break;
            case EXECUTE_BATCH:
                return invokeTargetAndRecord(method, args, getBatchedSql());
            case EXECUTE_SQL:
                return invokeTargetAndRecord(method, args, null != args && args.length > 0 ? String.class.cast(args[0]) : null);
        }

        return invokeTarget(method, args);
    }

    protected Object invokeTarget(Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    protected Object invokeTargetAndRecord(Method method, Object[] args, String sql) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        } finally {
            Sniffer.executeStatement(sql, System.currentTimeMillis() - start);
        }
    }

    protected void addBatch(String sql) {

        if (null == sql) return;

        if (null == batchedSql) batchedSql = new HashMap<String, Integer>();

        Integer count = batchedSql.get(sql);
        if (null != count) {
            batchedSql.put(sql, count + 1);
        } else {
            batchedSql.put(sql, 1);
        }
    }

    protected void clearBatch() {
        batchedSql = null;
    }

    protected String getBatchedSql() {
        if (null == batchedSql || batchedSql.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : batchedSql.entrySet()) {
            if (0 != sb.length()) sb.append("; ");
            sb.append(entry.getKey()).append(" (x").append(entry.getValue()).append(')');
        }
        return sb.toString();
    }

}
