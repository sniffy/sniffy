package io.sniffy;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

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
    public void testGetRegistry() {
        TestRestTemplate template = new TestRestTemplate();
        ResponseEntity<String> entity = template.getForEntity("http://localhost:5555/connectionregistry/", String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());
    }

    @Test
    public void testGetHomePage() {
        TestRestTemplate template = new TestRestTemplate();
        ResponseEntity<String> entity = template.getForEntity("http://localhost:5555/", String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());
    }

}
