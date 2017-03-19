package io.sniffy.boot;

/**
 * @since 3.1.2
 */
public @interface SniffyAdvancedConfiguration {

    /**
     * @since 3.1.2
     */
    String topSqlCapacity() default "1024";

    /**
     * @since 3.1.2
     */
    String excludePattern() default "";

    /**
     * @since 3.1.2
     */
    String injectHtmlExcludePattern() default "";

}
