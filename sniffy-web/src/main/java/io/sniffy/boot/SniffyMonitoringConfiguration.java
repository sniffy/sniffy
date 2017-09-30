package io.sniffy.boot;

public @interface SniffyMonitoringConfiguration {

    String influxDbUrl() default "";

    String influxDbDatabase() default "sniffy";

    String influxDbUsername() default "";

    String influxDbPassword() default "";

}
