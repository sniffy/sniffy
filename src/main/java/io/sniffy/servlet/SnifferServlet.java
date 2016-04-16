package io.sniffy.servlet;

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
import java.util.*;

class SnifferServlet extends HttpServlet {

    protected final Map<String, RequestStats> cache;

    protected byte[] javascript;
    protected byte[] map;

    public SnifferServlet(Map<String, RequestStats> cache) {
        this.cache = cache;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            javascript = loadResource("/META-INF/resources/webjars/sniffy/3.0.7-SNAPSHOT/dist/sniffy.min.js");
            map = loadResource("/META-INF/resources/webjars/sniffy/3.0.7-SNAPSHOT/dist/sniffy.map");
        } catch (IOException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (SnifferFilter.JAVASCRIPT_URI.equals(path)) {
            serveContent(response, "application/javascript", javascript);
        } else if (SnifferFilter.JAVASCRIPT_MAP_URI.equals(path)) {
            serveContent(response, "application/javascript", map);
        } else if (path.startsWith(SnifferFilter.REQUEST_URI_PREFIX)) {
            byte[] requestStatsJson = getRequestStatsJson(path.substring(SnifferFilter.REQUEST_URI_PREFIX.length()));

            if (null == requestStatsJson) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                response.flushBuffer();
            } else {
                response.setContentType("application/json");
                response.setContentLength(requestStatsJson.length);

                ServletOutputStream outputStream = response.getOutputStream();
                outputStream.write(requestStatsJson);
                outputStream.flush();
            }
        }

    }

    private byte[] getRequestStatsJson(String requestId) {
        RequestStats requestStats = cache.get(requestId);
        if (null != requestStats) {
            StringBuilder sb = new StringBuilder();
            sb.append("{").append("\"time\":").append(requestStats.getElapsedTime());
            if (null != requestStats.getExecutedStatements()) {
                sb.append(",\"executedQueries\":[");
                Iterator<StatementMetaData> statementsIt = requestStats.getExecutedStatements().iterator();
                while (statementsIt.hasNext()) {
                    StatementMetaData statement = statementsIt.next();
                    sb.
                            append("{").
                            append("\"query\":").
                            append(StringUtil.escapeJsonString(statement.sql)).
                            append(",").
                            append("\"stackTrace\":").
                            append(StringUtil.escapeJsonString(statement.stackTrace)).
                            append(",").
                            append("\"time\":").
                            append(String.format(Locale.ENGLISH, "%.3f", (double) statement.elapsedTime / 1000 / 1000)).
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
