package com.github.bedrin.jdbc.sniffer;

import java.sql.Driver;

/**
 * Utility interface for storing the JDBC Sniffer constants
 */
interface Constants {

    /**
     * Prefix to be used in JDBC URL, for exaple {@code sniffer:jdbc:h2:mem:}
     */
    String DRIVER_PREFIX = "sniffer:";

    /**
     * The major version of JDBC Sniffer
     * @see Driver#getMajorVersion()
     */
    int MAJOR_VERSION = 2;

    /**
     * The major version of JDBC Sniffer
     * @see Driver#getMinorVersion() ()
     */
    int MINOR_VERSION = 1;

}
