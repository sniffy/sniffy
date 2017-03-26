package io.sniffy;

import com.jayway.jsonpath.JsonPath;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import ru.yandex.qatools.allure.annotations.Features;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SniffyAgentTest {

    @BeforeClass
    public static void startAgentServer() throws IOException {
        SniffyAgent.startServer(5555);
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
        assertEquals("google.com", JsonPath.read(entity.getBody(), "$.sockets[0].host"));
        assertEquals("443", JsonPath.read(entity.getBody(), "$.sockets[0].port"));
        assertEquals("OPEN", JsonPath.read(entity.getBody(), "$.sockets[0].status"));

        template.delete("http://localhost:5555/connectionregistry/socket/google.com/443");

        entity = template.getForEntity("http://localhost:5555/connectionregistry/", String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());
        assertEquals("google.com", JsonPath.read(entity.getBody(), "$.sockets[0].host"));
        assertEquals("443", JsonPath.read(entity.getBody(), "$.sockets[0].port"));
        assertEquals("CLOSED", JsonPath.read(entity.getBody(), "$.sockets[0].status"));
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
        assertEquals("OPEN", JsonPath.read(entity.getBody(), "$.dataSources[0].status"));

        template.delete("http://localhost:5555/connectionregistry/datasource/jdbc:data:source/user");

        entity = template.getForEntity("http://localhost:5555/connectionregistry/", String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());
        assertEquals("jdbc:data:source", JsonPath.read(entity.getBody(), "$.dataSources[0].url"));
        assertEquals("user", JsonPath.read(entity.getBody(), "$.dataSources[0].userName"));
        assertEquals("CLOSED", JsonPath.read(entity.getBody(), "$.dataSources[0].status"));
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

}
