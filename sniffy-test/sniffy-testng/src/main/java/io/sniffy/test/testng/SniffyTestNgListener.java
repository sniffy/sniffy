package io.sniffy.test.testng;

import io.sniffy.Sniffy;
import io.sniffy.SniffyAssertionError;
import io.sniffy.Spy;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.*;
import io.sniffy.sql.SqlExpectation;
import io.sniffy.sql.SqlQueries;
import io.sniffy.test.AnnotationProcessor;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.Range;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Provides integration with TestNG. Add {@code SniffyTestNgListener} as a listener to your TestNG test:
 * <pre>
 * <code>
 * {@literal @}Listeners(SniffyTestNgListener.class)
 * public class SampleTestNgTestSuite {
 *     // ... here goes some test methods
 * }
 * </code>
 * </pre>
 * @see SocketExpectations
 * @see SocketExpectation
 * @see NoSocketsAllowed
 * @since 3.1
 */
public class SniffyTestNgListener implements IInvokedMethodListener {

    private static final String SPY_ATTRIBUTE_NAME = "spy";
    private static final String DISABLE_SOCKETS_ATTRIBUTE_NAME = "disableSockets";

    static {
        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        Sniffy.initialize();
    }

    private static void fail(ITestResult testResult, String message) {
        testResult.setStatus(ITestResult.FAILURE);
        IllegalArgumentException throwable = new IllegalArgumentException(message);
        testResult.setThrowable(throwable);
        throw throwable;
    }

    public void beforeInvocation(IInvokedMethod invokedMethod, ITestResult testResult) {

        Method method = invokedMethod.getTestMethod().getConstructorOrMethod().getMethod();

        List<SqlExpectation> sqlExpectationList = null;
        try {
            sqlExpectationList = AnnotationProcessor.buildSqlExpectationList(method);
        } catch (IllegalArgumentException e) {
            fail(testResult, e.getMessage());
        }

        List<SocketExpectation> socketExpectationList = null;
        try {
            socketExpectationList = AnnotationProcessor.buildSocketExpectationList(method);
        } catch (IllegalArgumentException e) {
            fail(testResult, e.getMessage());
        }


        if ((null != sqlExpectationList && !sqlExpectationList.isEmpty()) ||
                (null != socketExpectationList && !socketExpectationList.isEmpty())) {

            Spy spy = Sniffy.spy();

            if (null != sqlExpectationList) {
                for (SqlExpectation sqlExpectation : sqlExpectationList) {
                    spy = spy.expect(new SqlQueries.SqlExpectation(
                                        Range.parse(sqlExpectation.count()).min,
                                        Range.parse(sqlExpectation.count()).max,
                                        Range.parse(sqlExpectation.rows()).min,
                                        Range.parse(sqlExpectation.rows()).max,
                                        sqlExpectation.threads(),
                                        sqlExpectation.query()
                                ));
                }
            }
            if (null != socketExpectationList) {
                for (SocketExpectation socketExpectation : socketExpectationList) {
                    spy = spy.expect(new TcpConnections.TcpExpectation(
                                        Range.parse(socketExpectation.connections()).min,
                                        Range.parse(socketExpectation.connections()).max,
                                        socketExpectation.threads(),
                                        "".equals(socketExpectation.hostName()) ? null : socketExpectation.hostName()
                                ));
                }
            }

            testResult.setAttribute(SPY_ATTRIBUTE_NAME, spy);
        }

        DisableSockets disableSockets = AnnotationProcessor.getAnnotationRecursive(method, DisableSockets.class);

        if (null != disableSockets) {
            ConnectionsRegistry.INSTANCE.setSocketAddressStatus(null, null, ConnectionsRegistry.ConnectionStatus.CLOSED);
            testResult.setAttribute(DISABLE_SOCKETS_ATTRIBUTE_NAME, disableSockets);
        }

    }

    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {

        Object spyAttribute = testResult.getAttribute(SPY_ATTRIBUTE_NAME);

        if (null != spyAttribute) {

            Spy spy = (Spy) spyAttribute;

            try {
                spy.close();
            } catch (SniffyAssertionError sniffyError) {

                testResult.setStatus(ITestResult.FAILURE);

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

            testResult.removeAttribute(SPY_ATTRIBUTE_NAME);

        }

        Object disableSockets = testResult.getAttribute(DISABLE_SOCKETS_ATTRIBUTE_NAME);
        testResult.removeAttribute(DISABLE_SOCKETS_ATTRIBUTE_NAME);

        if (null != disableSockets) {
            ConnectionsRegistry.INSTANCE.clear();
        }

    }

}
