package io.sniffy.util;

public interface WrapperFactory<E,W> {

    W wrap(E delegate);

}
