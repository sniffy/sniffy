package com.github.bedrin.jdbc.sniffer.log;

import com.github.bedrin.jdbc.sniffer.BaseTest;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public abstract class BaseQueryLoggerTest extends BaseTest {

    protected static void setQueryLoggerImplementation(QueryLogger queryLogger) throws NoSuchFieldException, IllegalAccessException {
        Field declaredField = QueryLogger.class.getDeclaredField("INSTANCE");
        declaredField.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField( "modifiers" );
        modifiersField.setAccessible( true );
        modifiersField.setInt( declaredField, declaredField.getModifiers() & ~Modifier.FINAL );

        declaredField.set(null, queryLogger);
    }

}
