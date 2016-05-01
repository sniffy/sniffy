package io.sniffy.test;

public @interface Count {

    int value() default -1;

    int min() default -1;

    int max() default -1;

}
