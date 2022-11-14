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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SniffySelectorMockTest {

    @Mock
    private SelectorProvider selectorProviderMock;

    @Mock
    private AbstractSelector abstractSelectorMock;

    private AtomicInteger implCloseSelectorInvocationCounter;
    private AbstractSelector delegate;

    private SniffySelector sniffySelector;

    @Before
    public void createSniffySelector() throws Exception {
        implCloseSelectorInvocationCounter = new AtomicInteger();
        delegate = new SniffySelector(selectorProviderMock, abstractSelectorMock) {

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
        verify(abstractSelectorMock).wakeup();
        verifyNoMoreInteractions(abstractSelectorMock);
    }

}
