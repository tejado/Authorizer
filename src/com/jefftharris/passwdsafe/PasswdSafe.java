package com.jefftharris.passwdsafe;

import java.security.Security;
import java.util.Iterator;
//import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.pwsafe.lib.file.PwsField;
import org.pwsafe.lib.file.PwsFile;
import org.pwsafe.lib.file.PwsFileFactory;
import org.pwsafe.lib.file.PwsRecord;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class PasswdSafe extends Activity {
    private static final String TAG = "PasswdSafe";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Security.removeProvider("BC");
            Security.addProvider(new BCProvider());

            TextView tv = new TextView(this);
            StringBuilder text = new StringBuilder();

            /*
            for (Provider prov : Security.getProviders())
            {
                for (Map.Entry<Object,Object> entry : prov.entrySet())
                {
                    text.append(entry.getKey() + ": " + entry.getValue() + "\n");
                }
            }
            */
            PwsFile pwsfile =
                PwsFileFactory.loadFile("/sdcard/test.psafe3",
                                        new StringBuilder("test123"));

            Iterator<PwsRecord> iter = pwsfile.getRecords();
            while (iter.hasNext()){
                PwsRecord rec = iter.next();

                Iterator<Integer> fielditer = rec.getFields();
                while (fielditer.hasNext()) {
                    PwsField field = rec.getField(fielditer.next());
                    text.append(field.getType() + ": " + field + "\n");
                }
            }

            pwsfile.dispose();


            setContentView(R.layout.main);
            tv = (TextView)findViewById(R.id.tv);
            tv.setText(text.toString());
            tv.invalidate();
            //ScrollView sv = new ScrollView(this);
            //sv.addView(tv);
        } catch (Exception e)
        {
            Log.e(TAG, "Exception", e);
            new AlertDialog.Builder(this)
            .setMessage(e.toString())
            .show();
        }
    }
}