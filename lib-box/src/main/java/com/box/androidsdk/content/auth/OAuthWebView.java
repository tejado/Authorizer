package com.box.androidsdk.content.auth;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.box.androidsdk.content.BoxConfig;
import com.box.sdk.android.R;
import com.box.androidsdk.content.utils.SdkUtils;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.Formatter;

/**
 * A WebView used for OAuth flow.
 */
public class OAuthWebView extends WebView {

    private static final String STATE = "state";
    private static final String URL_QUERY_LOGIN = "box_login";

    /**
     * A state string query param set when loading the OAuth url. This will be validated in the redirect url.
     */
    private String state;

    /**
     * An optional account email that should be prefilled in for the user if available.
     */
    private String mBoxAccountEmail;

    /**
     * Constructor.
     * 
     * @param context
     *            context
     * @param attrs
     *            attrs
     */
    public OAuthWebView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * State string. This string is optionally appended to the OAuth url query param. If appended, it will be returned as query param in the redirect url too.
     * You can then verify that the two strings are the same as a security check.
     */
    public String getStateString() {
        return state;
    }

    public void setBoxAccountEmail(final String boxAccountEmail){
        mBoxAccountEmail = boxAccountEmail;
    }

    /**
     * Start authentication.
     */
    public void authenticate(final String clientId, final String redirectUrl) {
        authenticate(buildUrl(clientId, redirectUrl));
    }

    /**
     *
     * @param authenticationUriBuilder A builder that is used to construct the url for authentication. This method is only necessary in advanced scenarios,
     *                          otherwise the default authetnicate(clientId, redirectUrl) method should be used.
     */
    public void authenticate(final Uri.Builder authenticationUriBuilder){
        state = SdkUtils.generateStateToken();
        authenticationUriBuilder.appendQueryParameter(STATE, state);
        loadUrl(authenticationUriBuilder.build().toString());
    }

    protected Uri.Builder buildUrl(String clientId, final String redirectUrl) {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https");
        builder.authority("app.box.com");
        builder.appendPath("api");
        builder.appendPath("oauth2");
        builder.appendPath("authorize");
        builder.appendQueryParameter("response_type", BoxApiAuthentication.RESPONSE_TYPE_CODE);
        builder.appendQueryParameter("client_id", clientId);
        builder.appendQueryParameter("redirect_uri", redirectUrl);
        if (mBoxAccountEmail != null){
            builder.appendQueryParameter(URL_QUERY_LOGIN, mBoxAccountEmail);
        }
        return builder;
    }



    /**
     * WebViewClient for the OAuth WebView.
     */
    public static class OAuthWebViewClient extends WebViewClient {

        private boolean sslErrorDialogContinueButtonClicked;

        private WebEventListener mWebEventListener;
        private String mRedirectUrl;
        private OnPageFinishedListener mOnPageFinishedListener;

        private static final int WEB_VIEW_TIMEOUT = 30000;
        private WebViewTimeOutRunnable mTimeOutRunnable;
        private Handler mHandler = new Handler(Looper.getMainLooper());

        /**
         * a state string query param set when loading the OAuth url. This will be validated in the redirect url.
         */
        private String state;

        /**
         * Constructor.
         *
         * @param eventListener
         *            listener to be notified when events happen on this webview
         * @param  redirectUrl
         *            (optional) redirect url, for validation only.
         * @param stateString
         *            a state string query param set when loading the OAuth url. This will be validated in the redirect url.
         */
        public OAuthWebViewClient(WebEventListener eventListener, String redirectUrl, String stateString) {
            super();
            this.mWebEventListener = eventListener;
            this.mRedirectUrl = redirectUrl;
            this.state = stateString;
        }

        @Override
        public void onPageStarted(final WebView view, final String url, final Bitmap favicon) {
            try {
                Uri uri = getURIfromURL(url);
                String code = getValueFromURI(uri, BoxApiAuthentication.RESPONSE_TYPE_CODE);
                String error = getValueFromURI(uri, BoxApiAuthentication.RESPONSE_TYPE_ERROR);

                if (!SdkUtils.isEmptyString(error)) {
                    mWebEventListener.onAuthFailure(new AuthFailure(AuthFailure.TYPE_USER_INTERACTION, null));
                } else if (!SdkUtils.isEmptyString(code)) {
                    String baseDomain = getValueFromURI(uri, BoxApiAuthentication.RESPONSE_TYPE_BASE_DOMAIN);
                    if (baseDomain != null){
                        mWebEventListener.onReceivedAuthCode(code, baseDomain);

                    } else {
                        mWebEventListener.onReceivedAuthCode(code);
                    }
                }
            } catch (InvalidUrlException e) {
                mWebEventListener.onAuthFailure(new AuthFailure(AuthFailure.TYPE_URL_MISMATCH, null));
            }
            if (mTimeOutRunnable != null){
                mHandler.removeCallbacks(mTimeOutRunnable);
            }
            mTimeOutRunnable = new WebViewTimeOutRunnable(view,url);
            mHandler.postDelayed(mTimeOutRunnable, WEB_VIEW_TIMEOUT);
        }

