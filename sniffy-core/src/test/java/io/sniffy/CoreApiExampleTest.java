package io.sniffy;

import io.sniffy.sql.SqlQueries;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class CoreApiExampleTest extends BaseTest {

    @Test
    public void testVerifyApi() throws SQLException {
        // tag::testVerifyApi[]
        Connection connection = DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa"); // <1>
        Spy<?> spy = Sniffy.spy(); // <2>
        connection.createStatement().execute("SELECT 1 FROM DUAL"); // <3>
        spy.verify(SqlQueries.atMostOneQuery()); // <4>
        spy.verify(SqlQueries.noneQueries().otherThreads()); // <5>
        // end::testVerifyApi[]
    }

    @Test
    public void testFunctionalApi() throws SQLException {
        // tag::testFunctionalApi[]
        final Connection connection = DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa"); // <1>
        Sniffy.execute(
                () -> connection.createStatement().execute("SELECT 1 FROM DUAL")
        ).verify(SqlQueries.atMostOneQuery()); // <2>
        // end::testFunctionalApi[]
    }

    @Test
    public void testResourceApi() throws SQLException {
        // tag::testResourceApi[]
        final Connection connection = DriverManager.getConnection("sniffy:jdbc:h2:mem:", "sa", "sa"); // <1>
        try (@SuppressWarnings("unused") Spy s = Sniffy. // <2>
                expect(SqlQueries.atMostOneQuery()).
                expect(SqlQueries.noneQueries().otherThreads());
             Statement statement = connection.createStatement()) {
            statement.execute("SELECT 1 FROM DUAL");
        }
        // end::testResourceApi[]
    }

}
