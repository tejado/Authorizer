package com.box.androidsdk.content.auth;

import com.box.androidsdk.content.BoxApi;
import com.box.androidsdk.content.BoxConstants;
import com.box.androidsdk.content.models.BoxMDMData;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.requests.BoxRequest;
import com.box.androidsdk.content.utils.SdkUtils;

import java.util.Locale;

/**
 * Package protected on purpose. It's supposed to be used ONLY by BoxAuthentication class. Do NOT use it elsewhere.
 */
class BoxApiAuthentication extends BoxApi {

    final static String RESPONSE_TYPE_CODE = "code";
    final static String RESPONSE_TYPE_ERROR = "error";
    final static String RESPONSE_TYPE_BASE_DOMAIN = "base_domain";

    final static String REFRESH_TOKEN = "refresh_token";
    final static String GRANT_TYPE = "grant_type";
    final static String GRANT_TYPE_AUTH_CODE = "authorization_code";
    final static String GRANT_TYPE_REFRESH = "refresh_token";
    final static String OAUTH_TOKEN_REQUEST_URL = "%s/oauth2/token";
    final static String OAUTH_TOKEN_REVOKE_URL = "%s/oauth2/revoke";

    /**
     * Constructor.
     */
    BoxApiAuthentication(BoxSession account) {
        super(account);
        mBaseUri = BoxConstants.OAUTH_BASE_URI;
    }

    @Override
    protected String getBaseUri() {
        if (mSession != null && mSession.getAuthInfo() != null && mSession.getAuthInfo().getBaseDomain() != null){
            return String.format(BoxConstants.OAUTH_BASE_URI_TEMPLATE, mSession.getAuthInfo().getBaseDomain());
        }
        return super.getBaseUri();
    }

    /**
     * Refresh OAuth, to be called when OAuth expires.
     */
    BoxRefreshAuthRequest refreshOAuth(String refreshToken, String clientId, String clientSecret) {
        BoxRefreshAuthRequest request = new BoxRefreshAuthRequest(mSession, getTokenUrl(), refreshToken, clientId, clientSecret);
        return request;
    }

    /**
     * Create OAuth, to be called the first time session tries to authenticate.
     */
    BoxCreateAuthRequest createOAuth(String code, String clientId, String clientSecret) {
        BoxCreateAuthRequest request = new BoxCreateAuthRequest(mSession, getTokenUrl(), code, clientId, clientSecret);
        return request;
    }

    /**
     * Revoke OAuth, to be called when you need to logout a user/revoke authentication.
     */
    BoxRevokeAuthRequest revokeOAuth(String token, String clientId, String clientSecret) {
        BoxRevokeAuthRequest request = new BoxRevokeAuthRequest(mSession, getTokenRevokeUrl(), token, clientId, clientSecret);
        return request;
    }

    protected String getTokenUrl() { return String.format(Locale.ENGLISH, OAUTH_TOKEN_REQUEST_URL, getBaseUri()); }

    protected String getTokenRevokeUrl() { return String.format(Locale.ENGLISH, OAUTH_TOKEN_REVOKE_URL, getBaseUri()); }

    /**
     * A BoxRequest to refresh OAuth. Note this is package protected on purpose. Third party apps are not supposed to use this directly.
     */
    static class BoxRefreshAuthRequest extends BoxRequest<BoxAuthentication.BoxAuthenticationInfo, BoxRefreshAuthRequest> {

        public BoxRefreshAuthRequest(BoxSession session, final String requestUrl, String refreshToken, String clientId, String clientSecret) {
            super(BoxAuthentication.BoxAuthenticationInfo.class, requestUrl, session);
            mContentType = ContentTypes.URL_ENCODED;
            mRequestMethod = Methods.POST;
            mBodyMap.put(GRANT_TYPE, GRANT_TYPE_REFRESH);
            mBodyMap.put(REFRESH_TOKEN, refreshToken);
            mBodyMap.put(BoxConstants.KEY_CLIENT_ID, clientId);
            mBodyMap.put(BoxConstants.KEY_CLIENT_SECRET, clientSecret);
            if (session.getDeviceId() != null){
                setDevice(session.getDeviceId(), session.getDeviceName());
            }
            if (session.getRefreshTokenExpiresAt() != null) {
                setRefreshExpiresAt(session.getRefreshTokenExpiresAt());
            }
        }

        /**
         * Sets device id and device name. This is required for some enterprise users making use of the device pinning feature.
         * @param deviceId a device id that uniquely identifies the user's device.
         * @param deviceName optional. This
         * @return A BoxRequest to refresh OAuth.
         */
        public BoxRefreshAuthRequest setDevice(String deviceId, String deviceName) {
            if (!SdkUtils.isEmptyString(deviceId)) {
                mBodyMap.put(BoxConstants.KEY_BOX_DEVICE_ID, deviceId);
            }
            if (!SdkUtils.isEmptyString(deviceName)) {
                mBodyMap.put(BoxConstants.KEY_BOX_DEVICE_NAME, deviceName);
            }
            return this;
        }