        @Override
        public void onPageFinished(final WebView view, final String url) {
            if (mTimeOutRunnable != null){
                mHandler.removeCallbacks(mTimeOutRunnable);
            }
            super.onPageFinished(view, url);
            if (mOnPageFinishedListener != null) {
                mOnPageFinishedListener.onPageFinished(view, url);
            }
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            if (mTimeOutRunnable != null){
                mHandler.removeCallbacks(mTimeOutRunnable);
            }
            if (mWebEventListener.onAuthFailure(new AuthFailure(new WebViewException(errorCode,description,failingUrl)))){
                return;
            }
            switch(errorCode){
                case ERROR_CONNECT:
                case ERROR_HOST_LOOKUP:
                    if (!SdkUtils.isInternetAvailable(view.getContext())){
                        String html = SdkUtils.getAssetFile(view.getContext(), "offline.html");
                        Formatter formatter = new Formatter();
                        formatter.format(html, view.getContext().getString(R.string.boxsdk_no_offline_access), view.getContext().getString(R.string.boxsdk_no_offline_access_detail),
                                view.getContext().getString(R.string.boxsdk_no_offline_access_todo));
                        view.loadData(formatter.toString(), "text/html", "UTF-8");
                        formatter.close();
                        break;
                    }
                case ERROR_TIMEOUT:
                    String html = SdkUtils.getAssetFile(view.getContext(), "offline.html");
                    Formatter formatter = new Formatter();
                    formatter.format(html, view.getContext().getString(R.string.boxsdk_unable_to_connect), view.getContext().getString(R.string.boxsdk_unable_to_connect_detail),
                            view.getContext().getString(R.string.boxsdk_unable_to_connect_todo));
                    view.loadData(formatter.toString(), "text/html", "UTF-8");
                    formatter.close();
                    break;

            }
            super.onReceivedError(view, errorCode, description, failingUrl);



        }

