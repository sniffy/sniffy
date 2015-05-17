package com.github.bedrin.jdbc.sniffer.sql;

import org.junit.Test;

import static org.junit.Assert.*;


public class QueryTest {

    @Test
    public void testParse() throws Exception {
        assertEquals(Query.Type.SELECT, Query.parse("SELECT ID, CITY, STATE FROM STATION;").type);
        assertEquals(Query.Type.INSERT, Query.parse("INSERT INTO STATS VALUES (13, 1, 57.4, 0.31); ").type);
        assertEquals(Query.Type.UPDATE, Query.parse("UPDATE STATS SET RAIN_I = RAIN_I + 0.01;").type);
        assertEquals(Query.Type.DELETE, Query.parse("DELETE FROM STATION WHERE LONG_W < 90;").type);
        assertEquals(Query.Type.OTHER, Query.parse("CREATE TABLE STATION \n" +
                "(ID INTEGER PRIMARY KEY, \n" +
                "CITY CHAR(20), \n" +
                "STATE CHAR(2), \n" +
                "LAT_N REAL, \n" +
                "LONG_W REAL);").type);
    }

}