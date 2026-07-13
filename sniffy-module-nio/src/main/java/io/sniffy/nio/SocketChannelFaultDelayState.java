package io.sniffy.nio;

import io.sniffy.socket.SniffyNetworkConnection;

/**
 * Owns the short cross-direction accounting state used to calculate latency
 * fault cycles. The caller performs policy resolution and sleeping after this
 * collaborator releases {@link #lock} so physical reads and writes stay full duplex.
 */
final class SocketChannelFaultDelayState {

    private final Object lock;

    private int potentiallyBufferedInputBytes;
    private int potentiallyBufferedOutputBytes;
    private long lastReadThreadId;
    private long lastWriteThreadId;

    SocketChannelFaultDelayState(Object lock) {
        this.lock = lock;
    }

    int recordRead(int bytesDown) {
        int delayCycles = 0;
        synchronized (lock) {
            lastReadThreadId = Thread.currentThread().getId();
            if (lastReadThreadId == lastWriteThreadId) {
                potentiallyBufferedOutputBytes = 0;
            }
            potentiallyBufferedInputBytes -= bytesDown;
            if (potentiallyBufferedInputBytes < 0) {
                delayCycles = 1 + (-1 * potentiallyBufferedInputBytes)
                        / SniffyNetworkConnection.DEFAULT_TCP_WINDOW_SIZE;
                potentiallyBufferedInputBytes = SniffyNetworkConnection.DEFAULT_TCP_WINDOW_SIZE;
            }
        }
        return delayCycles;
    }

    int recordWrite(int bytesUp) {
        int delayCycles = 0;
        synchronized (lock) {
            lastWriteThreadId = Thread.currentThread().getId();
            if (lastReadThreadId == lastWriteThreadId) {
                potentiallyBufferedInputBytes = 0;
            }
            potentiallyBufferedOutputBytes -= bytesUp;
            if (potentiallyBufferedOutputBytes < 0) {
                delayCycles = 1 + (-1 * potentiallyBufferedOutputBytes)
                        / SniffyNetworkConnection.DEFAULT_TCP_WINDOW_SIZE;
                potentiallyBufferedOutputBytes = SniffyNetworkConnection.DEFAULT_TCP_WINDOW_SIZE;
            }
        }
        return delayCycles;
    }

    int getPotentiallyBufferedInputBytes() {
        synchronized (lock) {
            return potentiallyBufferedInputBytes;
        }
    }

    void setPotentiallyBufferedInputBytes(int potentiallyBufferedInputBytes) {
        synchronized (lock) {
            this.potentiallyBufferedInputBytes = potentiallyBufferedInputBytes;
        }
    }

    int getPotentiallyBufferedOutputBytes() {
        synchronized (lock) {
            return potentiallyBufferedOutputBytes;
        }
    }

    void setPotentiallyBufferedOutputBytes(int potentiallyBufferedOutputBytes) {
        synchronized (lock) {
            this.potentiallyBufferedOutputBytes = potentiallyBufferedOutputBytes;
        }
    }

    long getLastReadThreadId() {
        synchronized (lock) {
            return lastReadThreadId;
        }
    }

    void setLastReadThreadId(long lastReadThreadId) {
        synchronized (lock) {
            this.lastReadThreadId = lastReadThreadId;
        }
    }

    long getLastWriteThreadId() {
        synchronized (lock) {
            return lastWriteThreadId;
        }
    }

    void setLastWriteThreadId(long lastWriteThreadId) {
        synchronized (lock) {
            this.lastWriteThreadId = lastWriteThreadId;
        }
    }

}
