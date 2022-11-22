package io.sniffy.nio;

import io.sniffy.reflection.field.FieldRef;
import io.sniffy.socket.BaseSocketTest;
import org.junit.Test;

import java.nio.channels.SocketChannel;
import java.util.Map;

import static io.sniffy.reflection.Unsafe.$;
import static org.junit.Assert.assertTrue;

public class SniffySocketChannelTest extends BaseSocketTest {

    @Test
    public void testFields() throws Exception {

        Map<String, FieldRef<SocketChannel, ?>> fieldsMap = $(SocketChannel.class).getDeclaredFields(false, false);

        assertTrue(fieldsMap.containsKey("keys"));
        assertTrue(fieldsMap.containsKey("keyCount"));
        assertTrue(fieldsMap.containsKey("keyLock"));

        assertTrue(fieldsMap.containsKey("nonBlocking") || fieldsMap.containsKey("blocking"));
        assertTrue(fieldsMap.containsKey("regLock"));

        assertTrue(fieldsMap.containsKey("open") || fieldsMap.containsKey("closed"));
        assertTrue(fieldsMap.containsKey("closeLock"));

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
