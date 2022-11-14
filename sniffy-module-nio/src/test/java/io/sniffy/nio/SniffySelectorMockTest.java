package io.sniffy.nio;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SniffySelectorMockTest {

    @Mock
    private SelectorProvider selectorProviderMock;

    @Mock
    private AbstractSelector abstractSelectorMock;

    private AbstractSelector delegate;

    private SniffySelector sniffySelector;

    @Before
    public void createSniffySelector() throws Exception {
        delegate = new SniffySelector(selectorProviderMock, abstractSelectorMock);
        sniffySelector = new SniffySelector(selectorProviderMock, delegate);
    }

    @Test
    public void testClose() throws Exception {
        assertTrue(sniffySelector.isOpen());
        assertTrue(delegate.isOpen());
        sniffySelector.close();
        assertFalse(sniffySelector.isOpen());
        assertFalse(delegate.isOpen());
    }

    @Test
    public void testCloseUpdatesKeysForRelatedChannels() throws Exception {
        // TODO: implement
    }

    @Test
    public void testWakeUp() throws Exception {
        assertEquals(sniffySelector, sniffySelector.wakeup());
        verify(abstractSelectorMock).wakeup();
        verifyNoMoreInteractions(abstractSelectorMock);
    }

}
