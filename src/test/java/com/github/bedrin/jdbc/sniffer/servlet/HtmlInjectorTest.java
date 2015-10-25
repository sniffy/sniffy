package com.github.bedrin.jdbc.sniffer.servlet;

import org.junit.Test;
import org.testng.Assert;

import static org.junit.Assert.*;

/**
 * Created by bedrin on 25.10.2015.
 */
public class HtmlInjectorTest {

    @Test
    public void testInjectBeforeBody() throws Exception {

        String actualContent = "<html><head><title>Title</title></head><body>Hello, World!</body></html>";
        Buffer buffer = new Buffer();
        buffer.write(actualContent.getBytes());

        HtmlInjector htmlInjector = new HtmlInjector(buffer);
        htmlInjector.injectAtTheEnd("<injected/>");

        Assert.assertEquals("<html><head><title>Title</title></head><body>Hello, World!<injected/></body></html>", buffer.toString());
    }

    @Test
    public void testInjectBeforeHtml() throws Exception {

        String actualContent = "<html><head><title>Title</title></head><body>Hello, World!</html>";
        Buffer buffer = new Buffer();
        buffer.write(actualContent.getBytes());

        HtmlInjector htmlInjector = new HtmlInjector(buffer);
        htmlInjector.injectAtTheEnd("<injected/>");

        Assert.assertEquals("<html><head><title>Title</title></head><body>Hello, World!<injected/></html>", buffer.toString());
    }

    @Test
    public void testInjectAtTheEnd() throws Exception {

        String actualContent = "<html><head><title>Title</title></head><body>Hello, World!";
        Buffer buffer = new Buffer();
        buffer.write(actualContent.getBytes());

        HtmlInjector htmlInjector = new HtmlInjector(buffer);
        htmlInjector.injectAtTheEnd("<injected/>");

        Assert.assertEquals("<html><head><title>Title</title></head><body>Hello, World!<injected/>", buffer.toString());
    }

}