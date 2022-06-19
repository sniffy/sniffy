package io.sniffy.tls;

import io.qameta.allure.Issue;
import io.sniffy.*;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.log.PolyglogLevel;
import io.sniffy.socket.AddressMatchers;
import io.sniffy.socket.NetworkPacket;
import io.sniffy.socket.SocketMetaData;
import org.jboss.logging.Logger;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.*;
import org.jboss.remoting3.spi.NetworkServerProvider;
import org.junit.*;
import org.junit.rules.TestName;
import org.wildfly.security.WildFlyElytronProvider;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.auth.realm.SimpleMapBackedSecurityRealm;
import org.wildfly.security.auth.server.MechanismConfiguration;
import org.wildfly.security.auth.server.SaslAuthenticationFactory;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.spec.ClearPasswordSpec;
import org.wildfly.security.permission.PermissionVerifier;
import org.wildfly.security.sasl.SaslMechanismSelector;
import org.wildfly.security.sasl.util.SaslMechanismInformation;
import org.wildfly.security.sasl.util.ServiceLoaderSaslServerFactory;
import org.xnio.*;

import javax.net.ssl.SSLContext;
import javax.security.sasl.SaslServerFactory;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivilegedAction;
import java.security.Security;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

public class CaptureJBossRemotingCaptureTrafficTest {

    @BeforeClass
    public static void loadTlsModule() {
        SniffyConfiguration.INSTANCE.setDecryptTls(true);
        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        SniffyConfiguration.INSTANCE.setMonitorNio(true);
        SniffyConfiguration.INSTANCE.setLogLevel(PolyglogLevel.TRACE);
        SniffyConfiguration.INSTANCE.setPacketMergeThreshold(10000);
        Sniffy.initialize();
    }

    /////////////

    protected final Endpoint clientEndpoint;
    protected final Endpoint serverEndpoint;

    private Closeable server;

    private static String providerName;

    @BeforeClass
    public static void doBeforeClass() {
        final WildFlyElytronProvider provider = new WildFlyElytronProvider();
        Security.addProvider(provider);
        providerName = provider.getName();
    }

    @AfterClass
    public static void doAfterClass() {
        Security.removeProvider(providerName);
    }

    public CaptureJBossRemotingCaptureTrafficTest() throws IOException {
        final EndpointBuilder endpointBuilder = Endpoint.builder();
        final XnioWorker.Builder workerBuilder = endpointBuilder.buildXnioWorker(Xnio.getInstance());
        workerBuilder.setCoreWorkerPoolSize(THREAD_POOL_SIZE).setMaxWorkerPoolSize(THREAD_POOL_SIZE).setWorkerIoThreads(IO_THREAD_COUNT);
        endpointBuilder.setEndpointName("connection-test-client");
        clientEndpoint = endpointBuilder.build();
        endpointBuilder.setEndpointName("connection-test-server");
        serverEndpoint = endpointBuilder.build();
    }

    @Rule
    public TestName name = new TestName();

    @Before
    public void before() throws Exception {
        System.gc();
        System.runFinalization();
        Logger.getLogger("TEST").infof("Running test %s", name.getMethodName());
        final NetworkServerProvider networkServerProvider = serverEndpoint.getConnectionProviderInterface("remote", NetworkServerProvider.class);
        final SecurityDomain.Builder domainBuilder = SecurityDomain.builder();
        final SimpleMapBackedSecurityRealm mainRealm = new SimpleMapBackedSecurityRealm();
        domainBuilder.addRealm("mainRealm", mainRealm).build();
        domainBuilder.setDefaultRealmName("mainRealm");
        final PasswordFactory passwordFactory = PasswordFactory.getInstance("clear");
        mainRealm.setPasswordMap("bob", passwordFactory.generatePassword(new ClearPasswordSpec("pass".toCharArray())));
        final SaslServerFactory saslServerFactory = new ServiceLoaderSaslServerFactory(getClass().getClassLoader());
        final SaslAuthenticationFactory.Builder builder = SaslAuthenticationFactory.builder();
        domainBuilder.setPermissionMapper((permissionMappable, roles) -> PermissionVerifier.ALL);
        builder.setSecurityDomain(domainBuilder.build());
        builder.setFactory(saslServerFactory);
        builder.setMechanismConfigurationSelector(mechanismInformation -> SaslMechanismInformation.Names.SCRAM_SHA_256.equals(mechanismInformation.getMechanismName()) ? MechanismConfiguration.EMPTY : null);
        final SaslAuthenticationFactory saslAuthenticationFactory = builder.build();
        // TODO: iterate with retries on IOException (or SocketException) and store the port in a property
        server = networkServerProvider.createServer(
                new InetSocketAddress("localhost", 30123),
                OptionMap.create(Options.SSL_ENABLED, Boolean.FALSE),
                saslAuthenticationFactory,
                SSLContext.getDefault()
        );
    }

