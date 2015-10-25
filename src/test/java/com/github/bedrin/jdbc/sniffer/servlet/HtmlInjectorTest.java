package com.github.bedrin.jdbc.sniffer.servlet;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by bedrin on 25.10.2015.
 */
public class HtmlInjectorTest {

    @Test
    public void testInjectAtTheEnd() throws Exception {

        String actualContent = "<html><head><title>Title</title></head><body>Hello, World!</body></html>";
        Buffer buffer = new Buffer();
        buffer.write(actualContent.getBytes());

        HtmlInjector htmlInjector = new HtmlInjector(buffer);
        htmlInjector.injectAtTheEnd();

    }

}