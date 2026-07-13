package io.sniffy.nio;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;

/**
 * Private backlink stored in the delegate key attachment. The final selector/channel context is
 * published with the link; synchronized initialization publishes one fully constructed wrapper.
 */
final class SelectionKeyLink {

    enum State {
        REGISTERING, ACTIVE, CLEANING, REMOVED
    }

    private final SniffySelector selector;
    private final AbstractSelectableChannel channel;
    private Object initialUserAttachment;

    private volatile SelectionKey delegate;
    private volatile SniffySelectionKey wrapper;
    private State state = State.REGISTERING;

    SelectionKeyLink(SniffySelector selector, AbstractSelectableChannel channel, Object initialUserAttachment) {
        this.selector = selector;
        this.channel = channel;
        this.initialUserAttachment = initialUserAttachment;
    }

    SniffySelectionKey wrapper(SelectionKey delegate) {
        SniffySelectionKey key = wrapper;
        if (key != null) {
            verifyDelegate(delegate);
            return key;
        }
        synchronized (this) {
            verifyOrSetDelegate(delegate);
            key = wrapper;
            if (key == null) {
                key = new SniffySelectionKey(delegate, selector, channel, initialUserAttachment);
                initialUserAttachment = null;
                wrapper = key;
            }
            return key;
        }
    }

    boolean belongsTo(SniffySelector selector) {
        return this.selector == selector;
    }

    SelectionKey delegate() {
        return delegate;
    }

    SniffySelectionKey existingWrapper() {
        return wrapper;
    }

    AbstractSelectableChannel channel() {
        return channel;
    }

    State state() {
        return state;
    }

    void activate() {
        transition(State.REGISTERING, State.ACTIVE);
    }

    boolean beginCleaning() {
        if (state == State.REGISTERING || state == State.ACTIVE) {
            state = State.CLEANING;
            return true;
        }
        return false;
    }

    void retryCleanup() {
        transition(State.CLEANING, State.ACTIVE);
    }

    void removed() {
        transition(State.CLEANING, State.REMOVED);
    }

    private void transition(State expected, State next) {
        if (state != expected) {
            throw new IllegalStateException("SelectionKey link state is " + state + ", expected " + expected);
        }
        state = next;
    }

    private void verifyDelegate(SelectionKey candidate) {
        SelectionKey linkedDelegate = delegate;
        if (linkedDelegate != candidate) {
            throw new IllegalStateException("SelectionKey link points to a different delegate key");
        }
    }

    private void verifyOrSetDelegate(SelectionKey candidate) {
        if (delegate == null) {
            delegate = candidate;
        } else {
            verifyDelegate(candidate);
        }
    }
}
