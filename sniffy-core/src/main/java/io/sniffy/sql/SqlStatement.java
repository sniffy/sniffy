package io.sniffy.sql;

/**
 * Describes the type of SQL query
 * @since 3.1
 */
public enum SqlStatement {
    SELECT,
    INSERT,
    UPDATE,
    DELETE,
    MERGE,
    OTHER,
    SYSTEM,
    ANY
}
