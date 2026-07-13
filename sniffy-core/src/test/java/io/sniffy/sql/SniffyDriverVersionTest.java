package io.sniffy.sql;

import org.junit.Test;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

public class SniffyDriverVersionTest {

    @Test
    public void testReportsSniffy4Version() throws SQLException {
        Driver driver = DriverManager.getDriver("sniffy:jdbc:h2:mem:");

        assertEquals(4, driver.getMajorVersion());
        assertEquals(0, driver.getMinorVersion());
    }

}
