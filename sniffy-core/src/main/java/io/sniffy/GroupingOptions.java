package io.sniffy;

/**
 * @since 3.1.10
 */
public class GroupingOptions {

    private final boolean groupByThread;
    private final boolean groupByStackTrace;
    private final boolean groupByConnection;

    public GroupingOptions(boolean groupByThread, boolean groupByStackTrace, boolean groupByConnection) {
        this.groupByThread = groupByThread;
        this.groupByStackTrace = groupByStackTrace;
        this.groupByConnection = groupByConnection;
    }

    public boolean isGroupByThread() {
        return groupByThread;
    }

    public boolean isGroupByStackTrace() {
        return groupByStackTrace;
    }

    public boolean isGroupByConnection() {
        return groupByConnection;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private boolean groupByThread;
        private boolean groupByStackTrace;
        private boolean groupByConnection;

        public Builder groupByThread(boolean groupByThread) {
            this.groupByThread = groupByThread;
            return this;
        }

        public Builder groupByStackTrace(boolean groupByStackTrace) {
            this.groupByStackTrace = groupByStackTrace;
            return this;
        }

        public Builder groupByConnection(boolean groupByConnection) {
            this.groupByConnection = groupByConnection;
            return this;
        }

        public GroupingOptions build() {
            return new GroupingOptions(groupByThread, groupByStackTrace, groupByConnection);
        }

    }

}
