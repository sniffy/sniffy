package io.sniffy;

import io.sniffy.junit.QueryCounter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.Socket;

/**
 * Container for multiple {@link SocketExpectation} annotations
 * todo consider making Inherited
 * @see QueryCounter
 * @since 3.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface SocketExpectations {
    SocketExpectation[] value() default {};
}
