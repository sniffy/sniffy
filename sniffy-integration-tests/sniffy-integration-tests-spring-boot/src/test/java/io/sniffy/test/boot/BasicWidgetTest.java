package io.sniffy.test.boot;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BasicWidgetTest {

    @LocalServerPort
    private int localServerPort;

    @Test
    public void customElementAndShadowDomBundleAreAvailable() throws Exception {
        URL pageUrl = new URL("http://127.0.0.1:" + localServerPort + "/index.html");
        HttpURLConnection pageConnection = (HttpURLConnection) pageUrl.openConnection();
        String html;
        try {
            assertTrue(pageConnection.getResponseCode() / 100 == 2);
            html = read(pageConnection.getInputStream());
        } finally {
            pageConnection.disconnect();
        }

        assertTrue(html.contains("id=\"sniffy-header\""));
        assertTrue(html.contains("<data id=\"sniffy\""));
        Matcher sourceMatcher = Pattern.compile("id=\"sniffy-header\"[^;]+src=\"([^\"]+)\"").matcher(html);
        assertTrue(sourceMatcher.find());
        URL scriptUrl = new URL(pageUrl, sourceMatcher.group(1));
        assertTrue(scriptUrl.toString().endsWith("/sniffy.min.js"));

        HttpURLConnection scriptConnection = (HttpURLConnection) scriptUrl.openConnection();
        try {
            assertTrue(scriptConnection.getResponseCode() / 100 == 2);
            String javascript = read(scriptConnection.getInputStream());
            assertTrue(javascript.contains("sniffy-profiler"));
            assertTrue(javascript.contains("attachShadow"));
            assertTrue(javascript.contains("mode:`open`"));
        } finally {
            scriptConnection.disconnect();
        }
    }

    private static String read(InputStream inputStream) throws Exception {
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
