package io.sniffy.junit;

import io.sniffy.*;
import io.sniffy.socket.TcpConnections;
import io.sniffy.util.Range;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Provides integration with JUnit. Add following field to your test class:
 * <pre>
 * <code>
 * {@literal @}Rule
 * public final QueryCounter queryCounter = new QueryCounter();
 * }
 * </code>
 * </pre>
 * @see Expectations
 * @see Expectation
 * @see NoQueriesAllowed
 * @since 1.3
 */
public class QueryCounter implements TestRule {

    public Statement apply(Statement statement, Description description) {

        Expectations expectations = description.getAnnotation(Expectations.class);
        Expectation expectation = description.getAnnotation(Expectation.class);
        NoQueriesAllowed notAllowedQueries = description.getAnnotation(NoQueriesAllowed.class);

        SocketExpectation socketExpectation = description.getAnnotation(SocketExpectation.class);
        SocketExpectations socketExpectations = description.getAnnotation(SocketExpectations.class);
        // TODO create NoSocketsAllowed annotation

        // If no annotations present, check the test class and its superclasses
        for (Class<?> testClass = description.getTestClass();
             null == expectations && null == expectation && null == notAllowedQueries && !Object.class.equals(testClass);
                testClass = testClass.getSuperclass()) {
            expectations = testClass.getAnnotation(Expectations.class);
            expectation = testClass.getAnnotation(Expectation.class);
            notAllowedQueries = testClass.getAnnotation(NoQueriesAllowed.class);
        }

        if (null != expectation && null != notAllowedQueries) {
            return new InvalidAnnotationsStatement(statement,
                    new IllegalArgumentException("Cannot specify @Expectation and @NotAllowedQueries on one test method")
            );
        } else if (null != expectations && null != notAllowedQueries) {
            return new InvalidAnnotationsStatement(statement,
                    new IllegalArgumentException("Cannot specify @Expectations and @NotAllowedQueries on one test method")
            );
        } else if (null != expectations || null != expectation) {

            List<Expectation> expectationList = new ArrayList<Expectation>();

            if (null != expectation) {
                expectationList.add(expectation);
            }

            if (null != expectations) {
                expectationList.addAll(Arrays.asList(expectations.value()));
            }

            for (Expectation expectation1 : expectationList) {
                try {
                    Range.parse(expectation1);
                } catch (IllegalArgumentException e) {
                    return new InvalidAnnotationsStatement(statement, e);
                }
            }


            List<SocketExpectation> socketExpectationList = new ArrayList<SocketExpectation>();

            if (null != socketExpectation) {
                socketExpectationList.add(socketExpectation);
            }

            if (null != socketExpectations) {
                socketExpectationList.addAll(Arrays.asList(socketExpectations.value()));
            }


            return new SnifferStatement(statement, expectationList, socketExpectationList);

        } else if (null != notAllowedQueries) {
            Expectation annotation = NoQueriesAllowed.class.getAnnotation(Expectation.class);

            List<SocketExpectation> socketExpectationList = new ArrayList<SocketExpectation>();

            if (null != socketExpectation) {
                socketExpectationList.add(socketExpectation);
            }

            if (null != socketExpectations) {
                socketExpectationList.addAll(Arrays.asList(socketExpectations.value()));
            }

            return new SnifferStatement(statement, Collections.singletonList(annotation), socketExpectationList);
        } else if (null != socketExpectations || null != socketExpectation) {
            List<SocketExpectation> socketExpectationList = new ArrayList<SocketExpectation>();

            if (null != socketExpectation) {
                socketExpectationList.add(socketExpectation);
            }

            if (null != socketExpectations) {
                socketExpectationList.addAll(Arrays.asList(socketExpectations.value()));
            }

            return new SnifferStatement(statement, Collections.emptyList(), socketExpectationList);
        } else {
            return statement;
        }

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
        private final List<Expectation> expectationList;
        private final List<SocketExpectation> socketExpectationList;

        public SnifferStatement(Statement delegate, List<Expectation> expectationList, List<SocketExpectation> socketExpectationList) {
            this.delegate = delegate;
            this.expectationList = expectationList;
            this.socketExpectationList = socketExpectationList;
        }

        @Override
        public void evaluate() throws Throwable {

            Spy spy = Sniffer.expect(expectationList);

            if (null != socketExpectationList) {
                for (SocketExpectation socketExpectation : socketExpectationList) {
                    spy = spy.expect(new TcpConnections.TcpExpectation(socketExpectation));
                }
            }

            spy.execute(new Sniffer.Executable() {
                public void execute() throws Throwable{
                    delegate.evaluate();
                }
            }).close();

        }

    }

}
