package io.sniffy.sql;

import java.lang.annotation.*;

/**
 * Container for multiple {@link SqlExpectation} annotations
 * @since 3.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
public @interface SqlExpectations {
    SqlExpectation[] value() default {};
}
