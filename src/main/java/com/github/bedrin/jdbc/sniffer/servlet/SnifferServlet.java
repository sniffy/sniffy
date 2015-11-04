package com.github.bedrin.jdbc.sniffer.servlet;

import com.github.bedrin.jdbc.sniffer.sql.StatementMetaData;
import com.github.bedrin.jdbc.sniffer.util.StringUtil;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class SnifferServlet extends HttpServlet {

    protected final Map<String, List<StatementMetaData>> cache;

    private final static ConcurrentHashMap<String, byte[]> resources = new ConcurrentHashMap<String, byte[]>();

    public SnifferServlet(Map<String, List<StatementMetaData>> cache) {
        this.cache = cache;
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (path.startsWith(SnifferFilter.REQUEST_URI_PREFIX)) {
            byte[] statements = getStatementsJson(path.substring(SnifferFilter.REQUEST_URI_PREFIX.length()));

            if (null == statements) {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                response.flushBuffer();
            } else {
                response.setContentType("application/json");
                response.setContentLength(statements.length);

                ServletOutputStream outputStream = response.getOutputStream();
                outputStream.write(statements);
                outputStream.flush();
            }
        } else if (path.startsWith(SnifferFilter.SNIFFER_URI_PREFIX)) {
            serveContent(response, path.substring(SnifferFilter.SNIFFER_URI_PREFIX.length() + 1));
        }

    }

    private byte[] getStatementsJson(String requestId) {
        List<StatementMetaData> statements = cache.get(requestId);
        if (null == statements) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (StatementMetaData statement : statements) {
                if (sb.length() > 1) {
                    sb.append(",");
                }
                sb.
                        append("{").
                        append("\"query\":").
                        append(StringUtil.escapeJsonString(statement.sql)).
                        append(",").
                        append("\"time\":").
                        append(String.format(Locale.ENGLISH, "%.3f", (double) statement.elapsedTime / 1000 / 1000)).
                        append("}");
            }
            sb.append("]");
            return sb.toString().getBytes();
        }
    }

    private static void serveContent(HttpServletResponse response, String path) throws IOException {

        byte[] content = resources.get(path);
        if (null == content) {
            synchronized (resources) {
                content = resources.get(path);
                if (null == content) {
                    // TODO think of security here
                    content = loadResource(path);
                    if (null == content) {
                        content = new byte[0];
                    }
                    resources.put(path, content);
                }
            }
        }

        if (0 == content.length) {
            response.sendError(404);
        } else {
            response.setContentType(guessContentTypeFromName(path));
            response.setContentLength(content.length);
            cacheForever(response);

            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(content);
            outputStream.flush();
        }

    }

    private static String guessContentTypeFromName(String path) {
        String contentType = URLConnection.guessContentTypeFromName(path);
        if (null == contentType) {
            if (path.endsWith(".js")) {
                return "application/javascript";
            } else if (path.endsWith(".css")) {
                return "text/css";
            }
        }
        return contentType;
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
