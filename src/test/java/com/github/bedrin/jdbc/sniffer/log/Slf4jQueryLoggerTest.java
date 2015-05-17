package com.github.bedrin.jdbc.sniffer.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.EchoEncoder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertTrue;

public class Slf4jQueryLoggerTest extends BaseQueryLoggerTest {

    @BeforeClass
    public static void setupJavaUtilLogger() throws Exception {
        setQueryLoggerImplementation(new Slf4jQueryLogger());
    }

    @Test
    public void testLog() throws Exception {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = context.getLogger(Slf4jQueryLogger.LOG.getName());

        logger.setAdditive(false);
        logger.setLevel(Level.TRACE);

        OutputStreamAppender<ILoggingEvent> appender = new OutputStreamAppender<>();
        appender.setContext(context);
        appender.setEncoder(new EchoEncoder<>());

        PatternLayout layout = new PatternLayout();
        layout.setContext(context);
        layout.setPattern("%msg");
        layout.start();


        appender.setLayout(layout);
        appender.setOutputStream(baos);
        appender.start();

        logger.addAppender(appender);

        context.start();

        executeStatement();

        appender.stop();

        assertTrue(new String(baos.toByteArray()).contains("SELECT 1 FROM DUAL"));
    }

}