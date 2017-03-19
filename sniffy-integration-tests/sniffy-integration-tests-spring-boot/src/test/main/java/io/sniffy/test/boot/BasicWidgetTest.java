package io.sniffy.test.boot;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class BasicWidgetTest {

    @Autowired
    private WebDriver webDriver;

    @Test
    public void testSniffyOpenedOnClockOnWidget() throws IOException {
        assertNotNull(webDriver);

        webDriver.navigate().to("/index.html");

        assertFalse(webDriver.findElement(By.id("sniffy-iframe")).isDisplayed());
        webDriver.findElement(By.className("sniffy-widget-icon-container")).click();
        assertTrue(webDriver.findElement(By.id("sniffy-iframe")).isDisplayed());
    }

}
