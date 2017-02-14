package io.sniffy;

import io.sniffy.sql.SqlExpectation;
import io.sniffy.sql.SqlExpectations;

import java.lang.annotation.*;

/**
 * @see SqlExpectations
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
@Deprecated
public @interface Expectations {

    Expectation[] value() default {};

    final class SqlExpectationsAdapter implements SqlExpectations {

        private final Expectation[] expectations;

        public static SqlExpectations adapter(Expectations expectations) {
            return null == expectations ? null : new SqlExpectationsAdapter(expectations);
        }

        public SqlExpectationsAdapter(Expectations expectations) {
            this.expectations = expectations.value();
        }

        @Override
        public SqlExpectation[] value() {
            if (null == expectations) return null;
            SqlExpectation[] sqlExpectations = new SqlExpectation[expectations.length];
            for (int i = 0; i < expectations.length; i++) {
                sqlExpectations[i] = new Expectation.SqlExpectationAdapter(expectations[i]);
            }
            return sqlExpectations;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return SqlExpectations.class;
        }

    }

}
