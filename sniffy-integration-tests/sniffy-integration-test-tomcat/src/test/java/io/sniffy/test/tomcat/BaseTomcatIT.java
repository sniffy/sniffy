package io.sniffy.test.tomcat;

import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.VirtualWebappLoader;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.apache.naming.resources.FileDirContext;
import org.apache.naming.resources.VirtualDirContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import ru.yandex.qatools.allure.annotations.Issue;

import java.io.File;
import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BaseTomcatIT {

    private static Tomcat tomcat;

    @BeforeClass
    public static void startTomcat() throws Exception {

        tomcat = new Tomcat();
        tomcat.setPort(8081);

        StandardContext ctx = (StandardContext) tomcat.addWebapp("/test", new File("src/main/webapp/").getAbsolutePath());
        //declare an alternate location for your "WEB-INF/classes" dir:
        File additionWebInfClasses = new File("target/classes");
        VirtualDirContext resources = new VirtualDirContext();
        resources.setExtraResourcePaths("/WEB-INF/classes=" + additionWebInfClasses);
        ctx.setResources(resources);

        ctx.start();

        tomcat.start();

    }

    @AfterClass
    public static void stopTomcat() throws Exception {
        tomcat.stop();
    }

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
