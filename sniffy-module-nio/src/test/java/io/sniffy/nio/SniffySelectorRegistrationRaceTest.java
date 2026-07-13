package io.sniffy.nio;

import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.channels.ClosedSelectorException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static io.sniffy.nio.NioTestSupport.*;

public class SniffySelectorRegistrationRaceTest {

    @BeforeClass public static void initializeAccess() throws Exception { JdkNioAccess.resolve(); }

    @Test public void closeWinsBeforeDelegateRegistration() throws Exception {
        assertCloseWins(SniffySelector.RegistrationPoint.BEFORE_DELEGATE_REGISTRATION);
    }

    @Test public void closeWinsAfterDelegateRegistration() throws Exception {
        assertCloseWins(SniffySelector.RegistrationPoint.AFTER_DELEGATE_REGISTRATION);
    }

    @Test public void closeWinsBeforeActivation() throws Exception {
        assertCloseWins(SniffySelector.RegistrationPoint.BEFORE_ACTIVATION);
    }

    @Test public void repeatedRegisterCloseRacesRetainNeitherLinksNorWrapperKeys() throws Exception {
        for (int i = 0; i < 20; i++) {
            assertCloseWins(SniffySelector.RegistrationPoint.AFTER_DELEGATE_REGISTRATION);
        }
    }

    @Test public void closeWaitsForFinalizedRegistrationPublicationAndRemovesIt() throws Exception {
        RaceFixture fixture = new RaceFixture(SniffySelector.RegistrationPoint.ACTIVATION_FINALIZED);
        try {
            fixture.startRegistration();
            fixture.awaitPause();
            fixture.startClose();
            fixture.awaitDelegateClosed();
            fixture.releaseRegistration();
            fixture.awaitFinished();

            SelectionKey returned = fixture.returned.get();
            assertNotNull(returned);
            assertFalse(returned.isValid());
            assertNull(fixture.channel.keyFor(fixture.selector));
            assertEquals(0, fixture.selector.activeLinkCount());
        } finally {
            fixture.close();
        }
    }

    @Test public void registrationStartingAfterCloseBeginsFailsImmediately() throws Exception {
        SelectorProvider provider = NioFunctionalTestEnvironment.originalProvider();
        final SniffySelector selector = new SniffySelector(
                provider, NioFunctionalTestEnvironment.openRawSelector());
        Pipe rawPipe = provider.openPipe();
        Pipe pipe = new SniffyPipe(provider, rawPipe);
        final Pipe.SourceChannel channel = pipe.source();
        final AtomicReference<Throwable> closeFailure = new AtomicReference<Throwable>();
        Thread closing = daemonThread("sniffy-close-before-registration", new Runnable() {
            @Override public void run() {
                try { selector.close(); } catch (Throwable e) { closeFailure.set(e); }
            }
        });
        channel.configureBlocking(false);
        try {
            synchronized (selector) {
                closing.start();
                awaitCondition("selector close to begin", new Condition() {
                    @Override public boolean isSatisfied() { return !selector.isOpen(); }
                });
                try {
                    // Call the selector registration entry directly so this exercises its own
                    // !isOpen check, rather than AbstractSelectableChannel's earlier check.
                    selector.register((AbstractSelectableChannel) channel, SelectionKey.OP_READ, null);
                    fail("Registration starting after close began must fail");
                } catch (ClosedSelectorException expected) {
                    // beginRegistration rejects this before ownership or delegate registration
                }
                assertEquals(0, selector.activeLinkCount());
                assertNull(channel.keyFor(selector));
            }
            joinOrDumpAndFail(closing);
            assertNull(closeFailure.get());
        } finally {
            selector.wakeup();
            try { channel.close(); } finally {
                try { pipe.sink().close(); } finally {
                    try { selector.close(); } finally {
                        joinOrDumpAndFail(closing);
                    }
                }
            }
        }
    }

    @Test public void registrationRacingWithBlockedSelectAndCloseCannotBecomeActive() throws Exception {
        RaceFixture fixture = new RaceFixture(SniffySelector.RegistrationPoint.BEFORE_DELEGATE_REGISTRATION);
        try {
            fixture.startSelection();
            fixture.awaitSelectionBlocked();
            fixture.startRegistration();
            fixture.awaitPause();
            fixture.startClose();
            fixture.releaseSelection();
            fixture.awaitDelegateClosed();
            fixture.releaseRegistration();
            fixture.awaitFinished();

            assertNull(fixture.returned.get());
            assertTrue(fixture.registrationFailure.get() instanceof ClosedSelectorException);
            assertNull(fixture.channel.keyFor(fixture.selector));
            assertEquals(0, fixture.selector.activeLinkCount());
        } finally {
            fixture.close();
        }
    }

