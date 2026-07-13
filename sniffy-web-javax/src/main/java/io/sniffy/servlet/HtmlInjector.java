package io.sniffy.servlet;

/**
 * Package-local compatibility facade over namespace-neutral HTML injection logic.
 */
class HtmlInjector extends io.sniffy.servlet.internal.HtmlInjector {

    HtmlInjector(Buffer buffer) {
        super(buffer);
    }

    HtmlInjector(Buffer buffer, String characterEncoding) {
        super(buffer, characterEncoding);
    }
}
