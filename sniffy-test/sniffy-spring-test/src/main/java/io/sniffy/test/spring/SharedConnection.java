package io.sniffy.test.spring;

import java.lang.annotation.*;

/**
 * @since 3.1.6
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Inherited
public @interface SharedConnection {
}
