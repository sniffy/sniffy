package io.sniffy.test.tomcat;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import ru.yandex.qatools.allure.annotations.Issue;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BaseTomcatIT {

    @Test
    public void testSniffyInjected() {

        WebDriver webDriver = new HtmlUnitDriver(true);

        webDriver.navigate().to("http://127.0.0.1:8081/test");

        assertFalse(webDriver.findElement(By.id("sniffy-iframe")).isDisplayed());
        webDriver.findElement(By.className("sniffy-widget-icon-container")).click();
        assertTrue(webDriver.findElement(By.id("sniffy-iframe")).isDisplayed());

        webDriver.quit();
    }

    @Test
    @Issue("issues/321")
    public void testSniffyInjectedPath() {

        WebDriver webDriver = new HtmlUnitDriver(true);

        webDriver.navigate().to("http://127.0.0.1:8081/test/index.html");

        assertFalse(webDriver.findElement(By.id("sniffy-iframe")).isDisplayed());
        webDriver.findElement(By.className("sniffy-widget-icon-container")).click();
        assertTrue(webDriver.findElement(By.id("sniffy-iframe")).isDisplayed());

        webDriver.quit();
    }

    @Test
    @Issue("issues/319")
    public void testSniffyInjectedToUrlWithQueryParameters() {

        WebDriver webDriver = new HtmlUnitDriver(true);

        webDriver.navigate().to("http://127.0.0.1:8081/test?foo=bar");

        assertFalse(webDriver.findElement(By.id("sniffy-iframe")).isDisplayed());
        webDriver.findElement(By.className("sniffy-widget-icon-container")).click();
        assertTrue(webDriver.findElement(By.id("sniffy-iframe")).isDisplayed());

        webDriver.quit();
    }

}
