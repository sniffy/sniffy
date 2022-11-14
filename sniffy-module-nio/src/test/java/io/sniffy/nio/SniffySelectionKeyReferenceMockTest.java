package io.sniffy.nio;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.ref.WeakReference;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SniffySelectionKeyReferenceMockTest {

    @Mock
    private WeakReference<SelectionKey> referenceMock;

    @Mock
    private SniffySelector sniffySelectorMock;

    @Mock
    private AbstractSelectableChannel sniffyAbstractSelectableChannelMock;

    private SniffySelectionKey sniffySelectionKey;

    @Before
    public void createSniffySelectionKey() throws Exception {
        sniffySelectionKey = new SniffySelectionKey(referenceMock, sniffySelectorMock, sniffyAbstractSelectableChannelMock);
        verify(referenceMock).get();
        verifyNoMoreInteractions(referenceMock);
        //noinspection unchecked
        clearInvocations(referenceMock);
    }

    @Test
    public void testIsValid() {

        doReturn(null).when(referenceMock).get();

        assertFalse(sniffySelectionKey.isValid());

        verify(referenceMock).get();
        verifyNoMoreInteractions(referenceMock);
    }

    @Test
    public void testCancel() {

        doReturn(null).when(referenceMock).get();

        try {
            sniffySelectionKey.cancel();
            fail("Should have failed");
        } catch (NullPointerException e) {
            assertNotNull(e);
        }

        verify(referenceMock).get();
        verifyNoMoreInteractions(referenceMock);
    }

    @Test
    public void testGetInterestOps() {
        doReturn(null).when(referenceMock).get();

        try {
            sniffySelectionKey.interestOps();
            fail("Should have failed");
        } catch (NullPointerException e) {
            assertNotNull(e);
        }

        verify(referenceMock).get();
        verifyNoMoreInteractions(referenceMock);
    }

    @Test
    public void testSetInterestOps() {
        doReturn(null).when(referenceMock).get();

        try {
            sniffySelectionKey.interestOps(SelectionKey.OP_CONNECT);
            fail("Should have failed");
        } catch (NullPointerException e) {
            assertNotNull(e);
        }

        verify(referenceMock).get();
        verifyNoMoreInteractions(referenceMock);
    }

    @Test
    public void testReadyOps() {
        doReturn(null).when(referenceMock).get();

        try {
            sniffySelectionKey.readyOps();
            fail("Should have failed");
        } catch (NullPointerException e) {
            assertNotNull(e);
        }

        verify(referenceMock).get();
        verifyNoMoreInteractions(referenceMock);
    }

    @Test
    public void testInterestOpsAnd() {
        doReturn(null).when(referenceMock).get();

        try {
            sniffySelectionKey.interestOpsAnd(SelectionKey.OP_ACCEPT);
            fail("Should have failed");
        } catch (NullPointerException e) {
            assertNotNull(e);
        }

        verify(referenceMock).get();
        verifyNoMoreInteractions(referenceMock);
    }

    @Test
    public void testInterestOpsOr() {
        doReturn(null).when(referenceMock).get();

        try {
            sniffySelectionKey.interestOpsOr(SelectionKey.OP_ACCEPT);
            fail("Should have failed");
        } catch (NullPointerException e) {
            assertNotNull(e);
        }

        verify(referenceMock).get();
        verifyNoMoreInteractions(referenceMock);
    }

}
