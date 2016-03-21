package io.sniffy.socket;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class SnifferInputStreamTest {

    private static final byte[] DATA = new byte[]{1,2,3,4};

    @Mock
    private SnifferSocketImpl snifferSocket;

    @Test
    public void testReadByteByByte() throws IOException {

        ByteArrayInputStream bais = new ByteArrayInputStream(DATA);
        SnifferInputStream sis = new SnifferInputStream(snifferSocket, bais);

        int read;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((read = sis.read()) != -1) {
            baos.write(read);
        }

        verify(snifferSocket, times(5)).logSocket(anyInt(), anyInt(), anyInt());

    }

    @Test
    public void testReadByteArray() throws IOException {

        ByteArrayInputStream bais = new ByteArrayInputStream(DATA);
        SnifferInputStream sis = new SnifferInputStream(snifferSocket, bais);

        byte[] buff = new byte[4];
        assertEquals(4, sis.read(buff, 0, 4));
        assertArrayEquals(DATA, buff);

        verify(snifferSocket).logSocket(anyInt(), anyInt(), anyInt());

        assertEquals(0, sis.available());

        verify(snifferSocket).logSocket(anyInt(), anyInt(), anyInt());
    }

}
