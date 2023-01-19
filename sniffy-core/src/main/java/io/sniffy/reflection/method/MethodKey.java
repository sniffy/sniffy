package io.sniffy.reflection.method;

import java.lang.reflect.Method;
import java.util.Arrays;

public class MethodKey {

    private final String methodName;
    private final Object[] parameterTypes;

    public MethodKey(Method method) {
        this(method.getName(), method.getParameterTypes());
    }

    public MethodKey(String methodName, Object[] parameterTypes) {
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MethodKey that = (MethodKey) o;

        if (!methodName.equals(that.methodName)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(parameterTypes, that.parameterTypes);
    }

    @Override
    public int hashCode() {
        int result = methodName.hashCode();
        result = 31 * result + Arrays.hashCode(parameterTypes);
        return result;
    }

    @Override
    public String toString() {
        return "MethodKey{" +
                "methodName='" + methodName + '\'' +
                ", parameterTypes=" + Arrays.toString(parameterTypes) +
                '}';
    }

}
