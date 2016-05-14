package io.sniffy;

/**
 * Sniffy allows to validate the number of queries generated from different threads
 * You can use Threads enum to select all threads (ANY), current thread (CURRENT) or
 * all threads except current (OTHERS)
 * @see Sniffy
 * @see Spy
 * @since 2.2
 */
public enum Threads {
    ANY,
    CURRENT,
    OTHERS
}
