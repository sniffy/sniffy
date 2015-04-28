package com.github.bedrin.jdbc.sniffer.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.github.bedrin.jdbc.sniffer.util.StringUtil.LINE_SEPARATOR;

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

    public static boolean throwException(String className, String message) {
        try {
            Class<Throwable> throwableClass = (Class<Throwable>)Class.forName(className);
            Constructor<Throwable> constructor = throwableClass.getConstructor(String.class);
            Throwable throwable = constructor.newInstance(message);
            throwException(throwable);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (NoSuchMethodException e) {
            return false;
        } catch (InvocationTargetException e) {
            return false;
        } catch (InstantiationException e) {
            return false;
        } catch (IllegalAccessException e) {
            return false;
        }
    }


    public static void throwException(Throwable e) {
        ExceptionUtil.<RuntimeException>throwAny(e);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwAny(Throwable e) throws E {
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

    public static String generateMessage(String s, StackTraceElement[] closeStackTrace) {
        StringBuilder sb = new StringBuilder(s);
        for (StackTraceElement traceElement : closeStackTrace) {
            sb.append(LINE_SEPARATOR).append("\tat ").append(traceElement);
        }
        return sb.toString();
    }

}
