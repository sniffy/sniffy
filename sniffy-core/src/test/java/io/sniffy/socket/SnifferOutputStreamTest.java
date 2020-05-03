package io.sniffy.socket;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import ru.yandex.qatools.allure.annotations.Features;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

        verify(snifferSocket, times(4)).logSocket(anyLong(), eq(0), eq(1));

    }

    @Test
    public void testWriteByteArray() throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SnifferOutputStream sos = new SnifferOutputStream(snifferSocket, baos);

        sos.write(DATA);

        verify(snifferSocket).logSocket(anyLong(), eq(0), eq(DATA.length));

    }

    @Test
    @Features("issues/219")
    public void testWriteByteArrayThreeTcpPackets() throws IOException {

        int backup = snifferSocket.sendBufferSize;
        try {

            snifferSocket.sendBufferSize = 5;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            SnifferOutputStream sos = new SnifferOutputStream(snifferSocket, baos);

            byte[] THREE_BYTES_CHUNK = {1, 2, 3};
            byte[] ELEVEN_BYTES_CHUNK = {4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};

            byte[] ALL_DATA = new byte[THREE_BYTES_CHUNK.length + ELEVEN_BYTES_CHUNK.length];
            System.arraycopy(THREE_BYTES_CHUNK, 0, ALL_DATA, 0, THREE_BYTES_CHUNK.length);
            System.arraycopy(ELEVEN_BYTES_CHUNK, 0, ALL_DATA, THREE_BYTES_CHUNK.length, ELEVEN_BYTES_CHUNK.length);

            sos.write(THREE_BYTES_CHUNK);
            sos.write(ELEVEN_BYTES_CHUNK);

            verify(snifferSocket, times(2)).checkConnectionAllowed(eq(0));

            verify(snifferSocket).checkConnectionAllowed(eq(1));
            verify(snifferSocket).checkConnectionAllowed(eq(2));

            verify(snifferSocket).logSocket(anyLong(), eq(0), eq(THREE_BYTES_CHUNK.length));
            verify(snifferSocket).logSocket(anyLong(), eq(0), eq(ELEVEN_BYTES_CHUNK.length));

            verifyNoMoreInteractions(snifferSocket);

            assertArrayEquals(ALL_DATA, baos.toByteArray());

        } finally {
            snifferSocket.sendBufferSize = backup;
        }

    }

    @Test
    public void testWriteByteArrayWithOffset() throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SnifferOutputStream sos = new SnifferOutputStream(snifferSocket, baos);

        sos.write(DATA, 1, 2);

        verify(snifferSocket).logSocket(anyLong(), eq(0), eq(2));

    }

    @Test
    public void testFlush() throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SnifferOutputStream sos = new SnifferOutputStream(snifferSocket, baos);

        sos.flush();

        verify(snifferSocket).logSocket(anyLong());

    }

    @Test
    public void testClose() throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SnifferOutputStream sos = new SnifferOutputStream(snifferSocket, baos);

        sos.close();

        verify(snifferSocket).logSocket(anyLong());

    }

}
