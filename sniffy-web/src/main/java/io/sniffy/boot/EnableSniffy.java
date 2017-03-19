package io.sniffy.boot;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * @since 3.1
 */
@Retention(value = java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(value = { java.lang.annotation.ElementType.TYPE })
@Documented
@Import({SniffySpringConfiguration.class})
public @interface EnableSniffy {

    String monitorJdbc() default "true";
    String monitorSocket() default "true";

    String topSqlCapacity() default "1024";

    String filterEnabled() default "true";
    String injectHtml() default "true";
    String excludePattern() default "";

}
