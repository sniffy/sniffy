package io.sniffy;

import io.sniffy.sql.SqlExpectation;
import io.sniffy.sql.SqlStatement;
import io.sniffy.test.Count;

import java.lang.annotation.*;

/**
 * @see SqlExpectation
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Inherited
@Deprecated
public @interface Expectation {

    /**
     * @return exact number of expected queries
     * @since 2.0
     */
    @Deprecated
    int value() default -1;

    /**
     * @return maximum number of expected queries
     * @since 2.0
     */
    @Deprecated
    int atMost() default -1;

    /**
     * @return minimum number of expected queries
     * @since 2.0
     */
    @Deprecated
    int atLeast() default -1;

    /**
     * @return descriptor of which threads contribute to given expectation
     * @since 2.0
     */
    Threads threads() default Threads.CURRENT;

    /**
     * @return type of executed query
     * @since 2.2
     */
    Query query() default Query.ANY;

    final class CountAdapter implements Count {

        private final int value;
        private final int min;
        private final int max;

        private CountAdapter(int value, int min, int max) {
            this.value = value;
            this.min = min;
            this.max = max;
        }

        @Override
        public int value() {
            return value;
        }

        @Override
        public int min() {
            return min;
        }

        @Override
        public int max() {
            return max;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Count.class;
        }

    }

    final class SqlExpectationAdapter implements SqlExpectation {

        private final Expectation expectation;

        public static SqlExpectation adapter(Expectation expectation) {
            return null == expectation ? null : new Expectation.SqlExpectationAdapter(expectation);
        }

        public SqlExpectationAdapter(Expectation expectation) {
            this.expectation = expectation;
        }

        @Override
        public Count count() {
            return new CountAdapter(expectation.value(), expectation.atLeast(), expectation.atMost());
        }

        @Override
        public Count rows() {
            return new CountAdapter(-1, -1, -1);
        }

        @Override
        public Threads threads() {
            return expectation.threads();
        }

        @Override
        public SqlStatement query() {
            return LegacySpy.adapter(expectation.query());
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return SqlExpectation.class;
        }

    }

}