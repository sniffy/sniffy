package com.github.bedrin.jdbc.sniffer.log;

import org.apache.log4j.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertTrue;

public class Log4JQueryLoggerTest extends BaseQueryLoggerTest {

    @BeforeClass
    public static void setupJavaUtilLogger() throws Exception {
        setQueryLoggerImplementation(new Log4JQueryLogger());
    }

    @Test
    public void testLog() throws Exception {

        Appender consoleHandler = new ConsoleAppender(new PatternLayout("%m%n"), ConsoleAppender.SYSTEM_OUT);
        consoleHandler.setName("console");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Appender handler = new WriterAppender(new PatternLayout("%m%n"), baos);
        handler.setName("baos");

        Log4JQueryLogger.LOG.setLevel(org.apache.log4j.Level.DEBUG);
        Log4JQueryLogger.LOG.addAppender(handler);
        Log4JQueryLogger.LOG.addAppender(consoleHandler);

        executeStatement();

        assertTrue(new String(baos.toByteArray()).contains("SELECT 1 FROM DUAL"));
    }

}