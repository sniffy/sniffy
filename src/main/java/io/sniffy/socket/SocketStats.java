package io.sniffy.socket;

import java.util.concurrent.atomic.AtomicLong;

public class SocketStats {

    // TODO: should we store elapsedTimeUp, elapsedTimeDown, e.t.c separately ?
    public final AtomicLong elapsedTime = new AtomicLong();

    public final AtomicLong bytesDown = new AtomicLong();

    public final AtomicLong bytesUp = new AtomicLong();

}
