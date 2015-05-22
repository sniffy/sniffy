package com.github.bedrin.jdbc.sniffer.spring;

import com.github.bedrin.jdbc.sniffer.Sniffer;
import com.github.bedrin.jdbc.sniffer.Spy;
import com.github.bedrin.jdbc.sniffer.WrongNumberOfQueriesError;
import com.github.bedrin.jdbc.sniffer.junit.Expectation;
import com.github.bedrin.jdbc.sniffer.junit.Expectations;
import com.github.bedrin.jdbc.sniffer.junit.NoQueriesAllowed;
import com.github.bedrin.jdbc.sniffer.util.ExceptionUtil;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class QueryCounterListener extends AbstractTestExecutionListener {

    private static final String SPY_ATTRIBUTE_NAME = "spy";

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {

        Method testMethod = testContext.getTestMethod();

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

            testContext.setAttribute(SPY_ATTRIBUTE_NAME,
                    Sniffer.expect(expectationList)
            );

        } else if (null != notAllowedQueries) {
            testContext.setAttribute(SPY_ATTRIBUTE_NAME,
                    Sniffer.expect(Collections.singletonList(NoQueriesAllowed.class.getAnnotation(Expectation.class)))
            );
        }

    }

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {

        Object spyAttribute = testContext.getAttribute(SPY_ATTRIBUTE_NAME);

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
