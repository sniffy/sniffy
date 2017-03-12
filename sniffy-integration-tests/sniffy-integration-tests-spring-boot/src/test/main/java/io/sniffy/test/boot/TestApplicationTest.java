package io.sniffy.test.boot;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestApplicationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void exampleTest() throws IOException {
        ResponseEntity<String> entity = restTemplate.getForEntity("/", String.class);
        assertNotNull(entity);
        assertNotNull(entity.getHeaders().getFirst("Sniffy-Sql-Queries"));
    }

}
