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

    public static SqlExpectation_CountQueries noneQueries() {
        return exactQueries(0);
    }

    public static SqlExpectation_CountQueries atMostOneQuery() {
        return queriesBetween(0,1);
    }

    public static SqlExpectation_CountQueries exactQueries(int countQueries) {
        return queriesBetween(countQueries, countQueries);
    }

    public static SqlExpectation_CountQueries queriesBetween(int minQueries, int maxQueries) {
        return new SqlExpectation_CountQueries(minQueries, maxQueries);
    }

    public static SqlExpectation_MinQueries minQueries(int minQueries) {
        return new SqlExpectation_MinQueries(minQueries);
    }

    public static SqlExpectation_MaxQueries maxQueries(int maxQueries) {
        return new SqlExpectation_MaxQueries(maxQueries);
    }

    public static SqlExpectation_CountRows noneRows() {
        return exactRows(0);
    }

    public static SqlExpectation_CountRows atMostOneRow() {
        return rowsBetween(0,1);
    }

    public static SqlExpectation_CountRows exactRows(int countRows) {
        return rowsBetween(countRows, countRows);
    }

    public static SqlExpectation_CountRows rowsBetween(int minRows, int maxRows) {
        return new SqlExpectation_CountRows(minRows, maxRows);
    }

    public static SqlExpectation_CountRows minRows(int minRows) {
        return new SqlExpectation_MinRows(minRows);
    }

    public static SqlExpectation_CountRows maxRows(int maxRows) {
        return new SqlExpectation_MaxRows(maxRows);
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

        public SqlExpectation_CountQueries maxQueries(int maxQueries) {
            if (maxQueries < minQueries) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation_CountQueries(minQueries, maxQueries);
        }

    }

    public static class SqlExpectation_MaxQueries extends SqlExpectation_CountQueries {

        private SqlExpectation_MaxQueries(int maxQueries) {
            super(0, maxQueries);
        }

        public SqlExpectation_CountQueries minQueries(int minQueries) {
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

        public SqlExpectation_CountQueries_CountRows noneRows() {
            return exactRows(0);
        }

        public SqlExpectation_CountQueries_CountRows atMostOneRow() {
            return rowsBetween(0,1);
        }

        public SqlExpectation_CountQueries_CountRows exactRows(int countRows) {
            return rowsBetween(countRows, countRows);
        }

        public SqlExpectation_CountQueries_CountRows rowsBetween(int minRows, int maxRows) {
            return new SqlExpectation_CountQueries_CountRows(minQueries, maxQueries, minRows, maxRows);
        }

        public SqlExpectation_CountQueries_MinRows minRows(int minRows) {
            return new SqlExpectation_CountQueries_MinRows(minQueries, maxQueries, minRows);
        }

        public SqlExpectation_CountQueries_MaxRows maxRows(int maxRows) {
            return new SqlExpectation_CountQueries_MaxRows(minQueries, maxQueries, maxRows);
        }

    }

    public static class SqlExpectation_MinRows extends SqlExpectation_CountRows {

        private SqlExpectation_MinRows(int minRows) {
            super(minRows, Integer.MAX_VALUE);
        }

        public SqlExpectation_CountQueries maxRows(int maxRows) {
            if (maxRows < minRows) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation_CountQueries(minRows, maxRows);
        }

    }

    public static class SqlExpectation_MaxRows extends SqlExpectation_CountRows {

        private SqlExpectation_MaxRows(int maxRows) {
            super(0, maxRows);
        }

        public SqlExpectation_CountQueries minRows(int minRows) {
            if (maxRows < minRows) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation_CountQueries(minRows, maxRows);
        }

    }

    public static class SqlExpectation_CountRows extends SqlExpectation {

        private SqlExpectation_CountRows(int minRows, int maxRows) {
            super(0, Integer.MAX_VALUE, minRows, maxRows, Threads.CURRENT, ANY);
            if (minRows < 0) throw new IllegalArgumentException("min cannot be negative");
            if (maxRows < minRows) throw new IllegalArgumentException("max cannot be less than min");
        }

        public SqlExpectation_CountRows_QueryType type(Query query) {
            return new SqlExpectation_CountRows_QueryType(minQueries, maxQueries, query);
        }

        public SqlExpectation_CountRows_QueryType select() {
            return type(Query.SELECT);
        }

        public SqlExpectation_CountRows_QueryType insert() {
            return type(Query.INSERT);
        }

        public SqlExpectation_CountRows_QueryType update() {
            return type(Query.UPDATE);
        }

        public SqlExpectation_CountRows_QueryType delete() {
            return type(Query.DELETE);
        }

        public SqlExpectation_CountRows_QueryType merge() {
            return type(Query.MERGE);
        }

        // TODO: change name since it clashes with otherThreads()
        public SqlExpectation_CountRows_QueryType other() {
            return type(Query.OTHER);
        }

        public SqlExpectation_CountRows_Threads threads(Threads threads) {
            return new SqlExpectation_CountRows_Threads(minQueries, maxQueries, threads);
        }

        public SqlExpectation_CountRows_Threads currentThread() {
            return threads(Threads.CURRENT);
        }

        public SqlExpectation_CountRows_Threads otherThreads() {
            return threads(Threads.OTHERS);
        }

        public SqlExpectation_CountRows_Threads anyThreads() {
            return threads(Threads.ANY);
        }

    }



    public static class SqlExpectation_CountQueries_CountRows extends SqlExpectation {

        private SqlExpectation_CountQueries_CountRows(int minQueries, int maxQueries, int minRows, int maxRows) {
            super(minQueries, maxQueries, minRows, maxRows, Threads.CURRENT, ANY);
            if (minQueries < 0) throw new IllegalArgumentException("min cannot be negative");
            if (maxQueries < minQueries) throw new IllegalArgumentException("max cannot be less than min");
            if (minRows < 0) throw new IllegalArgumentException("min cannot be negative");
            if (maxRows < minRows) throw new IllegalArgumentException("max cannot be less than min");
        }

        // TODO: change method below

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

    public static class SqlExpectation_CountQueries_MinRows extends SqlExpectation_CountQueries_CountRows {

        private SqlExpectation_CountQueries_MinRows(int minQueries, int maxQueries, int minRows) {
            super(minQueries, maxQueries, minRows, Integer.MAX_VALUE);
        }

        public SqlExpectation_CountQueries_CountRows maxRows(int maxRows) {
            if (maxRows < minRows) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation_CountQueries_CountRows(minQueries, maxQueries, minRows, maxRows);
        }

    }

    public static class SqlExpectation_CountQueries_MaxRows extends SqlExpectation_CountQueries_CountRows {

        private SqlExpectation_CountQueries_MaxRows(int minQueries, int maxQueries, int maxRows) {
            super(minQueries, maxQueries, 0, maxRows);
        }

        public SqlExpectation_CountQueries_CountRows minRows(int minRows) {
            if (maxRows < minRows) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation_CountQueries_CountRows(minQueries, maxQueries, minRows, maxRows);
        }

    }

    public static class SqlExpectation_CountQueries_QueryType extends SqlExpectation {

        private SqlExpectation_CountQueries_QueryType(int minQueries, int maxQueries, Query query) {
            super(minQueries, maxQueries, 0, Integer.MAX_VALUE, Threads.CURRENT, query);
        }

        public SqlExpectation threads(Threads threads) {
            return new SqlExpectation_CountQueries_Threads_QueryType(minQueries, maxQueries, threads, type);
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

    public static class SqlExpectation_CountRows_QueryType extends SqlExpectation {

        private SqlExpectation_CountRows_QueryType(int minRows, int maxRows, Query query) {
            super( 0, Integer.MAX_VALUE, minRows, maxRows, Threads.CURRENT, query);
        }

        public SqlExpectation threads(Threads threads) {
            return new SqlExpectation_CountRows_Threads_QueryType(minRows, maxRows, threads, type);
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
            return new SqlExpectation_CountQueries_Threads_QueryType(minQueries, maxQueries, threads, query);
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

    public static class SqlExpectation_CountRows_Threads extends SqlExpectation {

        private SqlExpectation_CountRows_Threads(int minRows, int maxRows, Threads threads) {
            super(0, Integer.MAX_VALUE, minRows, maxRows, threads, ANY);
        }

        public SqlExpectation type(Query query) {
            return new SqlExpectation_CountRows_Threads_QueryType(minRows, maxRows, threads, query);
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

    public static class SqlExpectation_CountQueries_Threads_QueryType extends SqlExpectation {

        private SqlExpectation_CountQueries_Threads_QueryType(int minQueries, int maxQueries, Threads threads, Query queryType) {
            super(minQueries, maxQueries, 0, Integer.MAX_VALUE, threads, queryType);
        }

        // TODO implement rows methods

    }

    public static class SqlExpectation_CountRows_Threads_QueryType extends SqlExpectation {

        private SqlExpectation_CountRows_Threads_QueryType(int minRows, int maxRows, Threads threads, Query queryType) {
            super(0, Integer.MAX_VALUE, minRows, maxRows, threads, queryType);
        }

        // TODO implement rows methods

    }

}
