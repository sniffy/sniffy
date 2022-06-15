package io.sniffy.sql;

import io.sniffy.socket.SocketStats;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @since 3.1
 */
public class SqlStats extends SocketStats {

    public final AtomicLong rows = new AtomicLong();

    /**
     * Number of queries (or batches) executed
     */
    public final AtomicInteger queries = new AtomicInteger();

    public SqlStats(SqlStats that) {
        this(that.elapsedTime.longValue(), that.bytesDown.longValue(), that.bytesUp.longValue(), that.rows.intValue(), that.queries.intValue());
    }

    public SqlStats(long elapsedTime, long bytesDown, long bytesUp, long rows, int queries) {
        super(elapsedTime, bytesDown, bytesUp);
        this.rows.set(rows);
        this.queries.set(queries);
    }

    public void accumulate(long elapsedTime, long bytesDown, long bytesUp, long rows, int queries) {
        super.accumulate(elapsedTime, bytesDown, bytesUp);
        this.rows.addAndGet(rows);
        this.queries.addAndGet(queries);
    }

    public void accumulate(SqlStats that) {
        accumulate(that.elapsedTime.longValue(), that.bytesDown.intValue(), that.bytesUp.intValue(), that.rows.intValue(), that.queries.intValue());
    }

}
