package io.sniffy;

import java.lang.ref.WeakReference;

/**
 * @since 3.1.10
 */
public class ThreadMetaData {

    private final long threadId;
    private final String threadName;
    private final String threadGroupName;
    private final WeakReference<Thread> threadReference;

    public ThreadMetaData(Thread thread) {
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
