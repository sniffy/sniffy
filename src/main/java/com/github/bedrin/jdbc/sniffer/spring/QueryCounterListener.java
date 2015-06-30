package com.github.bedrin.jdbc.sniffer.spring;

import com.github.bedrin.jdbc.sniffer.Sniffer;
import com.github.bedrin.jdbc.sniffer.Spy;
import com.github.bedrin.jdbc.sniffer.WrongNumberOfQueriesError;
import com.github.bedrin.jdbc.sniffer.Expectation;
import com.github.bedrin.jdbc.sniffer.Expectations;
import com.github.bedrin.jdbc.sniffer.NoQueriesAllowed;
import com.github.bedrin.jdbc.sniffer.util.ExceptionUtil;
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

    private static final Method GET_TEST_METHOD;
    private static final Method SET_ATTRIBUTE_METHOD;
    private static final Method GET_ATTRIBUTE_METHOD;

    static {
        Method getTestMethod = null;
        Method setAttributeMethod = null;
        Method getAttributeMethod = null;
        try {
            getTestMethod = TestContext.class.getMethod("getTestMethod");
            setAttributeMethod = TestContext.class.getMethod("setAttribute", String.class, Object.class);
            getAttributeMethod = TestContext.class.getMethod("getAttribute", String.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace(); // TODO: what to do with the exception?
        }
        GET_TEST_METHOD = getTestMethod;
        SET_ATTRIBUTE_METHOD = setAttributeMethod;
        GET_ATTRIBUTE_METHOD = getAttributeMethod;
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

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {

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
                if (expectation1.value() != -1) {
                    if (expectation1.atMost() != -1 || expectation1.atLeast() != -1) {
                        throw new IllegalArgumentException("Cannot specify value parameter together with atLeast or atMost parameters");
                    }
                }
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

        if (null != spyAttribute) {

            Spy spy = (Spy) spyAttribute;

            try {
                spy.close();
            } catch (WrongNumberOfQueriesError jdbcSnifferError) {

                Throwable throwable = testContext.getTestException();
                if (null != throwable) {
                    if (!ExceptionUtil.addSuppressed(throwable, jdbcSnifferError)) {
                        jdbcSnifferError.printStackTrace();
                    }
                } else {
                    throw jdbcSnifferError;
                }

            }

        }

    }
}
