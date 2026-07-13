package io.sniffy.test.tomcat;

import io.qameta.allure.Issue;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BaseTomcatIT {

    private static Tomcat tomcat;
    private static int port;

    @BeforeClass
    public static void startTomcat() throws Exception {
        tomcat = new Tomcat();
        tomcat.setPort(0);
        tomcat.getConnector();

        Context context = tomcat.addContext("/test", new File("src/main/webapp").getAbsolutePath());
        Tomcat.addServlet(context, "integration", new IntegrationTestServlet());
        context.addServletMappingDecoded("/*", "integration");

        FilterDef filterDef = new FilterDef();
        filterDef.setFilterName("sniffy");
        filterDef.setFilter(new SniffyAnnotationFilter());
        context.addFilterDef(filterDef);
        FilterMap filterMap = new FilterMap();
        filterMap.setFilterName("sniffy");
        filterMap.addURLPattern("/*");
        context.addFilterMap(filterMap);

        tomcat.start();
        port = tomcat.getConnector().getLocalPort();
    }

    @AfterClass
    public static void stopTomcat() throws Exception {
        if (null != tomcat) {
            try {
                tomcat.stop();
            } finally {
                tomcat.destroy();
            }
        }
    }

    @Test
    public void testSniffyInjected() throws IOException {
        assertInstrumented("/test");
    }

    @Test
    @Issue("issues/321")
    public void testSniffyInjectedPath() throws IOException {
        assertInstrumented("/test/index.html");
    }

    @Test
    @Issue("issues/319")
    public void testSniffyInjectedToUrlWithQueryParameters() throws IOException {
        assertInstrumented("/test?foo=bar");
    }

    private static void assertInstrumented(String path) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:" + port + path).openConnection();
        connection.setRequestProperty("Accept", "text/html");
        try {
            assertTrue(connection.getResponseCode() / 100 == 2);
            assertNotNull(connection.getHeaderField("Sniffy-Sql-Queries"));
            String body = read(connection.getInputStream());
            assertTrue(body.contains("script id=\"sniffy-header\""));
            assertTrue(body.contains("Hello, World!"));
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream inputStream) throws IOException {
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
}
