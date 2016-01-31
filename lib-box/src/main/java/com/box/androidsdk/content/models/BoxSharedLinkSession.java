package com.box.androidsdk.content.models;

import java.util.ArrayList;

/**
 * Session created from a shared link.
 */
public class BoxSharedLinkSession extends BoxSession {

    String mSharedLink;
    String mPassword;

    protected ArrayList<OnSharedLinkResponseListener> mListeners = new ArrayList<OnSharedLinkResponseListener>();

    public BoxSharedLinkSession(String sharedLink, BoxSession session) {
        super(session);
        mSharedLink = sharedLink;
    }

    public String getSharedLink() {
        return mSharedLink;
    }

    public BoxSharedLinkSession setSharedLink(String sharedLink) {
        mSharedLink = sharedLink;
        return this;
    }

    public String getPassword() {
        return mPassword;
    }

    public BoxSharedLinkSession setPassword(String password) {
        mPassword = password;
        return this;
    }

    public synchronized BoxSharedLinkSession addOnSharedLinkResponseListener(OnSharedLinkResponseListener listener) {
        mListeners.add(listener);
        return this;
    }

    public interface OnSharedLinkResponseListener {
        public void onResponse(String uri, String password, Exception ex);
    }

}
