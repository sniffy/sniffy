package io.sniffy.socket;

import io.sniffy.Sniffer;
import io.sniffy.Spy;
import io.sniffy.test.SniffyAssertionError;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class TcpExpectationTest extends BaseSocketTest {

    @Test
    public void testExactConnections() {
        try (Spy<?> s = Sniffer.expect(TcpConnections.exact(2))) {
            performSocketOperation();
            performSocketOperation();
        }
    }

    @Test(expected = SniffyAssertionError.class)
    public void testAtMostOneConnection_Exception() {
        try (Spy<?> s = Sniffer.expect(TcpConnections.atMostOnce())) {
            performSocketOperation();
            performSocketOperation();
        }
    }

    @Test(expected = SniffyAssertionError.class)
    public void testMinConnections_Exception() {
        try (Spy<?> s = Sniffer.expect(TcpConnections.min(2))) {
            performSocketOperation();
        }
    }

    @Test(expected = SniffyAssertionError.class)
    public void testMaxOneConnection_Exception() {
        try (Spy<?> s = Sniffer.expect(TcpConnections.max(1))) {
            performSocketOperation();
            performSocketOperation();
        }
    }

    @Test(expected = SniffyAssertionError.class)
    public void testMinMaxOneConnection_Exception() {
        try (Spy<?> s = Sniffer.expect(TcpConnections.min(1).max(2))) {
            performSocketOperation();
            performSocketOperation();
            performSocketOperation();
        }
    }

    @Test(expected = SniffyAssertionError.class)
    public void testMaxMinOneConnection_Exception() {
        try (Spy<?> s = Sniffer.expect(TcpConnections.max(2).min(1))) {
            performSocketOperation();
            performSocketOperation();
            performSocketOperation();
        }
    }

    @Test
    public void testExactConnectionsCurrentThread() throws InterruptedException {
        try (Spy<?> s = Sniffer.expect(TcpConnections.exact(2).otherThreads())) {
            performSocketOperationOtherThread();
            performSocketOperationOtherThread();
        }
    }

    @Test(expected = SniffyAssertionError.class)
    public void testExactConnectionsCurrentThread_Exception() throws InterruptedException {
        try (Spy<?> s = Sniffer.expect(TcpConnections.exact(2).otherThreads())) {
            performSocketOperation();
            performSocketOperationOtherThread();
        }
    }

    private void performSocketOperationOtherThread() throws InterruptedException {
        Thread thread = new Thread(this::performSocketOperation);
        thread.start();
        thread.join();
    }

    @Test
    public void testExactConnectionsCurrentThreadHostName() throws InterruptedException {
        try (Spy<?> s = Sniffer.expect(TcpConnections.exact(2).otherThreads().host("localhost"))) {
            performSocketOperationOtherThread();
            performSocketOperationOtherThread();
        }
    }

    @Test(expected = SniffyAssertionError.class)
    public void testExactConnectionsCurrentThreadHostName_Exception() throws InterruptedException {
        try (Spy<?> s = Sniffer.expect(TcpConnections.exact(2).otherThreads().host("google.com"))) {
            performSocketOperationOtherThread();
            performSocketOperationOtherThread();
        }
    }

}
