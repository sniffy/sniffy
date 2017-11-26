package io.sniffy;

import com.jayway.jsonpath.JsonPath;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import ru.yandex.qatools.allure.annotations.Features;

import java.io.IOException;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SniffyAgentTest {

    @BeforeClass
    public static void startAgentServer() throws Exception {
        SniffyAgent.premain("5555", null);
    }

    @AfterClass
    public static void stopAgentServer() throws IOException {
        SniffyAgent.stopServer();
    }

    @Test
    @Features("issues/327")
    public void testGetRegistry() {
        TestRestTemplate template = new TestRestTemplate();
        ResponseEntity<String> entity = template.getForEntity("http://localhost:5555/connectionregistry/", String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());
    }

    @Test
    @Features("issues/327")
    public void testAllowConnection() {
        TestRestTemplate template = new TestRestTemplate();

        ResponseEntity<String> entity = template.postForEntity("http://localhost:5555/connectionregistry/socket/google.com/443", null, String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());

        entity = template.getForEntity("http://localhost:5555/connectionregistry/", String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());
        assertEquals(singletonList("443"), JsonPath.read(entity.getBody(), "$.sockets[?(@.host == 'google.com')].port"));
        assertEquals(singletonList(0), JsonPath.read(entity.getBody(), "$.sockets[?(@.host == 'google.com')].status"));

        template.delete("http://localhost:5555/connectionregistry/socket/google.com/443");

        entity = template.getForEntity("http://localhost:5555/connectionregistry/", String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());
        assertEquals(singletonList("443"), JsonPath.read(entity.getBody(), "$.sockets[?(@.host == 'google.com')].port"));
        assertEquals(singletonList(-1), JsonPath.read(entity.getBody(), "$.sockets[?(@.host == 'google.com')].status"));

    }

    @Test
    @Features({"issues/327","issues/219"})
    public void testSetConnectionDelay() {
        TestRestTemplate template = new TestRestTemplate();

        ResponseEntity<String> entity = template.postForEntity("http://localhost:5555/connectionregistry/socket/google.com/443", "10", String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());

        entity = template.getForEntity("http://localhost:5555/connectionregistry/", String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());
        assertEquals(singletonList("443"), JsonPath.read(entity.getBody(), "$.sockets[?(@.host == 'google.com')].port"));
        assertEquals(singletonList(10), JsonPath.read(entity.getBody(), "$.sockets[?(@.host == 'google.com')].status"));

        template.delete("http://localhost:5555/connectionregistry/socket/google.com/443");

        entity = template.getForEntity("http://localhost:5555/connectionregistry/", String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());
        assertEquals(singletonList("443"), JsonPath.read(entity.getBody(), "$.sockets[?(@.host == 'google.com')].port"));
        assertEquals(singletonList(-1), JsonPath.read(entity.getBody(), "$.sockets[?(@.host == 'google.com')].status"));
    }

    @Test
    @Features("issues/327")
    public void testAllowDataSource() {
        TestRestTemplate template = new TestRestTemplate();

        ResponseEntity<String> entity = template.postForEntity("http://localhost:5555/connectionregistry/datasource/jdbc:data:source/user", null, String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());

        entity = template.getForEntity("http://localhost:5555/connectionregistry/", String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());
        assertEquals("jdbc:data:source", JsonPath.read(entity.getBody(), "$.dataSources[0].url"));
        assertEquals("user", JsonPath.read(entity.getBody(), "$.dataSources[0].userName"));
        assertEquals((Integer) 0, JsonPath.read(entity.getBody(), "$.dataSources[0].status"));

        template.delete("http://localhost:5555/connectionregistry/datasource/jdbc:data:source/user");

        entity = template.getForEntity("http://localhost:5555/connectionregistry/", String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());
        assertEquals("jdbc:data:source", JsonPath.read(entity.getBody(), "$.dataSources[0].url"));
        assertEquals("user", JsonPath.read(entity.getBody(), "$.dataSources[0].userName"));
        assertEquals((Integer) (-1), JsonPath.read(entity.getBody(), "$.dataSources[0].status"));
    }

    @Test
    @Features("issues/327")
    public void testPersistent() {
        TestRestTemplate template = new TestRestTemplate();

        ResponseEntity<String> entity = template.postForEntity("http://localhost:5555/connectionregistry/persistent/", null, String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());

        entity = template.getForEntity("http://localhost:5555/connectionregistry/", String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());
        assertEquals(true, JsonPath.read(entity.getBody(), "$.persistent"));

        template.delete("http://localhost:5555/connectionregistry/persistent/");

        entity = template.getForEntity("http://localhost:5555/connectionregistry/", String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());
        assertEquals(false, JsonPath.read(entity.getBody(), "$.persistent"));
    }

    @Test
    @Features("issues/327")
    public void testGetHomePage() {
        TestRestTemplate template = new TestRestTemplate();
        ResponseEntity<String> entity = template.getForEntity("http://localhost:5555/", String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());
    }

    @Test
    @Features("issues/327")
    public void testGetFavicon() {
        TestRestTemplate template = new TestRestTemplate();
        ResponseEntity<String> entity = template.getForEntity("http://localhost:5555/favicon.ico", String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());
        assertEquals(MediaType.parseMediaType("image/x-icon"), entity.getHeaders().getContentType());
    }

    @Test
    @Features("issues/327")
    public void testGetIcon() {
        TestRestTemplate template = new TestRestTemplate();
        ResponseEntity<String> entity = template.getForEntity("http://localhost:5555/icon32.png", String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());
        assertEquals(MediaType.IMAGE_PNG, entity.getHeaders().getContentType());
    }

    @Test
    @Features("issues/327")
    public void testGetMissingResource() {
        TestRestTemplate template = new TestRestTemplate();
        ResponseEntity<String> entity = template.getForEntity("http://localhost:5555/missing/resource", String.class);
        assertTrue(entity.getStatusCode().is4xxClientError());
    }

    @Test
    @Features("issues/334")
    public void testCorsHeaders() {
        TestRestTemplate template = new TestRestTemplate();
        ResponseEntity<String> entity = template.getForEntity("http://localhost:5555/connectionregistry/", String.class);
        HttpHeaders response = entity.getHeaders();

        assertEquals("*", response.get("Access-Control-Allow-Origin").get(0));

        assertTrue(response.get("Access-Control-Allow-Methods").get(0).contains("GET"));
        assertTrue(response.get("Access-Control-Allow-Methods").get(0).contains("POST"));
        assertTrue(response.get("Access-Control-Allow-Methods").get(0).contains("PUT"));
        assertTrue(response.get("Access-Control-Allow-Methods").get(0).contains("DELETE"));

        assertTrue(response.get("Access-Control-Allow-Headers").get(0).contains("Sniffy-Inject-Html-Enabled"));
        assertTrue(response.get("Access-Control-Allow-Headers").get(0).contains("X-Requested-With"));
        assertTrue(response.get("Access-Control-Allow-Headers").get(0).contains("Content-Type"));

        assertEquals("true", response.get("Access-Control-Allow-Credentials").get(0));
    }

}
