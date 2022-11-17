package io.sniffy.nio;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.emptySet;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SniffySelectorMockTest {

    @Mock
    private SelectorProvider selectorProviderMock;

    @Mock
    private AbstractSelector selectorMock;

    private AtomicInteger implCloseSelectorInvocationCounter;
    private AbstractSelector delegate;

    private SniffySelector sniffySelector;

    @Before
    public void createSniffySelector() throws Exception {
        implCloseSelectorInvocationCounter = new AtomicInteger();
        delegate = new SniffySelector(selectorProviderMock, selectorMock) {

            @Override
            protected void implCloseSelector() throws IOException {
                implCloseSelectorInvocationCounter.incrementAndGet();
                super.implCloseSelector();
            }

        };
        sniffySelector = new SniffySelector(selectorProviderMock, delegate);
    }

    @Test
    public void testClose() throws Exception {
        assertTrue(sniffySelector.isOpen());
        assertTrue(delegate.isOpen());
        sniffySelector.close();
        assertEquals(1, implCloseSelectorInvocationCounter.get());
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
        verify(selectorMock).wakeup();
        verifyNoMoreInteractions(selectorMock);
    }

    @Test
    public void testSelectNow() throws Exception {
        doReturn(1).when(selectorMock).selectNow();
        doReturn(emptySet()).when(selectorMock).keys(); // TODO: test it
        assertEquals(1, sniffySelector.selectNow());
        verify(selectorMock).selectNow();
        verify(selectorMock, times(2)).keys(); // TODO: this is for cleaning up channels
        verifyNoMoreInteractions(selectorMock);
    }

    @Test
    public void testSelectNowUpdatesKeysForRelatedChannels() throws Exception {
        // TODO: implement
    }

}
