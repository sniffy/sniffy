package io.sniffy.util;

/**
 * @since 3.1.7
 */
public interface WrapperFactory<E,W> {

    W wrap(E delegate);

}
