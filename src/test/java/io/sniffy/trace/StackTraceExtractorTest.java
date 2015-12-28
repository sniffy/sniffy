package io.sniffy.trace;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
}
