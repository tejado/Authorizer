package com.box.androidsdk.content;

public class BoxConfig {

    /**
     * Flag for whether logging is enabled. This will log all requests and responses made by the SDK
     */
    public static boolean IS_LOG_ENABLED = false;

    /**
     * Flag for whether a user should be allowed to continue when there is an SSL error in the webview. Disabled by default.
     */
    public static boolean ALLOW_SSL_ERROR = false;

    /**
     * Flag for whether the app is currently run in debug mode. This is set by the {@link com.box.androidsdk.content.models.BoxSession}
     * object and is determined from the {@link android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE}
     */
    public static boolean IS_DEBUG = false;

    /**
     * Client id used for the OAuth flow
     */
    public static String CLIENT_ID = null;

    /**
     * Client secret used for the OAuth flow
     */
    public static String CLIENT_SECRET = null;

    /**
     * Sets whether sessions should authentivate with the Box Application if available.
     */
    public static boolean ENABLE_BOX_APP_AUTHENTICATION = false;

    /**
     * The redirect url used with OAuth flow
     */
    public static String REDIRECT_URL = "https://app.box.com/static/sync_redirect.html";

    /**
     * Device name used for the OAuth flow and refreshing
     */
    public static String DEVICE_NAME = null;

    /**
     * Device id used for the OAuth flow and refreshing
     */
    public static String DEVICE_ID = null;

}
