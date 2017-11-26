package io.sniffy;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertTrue;

public class SniffyUsageTest {

    @Test
    public void testOutputContainsLinkToWebSite() {

        PrintStream soutBackup = System.out;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos));
            SniffyUsage.main(new String[]{});
            assertTrue(new String(baos.toByteArray()).contains("sniffy.io"));
        } finally {
            System.setOut(soutBackup);
        }


    }

}