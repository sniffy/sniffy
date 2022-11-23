package io.sniffy.util;

public interface Matcher<T> {

    boolean matches(T t);

    void describe(StringBuilder appendable);

}
