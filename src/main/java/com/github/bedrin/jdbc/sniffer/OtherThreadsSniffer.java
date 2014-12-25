package com.github.bedrin.jdbc.sniffer;

public class OtherThreadsSniffer {

    public static int executedStatements() {
        int executedStatements = Sniffer.executedStatements() - ThreadLocalSniffer.executedStatements();
        return executedStatements > 0 ? executedStatements : 0;
    }

    // TODO: this method should reset other threads' sniffers as well
    public static void reset() {
        Sniffer.reset();
        ThreadLocalSniffer.reset();
    }

    public static void verifyNotMore() {
        verifyNotMoreThan(0);
    }

    public static void verifyNotMoreThanOne() {
        verifyNotMoreThan(1);
    }

    public static void verifyNotMoreThan(int allowedStatements) throws IllegalStateException {
        verifyRange(0, allowedStatements);
    }

    public static void verifyExact(int allowedStatements) throws IllegalStateException {
        verifyRange(allowedStatements, allowedStatements);
    }

    public static void verifyNotLessThan(int allowedStatements) throws IllegalStateException {
        verifyRange(allowedStatements, Integer.MAX_VALUE);
    }

    public static void verifyRange(int minAllowedStatements, int maxAllowedStatements) throws IllegalStateException {
        int actualStatements = executedStatements();
        if (actualStatements > maxAllowedStatements)
            throw new IllegalStateException(String.format("Allowed not more than %d statements, but actually caught %d statements", maxAllowedStatements, actualStatements));
        if (actualStatements < minAllowedStatements)
            throw new IllegalStateException(String.format("Allowed not less than %d statements, but actually caught %d statements", minAllowedStatements, actualStatements));
        reset();
    }

}
