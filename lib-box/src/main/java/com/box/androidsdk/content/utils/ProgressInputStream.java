package com.box.androidsdk.content.utils;

import com.box.androidsdk.content.listeners.ProgressListener;

import java.io.IOException;
import java.io.InputStream;

/**
 * An {@link java.io.InputStream} that can report the progress of reading from another InputStream to a
 * {@link com.box.androidsdk.content.listeners.ProgressListener}.
 */
public class ProgressInputStream extends InputStream {
    private final InputStream stream;
    private final ProgressListener listener;

    private long total;
    private long totalRead;
    private int progress;

    /**
     * Constructs a ProgressInputStream that wraps another InputStream.
     * @param  stream   the stream whose progress will be monitored.
     * @param  listener the listener that will receive progress updates.
     * @param  total    the total number of bytes that are expected to be read from the stream.
     */
    public ProgressInputStream(InputStream stream, ProgressListener listener, long total) {
        this.stream = stream;
        this.listener = listener;
        this.total = total;
    }

    /**
     * Gets the total number of bytes that are expected to be read from the stream.
     * @return the total number of bytes.
     */
    public long getTotal() {
        return this.total;
    }

    /**
     * Sets the total number of bytes that are expected to be read from the stream.
     * @param total the total number of bytes
     */
    public void setTotal(long total) {
        this.total = total;
    }

    @Override
    public void close() throws IOException {
        this.stream.close();
    }

    @Override
    public int read() throws IOException {
        int read = this.stream.read();
        this.totalRead++;
        this.listener.onProgressChanged(this.totalRead, this.total);

        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = this.stream.read(b, off, len);
        this.totalRead += read;
        this.listener.onProgressChanged(this.totalRead, this.total);

        return read;
    }
}
