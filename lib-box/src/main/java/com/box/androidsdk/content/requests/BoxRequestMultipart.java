package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.utils.BoxDateFormat;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.listeners.ProgressListener;
import com.box.androidsdk.content.utils.ProgressOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to make HTTP multipart requests to the Box API.
 *
 * <p>This class partially implements the HTTP multipart standard in order to upload files to Box. The body of this
 * request type cannot be set directly. Instead, it can be modified by adding multipart fields and setting file
 * contents. The body of multipart requests will not be logged since they are likely to contain binary data.</p>
 *
 */
class BoxRequestMultipart extends BoxHttpRequest {
    private static final Logger LOGGER = Logger.getLogger(BoxRequestMultipart.class.getName());
    private static final String BOUNDARY = "da39a3ee5e6b4b0d3255bfef95601890afd80709";
    private static final int BUFFER_SIZE = 8192;

    private final StringBuilder loggedRequest = new StringBuilder();

    private OutputStream outputStream;
    private InputStream inputStream;
    private String filename;
    private long fileSize;
    private Map<String, String> fields;
    private boolean firstBoundary;

    /**
     * Creates a multipart request with the default parameters.
     *
     * @param url   URL to connect to.
     * @param method    HTTP method for the request.
     * @param listener  ProgressListener for monitoring the progress of the request.
     * @throws IOException
     */
    public BoxRequestMultipart(URL url, BoxRequest.Methods method, ProgressListener listener) throws IOException{
        super(url, method, listener);

        this.fields = new HashMap<String, String>();
        this.firstBoundary = true;

        this.addHeader("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
    }

    /**
     * Adds or updates a multipart field in this request.
     * @param key   the field's key.
     * @param value the field's value.
     */
    public void putField(String key, String value) {
        this.fields.put(key, value);
    }

    /**
     * Adds or updates a multipart field in this request.
     * @param key   the field's key.
     * @param value the field's value.
     */
    public void putField(String key, Date value) {
        this.fields.put(key, BoxDateFormat.format(value));
    }

    /**
     * Sets the file contents of this request.
     * @param inputStream a stream containing the file contents.
     * @param filename    the name of the file.
     */
    public void setFile(InputStream inputStream, String filename) {
        this.inputStream = inputStream;
        this.filename = filename;
    }

    /**
     * Sets the file contents of this request.
     * @param inputStream a stream containing the file contents.
     * @param filename    the name of the file.
     * @param fileSize    the size of the file.
     */
    public void setFile(InputStream inputStream, String filename, long fileSize) {
        this.setFile(inputStream, filename);
        this.fileSize = fileSize;
    }

    /**
     * This method is unsupported in BoxRequestMultipart. Instead, the body should be modified via the {@code putField}
     * and {@code setFile} methods.
     * @param body  N/A
     * @throws UnsupportedOperationException this method is unsupported.
     */
    @Override
    public BoxHttpRequest setBody(InputStream body) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * This method is unsupported in BoxRequestMultipart. Instead, the body should be modified via the {@code putField}
     * and {@code setFile} methods.
     * @param body N/A
     * @throws UnsupportedOperationException this method is unsupported.
     */
    public void setBody(String body) {
        throw new UnsupportedOperationException();
    }


    protected void writeBody(HttpURLConnection connection, ProgressListener listener) throws BoxException{
        try {
            connection.setChunkedStreamingMode(0);
            connection.setDoOutput(true);
            this.outputStream = connection.getOutputStream();

            this.writePartHeader(new String[][] {{"name", "filename"}, {"filename", this.filename}},
                "application/octet-stream");

            OutputStream fileContentsOutputStream = this.outputStream;
            if (listener != null) {
                fileContentsOutputStream = new ProgressOutputStream(this.outputStream, listener, this.fileSize);
            }
            byte[] buffer = new byte[BUFFER_SIZE];
            int n = this.inputStream.read(buffer);
            while (n != -1) {
                fileContentsOutputStream.write(buffer, 0, n);
                n = this.inputStream.read(buffer);
            }

            if (LOGGER.isLoggable(Level.FINE)) {
                this.loggedRequest.append("<File Contents Omitted>");
            }

            for (Map.Entry<String, String> entry : this.fields.entrySet()) {
                this.writePartHeader(new String[][] {{"name", entry.getKey()}});
                this.writeOutput(entry.getValue());
            }

            this.writeBoundary();
        } catch (IOException e) {
            throw new BoxException("Couldn't connect to the Box API due to a network error.", e);
        }
    }

    protected void resetBody() throws IOException {
        this.firstBoundary = true;
        this.inputStream.reset();
        this.loggedRequest.setLength(0);
    }

    protected String bodyToString() {
        return this.loggedRequest.toString();
    }

    private void writeBoundary() throws IOException {
        if (!this.firstBoundary) {
            this.writeOutput("\r\n");
        }

        this.firstBoundary = false;
        this.writeOutput("--");
        this.writeOutput(BOUNDARY);
    }

    private void writePartHeader(String[][] formData) throws IOException {
        this.writePartHeader(formData, null);
    }

    private void writePartHeader(String[][] formData, String contentType) throws IOException {
        this.writeBoundary();
        this.writeOutput("\r\n");
        this.writeOutput("Content-Disposition: form-data");
        for (int i = 0; i < formData.length; i++) {
            this.writeOutput("; ");
            this.writeOutput(formData[i][0]);
            this.writeOutput("=\"");
            this.writeOutput(formData[i][1]);
            this.writeOutput("\"");
        }

        if (contentType != null) {
            this.writeOutput("\r\nContent-Type: ");
            this.writeOutput(contentType);
        }

        this.writeOutput("\r\n\r\n");
    }

    private void writeOutput(String s) throws IOException {
        this.outputStream.write(s.getBytes(Charset.forName("UTF-8")));
        if (LOGGER.isLoggable(Level.FINE)) {
            this.loggedRequest.append(s);
        }
    }

    private void writeOutput(int b) throws IOException {
        this.outputStream.write(b);
    }
}
