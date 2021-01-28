package io.sniffy.util;

import java.io.IOException;

public interface Matcher<T> {

    boolean matches(T t);

    void describe(StringBuilder appendable);

}
