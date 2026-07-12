package io.sniffy.nio;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks wrapper/delegate key links owned by one wrapper channel. */
final class ChannelRegistrationSupport {

    private final Set<SelectionKeyLink> keyLinks = Collections.newSetFromMap(
            new ConcurrentHashMap<SelectionKeyLink, Boolean>());

    void register(SelectionKeyLink link) {
        keyLinks.add(link);
    }

    void unregister(SelectionKeyLink link) {
        keyLinks.remove(link);
    }
}
