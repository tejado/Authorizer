package com.box.androidsdk.content.mocks;

import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.auth.BoxAuthentication;

public class MockBoxSession extends BoxSession {
    static {
        BoxAuthentication.AuthStorage storage = BoxAuthentication.getInstance().getAuthStorage();
        if (!(storage instanceof MockAuthStorage)) {
            BoxAuthentication.getInstance().setAuthStorage(new MockAuthStorage());
        }
    }

    public MockBoxSession(BoxSession session) {
        super(session);
    }
}
