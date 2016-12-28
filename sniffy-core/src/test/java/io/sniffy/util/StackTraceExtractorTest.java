package io.sniffy.util;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

public class StackTraceExtractorTest {

    interface TestBase {
        void testBaseMethod();
    }

    static class TestTraceExtractor implements InvocationHandler {

        private List<StackTraceElement> traceElements;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            traceElements = StackTraceExtractor.getTraceForProxiedMethod(method);
            return null;
        }
    }

    @Test
    public void testGetTraceForProxiedMethod() {
        TestTraceExtractor traceExtractor = new TestTraceExtractor();
        TestBase testProxy = (TestBase) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{TestBase.class}, traceExtractor);
        testProxy.testBaseMethod();
        Assert.assertTrue(traceExtractor.traceElements.size() > 0);
        Assert.assertEquals("Should start with base method", "testBaseMethod", traceExtractor.traceElements.get(0).getMethodName());
        Assert.assertEquals("Should be followed by unit test name",
                "testGetTraceForProxiedMethod", traceExtractor.traceElements.get(1).getMethodName());

    }

    @Test
    public void testPrintStackTrace() {
        TestTraceExtractor traceExtractor = new TestTraceExtractor();
        TestBase testProxy = (TestBase) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{TestBase.class}, traceExtractor);
        testProxy.testBaseMethod();
        String stackTraceString = StackTraceExtractor.printStackTrace(traceExtractor.traceElements);
        Assert.assertNotNull(stackTraceString);
        Assert.assertTrue(stackTraceString.startsWith("io.sniffy.util.StackTraceExtractorTest.TestBase.testBaseMethod(Unknown Source)"));
        Assert.assertTrue(stackTraceString.contains("io.sniffy.util.StackTraceExtractorTest.testPrintStackTrace(StackTraceExtractorTest.java"));
    }

    @Test
    public void testPrintStackTrace_Empty() {
        Assert.assertEquals("", StackTraceExtractor.printStackTrace(null));
        Assert.assertEquals("", StackTraceExtractor.printStackTrace(Collections.EMPTY_LIST));
    }
}
