package com.github.bedrin.jdbc.sniffer.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by bedrin on 22.03.2015.
 */
public class ExceptionUtil {

    protected ExceptionUtil() {

    }

    private final static Method addSuppressedMethod = getAddSuppressedMethod();

    private static Method getAddSuppressedMethod() {
        try {
            Class<Throwable> throwableClass = Throwable.class;
            return throwableClass.getMethod("addSuppressed", Throwable.class);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static void throwException(Throwable e) {
        ExceptionUtil.<RuntimeException>throwAny(e);
    }

    @SuppressWarnings("unchecked")
    public static <E extends Throwable> void throwAny(Throwable e) throws E {
        throw (E)e;
    }

    public static boolean addSuppressed(Throwable e, Throwable suppressed) {
        if (null == addSuppressedMethod) {
            return false;
        } else {
            try {
                addSuppressedMethod.invoke(e, suppressed);
            } catch (IllegalAccessException iae) {
                iae.printStackTrace();
            } catch (InvocationTargetException ite) {
                ite.printStackTrace();
            }
            return true;
        }
    }

}
