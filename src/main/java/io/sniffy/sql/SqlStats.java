package io.sniffy.sql;

import io.sniffy.socket.SocketStats;

public class SqlStats extends SocketStats {

    public SqlStats(SqlStats that) {
        super(that);
    }

    public SqlStats(long elapsedTime, long bytesDown, long bytesUp) {
        super(elapsedTime, bytesDown, bytesUp);
    }

}
