package com.box.androidsdk.content;

import com.box.androidsdk.content.models.BoxSession;

/**
 * Base class for the Box API endpoint classes.
 */
public class BoxApi {

    protected BoxSession mSession;

    protected String mBaseUri = BoxConstants.BASE_URI;
    protected String mBaseUploadUri = BoxConstants.BASE_UPLOAD_URI;

    /**
     * Constructs a BoxApi with the provided BoxSession.
     *
     * @param session   authenticated session to use with the BoxApi.
     */
    public BoxApi(BoxSession session) {
        this.mSession = session;
    }

    /**
     * Returns the base URI for the API.
     *
     * @return  base URI for the API.
     */
    protected String getBaseUri() {
        if (mSession != null && mSession.getAuthInfo() != null && mSession.getAuthInfo().getBaseDomain() != null){
            return String.format(BoxConstants.BASE_URI_TEMPLATE,mSession.getAuthInfo().getBaseDomain());
        }
        return mBaseUri;
    }

    /**
     * Returns the base URI for uploads for the API.
     *
     * @return  base upload URI.
     */
    protected String getBaseUploadUri() {
        if (mSession != null && mSession.getAuthInfo() != null && mSession.getAuthInfo().getBaseDomain() != null){
            return String.format(BoxConstants.BASE_UPLOAD_URI_TEMPLATE, mSession.getAuthInfo().getBaseDomain());
        }
        return mBaseUploadUri;
    }
}
