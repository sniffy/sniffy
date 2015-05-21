package com.github.bedrin.jdbc.sniffer.sql;

import com.github.bedrin.jdbc.sniffer.Query;

public class StatementMetaData {

    public final Query query;

    public StatementMetaData(Query query) {
        this.query = query;
    }

    public static StatementMetaData parse(String sql) {

        if (null == sql) return null;

        String normalized = sql.trim().toLowerCase();

        Query query;

        if (normalized.startsWith("select ")) {
            // TODO: can start with "WITH" statement
            query = Query.SELECT;
        } else if (normalized.startsWith("insert ")) {
            query = Query.INSERT;
        } else if (normalized.startsWith("update ")) {
            query = Query.UPDATE;
        } else if (normalized.startsWith("delete ")) {
            query = Query.DELETE;
        } else if (normalized.startsWith("merge ")) {
            // TODO: can start with "WITH" statement
            query = Query.MERGE;
        } else {
            query = Query.OTHER;
        }

        return new StatementMetaData(query);
    }

}
