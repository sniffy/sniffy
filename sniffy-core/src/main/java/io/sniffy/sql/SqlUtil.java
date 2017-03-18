package io.sniffy.sql;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.MULTILINE;

public class SqlUtil {

    public static final Pattern NORMALIZE_IN_STATEMENT_PATTERN = Pattern.compile(
            "\\sin\\s*\\((\\s*\\?\\s*,\\s*)+\\?\\s*\\)", CASE_INSENSITIVE | MULTILINE
    );

    public static String normalizeInStatement(String sql) {
        return NORMALIZE_IN_STATEMENT_PATTERN.matcher(sql).replaceAll(" in (?)");
    }

    public static SqlStatement guessQueryType(String sql) {
        // TODO: some queries can start with "WITH" statement
        // TODO: move to StatementInvocationHandler

        for (int i = 0; i < sql.length(); i++) {
            if (!Character.isWhitespace(sql.charAt(i))) {
                String normalized;
                if (sql.length() > (i + 7)) {
                    normalized = sql.substring(i, i + 7).toLowerCase();
                    if (normalized.equals("select ")) {
                        return SqlStatement.SELECT;
                    } else if (normalized.equals("insert ")) {
                        return SqlStatement.INSERT;
                    } else if (normalized.equals("update ")) {
                        return SqlStatement.UPDATE;
                    } else if (normalized.equals("delete ")) {
                        return SqlStatement.DELETE;
                    }
                }
                if (sql.length() > (i + 6) && sql.substring(i, i + 6).toLowerCase().equals("merge ")) {
                    return SqlStatement.MERGE;
                }
            }
        }

        return SqlStatement.OTHER;
    }
}
