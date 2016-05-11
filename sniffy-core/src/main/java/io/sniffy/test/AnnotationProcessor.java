package io.sniffy.test;

import io.sniffy.Expectation;
import io.sniffy.Expectations;
import io.sniffy.NoQueriesAllowed;
import io.sniffy.socket.NoSocketsAllowed;
import io.sniffy.socket.SocketExpectation;
import io.sniffy.socket.SocketExpectations;
import io.sniffy.util.Range;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AnnotationProcessor {

    public static List<SocketExpectation> buildSocketExpectationList(
            SocketExpectation socketExpectation,
            SocketExpectations socketExpectations,
            NoSocketsAllowed noSocketsAllowed) {

        List<SocketExpectation> socketExpectationList = new ArrayList<SocketExpectation>();

        if (null != socketExpectation && null != noSocketsAllowed) {
            throw new IllegalArgumentException("Cannot specify @Expectation and @NotAllowedQueries on one test method");
        } else if (null != socketExpectations && null != noSocketsAllowed) {
            throw new IllegalArgumentException("Cannot specify @Expectations and @NotAllowedQueries on one test method");
        } else if (null != socketExpectations || null != socketExpectation) {

            if (null != socketExpectation) {
                socketExpectationList.add(socketExpectation);
            }

            if (null != socketExpectations) {
                socketExpectationList.addAll(Arrays.asList(socketExpectations.value()));
            }

            for (SocketExpectation expectation1 : socketExpectationList) {
                Range.parse(expectation1.connections());
            }

        } else if (null != noSocketsAllowed) {
            SocketExpectation annotation = NoQueriesAllowed.class.getAnnotation(SocketExpectation.class);
            socketExpectationList.add(annotation);
        }

        return socketExpectationList;
    }

    public static List<SocketExpectation> buildSocketExpectationList(Method method) {

        SocketExpectations socketExpectations = method.getAnnotation(SocketExpectations.class);
        SocketExpectation socketExpectation = method.getAnnotation(SocketExpectation.class);
        NoSocketsAllowed noSocketsAllowed = method.getAnnotation(NoSocketsAllowed.class);

        // If no annotations present, check the test class and its superclasses
        for (Class<?> testClass = method.getDeclaringClass();
             null == socketExpectations && null == socketExpectation && null == noSocketsAllowed && !Object.class.equals(testClass);
             testClass = testClass.getSuperclass()) {
            socketExpectations = testClass.getAnnotation(SocketExpectations.class);
            socketExpectation = testClass.getAnnotation(SocketExpectation.class);
            noSocketsAllowed = testClass.getAnnotation(NoSocketsAllowed.class);
        }

        return AnnotationProcessor.buildSocketExpectationList(socketExpectation, socketExpectations, noSocketsAllowed);

    }

    public static List<Expectation> buildSqlExpectationList(
            Expectations expectations,
            Expectation expectation,
            NoQueriesAllowed notAllowedQueries) {

        List<Expectation> expectationList = new ArrayList<Expectation>();

        if (null != expectation && null != notAllowedQueries) {
            throw new IllegalArgumentException("Cannot specify @Expectation and @NotAllowedQueries on one test method");
        } else if (null != expectations && null != notAllowedQueries) {
            throw new IllegalArgumentException("Cannot specify @Expectations and @NotAllowedQueries on one test method");
        } else if (null != expectations || null != expectation) {

            if (null != expectation) {
                expectationList.add(expectation);
            }

            if (null != expectations) {
                expectationList.addAll(Arrays.asList(expectations.value()));
            }

            for (Expectation expectation1 : expectationList) {
                Range.parse(expectation1);
            }

        } else if (null != notAllowedQueries) {
            Expectation annotation = NoQueriesAllowed.class.getAnnotation(Expectation.class);
            expectationList.add(annotation);
        }

        return expectationList;
    }

    public static List<Expectation> buildSqlExpectationList(Method method) {

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

        return AnnotationProcessor.buildSqlExpectationList(expectations, expectation, notAllowedQueries);

    }

}
