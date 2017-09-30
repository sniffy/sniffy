package io.sniffy.test.tomcat;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.springframework.web.client.RestTemplate;
import ru.yandex.qatools.allure.annotations.Issue;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

public class BaseTomcatIT {

    @Test
    public void testSniffyInjected() {

        WebDriver webDriver = new HtmlUnitDriver(true);

        webDriver.navigate().to("http://127.0.0.1:8081/test/static");

        System.out.println(webDriver.getPageSource());
        assertFalse(webDriver.findElement(By.id("sniffy-iframe")).isDisplayed());
        webDriver.findElement(By.className("sniffy-widget-icon-container")).click();
        assertTrue(webDriver.findElement(By.id("sniffy-iframe")).isDisplayed());

        webDriver.quit();
    }

    @Test
    @Issue("issues/321")
    public void testSniffyInjectedPath() {

        WebDriver webDriver = new HtmlUnitDriver(true);

        webDriver.navigate().to("http://127.0.0.1:8081/test/static/index.html");

        assertFalse(webDriver.findElement(By.id("sniffy-iframe")).isDisplayed());
        webDriver.findElement(By.className("sniffy-widget-icon-container")).click();
        assertTrue(webDriver.findElement(By.id("sniffy-iframe")).isDisplayed());

        webDriver.quit();
    }

    @Test
    @Issue("issues/319")
    public void testSniffyInjectedToUrlWithQueryParameters() {

        WebDriver webDriver = new HtmlUnitDriver(true);

        webDriver.navigate().to("http://127.0.0.1:8081/test/static?foo=bar");

        assertFalse(webDriver.findElement(By.id("sniffy-iframe")).isDisplayed());
        webDriver.findElement(By.className("sniffy-widget-icon-container")).click();
        assertTrue(webDriver.findElement(By.id("sniffy-iframe")).isDisplayed());

        webDriver.quit();
    }

    @Test
    public void testSpringServletWithPathMapping() throws MalformedURLException, URISyntaxException {

        RestTemplate restTemplate = new RestTemplate();

        String pathParam = restTemplate.getForObject(new URI("http://127.0.0.1:8081/test/servlet/base/foo"), String.class);
        assertEquals("foo", pathParam);

    }

    @Test
    public void testSpringServletWithExtensionMapping() throws MalformedURLException, URISyntaxException {

        RestTemplate restTemplate = new RestTemplate();

        String pathParam = restTemplate.getForObject(new URI("http://127.0.0.1:8081/test/base/foo.do"), String.class);
        assertEquals("foo", pathParam);

    }

}
