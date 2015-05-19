package com.github.bedrin.jdbc.sniffer.sql;

public class Query {

    public final Type type;

    public Query(Type type) {
        this.type = type;
    }

    public enum Type {
        SELECT,
        INSERT,
        UPDATE,
        DELETE,
        MERGE,
        OTHER,
        ALL
    }

    public static Query parse(String sql) {

        if (null == sql) return null;

        String normalized = sql.trim().toLowerCase();

        Type type;

        if (normalized.startsWith("select ")) {
            // TODO: can start with "WITH" statement
            type = Type.SELECT;
        } else if (normalized.startsWith("insert ")) {
            type = Type.INSERT;
        } else if (normalized.startsWith("update ")) {
            type = Type.UPDATE;
        } else if (normalized.startsWith("delete ")) {
            type = Type.DELETE;
        } else if (normalized.startsWith("merge ")) {
            // TODO: can start with "WITH" statement
            type = Type.MERGE;
        } else {
            type = Type.OTHER;
        }

        return new Query(type);
    }

}
