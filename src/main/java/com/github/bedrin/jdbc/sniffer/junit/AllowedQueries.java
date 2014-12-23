package com.github.bedrin.jdbc.sniffer.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface AllowedQueries {
    int value();
    int maximum() default 0;
    int minimum() default 0;
    int exact() default 0;
    boolean threadLocal() default false;
}
