package io.sniffy.nio;

import org.junit.rules.ExternalResource;

/** JUnit 4 rule for the local static state commonly changed by NIO functional tests. */
public final class NioTestStateRule extends ExternalResource {

    private NioTestStateScope scope;

    @Override
    protected void before() {
        scope = new NioTestStateScope()
                .preserveSocketFaultInjection()
                .preserveConnectionsRegistry();
    }

    @Override
    protected void after() {
        try {
            scope.close();
        } catch (RuntimeException e) {
            throw e;
        } catch (Error e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError("Failed to restore NIO test state", e);
        }
    }
}
