package io.sniffy.socket;

import io.sniffy.Spy;
import io.sniffy.Threads;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @since 3.1
 */
// TODO: support bytesUp, bytesDown, bytesTotal and port expectations once code generation is in place
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
        protected final AddressMatcher addressMatcher;

        public TcpExpectation(int min, int max, Threads threads, AddressMatcher addressMatcher) {
            this.min = min;
            this.max = max;
            this.threads = threads;
            this.addressMatcher = addressMatcher;
        }

        @Override
        public <T extends Spy<T>> Spy<T> verify(Spy<T> spy) throws TcpConnectionsExpectationError {

            Map<SocketMetaData, SocketStats> socketOperations = spy.getSocketOperations(threads, addressMatcher, true);

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
            if (max < min) throw new IllegalArgumentException("max cannot be less than min");
            return new TcpExpectation_Count(min, max);
        }

    }

    public static class TcpExpectation_Max extends TcpExpectation_Count {

        private TcpExpectation_Max(int max) {
            super(0, max);
        }

        public TcpExpectation_Count min(int min) {
            if (max < min) throw new IllegalArgumentException("max cannot be less than min");
            return new TcpExpectation_Count(min, max);
        }

    }

    public static class TcpExpectation_Count extends TcpExpectation {

        private TcpExpectation_Count(int min, int max) {
            super(min, max, Threads.CURRENT, AddressMatchers.anyAddressMatcher());
            if (min < 0) throw new IllegalArgumentException("min cannot be negative");
            if (max < min) throw new IllegalArgumentException("max cannot be less than min");
        }

        public TcpExpectation_Count_AddressMatcher host(String host) {
            return new TcpExpectation_Count_AddressMatcher(min, max, AddressMatchers.exactAddressMatcher(host));
        }

        /**
         * @since 3.1.10
         */
        public TcpExpectation_Count_AddressMatcher addressMatcher(AddressMatcher addressMatcher) {
            return new TcpExpectation_Count_AddressMatcher(min, max, addressMatcher);
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

    public static class TcpExpectation_Count_AddressMatcher extends TcpExpectation {

        private TcpExpectation_Count_AddressMatcher(int min, int max, AddressMatcher addressMatcher) {
            super(min, max, Threads.CURRENT, addressMatcher);
        }

        public TcpExpectation threads(Threads threads) {
            return new TcpExpectation(min, max, threads, addressMatcher);
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
            super(min, max, threads, AddressMatchers.anyAddressMatcher());
        }

        public TcpExpectation host(String host) {
            return new TcpExpectation(min, max, threads, AddressMatchers.exactAddressMatcher(host));
        }

        /**
         * @since 3.1.10
         */
        public TcpExpectation addressMatcher(AddressMatcher addressMatcher) {
            return new TcpExpectation(min, max, threads, addressMatcher);
        }

    }


}
