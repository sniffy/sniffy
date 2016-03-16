package io.sniffy;

import io.sniffy.socket.SocketStats;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// TODO: consider making counters hierarchical, i.e. DML, DDL as top level , INSERT, CREATE TABLE as 2 level, e.t.c
class Counter {

    private final AtomicInteger select;
    private final AtomicInteger insert;
    private final AtomicInteger update;
    private final AtomicInteger delete;
    private final AtomicInteger merge;
    private final AtomicInteger other;

    private final ConcurrentMap<String, SocketStats> allSocketStats;


    public Counter() {
        this(
                new AtomicInteger(),
                new AtomicInteger(),
                new AtomicInteger(),
                new AtomicInteger(),
                new AtomicInteger(),
                new AtomicInteger(),
                new ConcurrentHashMap<String, SocketStats>()
        );
    }

    public Counter(
            AtomicInteger select,
            AtomicInteger insert,
            AtomicInteger update,
            AtomicInteger delete,
            AtomicInteger merge,
            AtomicInteger other,
            ConcurrentMap<String, SocketStats> allSocketStats) {
        this.select = select;
        this.insert = insert;
        this.update = update;
        this.delete = delete;
        this.merge = merge;
        this.other = other;
        this.allSocketStats = allSocketStats;
    }

    public Counter(Counter that) {
        this.select = new AtomicInteger(that.select.intValue());
        this.insert = new AtomicInteger(that.insert.intValue());
        this.update = new AtomicInteger(that.update.intValue());
        this.delete = new AtomicInteger(that.delete.intValue());
        this.merge = new AtomicInteger(that.merge.intValue());
        this.other = new AtomicInteger(that.other.intValue());
        this.allSocketStats = new ConcurrentHashMap<String, SocketStats>(that.allSocketStats);
    }

    protected void socketOperation(String address, SocketStats socketStats) {

        SocketStats existingSocketStats = allSocketStats.putIfAbsent(address, socketStats);
        if (null != existingSocketStats) {
            existingSocketStats.inc(socketStats);
        }

    }

    protected ConcurrentMap<String, SocketStats> getSocketOperations() {
        return allSocketStats;
    }

    protected int executeStatement(Query query) {
        switch (query) {
            case SELECT:
                return select.incrementAndGet();
            case INSERT:
                return insert.incrementAndGet();
            case UPDATE:
                return update.incrementAndGet();
            case DELETE:
                return delete.incrementAndGet();
            case MERGE:
                return merge.incrementAndGet();
            case OTHER:
            default:
                return other.incrementAndGet();
        }
    }

    protected int executedStatements(Query query) {
        switch (query) {
            case ANY:
                return select.get() + insert.get() + update.get() + delete.get() + merge.get() + other.get();
            case SELECT:
                return select.get();
            case INSERT:
                return insert.get();
            case UPDATE:
                return update.get();
            case DELETE:
                return delete.get();
            case MERGE:
                return merge.get();
            case OTHER:
            default:
                return other.get();
        }
    }

}
