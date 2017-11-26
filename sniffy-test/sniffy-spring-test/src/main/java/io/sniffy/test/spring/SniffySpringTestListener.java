package io.sniffy.test.spring;

import io.sniffy.Sniffy;
import io.sniffy.SniffyAssertionError;
import io.sniffy.Spy;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.DisableSockets;
import io.sniffy.socket.SocketExpectation;
import io.sniffy.socket.TcpConnections;
import io.sniffy.sql.SqlExpectation;
import io.sniffy.sql.SqlQueries;
import io.sniffy.test.AnnotationProcessor;
import io.sniffy.test.SharedConnectionDataSource;
import io.sniffy.util.ExceptionUtil;
import io.sniffy.util.Range;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @since 3.1
 */
public class SniffySpringTestListener extends AbstractTestExecutionListener {

    /** Logger used by this class. Available to subclasses. */
    protected final Log logger = LogFactory.getLog(getClass());

    private static final String SPY_ATTRIBUTE_NAME = "spy";
    private static final String DISABLE_SOCKETS_ATTRIBUTE_NAME = "disableSockets";
    private static final String SHARED_CONNECTION_DATASOURCES_ATTRIBUTE_NAME = "sharedConnectionDataSources";

    // In different version of spring org.springframework.test.context.TestContext is either class or interface
    // In order to keep binary compatibility with all version we should use reflection

    private static final Method GET_TEST_METHOD;
    private static final Method SET_ATTRIBUTE_METHOD;
    private static final Method GET_ATTRIBUTE_METHOD;
    private static final Method REMOVE_ATTRIBUTE_METHOD;
    private static final Method GET_TEST_EXCEPTION_METHOD;

    private static final NoSuchMethodException INITIALIZATION_EXCEPTION;

    static {
        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        Sniffy.initialize();

        Method getTestMethod = null;
        Method setAttributeMethod = null;
        Method getAttributeMethod = null;
        Method removeAttributeMethod = null;
        Method getTestExceptionMethod = null;

        NoSuchMethodException initializationException = null;

        try {
            // TODO: do we have reflection because of old spring compatibility?
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

        List<SocketExpectation> socketExpectationList = AnnotationProcessor.buildSocketExpectationList(testMethod);
        List<SqlExpectation> sqlExpectationList = AnnotationProcessor.buildSqlExpectationList(testMethod);

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

            setAttribute(testContext, SPY_ATTRIBUTE_NAME, spy);
        }

        DisableSockets disableSockets = AnnotationProcessor.getAnnotationRecursive(testMethod, DisableSockets.class);

        if (null != disableSockets) {
            ConnectionsRegistry.INSTANCE.setSocketAddressStatus(null, null, -1);
            setAttribute(testContext, DISABLE_SOCKETS_ATTRIBUTE_NAME, disableSockets);
        }

        SharedConnection sharedConnection = AnnotationProcessor.getAnnotationRecursive(testMethod, SharedConnection.class);

        if (null != sharedConnection) {
            Map<String, SharedConnectionDataSource> sharedConnectionDataSources =
                    testContext.getApplicationContext().getBeansOfType(SharedConnectionDataSource.class);
            if (null == sharedConnectionDataSources) {
                logger.warn("Unable to start a shared connection session cause no instances of SharedConnectionDataSource found");
            } else {
                for (SharedConnectionDataSource sharedConnectionDataSource : sharedConnectionDataSources.values()) {
                    sharedConnectionDataSource.setCurrentThreadAsMaster();
                }
                setAttribute(testContext, SHARED_CONNECTION_DATASOURCES_ATTRIBUTE_NAME, sharedConnectionDataSources.values());
            }
        }

    }

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {

        Object sharedConnectionDataSourcesAttribute = getAttribute(testContext, SHARED_CONNECTION_DATASOURCES_ATTRIBUTE_NAME);
        if (null != sharedConnectionDataSourcesAttribute && sharedConnectionDataSourcesAttribute instanceof Collection) {
            @SuppressWarnings("unchecked") Collection<SharedConnectionDataSource> sharedConnectionDataSources =
                    (Collection<SharedConnectionDataSource>) sharedConnectionDataSourcesAttribute;
            for (SharedConnectionDataSource sharedConnectionDataSource : sharedConnectionDataSources) {
                sharedConnectionDataSource.resetMasterConnection();
            }
            removeAttribute(testContext, SHARED_CONNECTION_DATASOURCES_ATTRIBUTE_NAME);
        }

        Object spyAttribute = getAttribute(testContext, SPY_ATTRIBUTE_NAME);
        removeAttribute(testContext, SPY_ATTRIBUTE_NAME);

        if (null != spyAttribute) {

            Spy spy = (Spy) spyAttribute;

            try {
                spy.close();
            } catch (SniffyAssertionError sniffyError) {

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

        Object disableSockets = getAttribute(testContext, DISABLE_SOCKETS_ATTRIBUTE_NAME);
        removeAttribute(testContext, DISABLE_SOCKETS_ATTRIBUTE_NAME);

        if (null != disableSockets) {
            ConnectionsRegistry.INSTANCE.clear();
        }

    }
}
