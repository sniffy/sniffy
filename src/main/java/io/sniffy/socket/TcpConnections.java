package io.sniffy.socket;

import io.sniffy.*;
import io.sniffy.util.Range;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// TODO: support bytesUp, bytesDown, bytesTotal and port expectations once code generation is in place
// TODO add validation to methods
public class TcpConnections {

    private TcpConnections() {

    }

    public static TcpExpectation_Count none() {
        return exact(0);
    }

    public static TcpExpectation_Count atMostOnce() {
        return between(0,1);
    }

    public static TcpExpectation_Count exact(int count) {
        return between(count, count);
    }

    public static TcpExpectation_Count between(int min, int max) {
        return new TcpExpectation_Count(min, max);
    }

    public static TcpExpectation_Min min(int min) {
        return new TcpExpectation_Min(min);
    }

    public static TcpExpectation_Max max(int max) {
        return new TcpExpectation_Max(max);
    }

    public static class TcpExpectation implements Spy.Expectation {

        protected final int min;
        protected final int max;
        protected final Threads threads;
        protected final String host;

        public TcpExpectation(SocketExpectation socketExpectation) {
            this(
                    Range.parse(socketExpectation.connections()).min,
                    Range.parse(socketExpectation.connections()).max,
                    socketExpectation.threads(),
                    "".equals(socketExpectation.hostName()) ? null : socketExpectation.hostName()
            );
        }

        protected TcpExpectation(int min, int max, Threads threads, String host) {
            this.min = min;
            this.max = max;
            this.threads = threads;
            this.host = host;
        }

        @Override
        public <T extends Spy<T>> Spy<T> verify(Spy<T> spy) throws WrongNumberOfQueriesError {

            Map<SocketMetaData, SocketStats> socketOperations = spy.getSocketOperations(threads, host, true);

            Set<Integer> connectionIds = new HashSet<Integer>();
            for (SocketMetaData socketMetaData : socketOperations.keySet()) {
                connectionIds.add(socketMetaData.connectionId);
            }

            int numConnections = connectionIds.size();

            if ((-1 != max && numConnections > max) || (-1 != min && numConnections < min)) {
                throw new TcpConnectionsExpectationError(this, socketOperations, numConnections);
            }

            return spy;

        }

    }

    public static class TcpExpectation_Min extends TcpExpectation_Count {

        private TcpExpectation_Min(int min) {
            super(min, Integer.MAX_VALUE);
        }

        public TcpExpectation_Count max(int max) {
            return new TcpExpectation_Count(min, max);
        }

    }

    public static class TcpExpectation_Max extends TcpExpectation_Count {

        private TcpExpectation_Max(int max) {
            super(0, max);
        }

        public TcpExpectation_Count min(int min) {
            return new TcpExpectation_Count(min, max);
        }

    }

    public static class TcpExpectation_Count extends TcpExpectation {

        private TcpExpectation_Count(int min, int max) {
            super(min, max, Threads.CURRENT, null);
        }

        public TcpExpectation_Count_Host host(String host) {
            return new TcpExpectation_Count_Host(min, max, host);
        }

        public TcpExpectation_Count_Threads threads(Threads threads) {
            return new TcpExpectation_Count_Threads(min, max, threads);
        }

        public TcpExpectation_Count_Threads currentThread() {
            return threads(Threads.CURRENT);
        }

        public TcpExpectation_Count_Threads otherThreads() {
            return threads(Threads.OTHERS);
        }

        public TcpExpectation_Count_Threads anyThreads() {
            return threads(Threads.ANY);
        }

    }

    public static class TcpExpectation_Count_Host extends TcpExpectation {

        private TcpExpectation_Count_Host(int min, int max, String host) {
            super(min, max, Threads.CURRENT, host);
        }

        public TcpExpectation threads(Threads threads) {
            return new TcpExpectation(min, max, threads, host);
        }

        public TcpExpectation currentThread() {
            return threads(Threads.CURRENT);
        }

        public TcpExpectation otherThreads() {
            return threads(Threads.OTHERS);
        }

        public TcpExpectation anyThreads() {
            return threads(Threads.ANY);
        }

    }

    public static class TcpExpectation_Count_Threads extends TcpExpectation {

        private TcpExpectation_Count_Threads(int min, int max, Threads threads) {
            super(min, max, threads, null);
        }

        public TcpExpectation host(String host) {
            return new TcpExpectation(min, max, threads, host);
        }

    }


}
