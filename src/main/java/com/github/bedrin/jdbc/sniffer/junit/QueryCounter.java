package com.github.bedrin.jdbc.sniffer.junit;

import com.github.bedrin.jdbc.sniffer.Spy;
import com.github.bedrin.jdbc.sniffer.Sniffer;
import com.github.bedrin.jdbc.sniffer.Threads;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class QueryCounter implements TestRule {

    private final boolean disallowByDefault;

    public QueryCounter() {
        this(false);
    }

    public QueryCounter(boolean disallowByDefault) {
        this.disallowByDefault = disallowByDefault;
    }

    @Override
    public Statement apply(Statement statement, Description description) {

        ExpectedQueries allowedQueries = description.getAnnotation(ExpectedQueries.class);
        NoQueriesAllowed notAllowedQueries = description.getAnnotation(NoQueriesAllowed.class);

        if (null != allowedQueries && null != notAllowedQueries) {
            throw new IllegalArgumentException("Cannot specify @AllowedQueries and @NotAllowedQueries on one test method");
        } else if (null != allowedQueries) {

            Integer min = null, max = null;

            if (allowedQueries.value() != -1) {
                if (allowedQueries.atMost() != -1 || allowedQueries.atLeast() != -1) {
                    throw new IllegalArgumentException("Cannot specify value parameter together with atLeast or atMost parameters");
                }
                max = allowedQueries.value();
            }

            if (allowedQueries.atLeast() != -1) {
                min = allowedQueries.atLeast();
            }

            return new SnifferStatement(statement, min, max, allowedQueries.threads());

        } else if (null != notAllowedQueries) {
            return new SnifferStatement(statement, 0, 0, Threads.ANY);
        } else {
            return statement;
        }

    }

    private static class SnifferStatement extends Statement {

        private final Statement delegate;
        private final Integer minimumQueries;
        private final Integer maximumQueries;
        private final Threads threadMatcher;

        public SnifferStatement(Statement delegate, Integer minimumQueries, Integer maximumQueries, Threads threadMatcher) {
            this.delegate = delegate;
            this.minimumQueries = minimumQueries;
            this.maximumQueries = maximumQueries;
            this.threadMatcher = threadMatcher;
        }

        @Override
        public void evaluate() throws Throwable {

            Spy spy = Sniffer.spy();
            delegate.evaluate();

            if (null != minimumQueries && null != maximumQueries) {
                spy.verifyBetween(minimumQueries, maximumQueries, threadMatcher);
            } else if (null != minimumQueries) {
                spy.verifyAtLeast(minimumQueries, threadMatcher);
            } else if (null != maximumQueries) {
                spy.verifyAtMost(maximumQueries, threadMatcher);
            }
        }

    }

}
