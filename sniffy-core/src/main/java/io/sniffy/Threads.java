package io.sniffy;

import java.io.IOException;

/**
 * Sniffy allows to validate the number of queries generated from different threads
 * You can use Threads enum to select all threads (ANY), current thread (CURRENT) or
 * all threads except current (OTHERS)
 * @see Sniffy
 * @see Spy
 * @since 2.2
 */
public enum Threads implements ThreadMatcher {
    ANY,
    CURRENT,
    OTHERS;

    @Override
    public boolean matches(ThreadMetaData threadMetaData) {
        switch (this) {
            case ANY:
                return true;
            case CURRENT:
                return Thread.currentThread().getId() == threadMetaData.getThreadId();
            case OTHERS:
                return Thread.currentThread().getId() != threadMetaData.getThreadId();
            default:
                return false;
        }
    }

    @Override
    public void describe(StringBuilder appendable) {
        switch (this) {
            case CURRENT:
                appendable.append(" current thread");
                break;
            case OTHERS:
                appendable.append(" other threads");
                break;
            case ANY:
            default:
        }
    }


}
