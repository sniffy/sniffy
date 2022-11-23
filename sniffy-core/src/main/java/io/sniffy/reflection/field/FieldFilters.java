package io.sniffy.reflection.field;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class FieldFilters {

    public static FieldFilter ofType(final Class<?> type) {
        return new FieldFilter() {
            @Override
            public boolean include(String name, Field field) {
                return field.getType().equals(type); // TODO: isAssignableFrom
            }
        };
    }

    public static FieldFilter staticField() {
        return new FieldFilter() {
            @Override
            public boolean include(String name, Field field) {
                return Modifier.isStatic(field.getModifiers());
            }
        };
    }

    public static FieldFilter and(final FieldFilter filter1, final FieldFilter filter2) {
        return new FieldFilter() {
            @Override
            public boolean include(String name, Field field) {
                return filter1.include(name, field) && filter2.include(name, field);
            }
        };
    }

}
