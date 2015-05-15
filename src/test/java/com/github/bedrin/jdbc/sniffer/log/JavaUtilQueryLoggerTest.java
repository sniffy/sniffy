package com.github.bedrin.jdbc.sniffer.log;

import com.github.bedrin.jdbc.sniffer.BaseTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.log.TextFormatter;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.logging.*;

import static org.junit.Assert.*;

public class JavaUtilQueryLoggerTest extends BaseTest {

    @BeforeClass
    public static void setupJavaUtilLogger() throws Exception {
        Field declaredField = QueryLogger.class.getDeclaredField("INSTANCE");
        declaredField.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField( "modifiers" );
        modifiersField.setAccessible( true );
        modifiersField.setInt( declaredField, declaredField.getModifiers() & ~Modifier.FINAL );

        declaredField.set(null, new JavaUtilQueryLogger());
    }

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