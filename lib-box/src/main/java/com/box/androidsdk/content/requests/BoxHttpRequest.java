package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.listeners.ProgressListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Class that represents an HTTP request.
 */
class BoxHttpRequest {

    protected final HttpURLConnection mUrlConnection;
    protected final ProgressListener mListener;

    /**
     * Constructs an HTTP request with the default parameters.
     *
     * @param url   URL to connect to.
     * @param method    method type for the HTTP request.
     * @param listener  progress listener for a long-running API call.
     * @throws IOException
     */
    public BoxHttpRequest(URL url, BoxRequest.Methods method, ProgressListener listener) throws IOException {
        mUrlConnection = (HttpURLConnection) url.openConnection();
        mUrlConnection.setRequestMethod(method.toString());
        mListener = listener;
    }

    /**
     * Adds an HTTP header to the request.
     *
     * @param key   the header key.
     * @param value the header value.
     * @return  request with the updated header.
     */
    public BoxHttpRequest addHeader(String key, String value) {
        mUrlConnection.addRequestProperty(key, value);
        return this;
    }

    /**
     * Sets the body for the HTTP request to the contents of an InputStream.
     *
     * @param body  InputStream to use for the contents of the body.
     * @return  request with the updated body input stream.
     * @throws IOException
     */
    public BoxHttpRequest setBody(InputStream body) throws IOException {
        mUrlConnection.setDoOutput(true);
        OutputStream output = mUrlConnection.getOutputStream();
        int b = body.read();
        while (b != -1) {
            output.write(b);
            b = body.read();
        }
        output.close();
        return this;
    }

    /**
     * Returns the URL connection for the request.
     *
     * @return  URL connection for the request.
     */
    public HttpURLConnection getUrlConnection() {
        return mUrlConnection;
    }

}
