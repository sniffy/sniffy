package io.sniffy.nio;

import io.sniffy.socket.BaseSocketTest;
import io.sniffy.util.ReflectionUtil;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class SniffySocketChannelTest extends BaseSocketTest {

    @Test
    public void testFields() throws Exception {

        Map<String, Field> fieldsMap = new HashMap<String, Field>();

        for (Field field : ReflectionUtil.getDeclaredFieldsHierarchy(SocketChannel.class)) {
            if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                fieldsMap.put(field.getName(), field);
            }
        }

        fieldsMap.remove("provider");

        fieldsMap.remove("keys");
        fieldsMap.remove("keyCount");
        fieldsMap.remove("keyLock");

        fieldsMap.remove("regLock");
        fieldsMap.remove("nonBlocking");
        fieldsMap.remove("blocking"); // TODO: check if it's processed correctly; it's present5 on JDK 10

        fieldsMap.remove("closeLock");
        fieldsMap.remove("open");
        fieldsMap.remove("closed");

        fieldsMap.remove("interruptor");
        fieldsMap.remove("interrupted");

        assertTrue(fieldsMap + " should be empty",fieldsMap.isEmpty());

    }

}
