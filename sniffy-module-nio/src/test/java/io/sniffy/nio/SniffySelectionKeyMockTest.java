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

    @Mock
    private SelectionKey delegate;

    @Mock
    private SniffySelector sniffySelectorMock;

    @Mock
    private AbstractSelectableChannel sniffyAbstractSelectableChannelMock;

    private SniffySelectionKey sniffySelectionKey;

    @Before
    public void createSniffySocket() throws Exception {
        sniffySelectionKey = new SniffySelectionKey(delegate, sniffySelectorMock, sniffyAbstractSelectableChannelMock);
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

    // TODO: test Java 11+ method
    // TODO: test weak reference handling

}
