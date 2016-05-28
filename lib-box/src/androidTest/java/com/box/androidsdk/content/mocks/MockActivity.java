package com.box.androidsdk.content.mocks;

import android.app.Activity;
import android.content.Context;

public class MockActivity extends Activity {
    private Context appContext;

    public MockActivity() {
        super();
    }

    public MockActivity(Context apContext) {
        this();
        appContext = apContext;
    }

    public Context getApplicationContext() {
        return appContext;
    }
}
