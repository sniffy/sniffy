package io.sniffy.socket;

import java.util.concurrent.atomic.AtomicLong;

public class SocketStats {

    public final AtomicLong elapsedTime = new AtomicLong();
    public final AtomicLong bytesDown = new AtomicLong();
    public final AtomicLong bytesUp = new AtomicLong();

    public SocketStats(SocketStats that) {
        inc(that);
    }

    public SocketStats(long elapsedTime, long bytesDown, long bytesUp) {
        this.elapsedTime.set(elapsedTime);
        this.bytesDown.set(bytesDown);
        this.bytesUp.set(bytesUp);
    }

    public void inc(SocketStats that) {
        elapsedTime.addAndGet(that.elapsedTime.get());
        bytesDown.addAndGet(that.bytesDown.get());
        bytesUp.addAndGet(that.bytesUp.get());
    }

    public SocketStats dec(SocketStats that) {
        SocketStats result = new SocketStats(this);
        result.elapsedTime.addAndGet(-1 * that.elapsedTime.get());
        result.bytesDown.addAndGet(-1 * that.bytesDown.get());
        result.bytesUp.addAndGet(-1 * that.bytesUp.get());
        return result;
    }

}
