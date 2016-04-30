package io.sniffy.sql;

import io.sniffy.Query;
import io.sniffy.Spy;
import io.sniffy.Threads;
import io.sniffy.WrongNumberOfQueriesError;

public class SqlQueries {

    private SqlQueries() {

    }

    public static SqlExpectation_Count none() {
        return exact(0);
    }

    public static SqlExpectation_Count atMostOnce() {
        return between(0,1);
    }

    public static SqlExpectation_Count exact(int count) {
        return between(count, count);
    }

    public static SqlExpectation_Count between(int min, int max) {
        return new SqlExpectation_Count(min, max);
    }

    public static SqlExpectation_Min min(int min) {
        return new SqlExpectation_Min(min);
    }

    public static SqlExpectation_Max max(int max) {
        return new SqlExpectation_Max(max);
    }

    private static class SqlExpectation implements Spy.Expectation{

        protected final int min;
        protected final int max;
        protected final Threads threads;
        protected final Query type;

        protected SqlExpectation(int min, int max, Threads threads, Query type) {
            this.min = min;
            this.max = max;
            this.threads = threads;
            this.type = type;
        }

        @Override
        public <T extends Spy<T>> Spy<T> verify(Spy<T> spy) throws WrongNumberOfQueriesError {

            int numQueries = spy.executedStatements(threads, type);

            if (numQueries > max || numQueries < min) {
                throw new WrongNumberOfQueriesError(
                        threads, type,
                        min, max, numQueries,
                        spy.getExecutedStatements(threads)
                );
            }

            return spy;

        }

    }

    public static class SqlExpectation_Min extends SqlExpectation_Count {

        private SqlExpectation_Min(int min) {
            super(min, Integer.MAX_VALUE);
        }

        public SqlExpectation_Count max(int max) {
            return new SqlExpectation_Count(min, max);
        }

    }

    public static class SqlExpectation_Max extends SqlExpectation_Count {

        private SqlExpectation_Max(int max) {
            super(0, max);
        }

        public SqlExpectation_Count min(int min) {
            return new SqlExpectation_Count(min, max);
        }

    }

    public static class SqlExpectation_Count extends SqlExpectation {

        private SqlExpectation_Count(int min, int max) {
            super(min, max, Threads.CURRENT, Query.ANY);
        }

        public SqlExpectation_Count_Query type(Query query) {
            return new SqlExpectation_Count_Query(min, max, query);
        }

        public SqlExpectation_Count_Query select() {
            return type(Query.SELECT);
        }

        public SqlExpectation_Count_Query insert() {
            return type(Query.INSERT);
        }

        public SqlExpectation_Count_Query update() {
            return type(Query.UPDATE);
        }

        public SqlExpectation_Count_Query delete() {
            return type(Query.DELETE);
        }

        public SqlExpectation_Count_Query merge() {
            return type(Query.MERGE);
        }

        public SqlExpectation_Count_Query other() {
            return type(Query.OTHER);
        }

        public SqlExpectation_Count_Threads threads(Threads threads) {
            return new SqlExpectation_Count_Threads(min, max, threads);
        }

        public SqlExpectation_Count_Threads currentThread() {
            return threads(Threads.CURRENT);
        }

        public SqlExpectation_Count_Threads otherThreads() {
            return threads(Threads.OTHERS);
        }

        public SqlExpectation_Count_Threads anyThreads() {
            return threads(Threads.CURRENT);
        }

    }

    public static class SqlExpectation_Count_Query extends SqlExpectation {

        private SqlExpectation_Count_Query(int min, int max, Query query) {
            super(min, max, Threads.CURRENT, query);
        }

        public SqlExpectation threads(Threads threads) {
            return new SqlExpectation(min, max, threads, type);
        }

        public SqlExpectation currentThread() {
            return threads(Threads.CURRENT);
        }

        public SqlExpectation otherThreads() {
            return threads(Threads.OTHERS);
        }

        public SqlExpectation anyThreads() {
            return threads(Threads.CURRENT);
        }

    }

    public static class SqlExpectation_Count_Threads extends SqlExpectation {

        private SqlExpectation_Count_Threads(int min, int max, Threads threads) {
            super(min, max, threads, Query.ANY);
        }

        public SqlExpectation type(Query query) {
            return new SqlExpectation(min, max, threads, query);
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
