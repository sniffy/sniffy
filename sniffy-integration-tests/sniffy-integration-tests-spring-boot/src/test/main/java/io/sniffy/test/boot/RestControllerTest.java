package io.sniffy.test.boot;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RestControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void exampleTest() throws IOException {
        ResponseEntity<String> entity = restTemplate.getForEntity("/", String.class);
        assertNotNull(entity);
        assertNotNull(entity.getHeaders().getFirst("Sniffy-Sql-Queries"));
    }

    @Test
    public void ouchTest() throws IOException {
        ClientHttpResponse response = restTemplate.execute("/ouch", HttpMethod.GET, request -> request.getHeaders().setAccept(Collections.singletonList(MediaType.TEXT_HTML)), v -> v);
        assertNotNull(response);
        List<String> sniffySqlQueriesHeaders = response.getHeaders().get("Sniffy-Sql-Queries");
        assertEquals(1, sniffySqlQueriesHeaders.size());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream is = response.getBody();
        int i;
        while ((i = is.read()) != -1) {
            baos.write(i);
        }

        String responseString = new String(baos.toByteArray());
        assertNotNull(responseString);

        assertTrue(responseString.contains("script id=\"sniffy-header\""));
        assertEquals(responseString.indexOf("script id=\"sniffy-header\""), responseString.lastIndexOf("script id=\"sniffy-header\""));
    }

}
