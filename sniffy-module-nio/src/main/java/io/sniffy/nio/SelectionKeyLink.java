package io.sniffy.nio;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;

/**
 * Private backlink stored in the delegate key attachment. The final selector/channel context is
 * published with the link; synchronized initialization publishes one fully constructed wrapper.
 */
final class SelectionKeyLink {

    private final SniffySelector selector;
    private final AbstractSelectableChannel channel;
    private final Object initialUserAttachment;

    private volatile SelectionKey delegate;
    private volatile SniffySelectionKey wrapper;

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

    void propagateWrapperCancellation() {
        SniffySelectionKey wrapperKey = wrapper;
        SelectionKey delegateKey = delegate;
        if (wrapperKey != null && !wrapperKey.isValid() && delegateKey != null && delegateKey.isValid()) {
            delegateKey.cancel();
        }
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
