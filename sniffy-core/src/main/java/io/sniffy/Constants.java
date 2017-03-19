package io.sniffy;

import java.sql.Driver;

/**
 * Utility interface for storing the Sniffer constants
 */
public interface Constants {

    /**
     * Prefix to be used in JDBC URL, for example {@code sniffy:jdbc:h2:mem:}
     */
    String DRIVER_PREFIX = "sniffy:";

    /**
     * The major version of Sniffy
     * @see Driver#getMajorVersion()
     */
    int MAJOR_VERSION = 3;

    /**
     * The major version of Sniffy
     * @see Driver#getMinorVersion() ()
     */
    int MINOR_VERSION = 1;

    /**
     * The major version of Sniffy
     * @see Driver#getMinorVersion() ()
     */
    int PATCH_VERSION = 3;


}
