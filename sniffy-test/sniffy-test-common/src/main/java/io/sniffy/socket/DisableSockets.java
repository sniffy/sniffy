package io.sniffy.socket;

import java.lang.annotation.*;

/**
 * Disables network
 * @since 3.1.3
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
// TODO: support explicit list of hosts disallowed to connect
public @interface DisableSockets {
}
