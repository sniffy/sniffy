package com.github.bedrin.jdbc.sniffer.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container for multiple {@link Expectation} annotations
 * todo consider making Inherited
 * @see QueryCounter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Expectations {
    Expectation[] value() default {};
}