        @Override
        public BoxAuthentication.BoxAuthenticationInfo send() throws BoxException {
            BoxAuthentication.BoxAuthenticationInfo info = super.send();
            info.setUser(mSession.getUser());
            return info;
        }

        /**
         * @param expiresAt the unix time in seconds when refresh token should expire. Must be less than 60 days from current time.
         * @return  A BoxRequest to refresh OAuth information.
         */
        public BoxRefreshAuthRequest setRefreshExpiresAt(long expiresAt) {
            mBodyMap.put(BoxConstants.KEY_BOX_REFRESH_TOKEN_EXPIRES_AT, Long.toString(expiresAt));
            return this;
        }
    }

    /**
     * A BoxRequest to create OAuth information. Note this is package protected on purpose. Third party apps are not supposed to use this directly.
     */
    static class BoxCreateAuthRequest extends BoxRequest<BoxAuthentication.BoxAuthenticationInfo, BoxCreateAuthRequest> {

        public BoxCreateAuthRequest(BoxSession session, final String requestUrl, String code, String clientId, String clientSecret) {
            super(BoxAuthentication.BoxAuthenticationInfo.class, requestUrl, session);
            mRequestMethod = Methods.POST;
            setContentType(ContentTypes.URL_ENCODED);
            mBodyMap.put(GRANT_TYPE, GRANT_TYPE_AUTH_CODE);
            mBodyMap.put(RESPONSE_TYPE_CODE, code);
            mBodyMap.put(BoxConstants.KEY_CLIENT_ID, clientId);
            mBodyMap.put(BoxConstants.KEY_CLIENT_SECRET, clientSecret);
            if (session.getDeviceId() != null){
                setDevice(session.getDeviceId(), session.getDeviceName());
            }
            if (session.getManagementData() != null){
                setMdmData(session.getManagementData());
            }
            if (session.getRefreshTokenExpiresAt() != null) {
                setRefreshExpiresAt(session.getRefreshTokenExpiresAt());
            }
        }

        /**
         * Sets MDM data that may be required by some enterprises depending on the application.
         * @param mdmData an object containing information that may be required for certain enterprise accounts to login.
         * @return  A BoxRequest to create OAuth information.
         */
        public BoxCreateAuthRequest setMdmData(final BoxMDMData mdmData) {
            if (mdmData != null) {
                mBodyMap.put(BoxMDMData.BOX_MDM_DATA, mdmData.toJson());
            }
            return this;
        }

        /**
         * Sets device id and device name. This is required for some enterprise users making use of the device pinning feature.
         * @param deviceId a device id that uniquely identifies the user's device.
         * @param deviceName optional. This
         * @return  A BoxRequest to create OAuth information.
         */
        public BoxCreateAuthRequest setDevice(String deviceId, String deviceName) {
            if (!SdkUtils.isEmptyString(deviceId)) {
                mBodyMap.put(BoxConstants.KEY_BOX_DEVICE_ID, deviceId);
            }
            if (!SdkUtils.isEmptyString(deviceName)) {
                mBodyMap.put(BoxConstants.KEY_BOX_DEVICE_NAME, deviceName);
            }
            return this;
        }

        /**
         * @param expiresAt the unix time in seconds when refresh token should expire. Must be less than 60 days from current time.
         * @return  A BoxRequest to create OAuth information.
         */
        public BoxCreateAuthRequest setRefreshExpiresAt(long expiresAt) {
            mBodyMap.put(BoxConstants.KEY_BOX_REFRESH_TOKEN_EXPIRES_AT, Long.toString(expiresAt));
            return this;
        }
    }

    /**
     * A BoxRequest to revoke OAuth. Note this is package protected on purpose. Third party apps are not supposed to use this directly.
     */
    static class BoxRevokeAuthRequest extends BoxRequest<BoxAuthentication.BoxAuthenticationInfo, BoxRevokeAuthRequest> {

        /**
         * Creates a request to revoke authentication (i.e. log out a user) with the default parameters.
         *
         * @param session   BoxSession to revoke token from.
         * @param token can be either access token or refresh token.
         * @param clientId  client id of the application.
         * @param clientSecret  client secret of the application.
         */
        public BoxRevokeAuthRequest(BoxSession session, final String requestUrl, String token, String clientId, String clientSecret) {
            super(BoxAuthentication.BoxAuthenticationInfo.class, requestUrl, session);
            mRequestMethod = Methods.POST;
            setContentType(ContentTypes.URL_ENCODED);
            mBodyMap.put(BoxConstants.KEY_CLIENT_ID, clientId);
            mBodyMap.put(BoxConstants.KEY_CLIENT_SECRET, clientSecret);
            mBodyMap.put(BoxConstants.KEY_TOKEN, token);
        }
    }
}
