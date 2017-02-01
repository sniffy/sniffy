package io.sniffy.socket;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class SnifferOutputStreamTest {

    private static final byte[] DATA = new byte[]{1,2,3,4};

    @Mock
    private SnifferSocketImpl snifferSocket;

    @Test
    public void testWriteByteByByte() throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SnifferOutputStream sos = new SnifferOutputStream(snifferSocket, baos);

        for (byte b : DATA) {
            sos.write(b);
        }

        verify(snifferSocket, times(4)).logSocket(anyInt(), eq(0), eq(1));

    }

    @Test
    public void testWriteByteArray() throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SnifferOutputStream sos = new SnifferOutputStream(snifferSocket, baos);

        sos.write(DATA);

        verify(snifferSocket).logSocket(anyInt(), eq(0), eq(DATA.length));

    }

    @Test
    public void testWriteByteArrayWithOffset() throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SnifferOutputStream sos = new SnifferOutputStream(snifferSocket, baos);

        sos.write(DATA, 1, 2);

        verify(snifferSocket).logSocket(anyInt(), eq(0), eq(2));

    }

    @Test
    public void testFlush() throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SnifferOutputStream sos = new SnifferOutputStream(snifferSocket, baos);

        sos.flush();

        verify(snifferSocket).logSocket(anyInt());

    }

    @Test
    public void testClose() throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SnifferOutputStream sos = new SnifferOutputStream(snifferSocket, baos);

        sos.close();

        verify(snifferSocket).logSocket(anyInt());

    }

}
