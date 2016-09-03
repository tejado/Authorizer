package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.utils.ProgressInputStream;
import com.box.androidsdk.content.listeners.ProgressListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.zip.GZIPInputStream;


/**
 * Used to read HTTP responses from the Box API.
 *
 * <p>All responses from the REST API are read using this class or one of its subclasses. This class wraps {@link
 * HttpURLConnection} in order to provide a simpler interface that can automatically handle various conditions specific
 * to Box's API. When a response is constructed, it will throw a {@link com.box.androidsdk.content.BoxException} if the response from the API
 * was an error. Therefore every BoxAPIResponse instance is guaranteed to represent a successful response.</p>
 *
 * <p>This class usually isn't instantiated directly, but is instead returned after calling {@link BoxRequest#send}.
 * </p>
 */
public class BoxHttpResponse {
    private static final int BUFFER_SIZE = 8192;

    protected final HttpURLConnection mConnection;
    protected int mResponseCode;
    protected String mContentType;
    private String mBodyString;
    private String mContentEncoding;

    /**
     * The raw InputStream is the stream returned directly from HttpURLConnection.getInputStream(). We need to keep
     * track of this stream in case we need to access it after wrapping it inside another stream.
     */
    private InputStream rawInputStream;

    /**
     * The regular InputStream is the stream that will be returned by getBody(). This stream might be a GZIPInputStream
     * or a ProgressInputStream (or both) that wrap the raw InputStream.
     */
    private InputStream mInputStream;

    /**
     * Constructs a BoxAPIResponse using an HttpURLConnection.
     * @param  connection a connection that has already sent a request to the API.
     */
    public BoxHttpResponse(HttpURLConnection connection) {
        mConnection = connection;
        mInputStream = null;
    }

    /**
     * Open connection to the resource.
     *
     * @throws IOException
     */
    public void open() throws IOException {
        mConnection.connect();
        mContentType = mConnection.getContentType();
        mResponseCode = mConnection.getResponseCode();
        mContentEncoding = mConnection.getContentEncoding();
    }

    /**
     * Gets the response code returned by the API.
     * @return the response code returned by the API.
     */
    public int getResponseCode() {
        return this.mResponseCode;
    }

    /**
     * Gets the length of this response's body as indicated by the "Content-Length" header.
     * @return the length of the response's body.
     */
    public int getContentLength() {
        return mConnection.getContentLength();
    }


    public String getContentType() {
        return mContentType;
    }

    /**
     * Gets an InputStream for reading this response's body.
     * @return an InputStream for reading the response's body.
     */
    public InputStream getBody() throws BoxException {
        return this.getBody(null);
    }

    /**
     * Gets an InputStream for reading this response's body which will report its read progress to a ProgressListener.
     * @param  listener a listener for monitoring the read progress of the body.
     * @return an InputStream for reading the response's body.
     */
    public InputStream getBody(ProgressListener listener) throws BoxException {
        if (this.mInputStream == null) {
            String contentEncoding = mConnection.getContentEncoding();
            try {
                if (this.rawInputStream == null) {
                    this.rawInputStream = mConnection.getInputStream();
                }

                if (listener == null) {
                    this.mInputStream = this.rawInputStream;
                } else {
                    this.mInputStream = new ProgressInputStream(this.rawInputStream, listener,
                            this.getContentLength());
                }

                if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip")) {
                    this.mInputStream = new GZIPInputStream(this.mInputStream);
                }
                return mInputStream;
            } catch (IOException e) {
                throw new BoxException("Couldn't connect to the Box API due to a network error.", e);
            }
        }

        return this.mInputStream;
    }

    /**
     * Disconnects this response from the server and frees up any network resources. The body of this response can no
     * longer be read after it has been disconnected.
     */
    public void disconnect() throws BoxException {
        try {
            if (this.rawInputStream == null) {
                this.rawInputStream = mConnection.getInputStream();
            }

            // We need to manually read from the raw input stream in case there are any remaining bytes. There's a bug
            // where a wrapping GZIPInputStream may not read to the end of a chunked response, causing Java to not
            // return the connection to the connection pool.
            byte[] buffer = new byte[BUFFER_SIZE];
            int n = this.rawInputStream.read(buffer);
            while (n != -1) {
                n = this.rawInputStream.read(buffer);
            }
            this.rawInputStream.close();

            if (this.mInputStream != null) {
                this.mInputStream.close();
            }
        } catch (IOException e) {
            throw new BoxException("Couldn't finish closing the connection to the Box API due to a network error or "
                    + "because the stream was already closed.", e);
        }
    }

    /**
     * Returns a string representation of this response's body. This method is used when logging this response's body.
     * By default, it returns an empty string (to avoid accidentally logging binary data) unless the response contained
     * an error message.
     * @return a string representation of this response's body.
     */
    public String getStringBody() throws BoxException {
        if (mBodyString != null) {
            return mBodyString;
        }

        InputStream stream = null;
        try {
            stream = isErrorCode(this.mResponseCode) ?
                    mConnection.getErrorStream() :
                    mConnection.getInputStream();
            mBodyString = readStream(stream);
        } catch (IOException e) {
            throw new BoxException("Unable to get string body", e);
        }
        return mBodyString;
    }

    private String readStream(InputStream inputStream) throws IOException, BoxException {
        if (inputStream == null) {
            return null;
        }

        // Wrap in gzip stream for gzip content encoding
        InputStream stream = mContentEncoding != null && mContentEncoding.equalsIgnoreCase("gzip") ?
            new GZIPInputStream(inputStream) :
            inputStream;

        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[BUFFER_SIZE];

        try {
            InputStreamReader reader = new InputStreamReader(stream, "UTF-8");

            int read = reader.read(buffer, 0, BUFFER_SIZE);
            while (read != -1) {
                builder.append(buffer, 0, read);
                read = reader.read(buffer, 0, BUFFER_SIZE);
            }

            reader.close();
        } catch (IOException e) {
            throw new BoxException("Unable to read stream", e);
        }

        return builder.toString();
    }


    /**
     * Gets the HTTP URL connection for this response.
     *
     * @return  HTTP URL connection for this response.
     */
    public HttpURLConnection getHttpURLConnection(){
        return mConnection;
    }

    private static boolean isErrorCode(int responseCode) {
        return responseCode >= 400;
    }
}
