package io.sniffy.servlet;

import io.sniffy.registry.ConnectionsRegistry;
import io.sniffy.registry.ConnectionsRegistryStorage;
import io.sniffy.socket.SocketMetaData;
import io.sniffy.socket.SocketStats;
import io.sniffy.sql.SqlStats;
import io.sniffy.sql.StatementMetaData;
import io.sniffy.util.StringUtil;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static io.sniffy.registry.ConnectionsRegistry.ConnectionStatus.CLOSED;
import static io.sniffy.registry.ConnectionsRegistry.ConnectionStatus.OPEN;
import static io.sniffy.servlet.SniffyFilter.SNIFFY_URI_PREFIX;

/**
 * @see SniffyFilter
 * @since 2.3.0
 */
class SniffyServlet extends HttpServlet {

    public static final String JAVASCRIPT_MIME_TYPE = "application/javascript";

    public static final String CONNECTION_REGISTRY_URI_PREFIX = SNIFFY_URI_PREFIX + "/connectionregistry/";
    public static final String SOCKET_REGISTRY_URI_PREFIX = SNIFFY_URI_PREFIX + "/connectionregistry/socket/";
    public static final String DATASOURCE_REGISTRY_URI_PREFIX = SNIFFY_URI_PREFIX + "/connectionregistry/datasource/";
    public static final String PERSISTENT_REGISTRY_URI_PREFIX = SNIFFY_URI_PREFIX + "/connectionregistry/persistent/";

    protected final Map<String, RequestStats> cache;

    protected byte[] javascript;
    protected byte[] javascriptSource;
    protected byte[] javascriptMap;

    public SniffyServlet(Map<String, RequestStats> cache) {
        this.cache = cache;
        try {
            javascript = loadResource("/META-INF/resources/webjars/sniffy/3.1.2-SNAPSHOT/dist/sniffy.min.js");
            javascriptSource = loadResource("/META-INF/resources/webjars/sniffy/3.1.2-SNAPSHOT/dist/sniffy.js");
            javascriptMap = loadResource("/META-INF/resources/webjars/sniffy/3.1.2-SNAPSHOT/dist/sniffy.map");
        } catch (IOException e) {
            // TODO: log me maybe?
        }
    }

    private void addCorsHeaders(HttpServletResponse response) {
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT");
        response.addHeader("Access-Control-Allow-Headers", "X-Requested-With,Content-Type");
        response.addHeader("Access-Control-Max-Age", "86400");
        response.addHeader("Access-Control-Allow-Credentials", "true");
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // TODO: allow prefix from configuration

        String requestURI = request.getRequestURI();
        int ix = requestURI.indexOf(SNIFFY_URI_PREFIX);

        if (ix < 0) return;

        String path = requestURI.substring(ix);

        if (SniffyFilter.JAVASCRIPT_URI.equals(path)) {
            addCorsHeaders(response);
            serveContent(response, JAVASCRIPT_MIME_TYPE, javascript);
        } else if (SniffyFilter.JAVASCRIPT_SOURCE_URI.equals(path)) {
            addCorsHeaders(response);
            serveContent(response, JAVASCRIPT_MIME_TYPE, javascriptSource);
        } else if (SniffyFilter.JAVASCRIPT_MAP_URI.equals(path)) {
            addCorsHeaders(response);
            serveContent(response, JAVASCRIPT_MIME_TYPE, javascriptMap);
        } else if (path.startsWith(SniffyFilter.REQUEST_URI_PREFIX)) {
            addCorsHeaders(response);
            byte[] requestStatsJson = getRequestStatsJson(path.substring(SniffyFilter.REQUEST_URI_PREFIX.length()));

            if (null == requestStatsJson) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType(JAVASCRIPT_MIME_TYPE);
                response.flushBuffer();
            } else {
                response.setContentType(JAVASCRIPT_MIME_TYPE);
                response.setContentLength(requestStatsJson.length);

                ServletOutputStream outputStream = response.getOutputStream();
                outputStream.write(requestStatsJson);
                outputStream.flush();
            }
        } else if (path.equals(CONNECTION_REGISTRY_URI_PREFIX)) {

            addCorsHeaders(response);

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(JAVASCRIPT_MIME_TYPE);

            ConnectionsRegistry.INSTANCE.writeTo(response.getWriter());

        } else if (path.startsWith(CONNECTION_REGISTRY_URI_PREFIX)) {

            addCorsHeaders(response);

            ConnectionsRegistry.ConnectionStatus status = null;
            if ("POST".equalsIgnoreCase(request.getMethod())) {
                status = OPEN;
            } else if ("DELETE".equalsIgnoreCase(request.getMethod())) {
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

            response.setStatus(HttpServletResponse.SC_CREATED);
            response.flushBuffer();

        }

    }

    private String[] splitBySlashAndDecode(String connectionString) throws UnsupportedEncodingException {
        String[] split = connectionString.split("/");
        for (int i = 0; i < split.length; i++) {
            split[i] = URLDecoder.decode(URLDecoder.decode(split[i], "UTF-8"));
        }
        return split;
    }

