package io.sniffy.testng;

import io.sniffy.Spy;
import io.sniffy.WrongNumberOfQueriesError;
import io.sniffy.Expectation;
import io.sniffy.Expectations;
import io.sniffy.NoQueriesAllowed;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.*;
import io.sniffy.util.ExceptionUtil;
import org.testng.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

        Expectations expectations = method.getAnnotation(Expectations.class);
        Expectation expectation = method.getAnnotation(Expectation.class);
        NoQueriesAllowed notAllowedQueries = method.getAnnotation(NoQueriesAllowed.class);

        // If no annotations present, check the test class and its superclasses
        for (Class<?> testClass = method.getDeclaringClass();
             null == expectations && null == expectation && null == notAllowedQueries && !Object.class.equals(testClass);
             testClass = testClass.getSuperclass()) {
            expectations = testClass.getAnnotation(Expectations.class);
            expectation = testClass.getAnnotation(Expectation.class);
            notAllowedQueries = testClass.getAnnotation(NoQueriesAllowed.class);
        }

        if (null != expectation && null != notAllowedQueries) {
            fail(testResult, "Cannot specify @Expectation and @NotAllowedQueries on one test method");
        } else if (null != expectations && null != notAllowedQueries) {
            fail(testResult, "Cannot specify @Expectations and @NotAllowedQueries on one test method");
        } else if (null != expectations || null != expectation) {

            List<Expectation> expectationList = new ArrayList<Expectation>();

            if (null != expectation) {
                expectationList.add(expectation);
            }

            if (null != expectations) {
                expectationList.addAll(Arrays.asList(expectations.value()));
            }

            for (Expectation expectation1 : expectationList) {
                if (expectation1.value() != -1) {
                    if (expectation1.atMost() != -1 || expectation1.atLeast() != -1) {
                        fail(testResult, "Cannot specify value parameter together with atLeast or atMost parameters");
                    }
                }
            }

            testResult.setAttribute(SPY_ATTRIBUTE_NAME, expect(expectationList));

        } else if (null != notAllowedQueries) {
            Expectation annotation = NoQueriesAllowed.class.getAnnotation(Expectation.class);
            testResult.setAttribute(SPY_ATTRIBUTE_NAME, expect(Collections.singletonList(annotation)));
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
