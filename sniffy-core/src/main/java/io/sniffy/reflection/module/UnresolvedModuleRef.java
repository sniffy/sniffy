package io.sniffy.reflection.module;

import io.sniffy.reflection.UnresolvedRef;
import io.sniffy.reflection.UnresolvedRefException;
import io.sniffy.reflection.UnsafeInvocationException;

import java.lang.reflect.InvocationTargetException;

public class UnresolvedModuleRef extends UnresolvedRef<ModuleRef> {

    public UnresolvedModuleRef(ModuleRef ref, Throwable throwable) {
        super(ref, throwable);
    }

    public void addOpens(String packageName) throws UnresolvedRefException, UnsafeInvocationException, InvocationTargetException {
        resolve().addOpens(packageName);
    }

    public boolean tryAddOpens(String packageName) {
        try {
            return resolve().tryAddOpens(packageName);
        } catch (UnresolvedRefException e) {
            return false;
        }
    }

}
