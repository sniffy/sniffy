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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
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

        verify(snifferSocket, times(4)).logSocket(anyInt(), eq(1), eq(0));
        verify(snifferSocket).logSocket(anyInt(), eq(0), eq(0));

    }

    @Test
    public void testReadByteArray() throws IOException {

        ByteArrayInputStream bais = new ByteArrayInputStream(DATA);
        SnifferInputStream sis = new SnifferInputStream(snifferSocket, bais);

        byte[] buff = new byte[4];
        assertEquals(4, sis.read(buff));
        assertArrayEquals(DATA, buff);

        assertEquals(0, sis.available());

        verify(snifferSocket).logSocket(anyInt(), eq(4), eq(0));
        verify(snifferSocket).logSocket(anyInt());
    }

    @Test
    public void testReadByteArrayRange() throws IOException {

        ByteArrayInputStream bais = new ByteArrayInputStream(DATA);
        SnifferInputStream sis = new SnifferInputStream(snifferSocket, bais);

        byte[] expected = new byte[DATA.length];
        System.arraycopy(DATA, 0, expected, 1, 2);

        byte[] buff = new byte[4];
        assertEquals(2, sis.read(buff, 1, 2));
        assertArrayEquals(expected, buff);

        assertEquals(2, sis.available());

        verify(snifferSocket).logSocket(anyInt(), eq(2), eq(0));
        verify(snifferSocket).logSocket(anyInt());
    }

    @Test
    public void testSkip() throws IOException {

        ByteArrayInputStream bais = new ByteArrayInputStream(DATA);
        SnifferInputStream sis = new SnifferInputStream(snifferSocket, bais);

        assertEquals(1, sis.skip(1));

        byte[] buff = new byte[4];
        assertEquals(3, sis.read(buff));
        assertArrayEquals(new byte[]{2,3,4,0}, buff);

        assertEquals(0, sis.available());

        verify(snifferSocket, times(2)).logSocket(anyInt()); // skip() and available() calls
        verify(snifferSocket).logSocket(anyInt(), eq(3), eq(0));
    }

    @Test
    public void testClose() throws IOException {

        ByteArrayInputStream bais = new ByteArrayInputStream(DATA);
        SnifferInputStream sis = new SnifferInputStream(snifferSocket, bais);

        sis.close();
        verify(snifferSocket).logSocket(anyInt());

    }

}
