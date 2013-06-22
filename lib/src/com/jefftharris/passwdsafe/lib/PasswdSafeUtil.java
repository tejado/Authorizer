/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.lib;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.ClipboardManager;
import android.util.Log;
import android.widget.Toast;

import com.jefftharris.passwdsafe.lib.view.AbstractDialogClickListener;

/**
 * The PasswdSafeUtil class contains common helper methods
 */
public class PasswdSafeUtil
{
    public static final boolean DEBUG = false;
    private static final String TAG = "PasswdSafeUtil";

    /** Start the main activity for a package */
    public static void startMainActivity(String pkgName, Activity act)
    {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(pkgName);
        PackageManager pm = act.getPackageManager();
        List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo info: infos) {
            ActivityInfo actInfo = info.activityInfo;
            if ((actInfo != null) && (pkgName.equals(actInfo.packageName))) {
                intent.setComponent(new ComponentName(actInfo.packageName,
                                                      actInfo.name));
                act.startActivity(intent);
                return;
            }
        }
    }

    /** Get the app title */
    public static final String getAppTitle(Context ctx)
    {
        return ctx.getString(R.string.app_name);
    }

    /** Get the app's version */
    public static final String getAppVersion(Context ctx)
    {
        PackageInfo info = getAppPackageInfo(ctx);
        if (info != null) {
            return info.versionName;
        }
        return "Unknown";
    }

    /** Get the package info for the app */
    public static final PackageInfo getAppPackageInfo(Context ctx)
    {
        try {
            PackageManager pkgMgr = ctx.getPackageManager();
            return pkgMgr.getPackageInfo(ctx.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public static void copyToClipboard(String str, Context ctx)
    {
        try {
            ClipboardManager clipMgr = (ClipboardManager)
                ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            clipMgr.setText(str);
        } catch (Throwable e) {
            String err = ctx.getString(R.string.copy_clipboard_error,
                                       getAppTitle(ctx));
            Toast.makeText(ctx, err, Toast.LENGTH_LONG).show();
            Log.e(TAG, err + ": " + e.toString());
        }
    }

    public static void showFatalMsg(Throwable t, Activity activity)
    {
        showFatalMsg(t, t.toString(), activity, true);
    }

    public static void showFatalMsg(String msg, Activity activity)
    {
        showFatalMsg(null, msg, activity, true);
    }

    public static void showFatalMsg(String msg,
                                    Activity activity,
                                    boolean copyTrace)
    {
        showFatalMsg(null, msg, activity, copyTrace);
    }

    public static void showFatalMsg(Throwable t,
                                    String msg,
                                    Activity activity)
    {
        showFatalMsg(t, msg, activity, true);
    }

    public static void showFatalMsg(Throwable t,
                                    String msg,
                                    final Activity activity,
                                    boolean copyTrace)
    {
        if (copyTrace && (t != null)) {
            StringWriter writer = new StringWriter();
            t.printStackTrace(new PrintWriter(writer));
            String trace = writer.toString();
            Log.e(TAG, trace);
            copyToClipboard(trace, activity);
        }

        AbstractDialogClickListener dlgClick = new AbstractDialogClickListener()
        {
            @Override
            public final void onOkClicked(DialogInterface dialog)
            {
                activity.finish();
            }

            @Override
            public final void onCancelClicked(DialogInterface dialog)
            {
                activity.finish();
            }
        };

        AlertDialog.Builder dlg = new AlertDialog.Builder(activity)
        .setTitle(getAppTitle(activity) + " - " +
                  activity.getString(R.string.error))
        .setMessage(msg)
        .setCancelable(false)
        .setPositiveButton(
             copyTrace ? R.string.copy_trace_and_close : R.string.close,
             dlgClick)
        .setOnCancelListener(dlgClick);
        dlg.show();
    }

    /** Log a debug message at info level */
    public static void dbginfo(String tag, String msg)
    {
        if (DEBUG)
            Log.i(tag, msg);
    }

    /** Log a debug message and exception at info level */
    public static void dbginfo(String tag, Throwable t, String msg)
    {
        if (DEBUG)
            Log.i(tag, msg, t);
    }

    /** Log a formatted debug message at info level */
    public static void dbginfo(String tag, String fmt, Object... args)
    {
        if (DEBUG) {
            Log.i(tag, String.format(fmt, args));
        }
    }

    /** Log a formatted debug message and exception at info level */
    public static void dbginfo(String tag, Throwable t,
                               String fmt, Object... args)
    {
        if (DEBUG) {
            Log.i(tag, String.format(fmt, args), t);
        }
    }

    /** Log a debug message at verbose level */
    public static void dbgverb(String tag, String fmt, Object... args)
    {
        if (DEBUG)
            Log.v(tag, String.format(fmt, args));
    }
}
