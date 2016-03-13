package io.sniffy.socket;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.SocketImpl;

import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;

@RunWith(PowerMockRunner.class)
@PrepareForTest(SocketImpl.class)
public class SnifferSocketImplTest {

    @Mock
    private SocketImpl delegate;

    private SnifferSocketImpl sniffySocket;

    @Before
    public void createSniffySocket() {
        sniffySocket = new SnifferSocketImpl(delegate);
    }

    @Test
    public void testSendUrgentData() throws Exception {

        sniffySocket.sendUrgentData(1);

        verifyPrivate(delegate).invoke("sendUrgentData",1);
        verifyNoMoreInteractions(delegate);

    }

    @Test
    public void testShutdownInput() throws Exception {

        sniffySocket.shutdownInput();

        verifyPrivate(delegate).invoke("shutdownInput");
        verifyNoMoreInteractions(delegate);

    }


}
