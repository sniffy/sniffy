package io.sniffy.sql;

import io.sniffy.BaseTest;
import io.sniffy.Sniffer;
import io.sniffy.Spy;
import io.sniffy.WrongNumberOfQueriesError;
import org.junit.Test;

public class SqlQueriesTest extends BaseTest {

    @Test
    public void testMinMaxQueries() {
        try (Spy $= Sniffer.expect(SqlQueries.min(2).max(3))) {
            executeStatement();
            executeStatement();
            executeStatement();
        }
    }

    @Test(expected = WrongNumberOfQueriesError.class)
    public void testMaxMinQueries_Exception() {
        try (Spy $= Sniffer.expect(SqlQueries.max(3).min(2))) {
            executeStatement();
        }
    }

}
