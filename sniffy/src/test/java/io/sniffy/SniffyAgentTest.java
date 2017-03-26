package io.sniffy;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import ru.yandex.qatools.allure.annotations.Features;

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
    @Features("issues/327")
    public void testGetRegistry() {
        TestRestTemplate template = new TestRestTemplate();
        ResponseEntity<String> entity = template.getForEntity("http://localhost:5555/connectionregistry/", String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());
    }

    @Test
    @Features("issues/327")
    public void testGetHomePage() {
        TestRestTemplate template = new TestRestTemplate();
        ResponseEntity<String> entity = template.getForEntity("http://localhost:5555/", String.class);
        assertTrue(entity.getStatusCode().is2xxSuccessful());
    }

}