    // TODO: stream JSON instead; otherwise we are creating unnecessary garbage out of interned strings mostly
    private byte[] getRequestStatsJson(String requestId) {
        RequestStats requestStats = cache.get(requestId);
        if (null != requestStats) {
            StringBuilder sb = new StringBuilder();
            sb.
                    append("{").
                    append("\"timeToFirstByte\":").
                    append(requestStats.getTimeToFirstByte()).
                    append(",").
                    append("\"time\":").
                    append(requestStats.getElapsedTime());
            if (null != requestStats.getExecutedStatements()) {
                sb.append(",\"executedQueries\":[");
                Set<Map.Entry<StatementMetaData, SqlStats>> entries = requestStats.getExecutedStatements().entrySet();
                Iterator<Map.Entry<StatementMetaData, SqlStats>> statementsIt = entries.iterator();
                while (statementsIt.hasNext()) {
                    Map.Entry<StatementMetaData, SqlStats> entry = statementsIt.next();
                    StatementMetaData statement = entry.getKey();
                    SqlStats sqlStats = entry.getValue();
                    sb.
                            append("{").
                            append("\"query\":").
                            append(StringUtil.escapeJsonString(statement.sql)).
                            append(",").
                            append("\"stackTrace\":").
                            append(StringUtil.escapeJsonString(statement.stackTrace)).
                            append(",").
                            append("\"time\":").
                            append(sqlStats.elapsedTime.longValue()).
                            append(",").
                            append("\"invocations\":").
                            append(sqlStats.queries.longValue()).
                            append(",").
                            append("\"rows\":").
                            append(sqlStats.rows.longValue()).
                            append(",").
                            append("\"type\":\"").
                            append(statement.query.name()).
                            append("\",").
                            append("\"bytesDown\":").
                            append(sqlStats.bytesDown.longValue()).
                            append(",").
                            append("\"bytesUp\":").
                            append(sqlStats.bytesUp.longValue()).
                            append("}");
                    if (statementsIt.hasNext()) {
                        sb.append(",");
                    }
                }
                sb.append("]");
            }
            if (null != requestStats.getSocketOperations()) {
                sb.append(",\"networkConnections\":[");
                Iterator<Map.Entry<SocketMetaData, SocketStats>> statementsIt = requestStats.getSocketOperations().entrySet().iterator();
                while (statementsIt.hasNext()) {
                    Map.Entry<SocketMetaData, SocketStats> entry = statementsIt.next();
                    SocketMetaData socketMetaData = entry.getKey();
                    SocketStats socketStats = entry.getValue();

                    sb.
                            append("{").
                            append("\"host\":").
                            append(StringUtil.escapeJsonString(socketMetaData.address.toString())).
                            append(",").
                            append("\"stackTrace\":").
                            append(StringUtil.escapeJsonString(socketMetaData.stackTrace)).
                            append(",").
                            append("\"time\":").
                            append(socketStats.elapsedTime.longValue()).
                            append(",").
                            append("\"bytesDown\":").
                            append(socketStats.bytesDown.longValue()).
                            append(",").
                            append("\"bytesUp\":").
                            append(socketStats.bytesUp.longValue()).
                            append("}");
                    if (statementsIt.hasNext()) {
                        sb.append(",");
                    }
                }
                sb.append("]");
            }
            if (null != requestStats.getExceptions() && !requestStats.getExceptions().isEmpty()) {
                sb.append(",\"exceptions\":[");
                Iterator<Throwable> exceptionsIt = requestStats.getExceptions().iterator();
                while (exceptionsIt.hasNext()) {
                    Throwable exception = exceptionsIt.next();

                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    exception.printStackTrace(pw);

                    sb.
                            append("{").
                            append("\"class\":").
                            append(StringUtil.escapeJsonString(exception.getClass().getName())).
                            append(",\"message\":").
                            append(StringUtil.escapeJsonString(exception.getMessage())).
                            append(",\"stackTrace\":").
                            append(StringUtil.escapeJsonString(sw.toString())).
                            append("}");
                    if (exceptionsIt.hasNext()) {
                        sb.append(",");
                    }
                }
                sb.append("]");
            }
            sb.append("}");
            return sb.toString().getBytes();
        } else {
            return null;
        }
    }

    /**
     * @todo support gzip encoding
     * @param response
     * @param mimeType
     * @param content
     * @throws IOException
     */
    private void serveContent(HttpServletResponse response, String mimeType, byte[] content) throws IOException {
        response.setContentType(mimeType);
        response.setContentLength(content.length);
        cacheForever(response);

        ServletOutputStream outputStream = response.getOutputStream();
        outputStream.write(content);
        outputStream.flush();
    }

    private static void cacheForever(HttpServletResponse response) {
        response.setHeader("Cache-Control", "max-age=31536000, public");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 1);
        response.setDateHeader("Expires", calendar.getTimeInMillis());
    }

    private static byte[] loadResource(String resourceName) throws IOException {
        InputStream is = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            is = SniffyFilter.class.getResourceAsStream(resourceName);
            byte[] buff = new byte[1024];
            int count;
            while ((count = is.read(buff)) > 0) {
                baos.write(buff, 0, count);
            }
            return baos.toByteArray();
        } finally {
            if (null != is) {
                is.close();
            }
        }
    }

}
