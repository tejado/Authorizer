package com.box.androidsdk.content.auth;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.box.androidsdk.content.BoxApiUser;
import com.box.androidsdk.content.BoxConstants;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.BoxFutureTask.OnCompletedListener;
import com.box.androidsdk.content.auth.BoxApiAuthentication.BoxCreateAuthRequest;
import com.box.androidsdk.content.auth.BoxAuthentication.BoxAuthenticationInfo;
import com.box.androidsdk.content.auth.OAuthWebView.AuthFailure;
import com.box.androidsdk.content.auth.OAuthWebView.OAuthWebViewClient;
import com.box.androidsdk.content.models.BoxError;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxUser;
import com.box.androidsdk.content.requests.BoxResponse;
import com.box.androidsdk.content.utils.SdkUtils;
import com.box.sdk.android.R;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Activity for OAuth. Use this activity by using the intent from createOAuthActivityIntent method. On completion, this activity will put the parcelable
 * BoxAndroidClient into the activity result. In the case of failure, the activity result will be {@link android.app.Activity#RESULT_CANCELED} together will a error message in
 * the intent extra.
 */
public class OAuthActivity extends Activity implements ChooseAuthenticationFragment.OnAuthenticationChosen, OAuthWebViewClient.WebEventListener, OAuthWebView.OnPageFinishedListener {
    public static final int REQUEST_BOX_APP_FOR_AUTH_CODE = 1;
    public static final String AUTH_CODE = "authcode";
    public static final String USER_ID = "userId";
    public static final String EXTRA_USER_ID_RESTRICTION = "restrictToUserId";
    public static final String EXTRA_SESSION = "session";

    /**
     * An optional boolean that can be set when creating the intent to launch this activity. If set to true it
     * will go directly to login flow, otherwise UI will be shown to let the user choose an already authenticated account first.
     */
    public static final String EXTRA_DISABLE_ACCOUNT_CHOOSING = "disableAccountChoosing";

    public static final int AUTH_TYPE_WEBVIEW = 0;
    public static final int AUTH_TYPE_APP = 1;

    protected static final String LOGIN_VIA_BOX_APP = "loginviaboxapp";

    public static final String AUTH_INFO = "authinfo";

    private static final String CHOOSE_AUTH_TAG = "choose_auth";

    private String mClientId;
    private String mClientSecret;
    private String mDeviceId;
    private String mDeviceName;
    private String mRedirectUrl;
    protected OAuthWebView oauthView;
    protected OAuthWebViewClient oauthClient;
    private static Dialog dialog;
    private boolean mAuthWasSuccessful = false;
    private int authType = AUTH_TYPE_WEBVIEW;
    private BoxSession mSession;

    private AtomicBoolean apiCallStarted = new AtomicBoolean(false);
    private BroadcastReceiver mConnectedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION) && SdkUtils.isInternetAvailable(context)) {
                // if we are not showing a web page then redo the authentication.
                if ((oauthView != null) && (oauthView.getUrl() != null) &&
                    !oauthView.getUrl().startsWith("http")){
                    startOAuth();
                }
            }
        }
    };


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentView());
        registerReceiver(mConnectedReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));


        Intent intent = getIntent();
        mClientId = intent.getStringExtra(BoxConstants.KEY_CLIENT_ID);
        mClientSecret = intent.getStringExtra(BoxConstants.KEY_CLIENT_SECRET);
        mDeviceId = intent.getStringExtra(BoxConstants.KEY_BOX_DEVICE_ID);
        mDeviceName = intent.getStringExtra(BoxConstants.KEY_BOX_DEVICE_NAME);
        mRedirectUrl = intent.getStringExtra(BoxConstants.KEY_REDIRECT_URL);
        boolean loginViaBoxApp = intent.getBooleanExtra(LOGIN_VIA_BOX_APP, false);
        authType = loginViaBoxApp ? AUTH_TYPE_APP : AUTH_TYPE_WEBVIEW;
        apiCallStarted.getAndSet(false);
        mSession = (BoxSession)intent.getSerializableExtra(EXTRA_SESSION);
        if (mSession != null){
            mSession.setApplicationContext(getApplicationContext());
        } else {
            mSession = new BoxSession(this, null, mClientId, mClientSecret, mRedirectUrl);
            mSession.setDeviceId(mDeviceId);
            mSession.setDeviceName(mDeviceName);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (oauthView == null || !oauthView.getUrl().startsWith("http")) {
            startOAuth();
        }
    }

    /**
     * Callback method to be called when authentication code is received. The code will then be used to make an API call to create OAuth tokens.
     */
    public void onReceivedAuthCode(String code) {
        onReceivedAuthCode(code, null);
    }

    /**
     * Callback method to be called when authentication code is received along with a base domain. The code will then be used to make an API call to create OAuth tokens.
     */
    public void onReceivedAuthCode(String code, String baseDomain) {
        if (authType == AUTH_TYPE_WEBVIEW) {
            oauthView.setVisibility(View.INVISIBLE);
        }
        startMakingOAuthAPICall(code, baseDomain);
    }

    @Override
    public void finish() {
        clearCachedAuthenticationData();
        if (!mAuthWasSuccessful) {
            BoxAuthentication.getInstance().onAuthenticationFailure(null, null);
        }
        super.finish();
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        dismissSpinner();
    }

    /**
     * Callback method to be called when authentication failed.
     */
    public boolean onAuthFailure(AuthFailure failure) {
        if (failure.type == OAuthWebView.AuthFailure.TYPE_WEB_ERROR){
            if (failure.mWebException.getErrorCode() == WebViewClient.ERROR_CONNECT || failure.mWebException.getErrorCode() == WebViewClient.ERROR_HOST_LOOKUP || failure.mWebException.getErrorCode() == WebViewClient.ERROR_TIMEOUT){
                return false;
            }
            Resources resources = this.getResources();
            Toast.makeText(
                    this,
                    String.format("%s\n%s: %s", resources.getString(com.box.sdk.android.R.string.boxsdk_Authentication_fail), resources.getString(com.box.sdk.android.R.string.boxsdk_details),
                            failure.mWebException.getErrorCode() + " " + failure.mWebException.getDescription()), Toast.LENGTH_LONG).show();

        } else if (SdkUtils.isEmptyString(failure.message)) {
            Toast.makeText(this, R.string.boxsdk_Authentication_fail, Toast.LENGTH_LONG).show();
        } else {
            switch (failure.type) {
                case AuthFailure.TYPE_URL_MISMATCH:
                    Resources resources = this.getResources();
                    Toast.makeText(
                            this,
                            String.format("%s\n%s: %s", resources.getString(R.string.boxsdk_Authentication_fail), resources.getString(R.string.boxsdk_details),
                                    resources.getString(R.string.boxsdk_Authentication_fail_url_mismatch)), Toast.LENGTH_LONG).show();
                    break;
                default:
                    Toast.makeText(this, R.string.boxsdk_Authentication_fail, Toast.LENGTH_LONG).show();
            }
        }
        finish();
        return true;
    }

    protected int getContentView() {
        return R.layout.boxsdk_activity_oauth;
    }

    protected void startOAuth() {
        // Use already logged in accounts if not disabled in this activity and not already showing this fragment.
        if (authType != AUTH_TYPE_APP && !getIntent().getBooleanExtra(EXTRA_DISABLE_ACCOUNT_CHOOSING, false) && getFragmentManager().findFragmentByTag(CHOOSE_AUTH_TAG) == null){
            Map<String, BoxAuthenticationInfo> map = BoxAuthentication.getInstance().getStoredAuthInfo(this);
            if (SdkUtils.isEmptyString(getIntent().getStringExtra(EXTRA_USER_ID_RESTRICTION)) && map != null && map.size() > 0) {
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.oauth_container,ChooseAuthenticationFragment.createAuthenticationActivity(this), CHOOSE_AUTH_TAG);
                transaction.addToBackStack(CHOOSE_AUTH_TAG);
                transaction.commit();
            }
        }
        switch (authType) {
            case AUTH_TYPE_APP:
                Intent intent = getBoxAuthApp();
                if (intent != null) {
                    intent.putExtra(BoxConstants.KEY_CLIENT_ID, mClientId);
                    intent.putExtra(BoxConstants.KEY_REDIRECT_URL, mRedirectUrl);
                    if (!SdkUtils.isEmptyString(getIntent().getStringExtra(EXTRA_USER_ID_RESTRICTION))) {
                        intent.putExtra(EXTRA_USER_ID_RESTRICTION, getIntent().getStringExtra(EXTRA_USER_ID_RESTRICTION));
                    }
                    startActivityForResult(intent, REQUEST_BOX_APP_FOR_AUTH_CODE);
                    break;
                }
            case AUTH_TYPE_WEBVIEW:
                showSpinner();
                this.oauthView = createOAuthView();
                this.oauthClient = createOAuthWebViewClient(oauthView.getStateString());
                oauthClient.setOnPageFinishedListener(this);
                oauthView.setWebViewClient(oauthClient);
                if (mSession.getBoxAccountEmail() != null){
                    oauthView.setBoxAccountEmail(mSession.getBoxAccountEmail());
                }
                oauthView.authenticate(mClientId, mRedirectUrl);
                break;
            default:
        }
    }

    protected Intent getBoxAuthApp(){
        // ensure that the signature of the Box application has an official signature.
        Intent intent = new Intent(BoxConstants.REQUEST_BOX_APP_FOR_AUTH_INTENT_ACTION);
        List<ResolveInfo> infos = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY | PackageManager.GET_RESOLVED_FILTER);

        if (infos == null || infos.size() < 1){
            return null;
        }
        String officialBoxAppString = getResources().getString(R.string.boxsdk_box_app_signature);
        for (ResolveInfo info : infos){
            try {
                Signature[] signatures = getPackageManager().getPackageInfo(info.activityInfo.packageName, PackageManager.GET_SIGNATURES).signatures;
                if (officialBoxAppString.equals(signatures[0].toCharsString())){
                    intent.setPackage(info.activityInfo.packageName);
                    Map<String, BoxAuthenticationInfo> authenticatedMap = BoxAuthentication.getInstance().getStoredAuthInfo(this);
                    if (authenticatedMap != null && authenticatedMap.size() > 0){
                        ArrayList<String> authenticatedUsers = new ArrayList<String>(authenticatedMap.size());
                        for (Map.Entry<String, BoxAuthenticationInfo> set : authenticatedMap.entrySet()){
                            if (set.getValue().getUser() != null){
                                authenticatedUsers.add(set.getValue().getUser().toJson());
                            }
                        }
                        if (authenticatedUsers.size() > 0) {
                            intent.putStringArrayListExtra(BoxConstants.KEY_BOX_USERS, authenticatedUsers);
                        }
                    }

                    return intent;
                }
            } catch (Exception e){

            }
        }

        return null;
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().findFragmentByTag(CHOOSE_AUTH_TAG) != null){
            finish();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onAuthenticationChosen(BoxAuthenticationInfo authInfo) {
        if (authInfo != null){
            BoxAuthentication.getInstance().onAuthenticated(authInfo, OAuthActivity.this);
            dismissSpinnerAndFinishAuthenticate(authInfo);

        }
    }

    @Override
    public void onDifferentAuthenticationChosen() {
        Fragment fragment = getFragmentManager().findFragmentByTag(CHOOSE_AUTH_TAG);
        if (fragment != null){
            getFragmentManager().popBackStack();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (RESULT_OK == resultCode && REQUEST_BOX_APP_FOR_AUTH_CODE == requestCode) {
            String userId = data.getStringExtra(USER_ID);
            String authCode = data.getStringExtra(AUTH_CODE);
            if (SdkUtils.isBlank(authCode) && !SdkUtils.isBlank(userId)){
                Map<String, BoxAuthenticationInfo> authenticatedMap = BoxAuthentication.getInstance().getStoredAuthInfo(this);
                BoxAuthenticationInfo info = authenticatedMap.get(userId);
                if (info != null){
                    onAuthenticationChosen(info);
                } else {
                    onAuthFailure(new AuthFailure(AuthFailure.TYPE_USER_INTERACTION, ""));
                }
            } else if (!SdkUtils.isBlank(authCode)){
                startMakingOAuthAPICall(authCode, null);
            }
        } else if (resultCode == RESULT_CANCELED){
            finish();
        }
    }



    /**
     * Start to create OAuth after getting the code.
     * 
     * @param code
     *            OAuth 2 authorization code
     */
    protected void startMakingOAuthAPICall(final String code, final String baseDomain) {
        if (apiCallStarted.getAndSet(true)) {
            return;
        }
        showSpinner();
        mSession.getAuthInfo().setBaseDomain(baseDomain);
        new Thread(){
            public void run(){
                try {
                    BoxAuthenticationInfo sessionAuth = BoxAuthentication.getInstance().create(mSession, code).get();

                    String restrictedUserId = getIntent().getStringExtra(EXTRA_USER_ID_RESTRICTION);
                    if (!SdkUtils.isEmptyString(restrictedUserId) && !sessionAuth.getUser().getId().equals(restrictedUserId)){
                        // the user logged in as does not match the user id this activity was restricted to, treat this as a failure.
                        throw new RuntimeException("Unexpected user logged in. Expected "+ restrictedUserId + " received " + sessionAuth.getUser().getId());
                    }
                    dismissSpinnerAndFinishAuthenticate(sessionAuth);
                } catch (Exception e){
                    dismissSpinnerAndFailAuthenticate(getAuthCreationErrorString(e));
                }


            }
        }.start();
    }

    protected void dismissSpinnerAndFinishAuthenticate(final BoxAuthenticationInfo auth) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                dismissSpinner();
                Intent intent = new Intent();
                intent.putExtra(AUTH_INFO, auth);
                setResult(Activity.RESULT_OK, intent);
                mAuthWasSuccessful = true;
                finish();
            }

        });
    }

    protected void dismissSpinnerAndFailAuthenticate(final String error) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                dismissSpinner();
                Toast.makeText(OAuthActivity.this, error, Toast.LENGTH_LONG).show();
                setResult(Activity.RESULT_CANCELED);
                finish();
            }

        });
    }

    protected OAuthWebView createOAuthView() {
        OAuthWebView webview = (OAuthWebView) findViewById(getOAuthWebViewRId());
        webview.setVisibility(View.VISIBLE);
        webview.getSettings().setJavaScriptEnabled(true);
        return webview;
    }

    protected OAuthWebViewClient createOAuthWebViewClient(String optionalState) {
        return new OAuthWebViewClient(this, mRedirectUrl, optionalState);
    }

    protected int getOAuthWebViewRId() {
        return R.id.oauthview;
    }

    /**
     * If you don't need the dialog, just return null.
     */
    protected Dialog showDialogWhileWaitingForAuthenticationAPICall() {
        return ProgressDialog.show(this, getText(R.string.boxsdk_Authenticating), getText(R.string.boxsdk_Please_wait));
    }

    protected void showSpinner() {
        try {
            dialog = showDialogWhileWaitingForAuthenticationAPICall();
        } catch (Exception e) {
            // WindowManager$BadTokenException will be caught and the app would not display
            // the 'Force Close' message
            dialog = null;
            return;
        }
    }

    protected void dismissSpinner() {
        if (dialog != null && dialog.isShowing()) {
            try {
                dialog.dismiss();
            } catch (IllegalArgumentException e) {
                // In certain case dialog already disattached from window.
            }
            dialog = null;
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mConnectedReceiver);
        apiCallStarted.set(false);
        dismissSpinner();
        super.onDestroy();
    }

    /**
     * Create intent to launch OAuthActivity. Notes about redirect url parameter: If you already set redirect url in <a
     * href="https://cloud.app.box.com/developers/services">box dev console</a>, you should pass in the same redirect url or use null for redirect url. If you
     * didn't set it in box dev console, you should pass in a url. In case you don't have a redirect server you can simply use "http://localhost".
     * 
     * @param context
     *            context
     * @param clientId
     *            your box client id
     * @param clientSecret
     *            your box client secret
     * @param redirectUrl
     *            redirect url, if you already set redirect url in <a href="https://cloud.app.box.com/developers/services">box dev console</a>, leave this null
     *            or use the same url, otherwise this field is required. You can use "http://localhost" if you don't have a redirect server.
     * @param loginViaBoxApp Whether login should be handled by the installed box android app. Set this to true only when you are sure or want
     *                       to make sure user installed box android app and want to use box android app to login.
     * @return  intent to launch OAuthActivity.
     */
    public static Intent createOAuthActivityIntent(final Context context, final String clientId, final String clientSecret, String redirectUrl, boolean loginViaBoxApp) {
        Intent intent = new Intent(context, OAuthActivity.class);
        intent.putExtra(BoxConstants.KEY_CLIENT_ID, clientId);
        intent.putExtra(BoxConstants.KEY_CLIENT_SECRET, clientSecret);
        if (!SdkUtils.isEmptyString(redirectUrl)) {
            intent.putExtra(BoxConstants.KEY_REDIRECT_URL, redirectUrl);
        }
        intent.putExtra(LOGIN_VIA_BOX_APP, loginViaBoxApp);
        return intent;
    }

    /**
     * Create intent to launch OAuthActivity using information from the given session.
     * @param context
                 context
     * @param session the BoxSession to use to get parameters required to authenticate via this activity.
     * @param loginViaBoxApp Whether login should be handled by the installed box android app. Set this to true only when you are sure or want
     *                       to make sure user installed box android app and want to use box android app to login.
     * @return intent to launch OAuthActivity.
     */
    public static Intent createOAuthActivityIntent(final Context context, BoxSession session, boolean loginViaBoxApp){
        Intent intent = createOAuthActivityIntent(context, session.getClientId(), session.getClientSecret(), session.getRedirectUrl(), loginViaBoxApp);
        intent.putExtra(EXTRA_SESSION, session);
        if (!SdkUtils.isEmptyString(session.getUserId())) {
            intent.putExtra(EXTRA_USER_ID_RESTRICTION, session.getUserId());
        }
        return intent;
    }

    private String getAuthCreationErrorString(Exception e) {
        String error = OAuthActivity.this.getString(R.string.boxsdk_Authentication_fail);
        if (e != null) {
            if (e instanceof BoxException){
                BoxError boxError = ((BoxException) e).getAsBoxError();
                if (boxError != null){
                    if (((BoxException) e).getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN || ((BoxException) e).getResponseCode() ==  HttpURLConnection.HTTP_UNAUTHORIZED || boxError.getError().equals("unauthorized_device")   ) {
                        error += ":" + getResources().getText(R.string.boxsdk_Authentication_fail_forbidden) + "\n";
                    } else {
                        error += ":";
                    }
                    error += boxError.getErrorDescription();
                    return error;
                }
            }
            error += ":" + e;
        }
        return error;
    }


    private void clearCachedAuthenticationData() {
        if (oauthView != null) {
            oauthView.clearCache(true);
            oauthView.clearFormData();
            oauthView.clearHistory();
        }
        // wipe out cookies.
        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        deleteDatabase("webview.db");
        deleteDatabase("webviewCache.db");
        File cacheDirectory = getCacheDir();
        SdkUtils.deleteFolderRecursive(cacheDirectory);
        cacheDirectory.mkdir();
    }
}
