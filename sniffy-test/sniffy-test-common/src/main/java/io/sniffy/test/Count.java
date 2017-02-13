package io.sniffy.test;

/**
 * @since 3.1
 */
public @interface Count {

    int value() default -1;

    int min() default -1;

    int max() default -1;

}
