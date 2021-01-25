package io.sniffy.sql;

import io.sniffy.ThreadMetaData;

/**
 * Represents an executed query - actual SQL, query type (SELECT, INSERT, e.t.c.) and the calling thread
 * @since 3.0.0
 */
public class StatementMetaData {

    @Deprecated
    public final String sql;
    @Deprecated
    public final SqlStatement query;
    @Deprecated
    public final String stackTrace;
    @Deprecated
    public final long ownerThreadId;

    private final ThreadMetaData threadMetaData;


    private final int hashCode;

    public StatementMetaData(String sql, SqlStatement query, String stackTrace, Thread ownerThread) {
        this(sql, query, stackTrace, new ThreadMetaData(ownerThread));
    }

    public StatementMetaData(String sql, SqlStatement query, String stackTrace, ThreadMetaData threadMetaData) {
        this.sql = null == sql ? null : sql.intern();
        this.query = query;
        this.stackTrace = null == stackTrace ? null : stackTrace.intern();
        this.threadMetaData = threadMetaData;
        this.ownerThreadId = threadMetaData.getThreadId();

        hashCode = computeHashCode();
    }

    private int computeHashCode() {
        int result = System.identityHashCode(sql);
        result = 31 * result + query.hashCode();
        result = 31 * result + System.identityHashCode(stackTrace);
        result = 31 * result + threadMetaData.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StatementMetaData that = (StatementMetaData) o;

        if (threadMetaData.getThreadId() != that.threadMetaData.getThreadId()) return false;
        //noinspection StringEquality
        if (sql != that.sql) return false;
        if (query != that.query) return false;
        //noinspection StringEquality
        return stackTrace == that.stackTrace;

    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public ThreadMetaData getThreadMetaData() {
        return threadMetaData;
    }
}
