package com.box.androidsdk.content.mocks;

import android.content.Context;

import com.box.androidsdk.content.auth.BoxAuthentication;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockAuthStorage extends BoxAuthentication.AuthStorage {

    private ConcurrentHashMap<String, BoxAuthentication.BoxAuthenticationInfo> map = new ConcurrentHashMap<String, BoxAuthentication.BoxAuthenticationInfo>();


    public ConcurrentHashMap<String, BoxAuthentication.BoxAuthenticationInfo> getAuthInfoMap() {
        return map;
    }

    @Override
    public void storeAuthInfoMap(Map<String, BoxAuthentication.BoxAuthenticationInfo> authInfo, Context context) {
        this.map = new ConcurrentHashMap(authInfo);
    }

    @Override
    public ConcurrentHashMap<String, BoxAuthentication.BoxAuthenticationInfo> loadAuthInfoMap(Context context) {
        return map;
    }
}

