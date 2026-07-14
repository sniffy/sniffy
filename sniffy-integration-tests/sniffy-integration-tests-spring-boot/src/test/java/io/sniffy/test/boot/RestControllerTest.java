package io.sniffy.test.boot;

import io.sniffy.sql.SniffyDataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static io.sniffy.servlet.SniffyFilter.JAVASCRIPT_URI;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RestControllerTest {

    @LocalServerPort
    private int localServerPort;

    @Autowired
    private DataSource dataSource;

    @Test
    public void servletFilterAddsHeadersAndInjectsHtml() throws IOException {
        Response response = get("/index.html", "text/html");
        assertEquals(200, response.status);
        assertNotNull(response.sqlQueriesHeader);
        assertTrue(response.body.contains("script id=\"sniffy-header\""));
    }

    @Test
    public void sharedProfilerResourceIsAvailable() throws IOException {
        Response response = get("/" + JAVASCRIPT_URI, "application/javascript");
        assertEquals(200, response.status);
        assertTrue(response.body.contains("sniffy-profiler"));
        assertTrue(response.body.contains("sourceMappingURL=sniffy.map"));
    }

    @Test
    public void errorResponseIsInstrumentedOnce() throws IOException {
        Response response = get("/ouch", "text/html");
        assertEquals(500, response.status);
        assertNotNull(response.sqlQueriesHeader);
        int first = response.body.indexOf("script id=\"sniffy-header\"");
        assertTrue(first >= 0);
        assertEquals(first, response.body.lastIndexOf("script id=\"sniffy-header\""));
    }

    @Test
    public void dataSourceIsInstrumented() {
        assertTrue(dataSource instanceof SniffyDataSource);
    }

    private Response get(String path, String accept) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(
                "http://127.0.0.1:" + localServerPort + path).openConnection();
        connection.setRequestProperty("Accept", accept);
        try {
            int status = connection.getResponseCode();
            InputStream inputStream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            return new Response(status, connection.getHeaderField("Sniffy-Sql-Queries"), read(inputStream));
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream inputStream) throws IOException {
        if (null == inputStream) {
            return "";
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, count);
            }
            return new String(outputStream.toByteArray(), "UTF-8");
        } finally {
            inputStream.close();
        }
    }

    private static class Response {
        private final int status;
        private final String sqlQueriesHeader;
        private final String body;

        private Response(int status, String sqlQueriesHeader, String body) {
            this.status = status;
            this.sqlQueriesHeader = sqlQueriesHeader;
            this.body = body;
        }
    }
}
