package io.sniffy.socket;

import io.sniffy.test.junit.SniffyRule;

import java.lang.annotation.*;

/**
 * Container for multiple {@link SocketExpectation} annotations
 * @see SniffyRule
 * @since 3.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
public @interface SocketExpectations {
    SocketExpectation[] value() default {};
}
