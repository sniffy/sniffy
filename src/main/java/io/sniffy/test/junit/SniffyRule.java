package io.sniffy.test.junit;

import io.sniffy.*;
import io.sniffy.socket.NoSocketsAllowed;
import io.sniffy.socket.SocketExpectation;
import io.sniffy.socket.SocketExpectations;
import io.sniffy.socket.TcpConnections;
import io.sniffy.sql.NoSql;
import io.sniffy.sql.SqlExpectation;
import io.sniffy.sql.SqlExpectations;
import io.sniffy.sql.SqlQueries;
import io.sniffy.test.AnnotationProcessor;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.List;

/**
 * Provides integration with JUnit. Add following field to your test class:
 * <pre>
 * <code>
 * {@literal @}Rule
 * public final SniffyRule sniffy = new SniffyRule();
 * }
 * </code>
 * </pre>
 * @see SocketExpectations
 * @see SocketExpectation
 * @see NoSocketsAllowed
 * @since 3.1
 */
public class SniffyRule implements TestRule {

    public Statement apply(Statement statement, Description description) {

        List<SqlExpectation> expectationList;
        try {
            expectationList = buildSqlExpectationList(description);
        } catch (IllegalArgumentException e) {
            return new InvalidAnnotationsStatement(statement, e);
        }

        List<SocketExpectation> socketExpectationList;
        try {
            socketExpectationList = buildSocketExpectationList(description);
        } catch (IllegalArgumentException e) {
            return new InvalidAnnotationsStatement(statement, e);
        }

        if (!expectationList.isEmpty() || !socketExpectationList.isEmpty()) {
            return new SnifferStatement(statement, expectationList, socketExpectationList);
        } else {
            return statement;
        }

    }

    private static List<SocketExpectation> buildSocketExpectationList(Description description) {

        SocketExpectation socketExpectation = description.getAnnotation(SocketExpectation.class);
        SocketExpectations socketExpectations = description.getAnnotation(SocketExpectations.class);
        NoSocketsAllowed noSocketsAllowed = description.getAnnotation(NoSocketsAllowed.class);

        for (Class<?> testClass = description.getTestClass();
             null == socketExpectations && null == socketExpectation && null == noSocketsAllowed && !Object.class.equals(testClass);
             testClass = testClass.getSuperclass()) {
            socketExpectations = testClass.getAnnotation(SocketExpectations.class);
            socketExpectation = testClass.getAnnotation(SocketExpectation.class);
            noSocketsAllowed = testClass.getAnnotation(NoSocketsAllowed.class);
        }

        return AnnotationProcessor.buildSocketExpectationList(socketExpectation, socketExpectations, noSocketsAllowed);

    }

    private static List<SqlExpectation> buildSqlExpectationList(Description description) {

        SqlExpectations sqlExpectations = description.getAnnotation(SqlExpectations.class);
        SqlExpectation sqlExpectation = description.getAnnotation(SqlExpectation.class);
        NoSql noSql = description.getAnnotation(NoSql.class);

        if (null == sqlExpectations) sqlExpectations = Expectations.SqlExpectationsAdapter.adapter(
                description.getAnnotation(Expectations.class)
        );
        if (null == sqlExpectation) sqlExpectation = Expectation.SqlExpectationAdapter.adapter(
                description.getAnnotation(Expectation.class)
        );
        if (null == noSql && null != description.getAnnotation(NoQueriesAllowed.class)) noSql =
                description.getAnnotation(NoQueriesAllowed.class).annotationType().getAnnotation(NoSql.class);

        return AnnotationProcessor.buildSqlExpectationList(description.getTestClass(), sqlExpectations, sqlExpectation, noSql);

    }

    private static class InvalidAnnotationsStatement extends Statement {

        private final Statement delegate;
        private final Throwable exception;

        public InvalidAnnotationsStatement(Statement delegate, Throwable exception) {
            this.delegate = delegate;
            this.exception = exception;
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                delegate.evaluate();
            } finally {
                throw exception;
            }
        }

    }

    private static class SnifferStatement extends Statement {

        private final Statement delegate;
        private final List<SqlExpectation> sqlExpectationList;
        private final List<SocketExpectation> socketExpectationList;

        public SnifferStatement(Statement delegate, List<SqlExpectation> sqlExpectationList, List<SocketExpectation> socketExpectationList) {
            this.delegate = delegate;
            this.sqlExpectationList = sqlExpectationList;
            this.socketExpectationList = socketExpectationList;
        }

        @Override
        public void evaluate() throws Throwable {

            Spy spy = Sniffy.spy();

            if (null != sqlExpectationList) {
                for (SqlExpectation sqlExpectation : sqlExpectationList) {
                    spy = spy.expect(new SqlQueries.SqlExpectation(sqlExpectation));
                }
            }
            if (null != socketExpectationList) {
                for (SocketExpectation socketExpectation : socketExpectationList) {
                    spy = spy.expect(new TcpConnections.TcpExpectation(socketExpectation));
                }
            }

            spy.execute(new Executable() {
                public void execute() throws Throwable{
                    delegate.evaluate();
                }
            }).close();

        }

    }

}
