/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import net.tjado.passwdsafe.lib.view.AbstractDialogClickListener;

/**
 * The PasswdSafeUtil class contains common helper methods
 */
@SuppressWarnings("SameParameterValue")
public class PasswdSafeUtil
{
    private static final String PACKAGE = "net.tjado.passwdsafe";
    public static final String NEW_INTENT = PACKAGE + ".action.NEW";
    public static final String VIEW_INTENT = PACKAGE + ".action.VIEW";

    public static final boolean DEBUG = BuildConfig.DEBUG;

    private static final String TAG = "AuthorizerUtil";

    private static boolean TESTING = false;

    /** Create an intent to open a URI */
    public static Intent createOpenIntent(Uri uri, String recToOpen)
    {
        Uri.Builder builder = uri.buildUpon();
        if (recToOpen != null) {
            builder.appendQueryParameter("recToOpen", recToOpen);
        }
        Intent intent = new Intent(VIEW_INTENT, builder.build());
        intent.setClassName(PACKAGE, PACKAGE + ".PasswdSafe");
        return intent;
    }

    /** Create an intent to create a new file */
    public static Intent createNewFileIntent(Uri dirUri)
    {
        Intent intent = new Intent(NEW_INTENT, dirUri);
        intent.setClassName(PACKAGE, PACKAGE + ".PasswdSafe");
        return intent;
    }

    /** Start the main activity for a package */
    public static void startMainActivity(String pkgName, Context ctx)
    {
        Intent intent = getMainActivityIntent(pkgName, ctx);
        if (intent != null) {
            ctx.startActivity(intent);
        }
    }

    /** Get the intent to start the main activity for a package */
    public static Intent getMainActivityIntent(String pkgName, Context ctx)
    {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(pkgName);
        PackageManager pm = ctx.getPackageManager();
        List<ResolveInfo> infos = pm.queryIntentActivities(intent, 0);
        if (infos == null) {
            return null;
        }
        for (ResolveInfo info: infos) {
            ActivityInfo actInfo = info.activityInfo;
            if ((actInfo != null) && (pkgName.equals(actInfo.packageName))) {
                intent.setComponent(new ComponentName(actInfo.packageName,
                                                      actInfo.name));
                if (!(ctx instanceof Activity)) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                return intent;
            }
        }
        return null;
    }

    /** Get the app title */
    public static String getAppTitle(Context ctx)
    {
        return ctx.getString(R.string.app_name);
    }

    /** Get the app's version */
    public static String getAppVersion(Context ctx)
    {
        PackageInfo info = getAppPackageInfo(ctx);
        if (info != null) {
            return info.versionName;
        }
        return "Unknown";
    }

    /** Get the package info for the app */
    public static PackageInfo getAppPackageInfo(Context ctx)
    {
        try {
            PackageManager pkgMgr = ctx.getPackageManager();
            return pkgMgr.getPackageInfo(ctx.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /**
     * Copy text to the clipboard
     */
    public static void copyToClipboard(String str, Context ctx)
    {
        try {
            ApiCompat.copyToClipboard(str, ctx);
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
        showFatalMsg(null, msg, activity, false);
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
            public final void onCancelClicked()
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

    public static void showErrorMsg(String msg, Context context)
    {
        AlertDialog.Builder dlg = new AlertDialog.Builder(context)
                .setTitle(PasswdSafeUtil.getAppTitle(context) + " - " +
                          context.getString(R.string.error))
                .setMessage(msg)
                .setCancelable(true)
                .setPositiveButton(R.string.close, null);
        dlg.show();
    }

    public static void showInfoMsg(String msg, Context context)
    {
        AlertDialog.Builder dlg = new AlertDialog.Builder(context)
                .setTitle(PasswdSafeUtil.getAppTitle(context) + " - " +
                          context.getString(R.string.info))
                .setMessage(msg)
                .setCancelable(true)
                .setPositiveButton(R.string.close, null);
        dlg.show();
    }

    /**
     * Get whether the app is running while testing
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isTesting()
    {
        return DEBUG && TESTING;
    }

    /**
     * Set whether the app is running while testing
     */
    public static void setIsTesting(boolean testing)
    {
        TESTING = DEBUG && testing;
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

    /* http://stackoverflow.com/a/20149601
    *  thanks to William T. Mallard <http://stackoverflow.com/users/933969/william-t-mallard> */
    public static Drawable scaleImage (Drawable image, float scaleFactor, Resources res) {

        if ((image == null) || !(image instanceof BitmapDrawable)) {
            return image;
        }

        Bitmap b = ((BitmapDrawable)image).getBitmap();

        int sizeX = Math.round(image.getIntrinsicWidth() * scaleFactor);
        int sizeY = Math.round(image.getIntrinsicHeight() * scaleFactor);

        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, sizeX, sizeY, false);

        image = new BitmapDrawable(res, bitmapResized);

        return image;

    }
}
