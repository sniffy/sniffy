package io.sniffy;

import io.sniffy.sql.SqlQueries;
import io.sniffy.sql.SqlStatement;
import io.sniffy.sql.SqlStats;
import io.sniffy.sql.StatementMetaData;

import java.util.Map;

import static io.sniffy.Threads.CURRENT;

/**
 * @see Spy
 * @see CurrentThreadSpy
 */
@Deprecated
abstract class LegacySpy<C extends Spy<C>> extends BaseSpy<C> {

    public final static SqlStatement adapter(Query query) {
        switch (query) {
            case SELECT:
                return SqlStatement.SELECT;
            case INSERT:
                return SqlStatement.INSERT;
            case UPDATE:
                return SqlStatement.UPDATE;
            case DELETE:
                return SqlStatement.DELETE;
            case MERGE:
                return SqlStatement.MERGE;
            case OTHER:
                return SqlStatement.OTHER;
            case SYSTEM:
                return SqlStatement.SYSTEM;
            case ANY:
            default:
                return SqlStatement.ANY;
        }
    }


    /**
     * Executes the {@link Sniffer.Executable#execute()} method on provided argument and verifies the expectations
     * @throws SniffyAssertionError if wrong number of queries was executed
     * @since 2.0
     */
    @Deprecated
    public C execute(Sniffer.Executable executable) throws SniffyAssertionError {
        return execute((io.sniffy.Executable) executable);
    }

    /**
     * @return WrongNumberOfQueriesError or null if there are no errors
     * @since 2.1
     */
    @Deprecated
    public WrongNumberOfQueriesError getWrongNumberOfQueriesError() {
        WrongNumberOfQueriesError wrongNumberOfQueriesError = null;
        Throwable throwable = getSniffyAssertionError();
        while (null != throwable) {
            if (throwable instanceof WrongNumberOfQueriesError) {
                if (null != wrongNumberOfQueriesError) {
                    wrongNumberOfQueriesError.initCause(throwable);
                }
                wrongNumberOfQueriesError = (WrongNumberOfQueriesError) throwable;
            }
            throwable = throwable.getCause();
        }
        return wrongNumberOfQueriesError;
    }


    /**
     * @return number of SQL statements executed by current thread since some fixed moment of time
     * @since 2.0
     */
    @Deprecated
    public int executedStatements() {
        return executedStatements(CURRENT);
    }

    /**
     * @param threadMatcher chooses {@link Thread}s for calculating the number of executed queries
     * @return number of SQL statements executed since some fixed moment of time
     * @since 2.0
     */
    @Deprecated
    public int executedStatements(Threads threadMatcher) {
        return executedStatements(threadMatcher, Query.ANY);
    }

    /**
     * @param threadMatcher chooses {@link Thread}s for calculating the number of executed queries
     * @return number of SQL statements executed since some fixed moment of time
     * @since 2.2
     */
    @Deprecated
    public int executedStatements(Threads threadMatcher, Query query) {
        checkOpened();

        int count = 0;

        SqlStatement sqlStatement = adapter(query);

        Map<StatementMetaData, SqlStats> executedStatements = getExecutedStatements(threadMatcher, false);
        if (null != executedStatements) for (Map.Entry<StatementMetaData, SqlStats> entry : executedStatements.entrySet()) {
            StatementMetaData statementMetaData = entry.getKey();
            SqlStats sqlStats = entry.getValue();
            if ((sqlStatement == SqlStatement.ANY && statementMetaData.query != SqlStatement.SYSTEM) || sqlStatement == statementMetaData.query) {
                count += sqlStats.queries.intValue();
            }
        }

        return count;

    }


