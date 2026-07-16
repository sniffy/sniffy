package io.sniffy.test.junit.jupiter;

import io.sniffy.Expectation;
import io.sniffy.Expectations;
import io.sniffy.NoQueriesAllowed;
import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.socket.AddressMatchers;
import io.sniffy.socket.DisableSockets;
import io.sniffy.socket.NoSocketsAllowed;
import io.sniffy.socket.SocketExpectation;
import io.sniffy.socket.SocketExpectations;
import io.sniffy.socket.TcpConnections;
import io.sniffy.sql.NoSql;
import io.sniffy.sql.SqlExpectation;
import io.sniffy.sql.SqlExpectations;
import io.sniffy.sql.SqlQueries;
import io.sniffy.test.AnnotationProcessor;
import io.sniffy.util.Range;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Native JUnit Jupiter extension for Sniffy SQL and socket expectations.
 * Register explicitly with {@code @ExtendWith(SniffyExtension.class)}.
 *
 * @since 4.0
 */
public class SniffyExtension implements BeforeEachCallback, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(SniffyExtension.class);

    static {
        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        SniffyConfiguration.INSTANCE.setMonitorNio(true);
        Sniffy.initialize();
    }

    public void beforeEach(ExtensionContext context) throws Exception {
        State state = resolveState(context);
        context.getStore(NAMESPACE).put(context.getUniqueId(), state);
        try {
            if (state.requiresSpy()) {
                Spy<?> spy = Sniffy.spy();
                state.spy = spy;
                for (SqlExpectation sqlExpectation : state.sqlExpectations) {
                    spy = spy.expect(new SqlQueries.SqlExpectation(
                            Range.parse(sqlExpectation.count()).min,
                            Range.parse(sqlExpectation.count()).max,
                            Range.parse(sqlExpectation.rows()).min,
                            Range.parse(sqlExpectation.rows()).max,
                            sqlExpectation.threads(),
                            sqlExpectation.query()
                    ));
                    state.spy = spy;
                }
                for (SocketExpectation socketExpectation : state.socketExpectations) {
                    spy = spy.expect(new TcpConnections.TcpExpectation(
                            Range.parse(socketExpectation.connections()).min,
                            Range.parse(socketExpectation.connections()).max,
                            socketExpectation.threads(),
                            "".equals(socketExpectation.hostName()) ? null : AddressMatchers.exactAddressMatcher(socketExpectation.hostName())
                    ));
                    state.spy = spy;
                }
            }
            if (state.disableSockets) {
                state.connectionsRegistrySnapshot = ConnectionsRegistry.INSTANCE.takeSnapshot();
                ConnectionsRegistry.INSTANCE.setSocketAddressStatus(null, null, -1);
            }
        } catch (Throwable setupFailure) {
            Throwable cleanupFailure = cleanup(state);
            context.getStore(NAMESPACE).remove(context.getUniqueId());
            if (cleanupFailure != null) {
                setupFailure.addSuppressed(cleanupFailure);
            }
            rethrow(setupFailure);
        }
    }

    public void afterEach(ExtensionContext context) throws Exception {
        State state = context.getStore(NAMESPACE).remove(context.getUniqueId(), State.class);
        if (state == null) {
            return;
        }
        Throwable primary = context.getExecutionException().orElse(null);
        Throwable cleanupFailure = cleanup(state);
        if (primary != null) {
            if (cleanupFailure != null) primary.addSuppressed(cleanupFailure);
            rethrow(primary);
        }
        if (cleanupFailure != null) rethrow(cleanupFailure);
    }

    private static Throwable cleanup(State state) {
        Throwable cleanupFailure = null;
        try {
            if (state.spy != null) {
                state.spy.close();
            }
        } catch (Throwable t) {
            cleanupFailure = t;
        } finally {
            try {
                state.restoreConnectionsRegistry();
            } catch (Throwable t) {
                if (cleanupFailure == null) cleanupFailure = t;
                else cleanupFailure.addSuppressed(t);
            }
        }
        return cleanupFailure;
    }

    private static State resolveState(ExtensionContext context) {
        AnnotatedElement method = context.getRequiredTestMethod();
        Class<?> testClass = context.getRequiredTestClass();
        SqlExpectations sqlExpectations = find(method, SqlExpectations.class);
        SqlExpectation sqlExpectation = find(method, SqlExpectation.class);
        NoSql noSql = find(method, NoSql.class);
        if (sqlExpectations == null) sqlExpectations = Expectations.SqlExpectationsAdapter.adapter(find(method, Expectations.class));
        if (sqlExpectation == null) sqlExpectation = Expectation.SqlExpectationAdapter.adapter(find(method, Expectation.class));
        if (noSql == null && find(method, NoQueriesAllowed.class) != null) noSql = NoQueriesAllowed.class.getAnnotation(NoSql.class);
        for (Class<?> clazz = testClass; sqlExpectations == null && sqlExpectation == null && noSql == null && clazz != null && !Object.class.equals(clazz); clazz = clazz.getSuperclass()) {
            sqlExpectations = find(clazz, SqlExpectations.class);
            sqlExpectation = find(clazz, SqlExpectation.class);
            noSql = find(clazz, NoSql.class);
            if (sqlExpectations == null) sqlExpectations = Expectations.SqlExpectationsAdapter.adapter(find(clazz, Expectations.class));
            if (sqlExpectation == null) sqlExpectation = Expectation.SqlExpectationAdapter.adapter(find(clazz, Expectation.class));
            if (noSql == null && find(clazz, NoQueriesAllowed.class) != null) noSql = NoQueriesAllowed.class.getAnnotation(NoSql.class);
        }

        SocketExpectations socketExpectations = find(method, SocketExpectations.class);
        SocketExpectation socketExpectation = find(method, SocketExpectation.class);
        NoSocketsAllowed noSocketsAllowed = find(method, NoSocketsAllowed.class);
        for (Class<?> clazz = testClass; socketExpectations == null && socketExpectation == null && noSocketsAllowed == null && clazz != null && !Object.class.equals(clazz); clazz = clazz.getSuperclass()) {
            socketExpectations = find(clazz, SocketExpectations.class);
            socketExpectation = find(clazz, SocketExpectation.class);
            noSocketsAllowed = find(clazz, NoSocketsAllowed.class);
        }

        DisableSockets disableSockets = find(method, DisableSockets.class);
        for (Class<?> clazz = testClass; disableSockets == null && clazz != null && !Object.class.equals(clazz); clazz = clazz.getSuperclass()) {
            disableSockets = find(clazz, DisableSockets.class);
        }

        return new State(
                AnnotationProcessor.buildSqlExpectationList(sqlExpectations, sqlExpectation, noSql),
                AnnotationProcessor.buildSocketExpectationList(socketExpectation, socketExpectations, noSocketsAllowed),
                disableSockets != null
        );
    }

    private static <A extends Annotation> A find(AnnotatedElement element, Class<A> annotationClass) {
        A direct = element.getAnnotation(annotationClass);
        if (direct != null) return direct;
        for (Annotation annotation : element.getAnnotations()) {
            A meta = findOnAnnotation(annotation.annotationType(), annotationClass, new ArrayList<Class<? extends Annotation>>());
            if (meta != null) return meta;
        }
        return null;
    }

    private static <A extends Annotation> A findOnAnnotation(Class<? extends Annotation> source, Class<A> annotationClass, List<Class<? extends Annotation>> visited) {
        if (visited.contains(source) || source.getName().startsWith("java.lang.annotation.")) return null;
        visited.add(source);
        A direct = source.getAnnotation(annotationClass);
        if (direct != null) return direct;
        for (Annotation annotation : source.getAnnotations()) {
            A nested = findOnAnnotation(annotation.annotationType(), annotationClass, visited);
            if (nested != null) return nested;
        }
        return null;
    }

    private static void rethrow(Throwable throwable) throws Exception {
        if (throwable instanceof Exception) throw (Exception) throwable;
        if (throwable instanceof Error) throw (Error) throwable;
        throw new RuntimeException(throwable);
    }

    private static class State {
        private final List<SqlExpectation> sqlExpectations;
        private final List<SocketExpectation> socketExpectations;
        private final boolean disableSockets;
        private ConnectionsRegistry.ConnectionsRegistrySnapshot connectionsRegistrySnapshot;
        private Spy<?> spy;

        private State(List<SqlExpectation> sqlExpectations, List<SocketExpectation> socketExpectations, boolean disableSockets) {
            this.sqlExpectations = sqlExpectations;
            this.socketExpectations = socketExpectations;
            this.disableSockets = disableSockets;
            this.connectionsRegistrySnapshot = null;
        }

        private boolean requiresSpy() {
            return !sqlExpectations.isEmpty() || !socketExpectations.isEmpty();
        }

        private void restoreConnectionsRegistry() {
            ConnectionsRegistry.INSTANCE.restoreSnapshot(connectionsRegistrySnapshot);
        }
    }
}
