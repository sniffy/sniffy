package io.sniffy.util;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class StackTraceExtractorTest {

    interface TestBase {
        void testBaseMethod();
    }

    static class TestTraceExtractor implements InvocationHandler {

        private Future<String> stackTrace;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            stackTrace = StackTraceExtractor.getStatckTraceFuture(method);
            return null;
        }
    }

    @Test
    public void testGetTraceForProxiedMethod() throws ExecutionException, InterruptedException {
        TestTraceExtractor traceExtractor = new TestTraceExtractor();
        TestBase testProxy = (TestBase) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{TestBase.class}, traceExtractor);
        testProxy.testBaseMethod();
        String[] stackTrace = traceExtractor.stackTrace.get().split("\\n");
        Assert.assertTrue(stackTrace.length > 0);
        Assert.assertTrue("Should start with base method", stackTrace[0].contains("testBaseMethod"));
        Assert.assertTrue("Should be followed by unit test name", stackTrace[1].contains("testGetTraceForProxiedMethod"));

    }

    @Test
    public void testPrintStackTrace() throws ExecutionException, InterruptedException {
        TestTraceExtractor traceExtractor = new TestTraceExtractor();
        TestBase testProxy = (TestBase) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{TestBase.class}, traceExtractor);
        testProxy.testBaseMethod();
        String stackTraceString = traceExtractor.stackTrace.get();
        Assert.assertNotNull(stackTraceString);
        Assert.assertTrue(stackTraceString.startsWith("io.sniffy.util.StackTraceExtractorTest.TestBase.testBaseMethod(Unknown Source)"));
        Assert.assertTrue(stackTraceString.contains("io.sniffy.util.StackTraceExtractorTest.testPrintStackTrace(StackTraceExtractorTest.java"));
    }

    @Test
    public void testPrintStackTrace_Empty() throws ExecutionException, InterruptedException {
        Assert.assertEquals("", StackTraceExtractor.getStatckTraceFuture((Method)null).get());
        Assert.assertEquals("", StackTraceExtractor.getStatckTraceFuture((String)null).get());
    }

}
