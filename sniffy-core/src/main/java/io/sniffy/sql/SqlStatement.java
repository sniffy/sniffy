package io.sniffy.sql;

/**
 * Describes the type of SQL query
 * @since 3.1
 */
public enum SqlStatement implements SqlVerbMatcher {
    SELECT,
    INSERT,
    UPDATE,
    DELETE,
    MERGE,
    OTHER,
    SYSTEM,
    ANY;


    @Override
    public boolean matches(SqlStatement sqlStatement) {
        return null == sqlStatement || this == sqlStatement;
    }

    @Override
    public void describe(StringBuilder appendable) {
        switch(this) {
            case SELECT:
                appendable.append("selected");
                break;
            case INSERT:
                appendable.append("inserted");
                break;
            case UPDATE:
                appendable.append("updated");
                break;
            case MERGE:
                appendable.append("merged");
                break;
            case DELETE:
                appendable.append("deleted");
                break;
            default:
                appendable.append("returned / affected");
                break;
        }
    }

}
