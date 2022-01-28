package io.sniffy.socket;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @since 3.1
 */
public class SocketStats {

    public final AtomicLong elapsedTime = new AtomicLong();
    public final AtomicLong bytesDown = new AtomicLong();
    public final AtomicLong bytesUp = new AtomicLong();

    public SocketStats(SocketStats that) {
        accumulate(that);
    }

    public SocketStats(long elapsedTime, long bytesDown, long bytesUp) {
        this.elapsedTime.set(elapsedTime);
        this.bytesDown.set(Math.max(bytesDown, 0));
        this.bytesUp.set(Math.max(bytesUp, 0));
    }

    public void accumulate(long elapsedTime, long bytesDown, long bytesUp) {
        this.elapsedTime.addAndGet(elapsedTime);
        this.bytesDown.addAndGet(Math.max(bytesDown, 0));
        this.bytesUp.addAndGet(Math.max(bytesUp, 0));
    }

    public void accumulate(SocketStats that) {
        accumulate(that.elapsedTime.longValue(), that.bytesDown.intValue(), that.bytesUp.intValue());
    }

}
