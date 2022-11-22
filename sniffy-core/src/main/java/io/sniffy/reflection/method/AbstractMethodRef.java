package io.sniffy.reflection.method;

import io.sniffy.reflection.ResolvableRef;
import io.sniffy.reflection.UnsafeException;
import io.sniffy.util.ExceptionUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AbstractMethodRef<C> implements ResolvableRef {

    protected final Method method;
    protected final Throwable throwable;

    public AbstractMethodRef(Method method, Throwable throwable) {
        this.method = method;
        this.throwable = throwable;
    }

    public boolean isResolved() {
        return null != method;
    }

    public Method getMethod() {
        return method;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    // TODO: keep current class abstract and move this method (along with void version) somewhere
    public <T> T invoke(C instance, Object... parameters) throws UnsafeException {
        try {
            if (null != throwable) {
                throw ExceptionUtil.throwException(throwable);
            } else {
                Object result = method.invoke(instance, parameters);
                //noinspection unchecked
                return (T) result;
            }
        } catch (IllegalAccessException e) {
            throw new UnsafeException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.throwException(e.getTargetException());
        }
    }

}
