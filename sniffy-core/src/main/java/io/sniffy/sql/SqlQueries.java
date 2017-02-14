package io.sniffy.sql;

import io.sniffy.Sniffy;
import io.sniffy.SniffyAssertionError;
import io.sniffy.Spy;
import io.sniffy.Threads;

import java.util.Map;

import static io.sniffy.sql.SqlStatement.ANY;
import static io.sniffy.sql.SqlStatement.SYSTEM;

/**
 * An utility class for creating {@link SqlExpectation}s
 * @see Spy#expect(Spy.Expectation)
 * @see Sniffy#expect(Spy.Expectation)
 * @since 3.1
 */
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

    public static SqlExpectation_MinRows minRows(int minRows) {
        return new SqlExpectation_MinRows(minRows);
    }

    public static SqlExpectation_MaxRows maxRows(int maxRows) {
        return new SqlExpectation_MaxRows(maxRows);
    }

    public static class SqlExpectation implements Spy.Expectation {

        protected final int minQueries;
        protected final int maxQueries;
        protected final int minRows;
        protected final int maxRows;
        protected final Threads threads;
        protected final SqlStatement type;

        public SqlExpectation(int minQueries, int maxQueries, int minRows, int maxRows, Threads threads, SqlStatement type) {
            this.minQueries = minQueries;
            this.maxQueries = maxQueries;
            this.minRows = minRows;
            this.maxRows = maxRows;
            this.threads = threads;
            this.type = type;
        }

        @Override
        public <T extends Spy<T>> Spy<T> verify(Spy<T> spy) throws SniffyAssertionError {

            int numQueries = 0;
            int numRows = 0;

            for (Map.Entry<StatementMetaData, SqlStats> entry : spy.getExecutedStatements(threads, true).entrySet()) {
                if ((ANY == type && SYSTEM != entry.getKey().query) || type == entry.getKey().query) {
                    SqlStats sqlStats = entry.getValue();
                    numQueries += sqlStats.queries.intValue();
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

            if (numRows > maxRows || numRows < minRows) {
                throw new WrongNumberOfRowsError(
                        threads, type,
                        minRows, maxRows, numRows,
                        spy.getExecutedStatements(threads, true)
                );
            }

            return spy;

        }

    }

    // queryCount

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

        public SqlExpectation_CountQueries_QueryType type(SqlStatement query) {
            return new SqlExpectation_CountQueries_QueryType(minQueries, maxQueries, query);
        }

        public SqlExpectation_CountQueries_QueryType select() {
            return type(SqlStatement.SELECT);
        }

        public SqlExpectation_CountQueries_QueryType insert() {
            return type(SqlStatement.INSERT);
        }

        public SqlExpectation_CountQueries_QueryType update() {
            return type(SqlStatement.UPDATE);
        }

        public SqlExpectation_CountQueries_QueryType delete() {
            return type(SqlStatement.DELETE);
        }

        public SqlExpectation_CountQueries_QueryType merge() {
            return type(SqlStatement.MERGE);
        }

        // TODO: change name since it clashes with otherThreads()
        public SqlExpectation_CountQueries_QueryType other() {
            return type(SqlStatement.OTHER);
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

    // rowCount

    public static class SqlExpectation_MinRows extends SqlExpectation_CountRows {

        private SqlExpectation_MinRows(int minRows) {
            super(minRows, Integer.MAX_VALUE);
        }

        public SqlExpectation_CountRows maxRows(int maxRows) {
            if (maxRows < minRows) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation_CountRows(minRows, maxRows);
        }

    }

    public static class SqlExpectation_MaxRows extends SqlExpectation_CountRows {

        private SqlExpectation_MaxRows(int maxRows) {
            super(0, maxRows);
        }

        public SqlExpectation_CountRows minRows(int minRows) {
            if (maxRows < minRows) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation_CountRows(minRows, maxRows);
        }

    }

    public static class SqlExpectation_CountRows extends SqlExpectation {

        private SqlExpectation_CountRows(int minRows, int maxRows) {
            super(0, Integer.MAX_VALUE, minRows, maxRows, Threads.CURRENT, ANY);
            if (minRows < 0) throw new IllegalArgumentException("min cannot be negative");
            if (maxRows < minRows) throw new IllegalArgumentException("max cannot be less than min");
        }

        public SqlExpectation_CountRows_QueryType type(SqlStatement query) {
            return new SqlExpectation_CountRows_QueryType(minRows, maxRows, query);
        }

        public SqlExpectation_CountRows_QueryType select() {
            return type(SqlStatement.SELECT);
        }

        public SqlExpectation_CountRows_QueryType insert() {
            return type(SqlStatement.INSERT);
        }

        public SqlExpectation_CountRows_QueryType update() {
            return type(SqlStatement.UPDATE);
        }

        public SqlExpectation_CountRows_QueryType delete() {
            return type(SqlStatement.DELETE);
        }

        public SqlExpectation_CountRows_QueryType merge() {
            return type(SqlStatement.MERGE);
        }

        // TODO: change name since it clashes with otherThreads()
        public SqlExpectation_CountRows_QueryType other() {
            return type(SqlStatement.OTHER);
        }

        public SqlExpectation_CountRows_Threads threads(Threads threads) {
            return new SqlExpectation_CountRows_Threads(minRows, maxRows, threads);
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

        // TODO: This method does not make sense as probably some others
        public SqlExpectation_CountQueries_CountRows noneQueries() {
            return exactQueries(0);
        }

        public SqlExpectation_CountQueries_CountRows atMostOneQuery() {
            return queriesBetween(0,1);
        }

        public SqlExpectation_CountQueries_CountRows exactQueries(int countQueries) {
            return queriesBetween(countQueries, countQueries);
        }

        public SqlExpectation_CountQueries_CountRows queriesBetween(int minQueries, int maxQueries) {
            return new SqlExpectation_CountQueries_CountRows(minQueries, maxQueries, minRows, maxRows);
        }

        public SqlExpectation_CountRows_MinQueries minQueries(int minQueries) {
            return new SqlExpectation_CountRows_MinQueries(minQueries, minRows, maxRows);
        }

        public SqlExpectation_CountRows_MaxQueries maxQueries(int maxQueries) {
            return new SqlExpectation_CountRows_MaxQueries(maxQueries, minRows, maxRows);
        }

    }

    // queryCount + rowCount

    public static class SqlExpectation_CountQueries_CountRows extends SqlExpectation {

        private SqlExpectation_CountQueries_CountRows(int minQueries, int maxQueries, int minRows, int maxRows) {
            super(minQueries, maxQueries, minRows, maxRows, Threads.CURRENT, ANY);
            if (minQueries < 0) throw new IllegalArgumentException("min cannot be negative");
            if (maxQueries < minQueries) throw new IllegalArgumentException("max cannot be less than min");
            if (minRows < 0) throw new IllegalArgumentException("min cannot be negative");
            if (maxRows < minRows) throw new IllegalArgumentException("max cannot be less than min");
        }

        public SqlExpectation_CountQueries_CountRows_QueryType type(SqlStatement query) {
            return new SqlExpectation_CountQueries_CountRows_QueryType(minQueries, maxQueries, minRows, maxRows, query);
        }

        public SqlExpectation_CountQueries_CountRows_QueryType select() {
            return type(SqlStatement.SELECT);
        }

        public SqlExpectation_CountQueries_CountRows_QueryType insert() {
            return type(SqlStatement.INSERT);
        }

        public SqlExpectation_CountQueries_CountRows_QueryType update() {
            return type(SqlStatement.UPDATE);
        }

        public SqlExpectation_CountQueries_CountRows_QueryType delete() {
            return type(SqlStatement.DELETE);
        }

        public SqlExpectation_CountQueries_CountRows_QueryType merge() {
            return type(SqlStatement.MERGE);
        }

        // TODO: change name since it clashes with otherThreads()
        public SqlExpectation_CountQueries_CountRows_QueryType other() {
            return type(SqlStatement.OTHER);
        }

        public SqlExpectation_CountQueries_CountRows_Threads threads(Threads threads) {
            return new SqlExpectation_CountQueries_CountRows_Threads(minQueries, maxQueries, minRows, maxRows, threads);
        }

        public SqlExpectation_CountQueries_CountRows_Threads currentThread() {
            return threads(Threads.CURRENT);
        }

        public SqlExpectation_CountQueries_CountRows_Threads otherThreads() {
            return threads(Threads.OTHERS);
        }

        public SqlExpectation_CountQueries_CountRows_Threads anyThreads() {
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

    public static class SqlExpectation_CountRows_MinQueries extends SqlExpectation_CountQueries_CountRows {

        private SqlExpectation_CountRows_MinQueries(int minQueries, int minRows, int maxRows) {
            super(minQueries, Integer.MAX_VALUE, minRows, maxRows);
        }

        public SqlExpectation_CountQueries_CountRows maxQueries(int maxQueries) {
            if (maxQueries < minQueries) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation_CountQueries_CountRows(minQueries, maxQueries, minRows, maxRows);
        }

    }

    public static class SqlExpectation_CountRows_MaxQueries extends SqlExpectation_CountQueries_CountRows {

        private SqlExpectation_CountRows_MaxQueries(int maxQueries, int minRows, int maxRows) {
            super(0, maxQueries, minRows, maxRows);
        }

        public SqlExpectation_CountQueries_CountRows minQueries(int minQueries) {
            if (maxQueries < minQueries) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation_CountQueries_CountRows(minQueries, maxQueries, minRows, maxRows);
        }

    }

    // queryCount + queryType

    public static class SqlExpectation_CountQueries_QueryType extends SqlExpectation {

        private SqlExpectation_CountQueries_QueryType(int minQueries, int maxQueries, SqlStatement query) {
            super(minQueries, maxQueries, 0, Integer.MAX_VALUE, Threads.CURRENT, query);
        }

        public SqlExpectation_CountQueries_Threads_QueryType threads(Threads threads) {
            return new SqlExpectation_CountQueries_Threads_QueryType(minQueries, maxQueries, threads, type);
        }

        public SqlExpectation_CountQueries_Threads_QueryType currentThread() {
            return threads(Threads.CURRENT);
        }

        public SqlExpectation_CountQueries_Threads_QueryType otherThreads() {
            return threads(Threads.OTHERS);
        }

        public SqlExpectation_CountQueries_Threads_QueryType anyThreads() {
            return threads(Threads.ANY);
        }

        public SqlExpectation_CountQueries_CountRows_QueryType noneRows() {
            return exactRows(0);
        }

        public SqlExpectation_CountQueries_CountRows_QueryType atMostOneRow() {
            return rowsBetween(0,1);
        }

        public SqlExpectation_CountQueries_CountRows_QueryType exactRows(int countRows) {
            return rowsBetween(countRows, countRows);
        }

        public SqlExpectation_CountQueries_CountRows_QueryType rowsBetween(int minRows, int maxRows) {
            return new SqlExpectation_CountQueries_CountRows_QueryType(minQueries, maxQueries, minRows, maxRows, type);
        }

        public SqlExpectation_CountQueries_MinRows_QueryType minRows(int minRows) {
            return new SqlExpectation_CountQueries_MinRows_QueryType(minQueries, maxQueries, minRows, type);
        }

        public SqlExpectation_CountQueries_MaxRows_QueryType maxRows(int maxRows) {
            return new SqlExpectation_CountQueries_MaxRows_QueryType(minQueries, maxQueries, maxRows, type);
        }

    }

    // queryCount + rowCount + queryType

    public static class SqlExpectation_CountQueries_CountRows_QueryType extends SqlExpectation {

        private SqlExpectation_CountQueries_CountRows_QueryType(int minQueries, int maxQueries, int minRows, int maxRows, SqlStatement query) {
            super(minQueries, maxQueries, minRows, maxRows, Threads.CURRENT, query);
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

    public static class SqlExpectation_CountQueries_MinRows_QueryType extends SqlExpectation_CountQueries_CountRows_QueryType {

        private SqlExpectation_CountQueries_MinRows_QueryType(int minQueries, int maxQueries, int minRows, SqlStatement type) {
            super(minQueries, maxQueries, minRows, Integer.MAX_VALUE, type);
        }

        public SqlExpectation_CountQueries_CountRows_QueryType maxRows(int maxRows) {
            if (maxRows < minRows) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation_CountQueries_CountRows_QueryType(minQueries, maxQueries, minRows, maxRows, type);
        }

    }

    public static class SqlExpectation_CountQueries_MaxRows_QueryType extends SqlExpectation_CountQueries_CountRows_QueryType {

        private SqlExpectation_CountQueries_MaxRows_QueryType(int minQueries, int maxQueries, int maxRows, SqlStatement type) {
            super(minQueries, maxQueries, 0, maxRows, type);
        }

        public SqlExpectation_CountQueries_CountRows_QueryType minRows(int minRows) {
            if (maxRows < minRows) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation_CountQueries_CountRows_QueryType(minQueries, maxQueries, minRows, maxRows, type);
        }

    }

    public static class SqlExpectation_CountRows_MinQueries_QueryType extends SqlExpectation_CountQueries_CountRows_QueryType {

        private SqlExpectation_CountRows_MinQueries_QueryType(int minRows, int maxRows, int minQueries, SqlStatement type) {
            super(minQueries, Integer.MAX_VALUE, minRows, maxRows, type);
        }

        public SqlExpectation_CountQueries_CountRows_QueryType maxQueries(int maxQueries) {
            if (maxQueries < minQueries) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation_CountQueries_CountRows_QueryType(minQueries, maxQueries, minRows, maxRows, type);
        }

    }

    public static class SqlExpectation_CountRows_MaxQueries_QueryType extends SqlExpectation_CountQueries_CountRows_QueryType {

        private SqlExpectation_CountRows_MaxQueries_QueryType(int minRows, int maxRows, int maxQueries, SqlStatement type) {
            super(0, maxQueries, minRows, maxRows, type);
        }

        public SqlExpectation_CountQueries_CountRows_QueryType minQueries(int minQueries) {
            if (maxQueries < minQueries) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation_CountQueries_CountRows_QueryType(minQueries, maxQueries, minRows, maxRows, type);
        }

    }

    public static class SqlExpectation_CountRows_MinQueries_Threads extends SqlExpectation_CountQueries_CountRows_Threads {

        private SqlExpectation_CountRows_MinQueries_Threads(int minRows, int maxRows, int minQueries, Threads threads) {
            super(minQueries, Integer.MAX_VALUE, minRows, maxRows, threads);
        }

        public SqlExpectation_CountQueries_CountRows_Threads maxQueries(int maxQueries) {
            if (maxQueries < minQueries) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation_CountQueries_CountRows_Threads(minQueries, maxQueries, minRows, maxRows, threads);
        }

    }

    public static class SqlExpectation_CountRows_MaxQueries_Threads extends SqlExpectation_CountQueries_CountRows_Threads {

        private SqlExpectation_CountRows_MaxQueries_Threads(int minRows, int maxRows, int maxQueries, Threads threads) {
            super(0, maxQueries, minRows, maxRows, threads);
        }

        public SqlExpectation_CountQueries_CountRows_Threads minQueries(int minQueries) {
            if (maxQueries < minQueries) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation_CountQueries_CountRows_Threads(minQueries, maxQueries, minRows, maxRows, threads);
        }

    }

    // rowCount + queryType

    public static class SqlExpectation_CountRows_QueryType extends SqlExpectation {

        private SqlExpectation_CountRows_QueryType(int minRows, int maxRows, SqlStatement query) {
            super( 0, Integer.MAX_VALUE, minRows, maxRows, Threads.CURRENT, query);
        }

        public SqlExpectation_CountRows_Threads_QueryType threads(Threads threads) {
            return new SqlExpectation_CountRows_Threads_QueryType(minRows, maxRows, threads, type);
        }

        public SqlExpectation_CountRows_Threads_QueryType currentThread() {
            return threads(Threads.CURRENT);
        }

        public SqlExpectation_CountRows_Threads_QueryType otherThreads() {
            return threads(Threads.OTHERS);
        }

        public SqlExpectation_CountRows_Threads_QueryType anyThreads() {
            return threads(Threads.ANY);
        }

        public SqlExpectation_CountQueries_CountRows_QueryType noneQueries() {
            return exactQueries(0);
        }

        public SqlExpectation_CountQueries_CountRows_QueryType atMostOneQuery() {
            return queriesBetween(0,1);
        }

        public SqlExpectation_CountQueries_CountRows_QueryType exactQueries(int countQueries) {
            return queriesBetween(countQueries, countQueries);
        }

        public SqlExpectation_CountQueries_CountRows_QueryType queriesBetween(int minQueries, int maxQueries) {
            return new SqlExpectation_CountQueries_CountRows_QueryType(minQueries, maxQueries, minRows, maxRows, type);
        }

        public SqlExpectation_CountRows_MinQueries_QueryType minQueries(int minQueries) {
            return new SqlExpectation_CountRows_MinQueries_QueryType(minRows, maxRows, minQueries, type);
        }

        public SqlExpectation_CountRows_MaxQueries_QueryType maxQueries(int maxQueries) {
            return new SqlExpectation_CountRows_MaxQueries_QueryType(minRows, maxRows, maxQueries, type);
        }

    }

    // queryCount + threads

    public static class SqlExpectation_CountQueries_Threads extends SqlExpectation {

        private SqlExpectation_CountQueries_Threads(int minQueries, int maxQueries, Threads threads) {
            super(minQueries, maxQueries, 0, Integer.MAX_VALUE, threads, ANY);
        }

        public SqlExpectation_CountQueries_Threads_QueryType type(SqlStatement query) {
            return new SqlExpectation_CountQueries_Threads_QueryType(minQueries, maxQueries, threads, query);
        }

        public SqlExpectation_CountQueries_Threads_QueryType select() {
            return type(SqlStatement.SELECT);
        }

        public SqlExpectation_CountQueries_Threads_QueryType insert() {
            return type(SqlStatement.INSERT);
        }

        public SqlExpectation_CountQueries_Threads_QueryType update() {
            return type(SqlStatement.UPDATE);
        }

        public SqlExpectation_CountQueries_Threads_QueryType delete() {
            return type(SqlStatement.DELETE);
        }

        public SqlExpectation_CountQueries_Threads_QueryType merge() {
            return type(SqlStatement.MERGE);
        }

        public SqlExpectation_CountQueries_Threads_QueryType other() {
            return type(SqlStatement.OTHER);
        }

        public SqlExpectation_CountQueries_CountRows_Threads noneRows() {
            return exactRows(0);
        }

        public SqlExpectation_CountQueries_CountRows_Threads atMostOneRow() {
            return rowsBetween(0,1);
        }

        public SqlExpectation_CountQueries_CountRows_Threads exactRows(int countRows) {
            return rowsBetween(countRows, countRows);
        }

        public SqlExpectation_CountQueries_CountRows_Threads rowsBetween(int minRows, int maxRows) {
            return new SqlExpectation_CountQueries_CountRows_Threads(minQueries, maxQueries, minRows, maxRows, threads);
        }

        public SqlExpectation_CountQueries_MinRows_Threads minRows(int minRows) {
            return new SqlExpectation_CountQueries_MinRows_Threads(minQueries, maxQueries, minRows, threads);
        }

        public SqlExpectation_CountQueries_MaxRows_Threads maxRows(int maxRows) {
            return new SqlExpectation_CountQueries_MaxRows_Threads(minQueries, maxQueries, maxRows, threads);
        }

    }

    // rowCount + threads

    public static class SqlExpectation_CountRows_Threads extends SqlExpectation {

        private SqlExpectation_CountRows_Threads(int minRows, int maxRows, Threads threads) {
            super(0, Integer.MAX_VALUE, minRows, maxRows, threads, ANY);
        }

        public SqlExpectation_CountRows_Threads_QueryType type(SqlStatement query) {
            return new SqlExpectation_CountRows_Threads_QueryType(minRows, maxRows, threads, query);
        }

        public SqlExpectation_CountRows_Threads_QueryType select() {
            return type(SqlStatement.SELECT);
        }

        public SqlExpectation_CountRows_Threads_QueryType insert() {
            return type(SqlStatement.INSERT);
        }

        public SqlExpectation_CountRows_Threads_QueryType update() {
            return type(SqlStatement.UPDATE);
        }

        public SqlExpectation_CountRows_Threads_QueryType delete() {
            return type(SqlStatement.DELETE);
        }

        public SqlExpectation_CountRows_Threads_QueryType merge() {
            return type(SqlStatement.MERGE);
        }

        public SqlExpectation_CountRows_Threads_QueryType other() {
            return type(SqlStatement.OTHER);
        }

        public SqlExpectation_CountQueries_CountRows_Threads noneQueries() {
            return exactQueries(0);
        }

        public SqlExpectation_CountQueries_CountRows_Threads atMostOneQuery() {
            return queriesBetween(0,1);
        }

        public SqlExpectation_CountQueries_CountRows_Threads exactQueries(int countQueries) {
            return queriesBetween(countQueries, countQueries);
        }

        public SqlExpectation_CountQueries_CountRows_Threads queriesBetween(int minQueries, int maxQueries) {
            return new SqlExpectation_CountQueries_CountRows_Threads(minQueries, maxQueries, minRows, maxRows, threads);
        }

        public SqlExpectation_CountRows_MinQueries_Threads minQueries(int minQueries) {
            return new SqlExpectation_CountRows_MinQueries_Threads(minRows, maxRows, minQueries, threads);
        }

        public SqlExpectation_CountRows_MaxQueries_Threads maxQueries(int maxQueries) {
            return new SqlExpectation_CountRows_MaxQueries_Threads(minRows, maxRows, maxQueries, threads);
        }

    }


    // queryCount + rowCount + threads

    public static class SqlExpectation_CountQueries_CountRows_Threads extends SqlExpectation {

        private SqlExpectation_CountQueries_CountRows_Threads(int minQueries, int maxQueries, int minRows, int maxRows, Threads threads) {
            super(minQueries, maxQueries, minRows, maxRows, threads, ANY);
        }

        public SqlExpectation type(SqlStatement query) {
            return new SqlExpectation(minQueries, maxQueries, minRows, maxRows, threads, query);
        }

        public SqlExpectation select() {
            return type(SqlStatement.SELECT);
        }

        public SqlExpectation insert() {
            return type(SqlStatement.INSERT);
        }

        public SqlExpectation update() {
            return type(SqlStatement.UPDATE);
        }

        public SqlExpectation delete() {
            return type(SqlStatement.DELETE);
        }

        public SqlExpectation merge() {
            return type(SqlStatement.MERGE);
        }

        public SqlExpectation other() {
            return type(SqlStatement.OTHER);
        }

    }

    public static class SqlExpectation_CountQueries_MinRows_Threads extends SqlExpectation_CountQueries_CountRows_Threads {

        private SqlExpectation_CountQueries_MinRows_Threads(int minQueries, int maxQueries, int minRows, Threads threads) {
            super(minQueries, maxQueries, minRows, Integer.MAX_VALUE, threads);
        }

        public SqlExpectation_CountQueries_CountRows_Threads maxRows(int maxRows) {
            if (maxRows < minRows) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation_CountQueries_CountRows_Threads(minQueries, maxQueries, minRows, maxRows, threads);
        }

    }

    public static class SqlExpectation_CountQueries_MaxRows_Threads extends SqlExpectation_CountQueries_CountRows_Threads {

        private SqlExpectation_CountQueries_MaxRows_Threads(int minQueries, int maxQueries, int maxRows, Threads threads) {
            super(minQueries, maxQueries, 0, maxRows, threads);
        }

        public SqlExpectation_CountQueries_CountRows_Threads minRows(int minRows) {
            if (maxRows < minRows) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation_CountQueries_CountRows_Threads(minQueries, maxQueries, minRows, maxRows, threads);
        }

    }

    // queryCount + thread + queryType

    public static class SqlExpectation_CountQueries_Threads_QueryType extends SqlExpectation {

        private SqlExpectation_CountQueries_Threads_QueryType(int minQueries, int maxQueries, Threads threads, SqlStatement queryType) {
            super(minQueries, maxQueries, 0, Integer.MAX_VALUE, threads, queryType);
        }

        public SqlExpectation noneRows() {
            return exactRows(0);
        }

        public SqlExpectation atMostOneRow() {
            return rowsBetween(0,1);
        }

        public SqlExpectation exactRows(int countRows) {
            return rowsBetween(countRows, countRows);
        }

        public SqlExpectation rowsBetween(int minRows, int maxRows) {
            return new SqlExpectation(minQueries, maxQueries, minRows, maxRows, threads, type);
        }

        public SqlExpectation_CountQueries_Threads_QueryType_MinRows minRows(int minRows) {
            return new SqlExpectation_CountQueries_Threads_QueryType_MinRows(minQueries, maxQueries, minRows, threads, type);
        }

        public SqlExpectation_CountQueries_Threads_QueryType_MaxRows maxRows(int maxRows) {
            return new SqlExpectation_CountQueries_Threads_QueryType_MaxRows(minQueries, maxQueries, maxRows, threads, type);
        }

    }

    public static class SqlExpectation_CountQueries_Threads_QueryType_MinRows extends SqlExpectation {

        private SqlExpectation_CountQueries_Threads_QueryType_MinRows(int minQueries, int maxQueries, int minRows, Threads threads, SqlStatement type) {
            super(minQueries, maxQueries, minRows, Integer.MAX_VALUE, threads, type);
        }

        public SqlExpectation maxRows(int maxRows) {
            if (maxRows < minRows) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation(minQueries, maxQueries, minRows, maxRows, threads, type);
        }

    }

    public static class SqlExpectation_CountQueries_Threads_QueryType_MaxRows extends SqlExpectation {

        private SqlExpectation_CountQueries_Threads_QueryType_MaxRows(int minQueries, int maxQueries, int maxRows, Threads threads, SqlStatement type) {
            super(minQueries, maxQueries, 0, maxRows, threads, type);
        }

        public SqlExpectation minRows(int minRows) {
            if (maxRows < minRows) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation(minQueries, maxQueries, minRows, maxRows, threads, type);
        }

    }

    // rowCount + thread + queryType

    public static class SqlExpectation_CountRows_Threads_QueryType extends SqlExpectation {

        private SqlExpectation_CountRows_Threads_QueryType(int minRows, int maxRows, Threads threads, SqlStatement queryType) {
            super(0, Integer.MAX_VALUE, minRows, maxRows, threads, queryType);
        }

        public SqlExpectation noneQueries() {
            return exactQueries(0);
        }

        public SqlExpectation atMostOneQuery() {
            return queriesBetween(0,1);
        }

        public SqlExpectation exactQueries(int countQueries) {
            return queriesBetween(countQueries, countQueries);
        }

        public SqlExpectation queriesBetween(int minQueries, int maxQueries) {
            return new SqlExpectation(minQueries, maxQueries, minRows, maxRows, threads, type);
        }

        public SqlExpectation_CountRows_Threads_QueryType_MinQueries minQueries(int minQueries) {
            return new SqlExpectation_CountRows_Threads_QueryType_MinQueries(minRows, maxRows, minQueries, threads, type);
        }

        public SqlExpectation_CountRows_Threads_QueryType_MaxQueries maxQueries(int maxQueries) {
            return new SqlExpectation_CountRows_Threads_QueryType_MaxQueries(minRows, maxRows, maxQueries, threads, type);
        }

    }

    public static class SqlExpectation_CountRows_Threads_QueryType_MinQueries extends SqlExpectation {

        private SqlExpectation_CountRows_Threads_QueryType_MinQueries(int minRows, int maxRows, int minQueries, Threads threads, SqlStatement type) {
            super(minQueries, Integer.MAX_VALUE, minRows, maxRows, threads, type);
        }

        public SqlExpectation maxQueries(int maxQueries) {
            if (maxQueries < minQueries) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation(minQueries, maxQueries, minRows, maxRows, threads, type);
        }

    }

    public static class SqlExpectation_CountRows_Threads_QueryType_MaxQueries extends SqlExpectation {

        private SqlExpectation_CountRows_Threads_QueryType_MaxQueries(int minRows, int maxRows, int maxQueries, Threads threads, SqlStatement type) {
            super(0, maxQueries, minRows, maxRows, threads, type);
        }

        public SqlExpectation minQueries(int minQueries) {
            if (maxQueries < minQueries) throw new IllegalArgumentException("max cannot be less than min");
            return new SqlExpectation(minQueries, maxQueries, minRows, maxRows, threads, type);
        }

    }

}