    // never methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 0, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C expectNever() {
        return expect(SqlQueries.noneQueries());
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 0, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C expectNever(Threads threadMatcher) {
        return expect(SqlQueries.noneQueries().threads(threadMatcher));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 0, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectNever(Query query) {
        return expect(SqlQueries.noneQueries().type(adapter(query)));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 0, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectNever(Threads threadMatcher, Query query) {
        return expect(SqlQueries.noneQueries().threads(threadMatcher).type(adapter(query)));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 0, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectNever(Query query, Threads threadMatcher) {
        return expect(SqlQueries.noneQueries().type(adapter(query)).threads(threadMatcher));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 0, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C verifyNever() throws WrongNumberOfQueriesError {
        return verify(SqlQueries.noneQueries());
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 0, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C verifyNever(Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.noneQueries().threads(threadMatcher));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 0, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyNever(Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.noneQueries().type(adapter(query)));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 0, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyNever(Threads threadMatcher, Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.noneQueries().threads(threadMatcher).type(adapter(query)));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 0, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyNever(Query query, Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.noneQueries().type(adapter(query)).threads(threadMatcher));
    }

    // atMostOnce methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 1, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C expectAtMostOnce() {
        return expect(SqlQueries.atMostOneQuery());
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 1, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C expectAtMostOnce(Threads threadMatcher) {
        return expect(SqlQueries.atMostOneQuery().threads(threadMatcher));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 1, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectAtMostOnce(Query query) {
        return expect(SqlQueries.atMostOneQuery().type(adapter(query)));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 1, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectAtMostOnce(Threads threadMatcher, Query query) {
        return expect(SqlQueries.atMostOneQuery().threads(threadMatcher).type(adapter(query)));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, 1, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectAtMostOnce(Query query, Threads threadMatcher) {
        return expect(SqlQueries.atMostOneQuery().type(adapter(query)).threads(threadMatcher));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 1, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C verifyAtMostOnce() throws WrongNumberOfQueriesError {
        return verify(SqlQueries.atMostOneQuery());
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 1, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C verifyAtMostOnce(Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.atMostOneQuery().threads(threadMatcher));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 1, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyAtMostOnce(Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.atMostOneQuery().type(adapter(query)));

    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 1, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyAtMostOnce(Threads threadMatcher, Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.atMostOneQuery().threads(threadMatcher).type(adapter(query)));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, 1, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyAtMostOnce(Query query, Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.atMostOneQuery().type(adapter(query)).threads(threadMatcher));
    }

    // atMost methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C expectAtMost(int allowedStatements) {
        return expect(SqlQueries.maxQueries(allowedStatements));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C expectAtMost(int allowedStatements, Threads threadMatcher) {
        return expect(SqlQueries.maxQueries(allowedStatements).threads(threadMatcher));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectAtMost(int allowedStatements, Query query) {
        return expect(SqlQueries.maxQueries(allowedStatements).type(adapter(query)));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectAtMost(int allowedStatements, Threads threadMatcher, Query query) {
        return expect(SqlQueries.maxQueries(allowedStatements).threads(threadMatcher).type(adapter(query)));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectAtMost(int allowedStatements, Query query, Threads threadMatcher) {
        return expect(SqlQueries.maxQueries(allowedStatements).type(adapter(query)).threads(threadMatcher));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C verifyAtMost(int allowedStatements) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.maxQueries(allowedStatements));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C verifyAtMost(int allowedStatements, Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.maxQueries(allowedStatements).threads(threadMatcher));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyAtMost(int allowedStatements, Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.maxQueries(allowedStatements).type(adapter(query)));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyAtMost(int allowedStatements, Threads threadMatcher, Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.maxQueries(allowedStatements).threads(threadMatcher).type(adapter(query)));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments 0, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyAtMost(int allowedStatements, Query query, Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.maxQueries(allowedStatements).type(adapter(query)).threads(threadMatcher));
    }

    // exact methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C expect(int allowedStatements) {
        return expect(SqlQueries.exactQueries(allowedStatements));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C expect(int allowedStatements, Threads threadMatcher) {
        return expect(SqlQueries.exactQueries(allowedStatements).threads(threadMatcher));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expect(int allowedStatements, Query query) {
        return expect(SqlQueries.exactQueries(allowedStatements).type(adapter(query)));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expect(int allowedStatements, Threads threadMatcher, Query query) {
        return expect(SqlQueries.exactQueries(allowedStatements).threads(threadMatcher).type(adapter(query)));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expect(int allowedStatements, Query query, Threads threadMatcher) {
        return expect(SqlQueries.exactQueries(allowedStatements).type(adapter(query)).threads(threadMatcher));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C verify(int allowedStatements) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.exactQueries(allowedStatements));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C verify(int allowedStatements, Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.exactQueries(allowedStatements).threads(threadMatcher));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verify(int allowedStatements, Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.exactQueries(allowedStatements).type(adapter(query)));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verify(int allowedStatements, Threads threadMatcher, Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.exactQueries(allowedStatements).threads(threadMatcher).type(adapter(query)));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@code allowedStatements}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verify(int allowedStatements, Query query, Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.exactQueries(allowedStatements).type(adapter(query)).threads(threadMatcher));
    }

    // atLeast methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C expectAtLeast(int allowedStatements) {
        return expect(SqlQueries.minQueries(allowedStatements));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C expectAtLeast(int allowedStatements, Threads threadMatcher) {
        return expect(SqlQueries.minQueries(allowedStatements).threads(threadMatcher));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectAtLeast(int allowedStatements, Query query) {
        return expect(SqlQueries.minQueries(allowedStatements).type(adapter(query)));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectAtLeast(int allowedStatements, Threads threadMatcher, Query query) {
        return expect(SqlQueries.minQueries(allowedStatements).threads(threadMatcher).type(adapter(query)));
    }

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C expectAtLeast(int allowedStatements, Query query, Threads threadMatcher) {
        return expect(SqlQueries.minQueries(allowedStatements).type(adapter(query)).threads(threadMatcher));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C verifyAtLeast(int allowedStatements) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.minQueries(allowedStatements));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C verifyAtLeast(int allowedStatements, Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.minQueries(allowedStatements).threads(threadMatcher));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@link Threads#CURRENT}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyAtLeast(int allowedStatements, Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.minQueries(allowedStatements).type(adapter(query)));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyAtLeast(int allowedStatements, Threads threadMatcher, Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.minQueries(allowedStatements).threads(threadMatcher).type(adapter(query)));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads, Query)} with arguments {@code allowedStatements}, {@link Integer#MAX_VALUE}, {@code threads}, {@code queryType}
     * @since 2.2
     */
    @Deprecated
    public C verifyAtLeast(int allowedStatements, Query query, Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.minQueries(allowedStatements).type(adapter(query)).threads(threadMatcher));
    }

    // between methods

    /**
     * Alias for {@link #expectBetween(int, int, Threads, Query)} with arguments {@code minAllowedStatements}, {@code maxAllowedStatements}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements) {
        return expect(SqlQueries.queriesBetween(minAllowedStatements, maxAllowedStatements));
    }

    /**
     * Adds an expectation to the current instance that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @since 2.0
     */
    @Deprecated
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher) {
        return expect(SqlQueries.queriesBetween(minAllowedStatements, maxAllowedStatements).threads(threadMatcher));
    }

    /**
     * Adds an expectation to the current instance that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @since 2.2
     */
    @Deprecated
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements, Query query) {
        return expect(SqlQueries.queriesBetween(minAllowedStatements, maxAllowedStatements).type(adapter(query)));
    }

    /**
     * Adds an expectation to the current instance that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @since 2.2
     */
    @Deprecated
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher, Query query) {
        return expect(SqlQueries.queriesBetween(minAllowedStatements, maxAllowedStatements).threads(threadMatcher).type(adapter(query)));
    }

    /**
     * Adds an expectation to the current instance that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @since 2.2
     */
    @Deprecated
    public C expectBetween(int minAllowedStatements, int maxAllowedStatements, Query query, Threads threadMatcher) {
        return expect(SqlQueries.queriesBetween(minAllowedStatements, maxAllowedStatements).type(adapter(query)).threads(threadMatcher));
    }

    /**
     * Alias for {@link #verifyBetween(int, int, Threads)} with arguments {@code minAllowedStatements}, {@link Threads#CURRENT}, {@link Query#ANY}
     * @since 2.0
     */
    @Deprecated
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.queriesBetween(minAllowedStatements, maxAllowedStatements));
    }

    /**
     * Verifies that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.0
     */
    @Deprecated
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.queriesBetween(minAllowedStatements, maxAllowedStatements).threads(threadMatcher));
    }

    /**
     * Verifies that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.2
     */
    @Deprecated
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements, Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.queriesBetween(minAllowedStatements, maxAllowedStatements).type(adapter(query)));
    }

    /**
     * Verifies that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.2
     */
    @Deprecated
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements, Threads threadMatcher, Query query) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.queriesBetween(minAllowedStatements, maxAllowedStatements).threads(threadMatcher).type(adapter(query)));
    }

    /**
     * Verifies that at least {@code minAllowedStatements} and at most
     * {@code maxAllowedStatements} were called between the creation of the current instance
     * and a call to {@link #verify()} method
     * @throws WrongNumberOfQueriesError if wrong number of queries was executed
     * @since 2.2
     */
    @Deprecated
    public C verifyBetween(int minAllowedStatements, int maxAllowedStatements, Query query, Threads threadMatcher) throws WrongNumberOfQueriesError {
        return verify(SqlQueries.queriesBetween(minAllowedStatements, maxAllowedStatements).type(adapter(query)).threads(threadMatcher));
    }

    public abstract Map<StatementMetaData, SqlStats> getExecutedStatements(Threads threadMatcher, boolean removeStackTraces);

    public abstract C expect(Spy.Expectation expectation);

    public abstract C verify(Spy.Expectation expectation);

    public abstract void verify() throws SniffyAssertionError;

    public abstract SniffyAssertionError getSniffyAssertionError();

    protected abstract void checkOpened();

    public abstract C execute(Executable executable) throws SniffyAssertionError;
}
