package com.github.bedrin.jdbc.sniffer.junit;

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
            return new SnifferStatement(statement, allowedQueries.value());
        } else if (null != notAllowedQueries) {
            return new SnifferStatement(statement, 0);
        } else {
            return statement;
        }

    }

    private static class SnifferStatement extends Statement {

        private final Statement delegate;
        private final int maximumQueries;

        public SnifferStatement(Statement delegate, int maximumQueries) {
            this.delegate = delegate;
            this.maximumQueries = maximumQueries;
        }

        @Override
        public void evaluate() throws Throwable {
            int count = Sniffer.executedStatements();
            delegate.evaluate();
            Sniffer.verifyNotMoreThan(count + maximumQueries);
        }

    }

}
