package com.box.androidsdk.content.mocks;

import com.box.androidsdk.content.models.BoxUser;

public class MockBoxUser extends BoxUser {
    private String mId;

    public void setId(String id) {
        this.mId = id;
    }

    @Override
    public String getId() {
        return mId;
    }

}
