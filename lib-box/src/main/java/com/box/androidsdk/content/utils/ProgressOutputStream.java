package com.box.androidsdk.content.utils;

import com.box.androidsdk.content.listeners.ProgressListener;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An {@link java.io.OutputStream} that can report the progress of writing to another OutputStream to a
 * {@link com.box.androidsdk.content.listeners.ProgressListener}.
 */
public class ProgressOutputStream extends OutputStream {
    private final OutputStream stream;
    private final ProgressListener listener;

    private long total;
    private long totalWritten;
    private int progress;

    /**
     * Constructs a ProgressOutputStream that wraps another OutputStream.
     * @param  stream   the stream whose progress will be monitored.
     * @param  listener the listener that will receive progress updates.
     * @param  total    the total number of bytes that are expected to be read from the stream.
     */
    public ProgressOutputStream(OutputStream stream, ProgressListener listener, long total) {
        this.stream = stream;
        this.listener = listener;
        this.total = total;
    }

    /**
     * Returns the total number of bytes expected to be read from the stream.
     *
     * @return  the total number of bytes expected to be read from the stream.
     */
    public long getTotal() {
        return this.total;
    }

    /**
     * Sets the total number of bytes expected to be read from the stream.
     *
     * @param total the total number of bytes that are expected to be read from the stream.
     */
    public void setTotal(long total) {
        this.total = total;
    }

    @Override
    public void close() throws IOException {
        this.stream.close();
    }

    @Override
    public void write(byte[] b) throws IOException {
        this.stream.write(b);
        this.totalWritten += b.length;
        this.listener.onProgressChanged(this.totalWritten, this.total);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        this.stream.write(b, off, len);
        if (len < b.length) {
            this.totalWritten += len;
        } else {
            this.totalWritten += b.length;
        }
        this.listener.onProgressChanged(this.totalWritten, this.total);
    }

    @Override
    public void write(int b) throws IOException {
        this.stream.write(b);
        this.totalWritten++;
        this.listener.onProgressChanged(this.totalWritten, this.total);
    }
}
