package io.sniffy.nio;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import sun.nio.ch.Util;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.emptySet;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SniffySelectorMockTest {

    @Mock
    private SelectorProvider selectorProviderMock;

    @Mock
    private SniffySelectorDelegate selectorMock;
    private SniffySelectorDelegate delegate;

    private SniffySelector sniffySelector;

    @SuppressWarnings({"NewClassNamingConvention", "FieldMayBeFinal", "unused", "Convert2Diamond"})
    public static class SniffySelectorDelegate extends SniffySelector {

        private final AtomicInteger implCloseSelectorInvocationCounter = new AtomicInteger();

        // Same fields are present in SelectorImpl class - we need to replicate them for testing
        protected Set<SelectionKey> selectedKeys = new HashSet<SelectionKey>();
        protected HashSet<SelectionKey> keys = new HashSet<SelectionKey>();
        private Set<SelectionKey> publicKeys = new HashSet<SelectionKey>();
        private Set<SelectionKey> publicSelectedKeys = new HashSet<SelectionKey>();

        public SniffySelectorDelegate(SelectorProvider provider, AbstractSelector delegate) {
            super(provider, delegate);
        }

        @Override
        protected void implCloseSelector() throws IOException {
            implCloseSelectorInvocationCounter.incrementAndGet();
            super.implCloseSelector();
        }

    }

    @Before
    public void createSniffySelector() throws Exception {
        delegate = new SniffySelectorDelegate(selectorProviderMock, selectorMock);
        sniffySelector = new SniffySelector(selectorProviderMock, delegate);
    }

    @Test
    public void testClose() throws Exception {
        assertTrue(sniffySelector.isOpen());
        assertTrue(delegate.isOpen());
        sniffySelector.close();
        assertEquals(1, delegate.implCloseSelectorInvocationCounter.get());
        assertFalse(sniffySelector.isOpen());
        assertFalse(delegate.isOpen());
    }

    @Test
    public void testCloseWorksJustOnce() throws Exception {
        assertTrue(sniffySelector.isOpen());
        assertTrue(delegate.isOpen());
        delegate.close();
        sniffySelector.close();
        assertEquals(1, delegate.implCloseSelectorInvocationCounter.get());
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
        // TODO: fix

    }

    @Test
    public void testSelectNowUpdatesKeysForRelatedChannels() throws Exception {
        // TODO: implement
    }

}
