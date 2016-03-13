package io.sniffy.socket;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by bedrin on 13.03.2016.
 */
public class SnifferOutputStream extends OutputStream {

    private final OutputStream delegate;

    public SnifferOutputStream(OutputStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public void write(int b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        delegate.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

}
