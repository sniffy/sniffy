package io.sniffy;

import java.sql.Driver;

/**
 * Utility interface for storing the Sniffer constants
 */
public interface Constants {

    /**
     * Prefix to be used in JDBC URL, for exaple {@code sniffer:jdbc:h2:mem:}
     */
    String DRIVER_PREFIX = "sniffer:";

    /**
     * The major version of Sniffy
     * @see Driver#getMajorVersion()
     */
    int MAJOR_VERSION = 3;

    /**
     * The major version of Sniffy
     * @see Driver#getMinorVersion() ()
     */
    int MINOR_VERSION = 0;

    /**
     * The major version of Sniffy
     * @see Driver#getMinorVersion() ()
     */
    int PATCH_VERSION = 3;

}
