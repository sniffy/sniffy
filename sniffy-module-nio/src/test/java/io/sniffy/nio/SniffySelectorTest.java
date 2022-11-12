package io.sniffy.nio;

import io.sniffy.socket.SnifferSocketImplFactory;
import io.sniffy.util.ObjectWrapper;
import io.sniffy.util.ReflectionUtil;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelector;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SniffySelectorTest {

    @Test
    public void testFields() throws Exception {

        Map<String, Field> fieldsMap = new HashMap<String, Field>();

        for (Field field : ReflectionUtil.getDeclaredFieldsHierarchy(AbstractSelector.class)) {
            if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                fieldsMap.put(field.getName(), field);
            }
        }

        fieldsMap.remove("closed");
        fieldsMap.remove("selectorOpen");

        fieldsMap.remove("interruptor");
        fieldsMap.remove("provider");
        fieldsMap.remove("cancelledKeys");

        assertTrue(fieldsMap + " should be empty",fieldsMap.isEmpty());

    }

    @Test
    public void testCloseSelector() throws Exception {

        SnifferSocketImplFactory.uninstall();
        SnifferSocketImplFactory.install();

        SniffySelectorProviderModule.initialize();
        SniffySelectorProvider.uninstall();
        SniffySelectorProvider.install();

        try (Selector selector = Selector.open()) {
            selector.close();
            assertFalse(selector.isOpen());
            assertTrue(selector instanceof ObjectWrapper);
            //noinspection unchecked
            AbstractSelector delegate = ((ObjectWrapper<AbstractSelector>) selector).getDelegate();
            assertFalse(delegate.isOpen());
        }

    }

}
