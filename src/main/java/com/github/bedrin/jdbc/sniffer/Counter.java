package com.github.bedrin.jdbc.sniffer;

import com.github.bedrin.jdbc.sniffer.sql.Query;

import java.util.concurrent.atomic.AtomicInteger;

// TODO: consider making counters hierarchical, i.e. DML, DDL as top level , INSERT, CREATE TABLE as 2 level, e.t.c
class Counter {

    final AtomicInteger select;
    final AtomicInteger insert;
    final AtomicInteger update;
    final AtomicInteger delete;
    final AtomicInteger merge;
    final AtomicInteger other;

    public Counter() {
        this(
                new AtomicInteger(),
                new AtomicInteger(),
                new AtomicInteger(),
                new AtomicInteger(),
                new AtomicInteger(),
                new AtomicInteger()
        );
    }

    public Counter(
            AtomicInteger select,
            AtomicInteger insert,
            AtomicInteger update,
            AtomicInteger delete,
            AtomicInteger merge,
            AtomicInteger other) {
        this.select = select;
        this.insert = insert;
        this.update = update;
        this.delete = delete;
        this.merge = merge;
        this.other = other;
    }

    public Counter(Counter that) {
        this.select = new AtomicInteger(that.select.intValue());
        this.insert = new AtomicInteger(that.insert.intValue());
        this.update = new AtomicInteger(that.update.intValue());
        this.delete = new AtomicInteger(that.delete.intValue());
        this.merge = new AtomicInteger(that.merge.intValue());
        this.other = new AtomicInteger(that.other.intValue());
    }

    int executeStatement(Query.Type queryType) {
        switch (queryType) {
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

    int executedStatements(Query.Type queryType) {
        switch (queryType) {
            case ALL:
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
