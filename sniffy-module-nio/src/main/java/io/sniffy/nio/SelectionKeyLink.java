package io.sniffy.nio;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

/**
 * Private backlink stored in the delegate key attachment. The final selector/channel context is
 * published with the link; synchronized initialization publishes one fully constructed wrapper.
 */
final class SelectionKeyLink {

    private final SniffySelector selector;
    private final SelectableChannel channel;
    private final Object initialUserAttachment;

    private volatile SelectionKey delegate;
    private volatile SniffySelectionKey wrapper;

    SelectionKeyLink(SniffySelector selector, SelectableChannel channel, Object initialUserAttachment) {
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
