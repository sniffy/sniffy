package com.github.bedrin.jdbc.sniffer;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

import static org.junit.Assert.*;

public class MockDriverTest {

    @BeforeClass
    public static void loadDriver() throws ClassNotFoundException {
        Class.forName("com.github.bedrin.jdbc.sniffer.MockDriver");
    }

    @Test
    public void testRegisterDriver() {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        boolean found = false;
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver instanceof MockDriver) found = true;
        }
        assertTrue(found);
    }

    @Test
    public void testGetDriver() throws ClassNotFoundException, SQLException {
        Driver driver = DriverManager.getDriver("sniffer:jdbc:h2:~/test");
        assertNotNull(driver);
    }

    @Test
    public void testGetMockConnection() throws ClassNotFoundException, SQLException {
        Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
        assertNotNull(connection);
        assertEquals(MockConnection.class, connection.getClass());
    }

    @Test
    public void testExecuteStatement() throws ClassNotFoundException, SQLException {
        Connection connection = DriverManager.getConnection("sniffer:jdbc:h2:~/test", "sa", "sa");
        connection.createStatement().execute("SELECT 1 FROM DUAL");
        assertEquals(1, Sniffer.executedStatements());
        Sniffer.verifyNotMoreThanOne();
        Sniffer.verifyNotMore();
    }

}