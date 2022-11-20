package io.sniffy.nio;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SniffySelectionKeyMockTest {

    public static abstract class Java11SelectionKey extends SelectionKey {

        public int interestOpsOr(int ops) {
            synchronized (this) {
                int oldVal = interestOps();
                interestOps(oldVal | ops);
                return oldVal;
            }
        }

        public int interestOpsAnd(int ops) {
            synchronized (this) {
                int oldVal = interestOps();
                interestOps(oldVal & ops);
                return oldVal;
            }
        }

    }

    @Mock
    private Java11SelectionKey delegate;

    @Mock
    private SniffySelector sniffySelectorMock;

    @Mock
    private AbstractSelectableChannel sniffyAbstractSelectableChannelMock;

    private SniffySelectionKey sniffySelectionKey;

    @Before
    public void createSniffySelectionKey() throws Exception {
        sniffySelectionKey = new SniffySelectionKey(sniffySelectorMock, sniffyAbstractSelectableChannelMock, null);
        sniffySelectionKey.setDelegate(delegate);
    }

    @Test
    public void testChannel() {
        assertEquals(sniffyAbstractSelectableChannelMock, sniffySelectionKey.channel());
    }

    @Test
    public void testSelector() {
        assertEquals(sniffySelectorMock, sniffySelectionKey.selector());
    }

    @Test
    public void testIsValid() {

        doReturn(true).when(delegate).isValid();
        assertTrue(sniffySelectionKey.isValid());

        doReturn(false).when(delegate).isValid();
        assertFalse(sniffySelectionKey.isValid());

        verify(delegate, times(2)).isValid();
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void testCancel() {
        sniffySelectionKey.cancel();
        verify(delegate).cancel();
        verify(delegate).isValid();
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void testGetInterestOps() {
        doReturn(SelectionKey.OP_CONNECT).when(delegate).interestOps();
        assertEquals(SelectionKey.OP_CONNECT, sniffySelectionKey.interestOps());
        verify(delegate).interestOps();
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void testSetInterestOps() {
        sniffySelectionKey.interestOps(SelectionKey.OP_CONNECT);
        //noinspection MagicConstant
        verify(delegate).interestOps(eq(SelectionKey.OP_CONNECT));
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void testReadyOps() {
        doReturn(SelectionKey.OP_CONNECT).when(delegate).readyOps();
        assertEquals(SelectionKey.OP_CONNECT, sniffySelectionKey.readyOps());
        verify(delegate).readyOps();
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void testInterestOpsAnd() {
        doReturn(SelectionKey.OP_CONNECT).when(delegate).interestOpsAnd(eq(SelectionKey.OP_READ));
        assertEquals(SelectionKey.OP_CONNECT, sniffySelectionKey.interestOpsAnd(SelectionKey.OP_READ));
        verify(delegate).interestOpsAnd(SelectionKey.OP_READ);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void testInterestOpsOr() {
        doReturn(SelectionKey.OP_CONNECT).when(delegate).interestOpsOr(eq(SelectionKey.OP_READ));
        assertEquals(SelectionKey.OP_CONNECT, sniffySelectionKey.interestOpsOr(SelectionKey.OP_READ));
        verify(delegate).interestOpsOr(SelectionKey.OP_READ);
        verifyNoMoreInteractions(delegate);
    }

    // TODO: test weak reference handling

}
