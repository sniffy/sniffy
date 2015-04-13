package com.github.bedrin.jdbc.sniffer.junit;

import com.github.bedrin.jdbc.sniffer.Spy;
import com.github.bedrin.jdbc.sniffer.Sniffer;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class QueryCounter implements TestRule {

    @Override
    public Statement apply(Statement statement, Description description) {

        Expectations expectations = description.getAnnotation(Expectations.class);
        Expectation expectation = description.getAnnotation(Expectation.class);
        NoQueriesAllowed notAllowedQueries = description.getAnnotation(NoQueriesAllowed.class);

        // If no annotations present, check the test class and its superclasses
        for (Class<?> testClass = description.getTestClass();
             null == expectations && null == expectation && null == notAllowedQueries && !Object.class.equals(testClass);
                testClass = testClass.getSuperclass()) {
            expectations = testClass.getDeclaredAnnotation(Expectations.class);
            expectation = testClass.getDeclaredAnnotation(Expectation.class);
            notAllowedQueries = testClass.getDeclaredAnnotation(NoQueriesAllowed.class);
        }

        if (null != expectation && null != notAllowedQueries) {
            throw new IllegalArgumentException("Cannot specify @Expectation and @NotAllowedQueries on one test method");
        } else if (null != expectations && null != notAllowedQueries) {
            throw new IllegalArgumentException("Cannot specify @Expectations and @NotAllowedQueries on one test method");
        } else if (null != expectations || null != expectation) {

            List<Expectation> expectationList = new ArrayList<Expectation>();

            if (null != expectation) {
                expectationList.add(expectation);
            }

            if (null != expectations) {
                expectationList.addAll(Arrays.asList(expectations.value()));
            }

            validateExpectation(expectationList);

            return new SnifferStatement(statement, expectationList);

        } else if (null != notAllowedQueries) {
            Expectation annotation = NoQueriesAllowed.class.getAnnotation(Expectation.class);
            return new SnifferStatement(statement, Collections.singletonList(annotation));
        } else {
            return statement;
        }

    }

    private void validateExpectation(List<Expectation> expectationList) {
        for (Expectation expectation : expectationList) {
            if (expectation.value() != -1) {
                if (expectation.atMost() != -1 || expectation.atLeast() != -1) {
                    throw new IllegalArgumentException("Cannot specify value parameter together with atLeast or atMost parameters");
                }
            }
        }
    }

    private static class SnifferStatement extends Statement {

        private final Statement delegate;
        private final List<Expectation> expectationList;

        public SnifferStatement(Statement delegate, List<Expectation> expectationList) {
            this.delegate = delegate;
            this.expectationList = expectationList;
        }

        @Override
        public void evaluate() throws Throwable {

            Spy spy = Sniffer.spy();

            for (Expectation expectation : expectationList) {
                if (-1 != expectation.value()) {
                    spy.expect(expectation.value(), expectation.threads());
                }
                if (-1 != expectation.atLeast() && -1 != expectation.atMost()) {
                    spy.expectBetween(expectation.atLeast(), expectation.atMost(), expectation.threads());
                } else if (-1 != expectation.atLeast()) {
                    spy.expectAtLeast(expectation.atLeast(), expectation.threads());
                } else if (-1 != expectation.atMost()) {
                    spy.expectAtMost(expectation.atMost(), expectation.threads());
                }
            }

            try {
                delegate.evaluate();
            } finally {
                spy.verify();
            }

        }

    }

}
