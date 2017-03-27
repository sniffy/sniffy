package io.sniffy.test;

import io.sniffy.Expectation;
import io.sniffy.Expectations;
import io.sniffy.NoQueriesAllowed;
import io.sniffy.socket.NoSocketsAllowed;
import io.sniffy.socket.SocketExpectation;
import io.sniffy.socket.SocketExpectations;
import io.sniffy.sql.NoSql;
import io.sniffy.sql.SqlExpectation;
import io.sniffy.sql.SqlExpectations;
import io.sniffy.util.Range;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @since 3.1
 */
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
            SocketExpectation annotation = NoSocketsAllowed.class.getAnnotation(SocketExpectation.class);
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

    public static List<SqlExpectation> buildSqlExpectationList(
            SqlExpectations expectations,
            SqlExpectation expectation,
            NoSql notAllowedQueries) {

        List<SqlExpectation> expectationList = new ArrayList<SqlExpectation>();

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

            for (SqlExpectation expectation1 : expectationList) {
                Range.parse(expectation1.count());
                Range.parse(expectation1.rows());
            }

        } else if (null != notAllowedQueries) {
            SqlExpectation annotation = NoSql.class.getAnnotation(SqlExpectation.class);
            expectationList.add(annotation);
        }

        return expectationList;
    }

    public static List<SqlExpectation> buildSqlExpectationList(Method method) {

        SqlExpectations sqlExpectations = method.getAnnotation(SqlExpectations.class);
        SqlExpectation sqlExpectation = method.getAnnotation(SqlExpectation.class);
        NoSql noSql = method.getAnnotation(NoSql.class);

        if (null == sqlExpectations) sqlExpectations = Expectations.SqlExpectationsAdapter.adapter(
                method.getAnnotation(Expectations.class)
        );
        if (null == sqlExpectation) sqlExpectation = Expectation.SqlExpectationAdapter.adapter(
                method.getAnnotation(Expectation.class)
        );
        if (null == noSql && null != method.getAnnotation(NoQueriesAllowed.class)) noSql =
                method.getAnnotation(NoQueriesAllowed.class).annotationType().getAnnotation(NoSql.class);

        return buildSqlExpectationList(method.getDeclaringClass(), sqlExpectations, sqlExpectation, noSql);


    }

    public static List<SqlExpectation> buildSqlExpectationList(Class<?> declaringClass, SqlExpectations sqlExpectations, SqlExpectation sqlExpectation, NoSql noSql) {
        // If no annotations present, check the test class and its superclasses
        for (Class<?> testClass = declaringClass;
             null == sqlExpectations && null == sqlExpectation && null == noSql && !Object.class.equals(testClass);
             testClass = testClass.getSuperclass()) {
            sqlExpectations = testClass.getAnnotation(SqlExpectations.class);
            sqlExpectation = testClass.getAnnotation(SqlExpectation.class);
            noSql = testClass.getAnnotation(NoSql.class);

            if (null == sqlExpectations) sqlExpectations = Expectations.SqlExpectationsAdapter.adapter(
                    testClass.getAnnotation(Expectations.class)
            );
            if (null == sqlExpectation) sqlExpectation = Expectation.SqlExpectationAdapter.adapter(
                    testClass.getAnnotation(Expectation.class)
            );
            if (null == noSql && null != testClass.getAnnotation(NoQueriesAllowed.class)) noSql =
                    testClass.getAnnotation(NoQueriesAllowed.class).annotationType().getAnnotation(NoSql.class);
        }

        return AnnotationProcessor.buildSqlExpectationList(sqlExpectations, sqlExpectation, noSql);
    }

    /**
     * @since 3.1.3
     */
    public static <T extends Annotation> T getAnnotationRecursive(Method method, Class<T> annotationClass) {
        T annotation;

        annotation = method.getAnnotation(annotationClass);
        if (null == annotation) {
            for (Class<?> clazz = method.getDeclaringClass();
                 null == annotation && !Object.class.equals(clazz);
                 clazz = clazz.getSuperclass()) {
                annotation = clazz.getAnnotation(annotationClass);
            }
        }

        return annotation;
    }

}