    private static void assertCloseWins(SniffySelector.RegistrationPoint point) throws Exception {
        RaceFixture fixture = new RaceFixture(point);
        try {
            fixture.startRegistration();
            fixture.awaitPause();
            fixture.startClose();
            fixture.awaitDelegateClosed();
            fixture.releaseRegistration();
            fixture.awaitFinished();

            assertNull(fixture.returned.get());
            assertTrue(fixture.registrationFailure.get() instanceof ClosedSelectorException);
            assertNull(fixture.channel.keyFor(fixture.selector));
            assertEquals(0, fixture.selector.activeLinkCount());
        } finally {
            fixture.close();
        }
    }

    private static final class RaceFixture {
        private final SelectorProvider provider = NioFunctionalTestEnvironment.originalProvider();
        private final AbstractSelector delegate = NioFunctionalTestEnvironment.openRawSelector();
        private final PausingSelector selector;
        private final Pipe rawPipe = provider.openPipe();
        private final Pipe pipe = new SniffyPipe(provider, rawPipe);
        private final Pipe.SourceChannel channel = pipe.source();
        private final AtomicReference<SelectionKey> returned = new AtomicReference<SelectionKey>();
        private final AtomicReference<Throwable> registrationFailure = new AtomicReference<Throwable>();
        private final AtomicReference<Throwable> closeFailure = new AtomicReference<Throwable>();
        private final AtomicReference<Throwable> selectionFailure = new AtomicReference<Throwable>();
        private Thread registrationThread;
        private Thread closeThread;
        private Thread selectionThread;

        RaceFixture(SniffySelector.RegistrationPoint point) throws Exception {
            selector = new PausingSelector(provider, delegate, point);
            channel.configureBlocking(false);
        }

        void startRegistration() {
            registrationThread = daemonThread("sniffy-paused-registration", new Runnable() {
                @Override public void run() {
                    try { returned.set(channel.register(selector, SelectionKey.OP_READ)); }
                    catch (Throwable e) { registrationFailure.set(e); }
                }
            });
            registrationThread.start();
        }

        void startClose() {
            closeThread = daemonThread("sniffy-registration-race-close", new Runnable() {
                @Override public void run() {
                    try { selector.close(); } catch (Throwable e) { closeFailure.set(e); }
                }
            });
            closeThread.start();
        }

        void startSelection() {
            selectionThread = daemonThread("sniffy-registration-race-select", new Runnable() {
                @Override public void run() {
                    try { selector.select(); } catch (Throwable e) { selectionFailure.set(e); }
                }
            });
            selectionThread.start();
        }

        void awaitSelectionBlocked() throws Exception {
            assertTrue(selector.selectionEntered.await(5, TimeUnit.SECONDS));
        }
        void releaseSelection() { selector.releaseSelection.countDown(); }

        void awaitPause() throws Exception { assertTrue(selector.reached.await(5, TimeUnit.SECONDS)); }
        void releaseRegistration() { selector.release.countDown(); }
        void awaitDelegateClosed() {
            awaitCondition("delegate selector close", new Condition() {
                @Override public boolean isSatisfied() { return !delegate.isOpen(); }
            });
        }
        void awaitFinished() throws Exception {
            joinOrDumpAndFail(registrationThread);
            joinOrDumpAndFail(closeThread);
            joinOrDumpAndFail(selectionThread);
            assertNull(closeFailure.get());
        }
        void close() throws Exception {
            selector.release.countDown();
            selector.releaseSelection.countDown();
            selector.wakeup();
            try { channel.close(); } finally {
                try { pipe.sink().close(); } finally {
                    try { selector.close(); } finally {
                        try { delegate.close(); } finally {
                            joinOrDumpAndFail(registrationThread);
                            joinOrDumpAndFail(closeThread);
                            joinOrDumpAndFail(selectionThread);
                        }
                    }
                }
            }
        }
    }

    private static final class PausingSelector extends SniffySelector {
        private final RegistrationPoint pauseAt;
        private final CountDownLatch reached = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final CountDownLatch selectionEntered = new CountDownLatch(1);
        private final CountDownLatch releaseSelection = new CountDownLatch(1);

        PausingSelector(SelectorProvider provider, AbstractSelector delegate, RegistrationPoint pauseAt) {
            super(provider, delegate);
            this.pauseAt = pauseAt;
        }

        @Override void registrationPoint(RegistrationPoint point, SelectionKeyLink link) {
            if (point == pauseAt) {
                reached.countDown();
                try { release.await(); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); throw new AssertionError(e);
                }
            }
        }

        @Override void selectionPoint(SelectionPoint point) {
            if (point == SelectionPoint.BEFORE_DELEGATE_SELECT) {
                selectionEntered.countDown();
                try {
                    if (!releaseSelection.await(5, TimeUnit.SECONDS)) {
                        throw new AssertionError("Timed out waiting to release delegate selection");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            }
            super.selectionPoint(point);
        }
    }
}
