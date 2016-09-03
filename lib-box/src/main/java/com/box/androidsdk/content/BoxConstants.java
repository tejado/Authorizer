package com.box.androidsdk.content;

public class BoxConstants {

    public static final String TAG = "BoxContentSdk";

    public static final String BASE_URI = "https://api.box.com/2.0";
    public static final String BASE_UPLOAD_URI = "https://upload.box.com/api/2.0";
    public static final String OAUTH_BASE_URI = "https://api.box.com";

    public static final String OAUTH_BASE_URI_TEMPLATE = "https://api.%s";
    public static final String BASE_URI_TEMPLATE = "https://api.%s/2.0";
    public static final String BASE_UPLOAD_URI_TEMPLATE = "https://upload.%s/api/2.0";


    public static final String FIELD_SIZE = "size";
    public static final String FIELD_CONTENT_CREATED_AT = "content_created_at";
    public static final String FIELD_CONTENT_MODIFIED_AT = "content_modified_at";
    public static final String FIELD_COMMENT_COUNT = "comment_count";

    public static final String ROOT_FOLDER_ID = "0";

    public static final String KEY_CLIENT_ID = "client_id";
    public static final String KEY_REDIRECT_URL = "redirect_uri";
    public static final String KEY_CLIENT_SECRET = "client_secret";
    public static final String KEY_BOX_DEVICE_ID = "device_id";
    public static final String KEY_BOX_DEVICE_NAME = "device_name";
    public static final String KEY_BOX_USERS = "boxusers";
    public static final String KEY_BOX_REFRESH_TOKEN_EXPIRES_AT = "box_refresh_token_expires_at";
    public static final String KEY_TOKEN = "token";

    public static final int HTTP_STATUS_TOO_MANY_REQUESTS = 429;

    public static final String REQUEST_BOX_APP_FOR_AUTH_INTENT_ACTION = "com.box.android.action.AUTHENTICATE_VIA_BOX_APP";

}
