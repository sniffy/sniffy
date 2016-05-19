package io.sniffy.servlet;

import io.sniffy.socket.SocketMetaData;
import io.sniffy.socket.SocketStats;
import io.sniffy.socket.SocketsRegistry;
import io.sniffy.sql.SqlStats;
import io.sniffy.sql.StatementMetaData;
import io.sniffy.util.StringUtil;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.*;

import static io.sniffy.servlet.SnifferFilter.SNIFFER_URI_PREFIX;
import static io.sniffy.socket.SocketsRegistry.SocketAddressStatus.CLOSED;
import static io.sniffy.socket.SocketsRegistry.SocketAddressStatus.OPEN;

class SnifferServlet extends HttpServlet {

    public static final String JAVASCRIPT_MIME_TYPE = "application/javascript";

    public static final String SOCKET_REGISTRY_URI_PREFIX = SNIFFER_URI_PREFIX + "/socketregistry/";

    protected final Map<String, RequestStats> cache;

    protected byte[] javascript;
    protected byte[] map;

    public SnifferServlet(Map<String, RequestStats> cache) {
        this.cache = cache;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            javascript = loadResource("/META-INF/resources/webjars/sniffy/3.1.0-RC3-SNAPSHOT/dist/sniffy.min.js");
            map = loadResource("/META-INF/resources/webjars/sniffy/3.1.0-RC3-SNAPSHOT/dist/sniffy.map");
        } catch (IOException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (SnifferFilter.JAVASCRIPT_URI.equals(path)) {
            serveContent(response, JAVASCRIPT_MIME_TYPE, javascript);
        } else if (SnifferFilter.JAVASCRIPT_MAP_URI.equals(path)) {
            serveContent(response, JAVASCRIPT_MIME_TYPE, map);
        } else if (path.startsWith(SnifferFilter.REQUEST_URI_PREFIX)) {
            byte[] requestStatsJson = getRequestStatsJson(path.substring(SnifferFilter.REQUEST_URI_PREFIX.length()));

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
        } else if (path.equals(SOCKET_REGISTRY_URI_PREFIX)) {

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(JAVASCRIPT_MIME_TYPE);

            Map<Map.Entry<String, Integer>, SocketsRegistry.SocketAddressStatus> discoveredAdresses =
                    SocketsRegistry.INSTANCE.getDiscoveredAdresses();

            if (discoveredAdresses.isEmpty()) {
                response.flushBuffer();
            } else {

                Iterator<Map.Entry<Map.Entry<String, Integer>, SocketsRegistry.SocketAddressStatus>> iterator =
                        discoveredAdresses.entrySet().iterator();

                PrintWriter writer = response.getWriter();

                writer.write('[');

                while (iterator.hasNext()) {
                    Map.Entry<Map.Entry<String,Integer>, SocketsRegistry.SocketAddressStatus> entry = iterator.next();

                    String hostName = entry.getKey().getKey();
                    Integer port = entry.getKey().getValue();

                    writer.write('{');
                    if (null != hostName) {
                        writer.write("\"host\":\"");
                        writer.write(hostName);
                        writer.write("\"");
                    }
                    if (null != port) {
                        if (null != hostName) writer.write(',');
                        writer.write("\"port\":\"");
                        writer.write(port.toString());
                        writer.write("\"");
                    }
                    writer.write(',');
                    writer.write("\"status\":\"");
                    writer.write(entry.getValue().name());
                    writer.write("\"");
                    writer.write('}');
                    if (iterator.hasNext()) writer.write(',');

                }

                writer.write(']');

                writer.flush();

            }

        } else if (path.startsWith(SOCKET_REGISTRY_URI_PREFIX)) {
            SocketsRegistry.SocketAddressStatus status = null;
            if ("POST".equalsIgnoreCase(request.getMethod())) {
                status = OPEN;
            } else if ("DELETE".equalsIgnoreCase(request.getMethod())) {
                status = CLOSED;
            }
            if (null != status) {
                String socketAddress = path.substring(SOCKET_REGISTRY_URI_PREFIX.length());
                SocketsRegistry.INSTANCE.setSocketAddressStatus(socketAddress, status);
                response.setStatus(HttpServletResponse.SC_CREATED);
                response.flushBuffer();
            }
        }

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
                            append(String.format(Locale.ENGLISH, "%.3f", sqlStats.elapsedTime.doubleValue() / 1000)).
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
                            append(String.format(Locale.ENGLISH, "%.3f", (double) socketStats.elapsedTime.longValue())).
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
            is = SnifferFilter.class.getResourceAsStream(resourceName);
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
