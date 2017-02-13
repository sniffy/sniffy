package io.sniffy.socket;

import io.sniffy.SniffyAssertionError;
import io.sniffy.Threads;

import java.util.Map;

import static io.sniffy.util.StringUtil.LINE_SEPARATOR;

/**
 * @since 3.1
 */
public class TcpConnectionsExpectationError extends SniffyAssertionError {

    private final TcpConnections.TcpExpectation tcpExpectation;
    private final Map<SocketMetaData, SocketStats> socketOperations;
    private final int numConnections;

    public TcpConnectionsExpectationError(
            TcpConnections.TcpExpectation tcpExpectation,
            Map<SocketMetaData, SocketStats> socketOperations,
            int numConnections) {
        super(buildDetailsMessage(tcpExpectation, socketOperations, numConnections));
        this.tcpExpectation = tcpExpectation;
        this.socketOperations = socketOperations;
        this.numConnections = numConnections;
    }

    public TcpConnections.TcpExpectation getTcpExpectation() {
        return tcpExpectation;
    }

    public Map<SocketMetaData, SocketStats> getSocketOperations() {
        return socketOperations;
    }

    public int getNumConnections() {
        return numConnections;
    }

    private static String buildDetailsMessage(
            TcpConnections.TcpExpectation tcpExpectation,
            Map<SocketMetaData, SocketStats> socketOperations,
            int numConnections) {
        StringBuilder sb = new StringBuilder();
        sb.append("Expected between ").append(tcpExpectation.min).append(" and ").append(tcpExpectation.max);
        if (Threads.CURRENT == tcpExpectation.threads) {
            sb.append(" current thread");
        } else if (Threads.OTHERS == tcpExpectation.threads) {
            sb.append(" other threads");
        }
        if (null != tcpExpectation.host) {
            sb.append(" ").append(tcpExpectation.host).append(" ");
        }
        sb.append(" connections").append(LINE_SEPARATOR);

        sb.append("Observed ").append(numConnections).append(" connections instead:").append(LINE_SEPARATOR);
        for (Map.Entry<SocketMetaData, SocketStats> entry : socketOperations.entrySet()) {
            SocketMetaData socketMetaData = entry.getKey();
            SocketStats socketStats = entry.getValue();
            sb.
                    append("Connection #").
                    append(socketMetaData.connectionId).
                    append(" to ").
                    append(socketMetaData.address).
                    append(" sent ").
                    append(socketStats.bytesUp).
                    append(" bytes and received ").
                    append(socketStats.bytesDown);
        }
        return sb.toString();
    }

}
