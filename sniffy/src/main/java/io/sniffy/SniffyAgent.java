package io.sniffy;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.sniffy.configuration.SniffyConfiguration;
import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.registry.ConnectionsRegistryStorage;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;

import static io.sniffy.registry.ConnectionsRegistry.ConnectionStatus.CLOSED;
import static io.sniffy.registry.ConnectionsRegistry.ConnectionStatus.OPEN;
import static io.sniffy.util.StringUtil.splitBySlashAndDecode;

public class SniffyAgent {

    public static final String CONNECTION_REGISTRY_URI_PREFIX = "/connectionregistry/";
    public static final String SOCKET_REGISTRY_URI_PREFIX = "/connectionregistry/socket/";
    public static final String DATASOURCE_REGISTRY_URI_PREFIX = "/connectionregistry/datasource/";
    public static final String PERSISTENT_REGISTRY_URI_PREFIX = "/connectionregistry/persistent/";

    private static HttpServer server;

    public static void main(String[] args) throws IOException {
        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        Sniffy.initialize();
        startServer(5555);
    }

    public static void premain(String args, Instrumentation instrumentation) throws Exception {
        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        Sniffy.initialize();
        startServer(Integer.parseInt(args));
    }

    protected static void startServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    protected static void stopServer() {
        server.stop(1);
    }

    static class MyHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {

            String path = httpExchange.getRequestURI().toString();

            if (path.equals(CONNECTION_REGISTRY_URI_PREFIX)) {

                addCorsHeaders(httpExchange);

                httpExchange.getResponseHeaders().add("Content-Type", "application/javascript");
                httpExchange.sendResponseHeaders(200, 0);

                ConnectionsRegistry.INSTANCE.writeTo(httpExchange.getResponseBody(), "UTF-8");

                httpExchange.getResponseBody().close();

            } else if (path.startsWith(CONNECTION_REGISTRY_URI_PREFIX)) {

                addCorsHeaders(httpExchange);

                ConnectionsRegistry.ConnectionStatus status = null;
                if ("POST".equalsIgnoreCase(httpExchange.getRequestMethod())) {
                    status = OPEN;
                } else if ("DELETE".equalsIgnoreCase(httpExchange.getRequestMethod())) {
                    status = CLOSED;
                }

                if (path.startsWith(SOCKET_REGISTRY_URI_PREFIX)) {
                    String connectionString = path.substring(SOCKET_REGISTRY_URI_PREFIX.length());
                    String[] split = splitBySlashAndDecode(connectionString);
                    ConnectionsRegistry.INSTANCE.setSocketAddressStatus(split[0], Integer.parseInt(split[1]), status);
                } else if (path.startsWith(DATASOURCE_REGISTRY_URI_PREFIX)) {
                    String connectionString = path.substring(DATASOURCE_REGISTRY_URI_PREFIX.length());
                    String[] split = splitBySlashAndDecode(connectionString);
                    ConnectionsRegistry.INSTANCE.setDataSourceStatus(split[0], split[1], status);
                } else if (path.startsWith(PERSISTENT_REGISTRY_URI_PREFIX)) {

                    if (OPEN == status) {
                        ConnectionsRegistry.INSTANCE.setPersistRegistry(true);
                        ConnectionsRegistryStorage.INSTANCE.storeConnectionsRegistry(ConnectionsRegistry.INSTANCE);
                    } else {
                        ConnectionsRegistry.INSTANCE.setPersistRegistry(false);
                    }

                }

                httpExchange.sendResponseHeaders(201, 0);
                httpExchange.getResponseBody().close();

            } else {

                String resourceName =
                        path.startsWith("/webjars/") ? "/META-INF/resources" + path :
                        "/web" + ("/".equals(path) ? "/index.html" : path);
                InputStream inputStream = SniffyAgent.class.getResourceAsStream(resourceName);

                if (null != inputStream) {

                    httpExchange.getResponseHeaders().add("Content-Type", getMimeType(resourceName));
                    httpExchange.sendResponseHeaders(200, 0);

                    byte[] buff = new byte[1024];
                    int read;
                    while (-1 != (read = inputStream.read(buff))) {
                        httpExchange.getResponseBody().write(buff, 0, read);
                    }

                    httpExchange.getResponseBody().close();
                } else {
                    httpExchange.sendResponseHeaders(404, 0);
                    httpExchange.getResponseBody().close();
                }

            }
        }

        private String getMimeType(String resourceName) {

            if (resourceName.endsWith(".html")) {
                return "text/html";
            } else if (resourceName.endsWith(".ico")) {
                return "image/x-icon";
            } else if (resourceName.endsWith(".png")) {
                return "image/png";
            } else if (resourceName.endsWith(".js")) {
                return "application/javascript";
            } else if (resourceName.endsWith(".css")) {
                return "text/css";
            } else {
                return "application/octet-stream";
            }

        }

        private void addCorsHeaders(HttpExchange httpExchange) {
            Headers headers = httpExchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
            headers.add("Access-Control-Allow-Headers", "X-Requested-With,Content-Type");
            headers.add("Access-Control-Max-Age", "86400");
            headers.add("Access-Control-Allow-Credentials", "true");
        }

    }

}
