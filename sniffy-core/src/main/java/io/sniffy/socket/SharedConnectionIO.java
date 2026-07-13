package io.sniffy.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Internal cross-module SPI used when a socket view delegates physical I/O ordering and lifecycle
 * to its owning NIO channel.
 *
 * <p>This type is public only because {@code sniffy-core} and {@code sniffy-module-nio} are separate
 * modules. It is not an application extension point, and application implementations are unsupported.
 * Classic socket monitoring deliberately does not implement this interface.</p>
 *
 * @since 3.2.0
 */
public interface SharedConnectionIO {

    int read(InputStream delegate) throws IOException;

    int read(InputStream delegate, byte[] bytes) throws IOException;

    int read(InputStream delegate, byte[] bytes, int offset, int length) throws IOException;

    void write(OutputStream delegate, int value) throws IOException;

    void write(OutputStream delegate, byte[] bytes) throws IOException;

    void write(OutputStream delegate, byte[] bytes, int offset, int length) throws IOException;

    void closeInputStream(InputStream delegate) throws IOException;

    void closeOutputStream(OutputStream delegate) throws IOException;
}
