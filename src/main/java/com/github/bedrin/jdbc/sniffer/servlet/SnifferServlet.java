package com.github.bedrin.jdbc.sniffer.servlet;

import com.github.bedrin.jdbc.sniffer.sql.StatementMetaData;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SnifferServlet extends HttpServlet {

    protected final Map<String, List<StatementMetaData>> cache;

    protected byte[] javascript;
    protected byte[] css;

    public SnifferServlet(Map<String, List<StatementMetaData>> cache) {
        this.cache = cache;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            javascript = loadResource("jdbcsniffer.min.js");
        } catch (IOException e) {
            throw new ServletException(e);
        }
        try {
            css = loadResource("jdbcsniffer.css");
        } catch (IOException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (SnifferFilter.JAVASCRIPT_URI.equals(path)) {
            serveContent(response, "application/javascript", javascript);
        } else if (SnifferFilter.CSS_URI.equals(path)) {
            serveContent(response, "text/css", css);
        } else if (path.startsWith("/request/")) {
            byte[] statements = getStatementsJson(path.substring("/request/".length()));

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
                        append("\"query\":\"").append(statement.sql).append("\",").
                        append("\"time\":").append(
                        String.format(Locale.ENGLISH, "%.3f", (double) statement.elapsedTime / 1000 / 1000)).
                        append("}");
            }
            sb.append("]");
            return sb.toString().getBytes();
        }
    }

    private void serveContent(HttpServletResponse response, String mimeType, byte[] content) throws IOException {
        response.setContentType(mimeType);
        response.setContentLength(content.length);
        cacheForever(response);

        ServletOutputStream outputStream = response.getOutputStream();
        outputStream.write(content);
        outputStream.flush();
    }

    private void cacheForever(HttpServletResponse response) {
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
