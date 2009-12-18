package com.jefftharris.passwdsafe;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class RecordView extends Activity
{
    private static final String TAG = "RecordView";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Log.i(TAG, "onCreate intent:" + getIntent());

        String fileName = intent.getData().getPath();
        String uuid = intent.getData().getQueryParameter("rec");
    }

}
