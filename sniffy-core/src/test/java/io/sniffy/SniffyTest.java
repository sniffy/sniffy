package io.sniffy;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SniffyTest {

    @Before
    public void clearSpies() {
        Sniffy.registeredSpies.clear();
    }

    @Test
    public void hasSpiesFromOtherThreads() throws Exception {
        try (@SuppressWarnings("unused") Spy spy = Sniffy.spy()) {
            AtomicBoolean hasSpies = new AtomicBoolean();
            Thread thread = new Thread(() -> hasSpies.set(Sniffy.hasSpies()));
            thread.start();
            thread.join();
            assertTrue(hasSpies.get());
        }
    }

    @Test
    public void hasNotSpiesFromOtherThreads() throws Exception {
        try (@SuppressWarnings("unused") Spy spy = Sniffy.spyCurrentThread()) {
            AtomicBoolean hasSpies = new AtomicBoolean();
            Thread thread = new Thread(() -> hasSpies.set(Sniffy.hasSpies()));
            thread.start();
            thread.join();
            assertFalse(hasSpies.get());
        }
    }

}