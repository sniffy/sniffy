package io.sniffy.nio;

import io.sniffy.socket.SnifferSocketImplFactory;
import io.sniffy.util.ObjectWrapper;
import io.sniffy.util.ReflectionUtil;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelector;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SniffySelectionKeyTest {

    @Test
    public void testFields() throws Exception {

        Map<String, Field> fieldsMap = new HashMap<String, Field>();

        for (Field field : ReflectionUtil.getDeclaredFieldsHierarchy(SelectionKey.class)) {
            if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                fieldsMap.put(field.getName(), field);
            }
        }

        fieldsMap.remove("attachment");
        fieldsMap.remove("attachmentUpdater");

        assertTrue(fieldsMap + " should be empty",fieldsMap.isEmpty());

    }

}
