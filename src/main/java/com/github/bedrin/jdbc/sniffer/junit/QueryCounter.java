package com.github.bedrin.jdbc.sniffer.junit;

import com.github.bedrin.jdbc.sniffer.ExpectedQueries;
import com.github.bedrin.jdbc.sniffer.Sniffer;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class QueryCounter implements TestRule {

    @Override
    public Statement apply(Statement statement, Description description) {

        AllowedQueries allowedQueries = description.getAnnotation(AllowedQueries.class);
        NotAllowedQueries notAllowedQueries = description.getAnnotation(NotAllowedQueries.class);

        if (null != allowedQueries && null != notAllowedQueries) {
            throw new IllegalArgumentException("Cannot specify @AllowedQueries and @NotAllowedQueries on one test method");
        } else if (null != allowedQueries) {

            Integer min = null, max = null;

            if (allowedQueries.value() != -1) {
                if (allowedQueries.max() != -1 || allowedQueries.min() != -1 ||
                        allowedQueries.exact() != -1) {
                    throw new IllegalArgumentException("Cannot specify value parameter together with other parameters");
                }
                max = allowedQueries.value();
            }

            if (allowedQueries.min() != -1) {
                min = allowedQueries.min();
            }

            if (allowedQueries.exact() != -1) {
                if (null != min || null != max) {
                    throw new IllegalArgumentException("Cannot specify exact parameter together with min or max parameters");
                }
                min = max = allowedQueries.exact();
            }

            return new SnifferStatement(statement, min, max, allowedQueries.threadLocal());

        } else if (null != notAllowedQueries) {
            return new SnifferStatement(statement, 0, 0, notAllowedQueries.threadLocal());
        } else {
            return statement;
        }

    }

    private static class SnifferStatement extends Statement {

        private final Statement delegate;
        private final Integer minimumQueries;
        private final Integer maximumQueries;
        private final boolean threadLocal;

        public SnifferStatement(Statement delegate, Integer minimumQueries, Integer maximumQueries, boolean threadLocal) {
            this.delegate = delegate;
            this.minimumQueries = minimumQueries;
            this.maximumQueries = maximumQueries;
            this.threadLocal = threadLocal;
        }

        @Override
        public void evaluate() throws Throwable {

            ExpectedQueries expectedQueries = Sniffer.expectedQueries();
            delegate.evaluate();

            if (threadLocal) {
                if (null != minimumQueries && null != maximumQueries) {
                    expectedQueries.verifyRange(minimumQueries, maximumQueries, Sniffer.CURRENT_THREAD);
                } else if (null != minimumQueries) {
                    expectedQueries.verifyNotLessThan(minimumQueries, Sniffer.CURRENT_THREAD);
                } else if (null != maximumQueries) {
                    expectedQueries.verifyNotMoreThan(maximumQueries, Sniffer.CURRENT_THREAD);
                }
            } else {
                if (null != minimumQueries && null != maximumQueries) {
                    expectedQueries.verifyRange(minimumQueries, maximumQueries, Sniffer.ANY_THREAD);
                } else if (null != minimumQueries) {
                    expectedQueries.verifyNotLessThan(minimumQueries, Sniffer.ANY_THREAD);
                } else if (null != maximumQueries) {
                    expectedQueries.verifyNotMoreThan(maximumQueries, Sniffer.ANY_THREAD);
                }
            }
        }

    }

}
