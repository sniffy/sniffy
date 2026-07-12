package io.sniffy.nio;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks wrapper/delegate key links for one wrapper channel so channel-level operations can
 * reconcile wrapper cancellation with delegate cancellation before the next select cycle.
 */
final class ChannelRegistrationSupport {

    private final Set<SelectionKeyLink> keyLinks = Collections.newSetFromMap(
            new ConcurrentHashMap<SelectionKeyLink, Boolean>());

    void register(SelectionKeyLink link) {
        keyLinks.add(link);
    }

    void unregister(SelectionKeyLink link) {
        keyLinks.remove(link);
    }

    void propagateCancelledKeys() {
        for (SelectionKeyLink link : keyLinks) {
            link.propagateWrapperCancellation();
        }
    }
}
