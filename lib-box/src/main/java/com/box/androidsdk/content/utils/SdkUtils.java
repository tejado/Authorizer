package com.box.androidsdk.content.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.eclipsesource.json.JsonValue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SdkUtils {

    /**
     * Per OAuth2 specs, auth code exchange should include a state token for CSRF validation
     *
     * @return a randomly generated String to use as a state token
     */
    public static String generateStateToken() {
        return UUID.randomUUID().toString();
    }

    private static final int BUFFER_SIZE = 8192;
    public static void copyStream(final InputStream inputStream, final OutputStream outputStream) throws IOException,
            InterruptedException {
        // Read the rest of the stream and write to the destination OutputStream.
        final byte[] buffer = new byte[BUFFER_SIZE];
        int bufferLength = 0;
        Exception exception = null;
        try {
            while ((bufferLength = inputStream.read(buffer)) > 0) {
                if (Thread.currentThread().isInterrupted()) {
                    InterruptedException e = new InterruptedException();
                    throw e;
                }
                outputStream.write(buffer, 0, bufferLength);
            }
        } catch (Exception e){
            exception = e;
            if (exception instanceof IOException){
                throw (IOException)e;
            }
            if (exception instanceof InterruptedException){
                throw (InterruptedException)e;
            }
        } finally {
            // Try to flush the OutputStream and close InputStream.
            if (exception == null) {
                outputStream.flush();
            }
            inputStream.close();
        }
    }

    public static OutputStream createArrayOutputStream(final OutputStream[] outputStreams){
        return new OutputStream() {


            @Override
            public void close() throws IOException {
                for (OutputStream o : outputStreams){
                    o.close();
                }
                super.close();
            }

            @Override
            public void flush() throws IOException {
                for (OutputStream o : outputStreams){
                    o.flush();
                }
                super.flush();
            }


            @Override
            public void write(int oneByte) throws IOException {
                for (OutputStream o : outputStreams){
                    o.write(oneByte);
                }
            }




            @Override
            public void write(byte[] buffer) throws IOException {
                for (OutputStream o : outputStreams){
                    o.write(buffer);
                }
            }

            @Override
            public void write(byte[] buffer, int offset, int count) throws IOException {
                for (OutputStream o : outputStreams){
                    o.write(buffer, offset, count);
                }
            }
        };
    }



    public static boolean isEmptyString(String str) {
        return str == null || str.length() == 0;
    }

    public static boolean isBlank(String str) {
        return str == null || str.trim().length() == 0;
    }

    public static String sha1(final InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] bytes = new byte[8192];
        int byteCount;
        while ((byteCount = inputStream.read(bytes)) > 0) {
            md.update(bytes, 0, byteCount);
        }
        inputStream.close();
        return new String(encodeHex(md.digest()));
    }

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    private static char[] encodeHex(byte[] data) {
        int l = data.length;
        char[] out = new char[l << 1];
        // two characters form the hex value.
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = HEX_CHARS[(0xF0 & data[i]) >>> 4];
            out[j++] = HEX_CHARS[0x0F & data[i]];
        }
        return out;
    }


    /**
     * Parse a given JsonValue to a long regardless of whether that value is a String or a long.
     * @param value a JsonValue to parse to a long.
     * @return a long representation of the given value. Can throw a runtime exception (ParseException, UnsupportedOperationException, or NumberFormatException).
     */
    public static long parseJsonValueToLong(JsonValue value){
        try {
            return value.asLong();
        } catch (UnsupportedOperationException e){
            String s = value.asString().replace("\"","");
            return Long.parseLong(s);
        }
    }

    /**
     * Parse a given JsonValue to an int regardless of whether that value is a String or an int.
     * @param value a JsonValue to parse to an int.
     * @return an int representation of the given value. Can throw a runtime exception (ParseException, UnsupportedOperationException, or NumberFormatException).
     */
    public static long parseJsonValueToInteger(JsonValue value){
        try {
            return value.asInt();
        } catch (UnsupportedOperationException e){
            String s = value.asString().replace("\"","");
            return Integer.parseInt(s);
        }
    }


    public static String concatStringWithDelimiter(String[] strings, String delimiter) {
        StringBuilder sbr = new StringBuilder();
        int size = strings.length;
        for (int i = 0; i < size - 1; i++) {
            sbr.append(strings[i]).append(delimiter);
        }
        sbr.append(strings[size - 1]);
        return sbr.toString();
    }


    public static ThreadPoolExecutor createDefaultThreadPoolExecutor(int corePoolSize,
                                                              int maximumPoolSize,
                                                              long keepAliveTime,
                                                              TimeUnit unit) {
        return new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
                new ThreadFactory() {

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r);
                    }
                });
    }

    public static <T extends Object> T cloneSerializable(T source) {
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        ByteArrayInputStream bais = null;
        ObjectInputStream ois = null;
        try {
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(source);

            bais = new ByteArrayInputStream(baos.toByteArray());
            ois = new ObjectInputStream(bais);
            return (T) ois.readObject();
        } catch (IOException e) {
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        } finally {
            closeQuietly(baos, oos, bais, ois);
        }
    }

    public static String convertSerializableToString(Serializable obj) {
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        try {
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);

            return new String(baos.toByteArray());
        } catch (IOException e) {
            return null;
        } finally {
            closeQuietly(baos, oos);
            closeQuietly(oos);
        }
    }

    public static void closeQuietly(Closeable... closeables) {
        for (Closeable c : closeables) {
            try {
                c.close();;
            } catch (Exception e) {

            }
        }
    }

    /**
     * Recursively delete a folder and all its subfolders and files.
     *
     * @param f
     *            directory to be deleted.
     * @return True if the folder was deleted.
     */
    public static boolean deleteFolderRecursive(final File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files == null) {
                return false;
            }
            for (File c : files) {
                deleteFolderRecursive(c);
            }
        }
        return f.delete();
    }

    /**
     * Check for an internet connection.
     * @param context
     * @return whether or not there is a valid internet connection
     */
    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobile = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return wifi.isConnected() || (mobile != null &&  mobile.isConnected());
    }


    /**
     * Helper method for reading an asset file into a string.
     * @param context current context.
     * @param assetName the asset name
     * @return a string representation of a file in assets.
     */
    public static String getAssetFile(final Context context, final String assetName) {
        // if the file is not found create it and return that.
        // if we do not have a file we copy our asset out to create one.
        AssetManager assetManager = context.getAssets();
        BufferedReader in = null;
        try {
            StringBuilder buf = new StringBuilder();
            InputStream is = assetManager.open(assetName);
            in = new BufferedReader(new InputStreamReader(is));

            String str;
            boolean isFirst = true;
            while ( (str = in.readLine()) != null ) {
                if (isFirst)
                    isFirst = false;
                else
                    buf.append('\n');
                buf.append(str);
            }
            return buf.toString();
        } catch (IOException e) {
          BoxLogUtils.e("getAssetFile", assetName, e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e){
                BoxLogUtils.e("getAssetFile", assetName, e);
            }
        }
        // should never get here unless the asset file is inaccessible or cannot be copied out.
        return null;
    }
}
