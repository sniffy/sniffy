package io.sniffy.reflection.constructor;

import io.sniffy.reflection.UnsafeException;
import io.sniffy.util.ExceptionUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.WrongMethodTypeException;

public class ZeroArgsConstructorRef<C> extends AbstractConstructorRef<C> {

    public ZeroArgsConstructorRef(MethodHandle method, Throwable throwable) {
        super(method, throwable);
    }

    @SuppressWarnings("TryWithIdenticalCatches")
    public void invoke(C instance) throws UnsafeException {
        try {
            if (null != throwable) {
                throw ExceptionUtil.throwException(throwable);
            } else {
                methodHandle.invoke(instance);
            }
        } catch (WrongMethodTypeException e) {
            throw new UnsafeException(e);
        } catch (ClassCastException e) {
            throw new UnsafeException(e);
        } catch (Throwable e) {
            throw ExceptionUtil.throwException(e);
        }
    }

}
