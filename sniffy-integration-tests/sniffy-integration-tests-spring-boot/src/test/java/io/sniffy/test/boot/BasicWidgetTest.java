package io.sniffy.test.boot;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BasicWidgetTest {

    @LocalServerPort
    private int localServerPort;

    @Test
    public void widgetIconOpensSniffyIframe() {
        WebDriver webDriver = new HtmlUnitDriver(true);
        try {
            webDriver.navigate().to("http://127.0.0.1:" + localServerPort + "/index.html");

            assertFalse(webDriver.findElement(By.id("sniffy-iframe")).isDisplayed());
            webDriver.findElement(By.className("sniffy-widget-icon-container")).click();
            assertTrue(webDriver.findElement(By.id("sniffy-iframe")).isDisplayed());
        } finally {
            webDriver.quit();
        }
    }

}
