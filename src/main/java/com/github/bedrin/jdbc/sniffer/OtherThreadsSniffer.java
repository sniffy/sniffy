package com.github.bedrin.jdbc.sniffer;

public class OtherThreadsSniffer {

    public static int executedStatements() {
        int executedStatements = 0;
        Sniffer currentThreadSniffer = ThreadLocalSniffer.getSniffer();
        for (Sniffer sniffer : Sniffer.getThreadLocalSniffers()) {
            if (sniffer != currentThreadSniffer) {
                executedStatements += sniffer.executedStatementsImpl();
            }
        }
        return executedStatements;
    }

    public static void reset() {
        Sniffer currentThreadSniffer = ThreadLocalSniffer.getSniffer();
        for (Sniffer sniffer : Sniffer.getThreadLocalSniffers()) {
            if (sniffer != currentThreadSniffer) {
                sniffer.resetImpl();
            }
        }
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
