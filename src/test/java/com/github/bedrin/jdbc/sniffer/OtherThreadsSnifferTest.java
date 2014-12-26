package com.github.bedrin.jdbc.sniffer;

import org.junit.Test;

import static org.junit.Assert.*;

public class OtherThreadsSnifferTest {

    @Test
    public void testVerifyExact() throws Exception {
        OtherThreadsSniffer.reset();
        Thread thread = new Thread(Sniffer::executeStatement);
        thread.start();
        thread.join();
        OtherThreadsSniffer.verifyExact(1);
    }
}