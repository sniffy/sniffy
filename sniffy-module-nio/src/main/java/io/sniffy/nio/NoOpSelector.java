package io.sniffy.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.util.Collections;
import java.util.Set;

/**
 * @since 3.1.14
 */
@Deprecated
public class NoOpSelector extends AbstractSelector {

    public final static NoOpSelector INSTANCE = new NoOpSelector();

    private NoOpSelector() {
        super(SniffySelectorProvider.provider());
    }

    @Override
    protected void implCloseSelector() throws IOException {

    }

    @Override
    protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
        return null;
    }

    @Override
    public Set<SelectionKey> keys() {
        return Collections.<SelectionKey>emptySet();
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        return Collections.<SelectionKey>emptySet();
    }

    @Override
    public int selectNow() throws IOException {
        return 0;
    }

    @Override
    public int select(long timeout) throws IOException {
        return 0;
    }

    @Override
    public int select() throws IOException {
        return 0;
    }

    @Override
    public Selector wakeup() {
        return this;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }
}
