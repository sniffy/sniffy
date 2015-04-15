package com.github.bedrin.jdbc.sniffer;

/**
 * JDBC Sniffer allows to validate the number of queries generated from different threads
 * You can use Threads enum to select all threads (ANY), current thread (CURRENT) or
 * all threads except current (OTHERS)
 * @see Sniffer
 * @see Spy
 */
public enum Threads {
    ANY,
    CURRENT,
    OTHERS
}
