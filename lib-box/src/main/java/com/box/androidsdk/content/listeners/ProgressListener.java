package com.box.androidsdk.content.listeners;

/**
 * The listener interface for monitoring the progress of a long-running API call.
 */
public interface ProgressListener {

    /**
     * Invoked when the progress of the API call changes.
     * @param numBytes   the number of bytes completed.
     * @param totalBytes the total number of bytes.
     */
    void onProgressChanged(long numBytes, long totalBytes);
}
