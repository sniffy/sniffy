package io.sniffy.nio;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.concurrent.locks.LockSupport;

import static org.junit.Assert.fail;

final class NioTestSupport {

    static final long THREAD_TIMEOUT_MILLIS = 5000L;

    private NioTestSupport() {
    }

    static Thread daemonThread(String name, Runnable task) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        return thread;
    }

    static void joinOrDumpAndFail(Thread thread) throws InterruptedException {
        joinOrDumpAndFail(thread, THREAD_TIMEOUT_MILLIS);
    }

    static void joinOrDumpAndFail(Thread thread, long timeoutMillis) throws InterruptedException {
        if (thread == null) return;
        thread.join(timeoutMillis);
        if (thread.isAlive()) {
            dumpThreads("Timed out waiting for " + thread.getName());
            thread.interrupt();
            thread.join(1000L);
            fail("Thread did not terminate: " + thread.getName() + " state=" + thread.getState());
        }
    }

    static void awaitStackFrame(Thread thread, String classNameFragment, String methodName) {
        long deadline = System.nanoTime() + THREAD_TIMEOUT_MILLIS * 1000000L;
        while (System.nanoTime() < deadline) {
            for (StackTraceElement frame : thread.getStackTrace()) {
                if (frame.getClassName().contains(classNameFragment)
                        && (methodName == null || methodName.equals(frame.getMethodName()))) {
                    return;
                }
            }
            if (!thread.isAlive()) break;
            LockSupport.parkNanos(1000000L);
        }
        dumpThreads("Thread did not reach " + classNameFragment + "." + methodName);
        fail("Expected stack frame was not reached by " + thread.getName()
                + "; state=" + thread.getState());
    }

    /**
     * A few integration tests must prove that the installed provider interoperates with the real
     * JDK selector. There is no public "now blocked in native select" signal, so those tests keep
     * the implementation-sensitive observation isolated here instead of spreading JDK names.
     */
    static void awaitRealSelectorBlocked(Thread thread) {
        awaitStackFrame(thread, "sun.nio.ch.", null);
    }

    /**
     * This integration test must exercise the real blocking socket-view accept so close reaches
     * the native JDK path. The JDK exposes no public "accept is blocked" signal, hence the single
     * implementation observation remains isolated here.
     */
    static void awaitRealServerSocketAcceptBlocked(Thread thread) {
        awaitStackFrame(thread, "ServerSocketChannel", "accept");
    }

    static void awaitCondition(String description, Condition condition) {
        long deadline = System.nanoTime() + THREAD_TIMEOUT_MILLIS * 1000000L;
        while (System.nanoTime() < deadline) {
            if (condition.isSatisfied()) return;
            LockSupport.parkNanos(1000000L);
        }
        dumpThreads("Timed out waiting for " + description);
        fail("Condition was not satisfied: " + description);
    }

    static void dumpThreads(String reason) {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        System.err.println("==== NIO concurrency diagnostic: " + reason + " ====");
        long[] deadlocked = bean.findDeadlockedThreads();
        System.err.println("Deadlocked thread ids: "
                + (deadlocked == null ? "none" : Arrays.toString(deadlocked)));
        ThreadInfo[] infos = bean.dumpAllThreads(true, true);
        for (ThreadInfo info : infos) {
            if (info == null) continue;
            System.err.println('"' + info.getThreadName() + '"'
                    + " id=" + info.getThreadId() + " state=" + info.getThreadState());
            if (info.getLockInfo() != null) {
                System.err.println("  waiting on " + info.getLockInfo()
                        + " owned by " + info.getLockOwnerName()
                        + " id=" + info.getLockOwnerId());
            }
            for (StackTraceElement frame : info.getStackTrace()) {
                System.err.println("    at " + frame);
                for (MonitorInfo monitor : info.getLockedMonitors()) {
                    if (monitor.getLockedStackFrame() != null
                            && monitor.getLockedStackFrame().equals(frame)) {
                        System.err.println("      - locked " + monitor);
                    }
                }
            }
            LockInfo[] synchronizers = info.getLockedSynchronizers();
            if (synchronizers.length > 0) {
                System.err.println("    Locked synchronizers:");
                for (LockInfo synchronizer : synchronizers) {
                    System.err.println("      - " + synchronizer);
                }
            }
        }
        System.err.println("==== End NIO concurrency diagnostic ====");
    }

    interface Condition {
        boolean isSatisfied();
    }
}