    @After
    public void after() {
        IoUtils.safeClose(server);
        IoUtils.safeClose(clientEndpoint);
        IoUtils.safeClose(serverEndpoint);
        System.gc();
        System.runFinalization();
        Logger.getLogger("TEST").infof("Finished test %s", name.getMethodName());
    }

    private static final int IO_THREAD_COUNT = (int) (Runtime.getRuntime().availableProcessors() * 1.5);
    private static final int THREAD_POOL_SIZE = 100;
    private static final int BUFFER_SIZE = 8192;

    private static final int MAX_SERVER_RECEIVE = 0x18000;
    private static final int MAX_SERVER_TRANSMIT = 0x14000;

    @Test
    @Issue("issues/536")
    public void testChannelOptions() throws Exception {
        serverEndpoint.registerService("test", new OpenListener() {
            @Override
            public void channelOpened(Channel channel) {
                //
                Assert.assertTrue(channel.getOption(RemotingOptions.RECEIVE_WINDOW_SIZE) <= MAX_SERVER_RECEIVE);
                Assert.assertTrue(channel.getOption(RemotingOptions.TRANSMIT_WINDOW_SIZE) <= MAX_SERVER_TRANSMIT);
            }

            @Override
            public void registrationTerminated() {
                //
            }
        }, OptionMap.create(RemotingOptions.RECEIVE_WINDOW_SIZE, MAX_SERVER_RECEIVE, RemotingOptions.TRANSMIT_WINDOW_SIZE, MAX_SERVER_TRANSMIT));

        try (Spy<?> spy = Sniffy.spy(SpyConfiguration.builder().captureNetworkTraffic(true).captureStackTraces(true).build())) {

            final Connection connection = AuthenticationContext.empty().with(MatchRule.ALL, AuthenticationConfiguration.empty().useName("bob").usePassword("pass").setSaslMechanismSelector(SaslMechanismSelector.NONE.addMechanism("SCRAM-SHA-256"))).run(new PrivilegedAction<Connection>() {
                public Connection run() {
                    try {
                        return clientEndpoint.connect(new URI("remote://localhost:30123"), OptionMap.EMPTY).get();
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            IoFuture<Channel> future = connection.openChannel("test", OptionMap.create(RemotingOptions.RECEIVE_WINDOW_SIZE, 0x8000, RemotingOptions.TRANSMIT_WINDOW_SIZE, 0x12000));
            Channel channel = future.get();
            try {
                Assert.assertEquals("transmit", 0x12000, (int) channel.getOption(RemotingOptions.TRANSMIT_WINDOW_SIZE));
                Assert.assertEquals("receive", 0x8000, (int) channel.getOption(RemotingOptions.RECEIVE_WINDOW_SIZE));
            } finally {
                if (channel != null) {
                    channel.close();
                }
            }
            future = connection.openChannel("test", OptionMap.create(RemotingOptions.RECEIVE_WINDOW_SIZE, 0x24000, RemotingOptions.TRANSMIT_WINDOW_SIZE, 0x24000));
            channel = future.get();
            try {
                Assert.assertEquals("transmit", MAX_SERVER_RECEIVE, (int) channel.getOption(RemotingOptions.TRANSMIT_WINDOW_SIZE));
                Assert.assertEquals("receive", MAX_SERVER_TRANSMIT, (int) channel.getOption(RemotingOptions.RECEIVE_WINDOW_SIZE));
            } finally {
                if (channel != null) {
                    channel.close();
                }
            }

            try {
                Map<SocketMetaData, List<NetworkPacket>> networkTraffic = spy.getNetworkTraffic(Threads.ANY, AddressMatchers.anyAddressMatcher(), GroupingOptions.builder().build());

                assertNotNull(networkTraffic);
            } catch (Exception e) {
                System.err.flush();
                System.err.println("Caught interresting exception! <<<");
                e.printStackTrace();
                System.err.println("Caught interresting exception! >>>");
                System.err.flush();
                throw e;
            }

        }

    }

}
