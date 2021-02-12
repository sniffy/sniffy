package io.sniffy;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @since 3.1.10
 */
public class ThreadMetaData {

    private final long threadId;
    private final String threadName;
    private final String threadGroupName;
    private final WeakReference<Thread> threadReference;

    private static Map<Thread, ThreadMetaData> CACHE = new WeakHashMap<Thread, ThreadMetaData>();
    private static ReadWriteLock CACHE_LOCK = new ReentrantReadWriteLock();

    public static ThreadMetaData create(Thread thread) {

        if (null == thread) return null;

        ThreadMetaData threadMetaData;

        Lock readLock = CACHE_LOCK.readLock();
        try {
            readLock.lock();
            threadMetaData = CACHE.get(thread);
        } finally {
            readLock.unlock();
        }

        if (null == threadMetaData) {
            Lock writeLock = CACHE_LOCK.writeLock();
            try {
                writeLock.lock();
                threadMetaData = CACHE.get(thread);
                if (null == threadMetaData) {
                    threadMetaData = new ThreadMetaData(thread);
                    CACHE.put(thread, threadMetaData);
                }
            } finally {
                writeLock.unlock();
            }
        }

        return threadMetaData;
    }

    private ThreadMetaData(Thread thread) {
        this.threadId = thread.getId();
        this.threadName = thread.getName();
        this.threadGroupName = null == thread.getThreadGroup() ? null : thread.getThreadGroup().getName();
        this.threadReference = new WeakReference<Thread>(thread);
    }

    public long getThreadId() {
        return threadId;
    }

    public String getThreadName() {
        return threadName;
    }

    public String getThreadGroupName() {
        return threadGroupName;
    }

    public WeakReference<Thread> getThreadReference() {
        return threadReference;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ThreadMetaData that = (ThreadMetaData) o;

        return threadId == that.threadId;
    }

    @Override
    public int hashCode() {
        return (int) (threadId ^ (threadId >>> 32));
    }

}
