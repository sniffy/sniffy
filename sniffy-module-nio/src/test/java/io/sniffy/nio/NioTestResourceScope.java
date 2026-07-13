package io.sniffy.nio;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/** Collects NIO test resources so one cleanup failure cannot skip the remaining teardown. */
final class NioTestResourceScope implements AutoCloseable {

    private final List<Closeable> closeables = new ArrayList<Closeable>();
    private final List<ExecutorService> executors = new ArrayList<ExecutorService>();
    private final List<Thread> workers = new ArrayList<Thread>();

    <T extends Closeable> T track(T closeable) {
        closeables.add(closeable);
        return closeable;
    }

    <T extends ExecutorService> T track(T executor) {
        executors.add(executor);
        return executor;
    }

    Thread track(Thread worker) {
        if (!worker.isDaemon()) {
            throw new IllegalArgumentException("NIO test workers must be daemon threads: " + worker.getName());
        }
        workers.add(worker);
        return worker;
    }

    @Override
    public void close() throws Exception {
        Throwable failure = null;

        for (int i = closeables.size() - 1; i >= 0; i--) {
            try {
                closeables.get(i).close();
            } catch (Throwable e) {
                failure = combine(failure, e);
            }
        }
        for (ExecutorService executor : executors) {
            executor.shutdownNow();
        }
        for (ExecutorService executor : executors) {
            try {
                if (!executor.awaitTermination(NioTestSupport.THREAD_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                    NioTestSupport.dumpThreads("NIO test executor did not terminate");
                    failure = combine(failure, new AssertionError("NIO test executor did not terminate"));
                }
            } catch (Throwable e) {
                failure = combine(failure, e);
            }
        }
        for (Thread worker : workers) {
            try {
                NioTestSupport.joinOrDumpAndFail(worker);
            } catch (Throwable e) {
                failure = combine(failure, e);
            }
        }

        if (failure != null) {
            if (failure instanceof Exception) throw (Exception) failure;
            if (failure instanceof Error) throw (Error) failure;
            throw new AssertionError(failure);
        }
    }

    private static Throwable combine(Throwable primary, Throwable secondary) {
        if (primary == null) return secondary;
        if (primary != secondary) primary.addSuppressed(secondary);
        return primary;
    }
}
