package io.sniffy;

import io.sniffy.junit.QueryCounter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container for multiple {@link Expectation} annotations
 * todo consider making Inherited
 * @see QueryCounter
 * @since 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Expectations {
    Expectation[] value() default {};
}
