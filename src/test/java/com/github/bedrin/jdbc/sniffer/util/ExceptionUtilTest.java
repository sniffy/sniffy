package com.github.bedrin.jdbc.sniffer.util;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class ExceptionUtilTest {

    @Test(expected = IOException.class)
    public void testThrowException() {
        ExceptionUtil.throwException(new IOException());
    }

    @Test
    public void testAddSuppressed() {
        Exception ex = new Exception("Test Exception");
        Exception suppressed = new Exception("Suppressed Exception");
        ExceptionUtil.addSuppressed(ex, suppressed);
        assertEquals("Test Exception", ex.getMessage());
        assertEquals(1, ex.getSuppressed().length);
        assertEquals("Suppressed Exception", ex.getSuppressed()[0].getMessage());
    }

}