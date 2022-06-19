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

                for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
                    System.out.println(entry.getKey() + " " + entry.getKey().getState());
                    for (StackTraceElement ste : entry.getValue()) {
                        System.out.println("\tat " + ste);
                    }
                    System.out.println();
                }

                System.out.flush();

            }
        };

        Thread t = new Thread(r);
        t.setDaemon(true);

        t.start();

    }

}
