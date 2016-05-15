package io.sniffy.servlet;

import org.junit.Assert;
import org.junit.Test;

public class BufferTest {

    @Test
    public void testInsertAt() throws Exception {
        Buffer buffer = new Buffer();
        buffer.write(new byte[]{1,2,3,4});
        buffer.insertAt(2, new byte[] {5,6});

        Assert.assertArrayEquals(new byte[] {1,2,5,6,3,4}, buffer.toByteArray());
    }

}