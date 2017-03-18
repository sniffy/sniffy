package io.sniffy.sql;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.MULTILINE;

public class SqlUtil {

    public static final Pattern NORMALIZE_IN_STATEMENT_PATTERN = Pattern.compile(
            "\\sin\\s*\\((\\s*\\?\\s*,\\s*)+\\?\\s*\\)", CASE_INSENSITIVE | MULTILINE
    );

    public static String normalizeInStatement(String sql) {
        return null == sql ? null :
                NORMALIZE_IN_STATEMENT_PATTERN.matcher(sql).replaceAll(" in (?)").intern();
    }

    public static SqlStatement guessQueryType(String sql) {
        // TODO: some queries can start with "WITH" statement
        // TODO: move to StatementInvocationHandler

        for (int i = 0; i < sql.length(); i++) {
            if (!Character.isWhitespace(sql.charAt(i))) {
                String normalized;
                if (sql.length() > (i + 7)) {
                    normalized = sql.substring(i, i + 7).toLowerCase();
                    if ("select ".equals(normalized)) {
                        return SqlStatement.SELECT;
                    } else if ("insert ".equals(normalized)) {
                        return SqlStatement.INSERT;
                    } else if ("update ".equals(normalized)) {
                        return SqlStatement.UPDATE;
                    } else if ("delete ".equals(normalized)) {
                        return SqlStatement.DELETE;
                    }
                }
                if (sql.length() > (i + 6) && sql.substring(i, i + 6).equalsIgnoreCase("merge ")) {
                    return SqlStatement.MERGE;
                }
            }
        }

        return SqlStatement.OTHER;
    }
}
