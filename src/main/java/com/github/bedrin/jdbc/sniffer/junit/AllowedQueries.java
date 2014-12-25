package com.github.bedrin.jdbc.sniffer.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface AllowedQueries {
    int value() default -1;
    int max() default -1;
    int min() default -1;
    int exact() default -1;
    boolean threadLocal() default false;
}
