package io.sniffy.tls;

import java.util.Map;

public class ScheduledThreadDump {

    public static void scheduleThreadDump(final int seconds) {

        Runnable r = new Runnable() {
            @Override
            public void run() {

                try {
                    Thread.sleep(seconds * 1000L);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.err.flush();
                System.err.println("Printing Java ThreadDump!");

                for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
                    System.err.println(entry.getKey() + " " + entry.getKey().getState());
                    for (StackTraceElement ste : entry.getValue()) {
                        System.err.println("\tat " + ste);
                    }
                    System.err.println();
                }

                System.err.flush();

            }
        };

        Thread t = new Thread(r);
        t.setDaemon(true);

        t.start();

    }

}
