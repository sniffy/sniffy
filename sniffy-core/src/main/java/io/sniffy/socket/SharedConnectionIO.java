package io.sniffy.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Optional bridge for socket views whose channel owns physical I/O ordering and lifecycle.
 * Classic socket monitoring does not implement this interface.
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
