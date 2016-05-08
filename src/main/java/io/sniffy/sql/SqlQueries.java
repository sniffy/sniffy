package io.sniffy.sql;

import io.sniffy.Query;
import io.sniffy.Spy;
import io.sniffy.Threads;
import io.sniffy.WrongNumberOfQueriesError;

import java.util.Map;

import static io.sniffy.Query.ANY;

public class SqlQueries {

    private SqlQueries() {
    }

    public static SqlExpectation_CountQueries none() {
        return exact(0);
    }

    public static SqlExpectation_CountQueries atMostOnce() {
        return between(0,1);
    }

    public static SqlExpectation_CountQueries exact(int count) {
        return between(count, count);
    }

    public static SqlExpectation_CountQueries between(int min, int max) {
        return new SqlExpectation_CountQueries(min, max);
    }

    public static SqlExpectation_MinQueries min(int min) {
        return new SqlExpectation_MinQueries(min);
    }

    public static SqlExpectation_MaxQueries max(int max) {
        return new SqlExpectation_MaxQueries(max);
    }

    private static class SqlExpectation implements Spy.Expectation {

        protected final int minQueries;
        protected final int maxQueries;
        protected final int minRows;
        protected final int maxRows;
        protected final Threads threads;
        protected final Query type;

        protected SqlExpectation(int minQueries, int maxQueries, int minRows, int maxRows, Threads threads, Query type) {
            this.minQueries = minQueries;
            this.maxQueries = maxQueries;
            this.minRows = minRows;
            this.maxRows = maxRows;
            this.threads = threads;
            this.type = type;
        }

        @Override
        public <T extends Spy<T>> Spy<T> verify(Spy<T> spy) throws WrongNumberOfQueriesError {

            int numQueries = 0;
            int numRows = 0;

            for (Map.Entry<StatementMetaData, SqlStats> entry : spy.getExecutedStatements(threads, true).entrySet()) {
                if (ANY == type || type == entry.getKey().query) {
                    SqlStats sqlStats = entry.getValue();
                    numQueries++;
                    numRows += sqlStats.rows.intValue();
                }
            }

            if (numQueries > maxQueries || numQueries < minQueries) {
                throw new WrongNumberOfQueriesError(
                        threads, type,
                        minQueries, maxQueries, numQueries,
                        spy.getExecutedStatements(threads, true).keySet()
                );
            }

            return spy;

        }

    }

    public static class SqlExpectation_MinQueries extends SqlExpectation_CountQueries {

        private SqlExpectation_MinQueries(int minQueries) {
            super(minQueries, Integer.MAX_VALUE);
        }

        public SqlExpectation_CountQueries max(int maxQueries) {
            if (maxQueries < minQueries) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation_CountQueries(minQueries, maxQueries);
        }

    }

    public static class SqlExpectation_MaxQueries extends SqlExpectation_CountQueries {

        private SqlExpectation_MaxQueries(int maxQueries) {
            super(0, maxQueries);
        }

        public SqlExpectation_CountQueries min(int minQueries) {
            if (maxQueries < minQueries) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation_CountQueries(minQueries, maxQueries);
        }

    }

    public static class SqlExpectation_CountQueries extends SqlExpectation {

        private SqlExpectation_CountQueries(int minQueries, int maxQueries) {
            super(minQueries, maxQueries, 0, Integer.MAX_VALUE, Threads.CURRENT, ANY);
            if (minQueries < 0) throw new IllegalArgumentException("min cannot be negative");
            if (maxQueries < minQueries) throw new IllegalArgumentException("max cannot be less than min");
        }

        public SqlExpectation_CountQueries_QueryType type(Query query) {
            return new SqlExpectation_CountQueries_QueryType(minQueries, maxQueries, query);
        }

        public SqlExpectation_CountQueries_QueryType select() {
            return type(Query.SELECT);
        }

        public SqlExpectation_CountQueries_QueryType insert() {
            return type(Query.INSERT);
        }

        public SqlExpectation_CountQueries_QueryType update() {
            return type(Query.UPDATE);
        }

        public SqlExpectation_CountQueries_QueryType delete() {
            return type(Query.DELETE);
        }

        public SqlExpectation_CountQueries_QueryType merge() {
            return type(Query.MERGE);
        }

        // TODO: change name since it clashes with otherThreads()
        public SqlExpectation_CountQueries_QueryType other() {
            return type(Query.OTHER);
        }

        public SqlExpectation_CountQueries_Threads threads(Threads threads) {
            return new SqlExpectation_CountQueries_Threads(minQueries, maxQueries, threads);
        }

        public SqlExpectation_CountQueries_Threads currentThread() {
            return threads(Threads.CURRENT);
        }

        public SqlExpectation_CountQueries_Threads otherThreads() {
            return threads(Threads.OTHERS);
        }

        public SqlExpectation_CountQueries_Threads anyThreads() {
            return threads(Threads.ANY);
        }

    }

    public static class SqlExpectation_CountQueries_QueryType extends SqlExpectation {

        private SqlExpectation_CountQueries_QueryType(int minQueries, int maxQueries, Query query) {
            super(minQueries, maxQueries, 0, Integer.MAX_VALUE, Threads.CURRENT, query);
        }

        public SqlExpectation threads(Threads threads) {
            return new SqlExpectation(minQueries, maxQueries, minRows, maxRows, threads, type);
        }

        public SqlExpectation currentThread() {
            return threads(Threads.CURRENT);
        }

        public SqlExpectation otherThreads() {
            return threads(Threads.OTHERS);
        }

        public SqlExpectation anyThreads() {
            return threads(Threads.ANY);
        }

    }

    public static class SqlExpectation_CountQueries_Threads extends SqlExpectation {

        private SqlExpectation_CountQueries_Threads(int minQueries, int maxQueries, Threads threads) {
            super(minQueries, maxQueries, 0, Integer.MAX_VALUE, threads, ANY);
        }

        public SqlExpectation type(Query query) {
            return new SqlExpectation(minQueries, maxQueries, minRows, maxRows, threads, query);
        }

        public SqlExpectation select() {
            return type(Query.SELECT);
        }

        public SqlExpectation insert() {
            return type(Query.INSERT);
        }

        public SqlExpectation update() {
            return type(Query.UPDATE);
        }

        public SqlExpectation delete() {
            return type(Query.DELETE);
        }

        public SqlExpectation merge() {
            return type(Query.MERGE);
        }

        public SqlExpectation other() {
            return type(Query.OTHER);
        }

    }

}
