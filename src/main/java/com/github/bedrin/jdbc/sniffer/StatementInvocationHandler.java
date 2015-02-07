package com.github.bedrin.jdbc.sniffer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class StatementInvocationHandler implements InvocationHandler {

    private final Object delegate;

    public StatementInvocationHandler(Object delegate) {
        this.delegate = delegate;
    }

    private final static Set<String> executeMethodNames = new HashSet<String>(Arrays.asList(
            "executeQuery", "executeUpdate", "execute", "executeBatch", "executeLargeUpdate"
    ));

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (executeMethodNames.contains(method.getName())) {
            Sniffer.executeStatement();
        }
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

}
