package io.sniffy.testng;

import io.sniffy.*;
import io.sniffy.socket.SocketExpectation;
import io.sniffy.socket.TcpConnections;
import io.sniffy.test.AnnotationProcessor;
import io.sniffy.util.ExceptionUtil;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

import java.lang.reflect.Method;
import java.util.List;

import static io.sniffy.Sniffer.expect;
import static org.testng.ITestResult.FAILURE;

/**
 * Provides integration with TestNG. Add {@code QueryCounter} as a listener to your TestNG test:
 * <pre>
 * <code>
 * {@literal @}Listeners(QueryCounter.class)
 * public class SampleTestNgTestSuite {
 *     // ... here goes some test methods
 * }
 * </code>
 * </pre>
 * @see Expectations
 * @see Expectation
 * @see NoQueriesAllowed
 * @since 2.1
 */
public class QueryCounter implements IInvokedMethodListener {

    private static final String SPY_ATTRIBUTE_NAME = "spy";

    private static void fail(ITestResult testResult, String message) {
        testResult.setStatus(FAILURE);
        IllegalArgumentException throwable = new IllegalArgumentException(message);
        testResult.setThrowable(throwable);
        throw throwable;
    }

    public void beforeInvocation(IInvokedMethod invokedMethod, ITestResult testResult) {

        Method method = invokedMethod.getTestMethod().getConstructorOrMethod().getMethod();

        List<Expectation> expectationList = null;
        try {
            expectationList = AnnotationProcessor.buildSqlExpectationList(method);
        } catch (IllegalArgumentException e) {
            fail(testResult, e.getMessage());
        }

        List<SocketExpectation> socketExpectationList = null;
        try {
            socketExpectationList = AnnotationProcessor.buildSocketExpectationList(method);
        } catch (IllegalArgumentException e) {
            fail(testResult, e.getMessage());
        }


        if ((null != expectationList && !expectationList.isEmpty()) ||
                (null != socketExpectationList && !socketExpectationList.isEmpty())) {

            Spy spy = expect(expectationList);

            if (null != socketExpectationList) {
                for (SocketExpectation socketExpectation : socketExpectationList) {
                    spy = spy.expect(new TcpConnections.TcpExpectation(socketExpectation));
                }
            }

            testResult.setAttribute(SPY_ATTRIBUTE_NAME, spy);
        }

    }

    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {

        Object spyAttribute = testResult.getAttribute(SPY_ATTRIBUTE_NAME);

        if (null != spyAttribute) {

            Spy spy = (Spy) spyAttribute;

            try {
                spy.close();
            } catch (WrongNumberOfQueriesError sniffyError) {

                testResult.setStatus(FAILURE);

                Throwable throwable = testResult.getThrowable();
                if (null != throwable) {
                    if (!ExceptionUtil.addSuppressed(throwable, sniffyError)) {
                        sniffyError.printStackTrace();
                    }
                } else {
                    throwable = sniffyError;
                }

                testResult.setThrowable(throwable);

            }

        }

    }

}
