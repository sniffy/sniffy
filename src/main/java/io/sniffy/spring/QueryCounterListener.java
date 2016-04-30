package io.sniffy.spring;

import io.sniffy.Sniffer;
import io.sniffy.Spy;
import io.sniffy.WrongNumberOfQueriesError;
import io.sniffy.Expectation;
import io.sniffy.Expectations;
import io.sniffy.NoQueriesAllowed;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.Range;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @since 2.2
 */
public class QueryCounterListener extends AbstractTestExecutionListener {

    private static final String SPY_ATTRIBUTE_NAME = "spy";

    // In different version of spring org.springframework.test.context.TestContext is either class or interface
    // In order to keep binary compatibility with all version we should use reflection

    private static final Method GET_TEST_METHOD;
    private static final Method SET_ATTRIBUTE_METHOD;
    private static final Method GET_ATTRIBUTE_METHOD;
    private static final Method REMOVE_ATTRIBUTE_METHOD;
    private static final Method GET_TEST_EXCEPTION_METHOD;

    private static final NoSuchMethodException INITIALIZATION_EXCEPTION;

    static {
        Method getTestMethod = null;
        Method setAttributeMethod = null;
        Method getAttributeMethod = null;
        Method removeAttributeMethod = null;
        Method getTestExceptionMethod = null;

        NoSuchMethodException initializationException = null;

        try {
            getTestMethod = TestContext.class.getMethod("getTestMethod");
            setAttributeMethod = TestContext.class.getMethod("setAttribute", String.class, Object.class);
            getAttributeMethod = TestContext.class.getMethod("getAttribute", String.class);
            removeAttributeMethod = TestContext.class.getMethod("removeAttribute", String.class);
            getTestExceptionMethod = TestContext.class.getMethod("getTestException");
        } catch (NoSuchMethodException e) {
            initializationException = e;
        }

        INITIALIZATION_EXCEPTION = initializationException;
        GET_TEST_METHOD = getTestMethod;
        SET_ATTRIBUTE_METHOD = setAttributeMethod;
        GET_ATTRIBUTE_METHOD = getAttributeMethod;
        REMOVE_ATTRIBUTE_METHOD = removeAttributeMethod;
        GET_TEST_EXCEPTION_METHOD = getTestExceptionMethod;
    }

    private static Method getTestMethod(TestContext testContext) throws InvocationTargetException, IllegalAccessException {
        return Method.class.cast(GET_TEST_METHOD.invoke(testContext));
    }

    private static void setAttribute(TestContext testContext, String name, Object value) throws InvocationTargetException, IllegalAccessException {
        SET_ATTRIBUTE_METHOD.invoke(testContext, name, value);
    }

    private static Object getAttribute(TestContext testContext, String name) throws InvocationTargetException, IllegalAccessException {
        return GET_ATTRIBUTE_METHOD.invoke(testContext, name);
    }

    private static Object removeAttribute(TestContext testContext, String name) throws InvocationTargetException, IllegalAccessException {
        return REMOVE_ATTRIBUTE_METHOD.invoke(testContext, name);
    }

    private static Throwable getTestException(TestContext testContext) throws InvocationTargetException, IllegalAccessException {
        return Throwable.class.cast(GET_TEST_EXCEPTION_METHOD.invoke(testContext));
    }

    private static void checkInitialized() throws NoSuchMethodException {
        if (null != INITIALIZATION_EXCEPTION) throw INITIALIZATION_EXCEPTION;
    }

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {

        checkInitialized();

        Method testMethod = getTestMethod(testContext);

        Expectations expectations = testMethod.getAnnotation(Expectations.class);
        Expectation expectation = testMethod.getAnnotation(Expectation.class);
        NoQueriesAllowed notAllowedQueries = testMethod.getAnnotation(NoQueriesAllowed.class);

        // If no annotations present, check the test class and its superclasses
        for (Class<?> testClass = testMethod.getDeclaringClass();
             null == expectations && null == expectation && null == notAllowedQueries && !Object.class.equals(testClass);
             testClass = testClass.getSuperclass()) {
            expectations = testClass.getAnnotation(Expectations.class);
            expectation = testClass.getAnnotation(Expectation.class);
            notAllowedQueries = testClass.getAnnotation(NoQueriesAllowed.class);
        }

        if (null != expectation && null != notAllowedQueries) {
            throw new IllegalArgumentException("Cannot specify @Expectation and @NotAllowedQueries on one test method");
        } else if (null != expectations && null != notAllowedQueries) {
            throw new IllegalArgumentException("Cannot specify @Expectations and @NotAllowedQueries on one test method");
        } else if (null != expectations || null != expectation) {

            List<Expectation> expectationList = new ArrayList<Expectation>();

            if (null != expectation) {
                expectationList.add(expectation);
            }

            if (null != expectations) {
                expectationList.addAll(Arrays.asList(expectations.value()));
            }

            for (Expectation expectation1 : expectationList) {
                Range.parse(expectation1);
            }

            setAttribute(testContext, SPY_ATTRIBUTE_NAME, Sniffer.expect(expectationList));

        } else if (null != notAllowedQueries) {
            setAttribute(testContext, SPY_ATTRIBUTE_NAME,
                    Sniffer.expect(Collections.singletonList(NoQueriesAllowed.class.getAnnotation(Expectation.class)))
            );
        }

    }

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {

        Object spyAttribute = getAttribute(testContext, SPY_ATTRIBUTE_NAME);
        removeAttribute(testContext, SPY_ATTRIBUTE_NAME);

        if (null != spyAttribute) {

            Spy spy = (Spy) spyAttribute;

            try {
                spy.close();
            } catch (WrongNumberOfQueriesError sniffyError) {

                Throwable throwable = getTestException(testContext);
                if (null != throwable) {
                    if (!ExceptionUtil.addSuppressed(throwable, sniffyError)) {
                        sniffyError.printStackTrace();
                    }
                } else {
                    throw sniffyError;
                }

            }

        }

    }
}
