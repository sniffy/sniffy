package io.sniffy;

/**
 * @since 3.1.10
 */
public interface ThreadMatcher {

    boolean matches(ThreadMetaData threadMetaData);

}
