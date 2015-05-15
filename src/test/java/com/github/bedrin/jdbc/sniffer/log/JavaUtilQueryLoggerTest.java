package com.github.bedrin.jdbc.sniffer.log;

import com.github.bedrin.jdbc.sniffer.BaseTest;
import org.junit.Test;
import org.testng.log.TextFormatter;

import java.io.ByteArrayOutputStream;
import java.util.logging.*;

import static org.junit.Assert.*;

public class JavaUtilQueryLoggerTest extends BaseTest {

    @Test
    public void testLog() throws Exception {

        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINE);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Handler handler = new StreamHandler(baos, new TextFormatter());
        handler.setFormatter(new TextFormatter());
        handler.setLevel(Level.FINE);

        JavaUtilQueryLogger.LOG.setLevel(Level.FINE);
        JavaUtilQueryLogger.LOG.addHandler(handler);
        JavaUtilQueryLogger.LOG.addHandler(consoleHandler);

        executeStatement();

        handler.flush();

        assertEquals("SELECT 1 FROM DUAL\n", new String(baos.toByteArray()));
    }

}