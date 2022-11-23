package io.sniffy.reflection.field;

import java.lang.reflect.Field;

public interface FieldFilter {

    boolean include(String name, Field field);

}