        @Override
        public void onReceivedHttpAuthRequest(final WebView view, final HttpAuthHandler handler, final String host, final String realm) {
            LayoutInflater factory = LayoutInflater.from(view.getContext());
            final View textEntryView = factory.inflate(R.layout.boxsdk_alert_dialog_text_entry, null);

            AlertDialog loginAlert = new AlertDialog.Builder(view.getContext()).setTitle(R.string.boxsdk_alert_dialog_text_entry).setView(textEntryView)
                .setPositiveButton(R.string.boxsdk_alert_dialog_ok, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(final DialogInterface dialog, final int whichButton) {
                        String userName = ((EditText) textEntryView.findViewById(R.id.username_edit)).getText().toString();
                        String password = ((EditText) textEntryView.findViewById(R.id.password_edit)).getText().toString();
                        handler.proceed(userName, password);
                    }
                }).setNegativeButton(R.string.boxsdk_alert_dialog_cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(final DialogInterface dialog, final int whichButton) {
                        handler.cancel();
                        mWebEventListener.onAuthFailure(new AuthFailure(AuthFailure.TYPE_USER_INTERACTION, null));
                    }
                }).create();
            loginAlert.show();
        }

        @Override
        public void onReceivedSslError(final WebView view, final SslErrorHandler handler, final SslError error) {
            if (mTimeOutRunnable != null){
                mHandler.removeCallbacks(mTimeOutRunnable);
            }
            Resources resources = view.getContext().getResources();
            StringBuilder sslErrorMessage = new StringBuilder(
                    resources.getString(R.string.boxsdk_There_are_problems_with_the_security_certificate_for_this_site));
            sslErrorMessage.append(" ");
            String sslErrorType;
            switch (error.getPrimaryError()) {
                case SslError.SSL_DATE_INVALID:
                    sslErrorType = view.getResources().getString(R.string.boxsdk_ssl_error_warning_DATE_INVALID);
                    break;
                case SslError.SSL_EXPIRED:
                    sslErrorType = resources.getString(R.string.boxsdk_ssl_error_warning_EXPIRED);
                    break;
                case SslError.SSL_IDMISMATCH:
                    sslErrorType = resources.getString(R.string.boxsdk_ssl_error_warning_ID_MISMATCH);
                    break;
                case SslError.SSL_NOTYETVALID:
                    sslErrorType = resources.getString(R.string.boxsdk_ssl_error_warning_NOT_YET_VALID);
                    break;
                case SslError.SSL_UNTRUSTED:
                    sslErrorType = resources.getString(R.string.boxsdk_ssl_error_warning_UNTRUSTED);
                    break;
                case SslError.SSL_INVALID:
                    sslErrorType = resources.getString(R.string.boxsdk_ssl_error_warning_INVALID);
                    break;
                default:
                    sslErrorType = resources.getString(R.string.boxsdk_ssl_error_warning_INVALID);
                    break;
            }
            sslErrorMessage.append(sslErrorType);
            sslErrorMessage.append(" ");
            sslErrorMessage.append(resources.getString(R.string.boxsdk_ssl_should_not_proceed));
            // Show the user a dialog to force them to accept or decline the SSL problem before continuing.
            sslErrorDialogContinueButtonClicked = false;
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(view.getContext()).setTitle(R.string.boxsdk_Security_Warning)
                    .setMessage(sslErrorMessage.toString()).setIcon(R.drawable.boxsdk_dialog_warning)
                    .setNegativeButton(R.string.boxsdk_Go_back, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(final DialogInterface dialog, final int whichButton) {
                            sslErrorDialogContinueButtonClicked = true;
                            handler.cancel();
                            mWebEventListener.onAuthFailure(new AuthFailure(AuthFailure.TYPE_USER_INTERACTION, null));
                        }
                    });

            // Only allow user to continue if explicitly granted in config
            if (BoxConfig.ALLOW_SSL_ERROR) {
                alertBuilder.setNeutralButton(R.string.boxsdk_ssl_error_details, null);
                alertBuilder.setPositiveButton(R.string.boxsdk_Continue, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int whichButton) {
                        sslErrorDialogContinueButtonClicked = true;
                        handler.proceed();
                    }
                });
            }

            final AlertDialog loginAlert = alertBuilder.create();
            loginAlert.setOnDismissListener(new OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (!sslErrorDialogContinueButtonClicked) {
                        mWebEventListener.onAuthFailure(new AuthFailure(AuthFailure.TYPE_USER_INTERACTION, null));
                    }
                }
            });
            loginAlert.show();
            if (BoxConfig.ALLOW_SSL_ERROR) {
                // this is to show more information on the exception.
                Button neutralButton = loginAlert.getButton(AlertDialog.BUTTON_NEUTRAL);
                if (neutralButton != null) {
                    neutralButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showCertDialog(view.getContext(), error);
                        }
                    });
                }
            }
        }

        protected void showCertDialog(final Context context, final SslError error){
            AlertDialog.Builder detailsBuilder = new AlertDialog.Builder(context).setTitle(R.string.boxsdk_Security_Warning).
                    setView(getCertErrorView(context, error.getCertificate()));
            detailsBuilder.create().show();
        }

        private View getCertErrorView(final Context context, final SslCertificate certificate){
            LayoutInflater factory = LayoutInflater.from(context);

            View certificateView = factory.inflate(
                    R.layout.ssl_certificate, null);

            // issued to:
            SslCertificate.DName issuedTo = certificate.getIssuedTo();
            if (issuedTo != null) {
                ((TextView) certificateView.findViewById(R.id.to_common))
                        .setText(issuedTo.getCName());
                ((TextView) certificateView.findViewById(R.id.to_org))
                        .setText(issuedTo.getOName());
                ((TextView) certificateView.findViewById(R.id.to_org_unit))
                        .setText(issuedTo.getUName());
            }


            // issued by:
            SslCertificate.DName issuedBy = certificate.getIssuedBy();
            if (issuedBy != null) {
                ((TextView) certificateView.findViewById(R.id.by_common))
                        .setText(issuedBy.getCName());
                ((TextView) certificateView.findViewById(R.id.by_org))
                        .setText(issuedBy.getOName());
                ((TextView) certificateView.findViewById(R.id.by_org_unit))
                        .setText(issuedBy.getUName());
            }

            // issued on:
            String issuedOn = formatCertificateDate(context, certificate.getValidNotBeforeDate());
            ((TextView) certificateView.findViewById(R.id.issued_on))
                    .setText(issuedOn);

            // expires on:
            String expiresOn = formatCertificateDate(context, certificate.getValidNotAfterDate());
            ((TextView) certificateView.findViewById(R.id.expires_on))
                    .setText(expiresOn);

            return certificateView;

        }

        private String formatCertificateDate(Context context, Date certificateDate) {
            if (certificateDate == null) {
                return "";
            }
            return DateFormat.getDateFormat(context).format(certificateDate);
        }

        /**
         * Destroy.
         */
        public void destroy() {
            mWebEventListener = null;
        }

        private Uri getURIfromURL(final String url) {
            Uri uri = Uri.parse(url);
            // In case redirect url is set. We only keep processing if current url matches redirect url.
            if (!SdkUtils.isEmptyString(mRedirectUrl)) {
                Uri redirectUri = Uri.parse(mRedirectUrl);
                if (redirectUri.getScheme() == null || !redirectUri.getScheme().equals(uri.getScheme()) || !redirectUri.getAuthority().equals(uri.getAuthority())) {
                    return null;
                }
            }
            return uri;
        }

        /**
         * Get response value.
         * 
         * @param uri uri from url
         * @param key key
         * @return response value
         * @throws InvalidUrlException
         */
        private String getValueFromURI(final Uri uri, final String key) throws InvalidUrlException {
            if (uri == null) {
                return null;
            }

            String value = null;

            try {
                value = uri.getQueryParameter(key);
            } catch (Exception e) {
                // uri cannot be parsed for query param.
            }
            if (!SdkUtils.isEmptyString(value)) {
                // Check state token
                if (!SdkUtils.isEmptyString(state)) {
                    String stateQ = uri.getQueryParameter(STATE);
                    if (!state.equals(stateQ)) {
                        throw new InvalidUrlException();
                    }

                }

            }
            return value;
        }

        public void setOnPageFinishedListener(OnPageFinishedListener listener) {
            this.mOnPageFinishedListener = listener;
        }

        public interface WebEventListener {

            /**
             * The failure that caused authentication to fail.
             * @param failure the failure that caused authentication to fail.
             * @return true if this is handled by the client and should not be handled by webview.
             */
            public boolean onAuthFailure(final AuthFailure failure);

            public void onReceivedAuthCode(final String code, final String baseDomain);

            public void onReceivedAuthCode(final String code);
        }


        class WebViewTimeOutRunnable implements Runnable {

             final String mFailingUrl;
             final WeakReference<WebView> mViewHolder;

                public WebViewTimeOutRunnable(final WebView view, final String failingUrl) {
                    mFailingUrl = failingUrl;
                    mViewHolder = new WeakReference<WebView>(view);
                }

                @Override
                public void run() {
                    onReceivedError(mViewHolder.get(), WebViewClient.ERROR_TIMEOUT, "loading timed out", mFailingUrl);

                }
        }


    }

    /**
     * Listener to listen to the event of a page load finishing.
     */
    public static interface OnPageFinishedListener {

        void onPageFinished(final WebView view, final String url);
    }

    /**
     * Exception indicating url validation failed.
     */
    private static class InvalidUrlException extends Exception {

        private static final long serialVersionUID = 1L;
    }

    /**
     * Class containing information of an authentication failure.
     */
    public static class AuthFailure {

        public static final int TYPE_USER_INTERACTION = 0;
        public static final int TYPE_URL_MISMATCH = 1;
        public static final int TYPE_WEB_ERROR = 2;

        public int type;
        public String message;
        public WebViewException mWebException;

        public AuthFailure(int failType, String failMessage) {
            this.type = failType;
            this.message = failMessage;
        }

        public AuthFailure(WebViewException exception){
            this(TYPE_WEB_ERROR, null);
            mWebException = exception;
        }


    }

    public static class WebViewException extends Exception {

        private final int mErrorCode;
        private final String mDescription;
        private final String mFailingUrl;

        public WebViewException(int errorCode, String description, String failingUrl){
            mErrorCode = errorCode;
            mDescription = description;
            mFailingUrl = failingUrl;
        }

        public int getErrorCode(){
            return mErrorCode;
        }

        public String getDescription(){
            return mDescription;
        }

        public String getFailingUrl(){
            return mFailingUrl;
        }
    }

}
