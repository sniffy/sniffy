package io.sniffy;

import io.sniffy.sql.SqlStatement;

/**
 * @since 2.2
 * @see SqlStatement
 */
@Deprecated
public enum Query {
    SELECT,
    INSERT,
    UPDATE,
    DELETE,
    MERGE,
    OTHER,
    SYSTEM,
    ANY
}
