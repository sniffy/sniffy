package io.sniffy.test.boot;

import io.sniffy.sql.SniffyDataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

import static io.sniffy.servlet.SniffyFilter.SNIFFY_RESOURCE_URI_PREFIX;
import static io.sniffy.servlet.SniffyFilter.SNIFFY_URI_PREFIX;
import static io.sniffy.servlet.SniffyFilter.JAVASCRIPT_URI;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RestControllerTest {

    @LocalServerPort
    private int localServerPort;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private javax.sql.DataSource dataSource;

    @Test
    public void dataSourceIsInstrumented() {
        assertTrue(dataSource instanceof SniffyDataSource);
    }

    @Test
    public void exampleTest() throws IOException {
        ResponseEntity<String> entity = restTemplate.getForEntity("http://localhost:" + localServerPort + "/restservice", String.class);
        assertNotNull(entity);
        assertNotNull(entity.getHeaders().getFirst("Sniffy-Sql-Queries"));
    }

    @Test
    public void sharedProfilerResourceIsAvailable() {
        ResponseEntity<String> entity = restTemplate.getForEntity(
                "http://localhost:" + localServerPort + "/" + JAVASCRIPT_URI, String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());
        assertTrue(entity.getBody().contains("sniffy-profiler"));
        assertTrue(entity.getBody().contains("sourceMappingURL=sniffy.map"));
    }

    public static class Connectivity {

        private boolean persistent;

        private List<SocketConnectivity> sockets;

        private List<DatabaseConnectivity> dataSources;

        public boolean isPersistent() {
            return persistent;
        }

        public void setPersistent(boolean persistent) {
            this.persistent = persistent;
        }

        public List<SocketConnectivity> getSockets() {
            return sockets;
        }

        public void setSockets(List<SocketConnectivity> sockets) {
            this.sockets = sockets;
        }

        public List<DatabaseConnectivity> getDataSources() {
            return dataSources;
        }

        public void setDataSources(List<DatabaseConnectivity> dataSources) {
            this.dataSources = dataSources;
        }
    }

    public static class SocketConnectivity {

        private String host;

        private String port;

        private Integer status;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    public static class DatabaseConnectivity {

        private String url;

        private String userName;

        private Integer status;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    @Test
    public void testDisableConnectivity() {

        {
            ResponseEntity<Connectivity> entity = restTemplate.getForEntity("http://localhost:" + localServerPort + "/" + SNIFFY_URI_PREFIX + "/connectionregistry/", Connectivity.class);

            DatabaseConnectivity databaseConnectivity = entity.getBody().getDataSources().get(0);

            assertEquals("jdbc:h2:mem:6ee1f026/8606/490f/b358/1ea2f87cb877", databaseConnectivity.getUrl());
            assertEquals("SA", databaseConnectivity.getUserName());
            assertEquals(0, databaseConnectivity.getStatus().intValue());
        }

        restTemplate.postForEntity("http://localhost:" + localServerPort + "/" + SNIFFY_URI_PREFIX + "/connectionregistry/datasource/jdbc:h2:mem:6ee1f026/8606/490f/b358/1ea2f87cb877/SA", -1, Object.class);

        {
            ResponseEntity<Connectivity> entity = restTemplate.getForEntity("http://localhost:" + localServerPort + "/" + SNIFFY_URI_PREFIX + "/connectionregistry/", Connectivity.class);

            DatabaseConnectivity databaseConnectivity = entity.getBody().getDataSources().get(0);

            assertEquals("jdbc:h2:mem:6ee1f026/8606/490f/b358/1ea2f87cb877", databaseConnectivity.getUrl());
            assertEquals("SA", databaseConnectivity.getUserName());
            assertEquals(-1, databaseConnectivity.getStatus().intValue());
        }

        restTemplate.postForEntity("http://localhost:" + localServerPort + "/" + SNIFFY_URI_PREFIX + "/connectionregistry/datasource/jdbc:h2:mem:6ee1f026/8606/490f/b358/1ea2f87cb877/SA", 0, Object.class);

    }

    @Test
    public void testSniffyAPIAvailableWithAndWithoutVersion() {

        {
            ResponseEntity<Connectivity> entity = restTemplate.getForEntity("http://localhost:" + localServerPort + "/" + SNIFFY_URI_PREFIX + "/connectionregistry/", Connectivity.class);

            DatabaseConnectivity databaseConnectivity = entity.getBody().getDataSources().get(0);

            assertEquals("jdbc:h2:mem:6ee1f026/8606/490f/b358/1ea2f87cb877", databaseConnectivity.getUrl());
            assertEquals("SA", databaseConnectivity.getUserName());
            assertEquals(0, databaseConnectivity.getStatus().intValue());
        }

        {
            ResponseEntity<Connectivity> entity = restTemplate.getForEntity("http://localhost:" + localServerPort + "/" + SNIFFY_RESOURCE_URI_PREFIX + "/connectionregistry/", Connectivity.class);

            DatabaseConnectivity databaseConnectivity = entity.getBody().getDataSources().get(0);

            assertEquals("jdbc:h2:mem:6ee1f026/8606/490f/b358/1ea2f87cb877", databaseConnectivity.getUrl());
            assertEquals("SA", databaseConnectivity.getUserName());
            assertEquals(0, databaseConnectivity.getStatus().intValue());
        }

    }

    @Test
    public void testDisableConnectivityUrlEncoding() throws IOException {

        String databaseUrl;

        {
            ResponseEntity<Connectivity> entity = restTemplate.getForEntity("http://localhost:" + localServerPort + "/" + SNIFFY_URI_PREFIX + "/connectionregistry/", Connectivity.class);

            DatabaseConnectivity databaseConnectivity = entity.getBody().getDataSources().get(0);

            databaseUrl = databaseConnectivity.getUrl();

            assertEquals("jdbc:h2:mem:6ee1f026/8606/490f/b358/1ea2f87cb877", databaseConnectivity.getUrl());
            assertEquals("SA", databaseConnectivity.getUserName());
            assertEquals(0, databaseConnectivity.getStatus().intValue());
        }

        int status = postForStatus("http://localhost:" + localServerPort + "/" + SNIFFY_URI_PREFIX +
                "/connectionregistry/datasource/" + URLEncoder.encode(databaseUrl, "UTF-8") + "/SA", "-1");

        assertEquals(400, status);

    }

    @Test
    public void testDisableConnectivityDoubleUrlEncoding() throws IOException {

        String databaseUrl;

        {
            ResponseEntity<Connectivity> entity = restTemplate.getForEntity("http://localhost:" + localServerPort + "/" + SNIFFY_URI_PREFIX + "/connectionregistry/", Connectivity.class);

            DatabaseConnectivity databaseConnectivity = entity.getBody().getDataSources().get(0);

            databaseUrl = databaseConnectivity.getUrl();

            assertEquals("jdbc:h2:mem:6ee1f026/8606/490f/b358/1ea2f87cb877", databaseConnectivity.getUrl());
            assertEquals("SA", databaseConnectivity.getUserName());
            assertEquals(0, databaseConnectivity.getStatus().intValue());
        }

        int status = postForStatus("http://localhost:" + localServerPort + "/" + SNIFFY_URI_PREFIX +
                "/connectionregistry/datasource/" + URLEncoder.encode(URLEncoder.encode(databaseUrl, "UTF-8"), "UTF-8") +
                "/SA", "-1");

        assertTrue(status / 100 == 2);


        {
            ResponseEntity<Connectivity> entity = restTemplate.getForEntity("http://localhost:" + localServerPort + "/" + SNIFFY_URI_PREFIX + "/connectionregistry/", Connectivity.class);

            DatabaseConnectivity databaseConnectivity = entity.getBody().getDataSources().get(0);

            assertEquals("jdbc:h2:mem:6ee1f026/8606/490f/b358/1ea2f87cb877", databaseConnectivity.getUrl());
            assertEquals("SA", databaseConnectivity.getUserName());
            assertEquals(-1, databaseConnectivity.getStatus().intValue());
        }

        restTemplate.postForEntity("http://localhost:" + localServerPort + "/" + SNIFFY_URI_PREFIX + "/connectionregistry/datasource/jdbc:h2:mem:6ee1f026/8606/490f/b358/1ea2f87cb877/SA", 0, Object.class);

    }

    @Test
    public void ouchTest() throws IOException {

        try {
            restTemplate.execute(
                    "http://localhost:" + localServerPort + "/ouch",
                    HttpMethod.GET,
                    request -> request.getHeaders().setAccept(Collections.singletonList(MediaType.TEXT_HTML)),
                    v -> v
            );
            fail("Should have throw exception");
        } catch (HttpServerErrorException e) {
            assertNotNull(e);
            List<String> sniffySqlQueriesHeaders = e.getResponseHeaders().get("Sniffy-Sql-Queries");
            assertEquals(1, sniffySqlQueriesHeaders.size());

            String responseString = e.getResponseBodyAsString();
            assertNotNull(responseString);

            assertTrue(responseString.contains("script id=\"sniffy-header\""));
            assertEquals(responseString.indexOf("script id=\"sniffy-header\""), responseString.lastIndexOf("script id=\"sniffy-header\""));
        }
        /*assertNotNull(response);
         */
    }

    private static int postForStatus(String url, String body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        byte[] bytes = body.getBytes("UTF-8");
        connection.setFixedLengthStreamingMode(bytes.length);
        OutputStream outputStream = connection.getOutputStream();
        try {
            outputStream.write(bytes);
        } finally {
            outputStream.close();
        }
        try {
            return connection.getResponseCode();
        } finally {
            connection.disconnect();
        }
    }

}
