package io.sniffy.socket;

import io.sniffy.Sniffy;
import io.sniffy.Spy;
import io.sniffy.Threads;
import org.junit.Test;

public class TcpExpectationTest extends BaseSocketTest {

    @Test
    public void testExactConnections() {
        try (Spy<?> s = Sniffy.expect(TcpConnections.exact(2))) {
            performSocketOperation();
            performSocketOperation();
        }
    }

    @Test(expected = TcpConnectionsExpectationError.class)
    public void testNone_Exception() {
        try (Spy<?> s = Sniffy.expect(TcpConnections.none())) {
            performSocketOperation();
        }
    }

    @Test(expected = TcpConnectionsExpectationError.class)
    public void testAtMostOneConnection_Exception() {
        try (Spy<?> s = Sniffy.expect(TcpConnections.atMostOnce())) {
            performSocketOperation();
            performSocketOperation();
        }
    }

    @Test(expected = TcpConnectionsExpectationError.class)
    public void testMinConnections_Exception() {
        try (Spy<?> s = Sniffy.expect(TcpConnections.min(2))) {
            performSocketOperation();
        }
    }

    @Test(expected = TcpConnectionsExpectationError.class)
    public void testMaxOneConnection_Exception() {
        try (Spy<?> s = Sniffy.expect(TcpConnections.max(1))) {
            performSocketOperation();
            performSocketOperation();
        }
    }

    @Test(expected = TcpConnectionsExpectationError.class)
    public void testMinMaxOneConnection_Exception() {
        try (Spy<?> s = Sniffy.expect(TcpConnections.min(1).max(2))) {
            performSocketOperation();
            performSocketOperation();
            performSocketOperation();
        }
    }

    @Test(expected = TcpConnectionsExpectationError.class)
    public void testMaxMinOneConnection_Exception() {
        try (Spy<?> s = Sniffy.expect(TcpConnections.max(2).min(1))) {
            performSocketOperation();
            performSocketOperation();
            performSocketOperation();
        }
    }

    @Test
    public void testExactConnectionsCurrentThread() throws InterruptedException {
        try (Spy<?> s = Sniffy.expect(TcpConnections.exact(2).otherThreads())) {
            performSocketOperationOtherThread();
            performSocketOperationOtherThread();
        }
    }

    @Test(expected = TcpConnectionsExpectationError.class)
    public void testExactConnectionsCurrentThread_Exception() throws InterruptedException {
        try (Spy<?> s = Sniffy.expect(TcpConnections.exact(2).otherThreads())) {
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
        try (Spy<?> s = Sniffy.expect(TcpConnections.exact(2).otherThreads().host("localhost"))) {
            performSocketOperationOtherThread();
            performSocketOperationOtherThread();
        }
    }

    @Test(expected = TcpConnectionsExpectationError.class)
    public void testExactConnectionsCurrentThreadHostName_Exception() throws InterruptedException {
        try (Spy<?> s = Sniffy.expect(TcpConnections.exact(2).otherThreads().host("google.com"))) {
            performSocketOperationOtherThread();
            performSocketOperationOtherThread();
        }
    }

    @Test(expected = TcpConnectionsExpectationError.class)
    public void testMinConnectionsHost_Exception() {
        try (Spy<?> s = Sniffy.expect(TcpConnections.min(2).host("google.com"))) {
            performSocketOperation();
            performSocketOperation();
        }
    }

    @Test
    public void testMinConnectionsCurrentThread() {
        try (Spy<?> s = Sniffy.expect(TcpConnections.min(2).currentThread())) {
            performSocketOperation();
            performSocketOperation();
        }
    }

    @Test
    public void testMinConnectionsAnyThreads() throws InterruptedException {
        try (Spy<?> s = Sniffy.expect(TcpConnections.min(3).anyThreads())) {
            performSocketOperation();
            performSocketOperation();
            performSocketOperationOtherThread();
        }
    }

    @Test
    public void testMinConnectionsHostThreads() throws InterruptedException {
        try (Spy<?> s = Sniffy.expect(TcpConnections.min(3).host("localhost:" + echoServerRule.getBoundPort()).threads(Threads.ANY))) {
            performSocketOperation();
            performSocketOperation();
            performSocketOperationOtherThread();
        }
    }

    @Test
    public void testMinConnectionsHostAnyThreads() throws InterruptedException {
        try (Spy<?> s = Sniffy.expect(TcpConnections.min(3).host("localhost:" + echoServerRule.getBoundPort()).anyThreads())) {
            performSocketOperation();
            performSocketOperation();
            performSocketOperationOtherThread();
        }
    }

    @Test
    public void testExactConnectionsHostCurrentThread() throws InterruptedException {
        try (Spy<?> s = Sniffy.expect(TcpConnections.exact(2).host("localhost:" + echoServerRule.getBoundPort()).currentThread())) {
            performSocketOperation();
            performSocketOperation();
            performSocketOperationOtherThread();
        }
    }

    @Test
    public void testExactConnectionsHostOtherThreads() throws InterruptedException {
        try (Spy<?> s = Sniffy.expect(TcpConnections.exact(1).host("localhost:" + echoServerRule.getBoundPort()).otherThreads())) {
            performSocketOperation();
            performSocketOperation();
            performSocketOperationOtherThread();
        }
    }

}
