package com.microsoft.authenticate;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.HttpClient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * AuthorizationRequest performs an Authorization Request by launching a WebView Dialog that
 * displays the login and consent page and then, on a successful login and consent, performs an
 * async AccessToken request.
 */
class AuthorizationRequest implements ObservableOAuthRequest, OAuthRequestObserver {

    /**
     * OAuthDialog is a Dialog that contains a WebView. The WebView loads the passed in Uri, and
     * loads the passed in WebViewClient that allows the WebView to be observed (i.e., when a page
     * loads the WebViewClient will be notified).
     */
    private class OAuthDialog extends Dialog implements OnCancelListener {

        /**
         * AuthorizationWebViewClient is a static (i.e., does not have access to the instance that
         * created it) class that checks for when the end_uri is loaded in to the WebView and calls
         * the AuthorizationRequest's onEndUri method.
         */
        @SuppressWarnings("NullableProblems")
        private class AuthorizationWebViewClient extends WebViewClient {

            /**
             * The cookie manager
             */
            private final CookieManager mCookieManager;

            /**
             * The cookie keys
             */
            private final Set<String> mCookieKeys;

            /**
             * Default constructor
             */
            @SuppressWarnings("deprecation")
            public AuthorizationWebViewClient() {
                CookieSyncManager.createInstance(getContext());
                mCookieManager = CookieManager.getInstance();
                mCookieKeys = new HashSet<>();
            }

            /**
             * Call back used when a page is being started.
             *
             * This will check to see if the given URL is one of the end_uris/redirect_uris and
             * based on the query parameters the method will either return an error, or proceed with
             * an AccessTokenRequest.
             *
             * @param view {@link WebView} that this is attached to.
             * @param url of the page being started
             */
            @Override
            public void onPageFinished(final WebView view, final String url) {
                final Uri uri = Uri.parse(url);

                // only clear cookies that are on the logout domain.
                if (uri.getHost().equals(mOAuthConfig.getLogoutUri().getHost())) {
                    this.saveCookiesInMemory(mCookieManager.getCookie(url));
                }

                final Uri endUri = mOAuthConfig.getDesktopUri();
                final boolean isEndUri = UriComparator.INSTANCE.compare(uri, endUri) == 0;
                if (!isEndUri) {
                    return;
                }

                this.saveCookiesToPreferences();

                AuthorizationRequest.this.onEndUri(uri);
                OAuthDialog.this.dismiss();
            }

            /**
             * Callback when the WebView received an Error.
             *
             * This method will notify the listener about the error and dismiss the WebView dialog.
             *
             * @param view the WebView that received the error
             * @param errorCode the error code corresponding to a WebViewClient.ERROR_* value
             * @param description the String containing the description of the error
             * @param failingUrl the url that encountered an error
             */
            @Override
            public void onReceivedError(final WebView view,
                                        final int errorCode,
                                        final String description,
                                        final String failingUrl) {
                AuthorizationRequest.this.onError("", description, failingUrl);
                OAuthDialog.this.dismiss();
            }

            @Override
            public void onReceivedSslError(final WebView view, final SslErrorHandler handler,
                                           final SslError error) {
                // Android does not like the SSL certificate we use, because it has '*' in it. Proceed with the errors.
                handler.proceed();
            }

            /**
             * Saves cookies into this object
             * @param cookie The cookie list to save
             */
            private void saveCookiesInMemory(final String cookie) {
                // Not all URLs will have cookies
                if (TextUtils.isEmpty(cookie)) {
                    return;
                }

                final String[] pairs = TextUtils.split(cookie, "; ");
                for (final String pair : pairs) {
                    final int index = pair.indexOf(EQUALS);
                    final String key = pair.substring(0, index);
                    mCookieKeys.add(key);
                }
            }

            /**
             * Saves the cookies to the preferences
             */
            private void saveCookiesToPreferences() {
                SharedPreferences preferences =
                        getContext().getSharedPreferences(PreferencesConstants.FILE_NAME,
                                                          Context.MODE_PRIVATE);

                // If the application tries to login twice, before calling logout, there could
                // be a cookie that was sent on the first login, that was not sent in the second
                // login. So, read the cookies in that was saved before, and perform a union
                // with the new cookies.
                final String value = preferences.getString(PreferencesConstants.COOKIES_KEY, "");
                final String[] valueSplit = TextUtils.split(value, PreferencesConstants.COOKIE_DELIMITER);

                mCookieKeys.addAll(Arrays.asList(valueSplit));

                final Editor editor = preferences.edit();
                final String allValues = TextUtils.join(PreferencesConstants.COOKIE_DELIMITER, mCookieKeys);
                editor.putString(PreferencesConstants.COOKIES_KEY, allValues);
                editor.apply();

                // we do not need to hold on to the cookieKeys in memory anymore.
                // It could be garbage collected when this object does, but let's clear it now,
                // since it will not be used again in the future.
                mCookieKeys.clear();
            }
        }

        /** Uri to load */
        private final Uri mRequestUri;

        /**
         * Constructs a new OAuthDialog.
         *
         * @param requestUri to load in the WebView
         */
        public OAuthDialog(final Uri requestUri) {
            super(mActivity, android.R.style.Theme_Translucent_NoTitleBar);
            this.setOwnerActivity(mActivity);

            mRequestUri = requestUri;
        }

        /**
         * Called when the user hits the back button on the dialog.
         * @param dialog The active dialog
         */
        @Override
        public void onCancel(final DialogInterface dialog) {
            final AuthException exception = new AuthException(ErrorMessages.SIGNIN_CANCEL);
            AuthorizationRequest.this.onException(exception);
        }

        @SuppressWarnings("deprecation")
        @SuppressLint("SetJavaScriptEnabled")
        @Override
        protected void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            this.setOnCancelListener(this);

            final FrameLayout content = new FrameLayout(this.getContext());
            final LinearLayout webViewContainer = new LinearLayout(this.getContext());
            final WebView webView = new WebView(this.getContext());

            webView.setWebViewClient(new AuthorizationWebViewClient());

            final WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);

            webView.loadUrl(mRequestUri.toString());
            webView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
                                                     LayoutParams.FILL_PARENT));
            webView.setVisibility(View.VISIBLE);

            webViewContainer.addView(webView);
            webViewContainer.setVisibility(View.VISIBLE);

            content.addView(webViewContainer);
            content.setVisibility(View.VISIBLE);

            content.forceLayout();
            webViewContainer.forceLayout();

            this.addContentView(content,
                                new LayoutParams(LayoutParams.FILL_PARENT,
                                                 LayoutParams.FILL_PARENT));
        }
    }

    /**
     * Compares just the scheme, authority, and path. It does not compare the query parameters or
     * the fragment.
     */
    private enum UriComparator implements Comparator<Uri> {
        /**
         * The instance of this UriComparator
         */
        INSTANCE;

        @Override
        public int compare(final Uri lhs, final Uri rhs) {
            final String[] lhsParts = {lhs.getScheme(), lhs.getAuthority(), lhs.getPath() };
            final String[] rhsParts = {rhs.getScheme(), rhs.getAuthority(), rhs.getPath() };

            for (int i = 0; i < lhsParts.length; i++) {
                final int compare = lhsParts[i].compareTo(rhsParts[i]);
                if (compare != 0) {
                    return compare;
                }
            }

            return 0;
        }
    }

    /**
     * AMPERSAND
     */
    private static final String AMPERSAND = "&";
    /**
     * EQUALS
     */
    private static final String EQUALS = "=";

    /**
     * Turns the fragment parameters of the uri into a map.
     *
     * @param uri to get fragment parameters from
     * @return a map containing the fragment parameters
     */
    private static Map<String, String> getFragmentParametersMap(final Uri uri) {
        final String fragment = uri.getFragment();
        final String[] keyValuePairs = TextUtils.split(fragment, AMPERSAND);
        final Map<String, String> fragmentParameters = new HashMap<>();

        for (final String keyValuePair : keyValuePairs) {
            final int index = keyValuePair.indexOf(EQUALS);
            final String key = keyValuePair.substring(0, index);
            final String value = keyValuePair.substring(index + 1);
            fragmentParameters.put(key, value);
        }

        return fragmentParameters;
    }

    /**
     * The activity
     */
    private final Activity mActivity;

    /**
     * The http client
     */
    private final HttpClient mClient;

    /**
     * The client id
     */
    private final String mClientId;

    /**
     * The observer
     */
    private final DefaultObservableOAuthRequest mObservable;

    /**
     * The redirect uri
     */
    private final String mRedirectUri;

    /**
     * The scopes
     */
    private final String mScope;

    /**
     * The OAuthConfig
     */
    private final OAuthConfig mOAuthConfig;

    /**
     * The default constructor
     * @param activity The activity
     * @param client The client
     * @param oAuthConfig The oauth configuration
     * @param clientId The client id
     * @param redirectUri The redirect uri
     * @param scope The scopes
     */
    public AuthorizationRequest(final Activity activity,
                                final HttpClient client,
                                final OAuthConfig oAuthConfig,
                                final String clientId,
                                final String redirectUri,
                                final String scope) {
        mActivity = activity;
        mClient = client;
        mClientId = clientId;
        mRedirectUri = redirectUri;
        mObservable = new DefaultObservableOAuthRequest();
        mScope = scope;
        mOAuthConfig = oAuthConfig;
    }

    @Override
    public void addObserver(final OAuthRequestObserver observer) {
        mObservable.addObserver(observer);
    }

    /**
     * Launches the login/consent page inside of a Dialog that contains a WebView and then performs
     * a AccessTokenRequest on successful login and consent. This method is async and will call the
     * passed in listener when it is completed.
     */
    public void execute() {
        final String displayType = this.getDisplayParameter();
        final String responseType = OAuth.ResponseType.CODE.toString().toLowerCase(Locale.US);
        final String locale = Locale.getDefault().toString();
        final Uri requestUri = mOAuthConfig.getAuthorizeUri()
                                        .buildUpon()
                                        .appendQueryParameter(OAuth.CLIENT_ID, mClientId)
                                        .appendQueryParameter(OAuth.SCOPE, mScope)
                                        .appendQueryParameter(OAuth.DISPLAY, displayType)
                                        .appendQueryParameter(OAuth.RESPONSE_TYPE, responseType)
                                        .appendQueryParameter(OAuth.LOCALE, locale)
                                        .appendQueryParameter(OAuth.REDIRECT_URI, mRedirectUri)
                                        .build();

        OAuthDialog oAuthDialog = new OAuthDialog(requestUri);
        oAuthDialog.show();
    }

    @Override
    public void onException(final AuthException exception) {
        mObservable.notifyObservers(exception);
    }

    @Override
    public void onResponse(final OAuthResponse response) {
        mObservable.notifyObservers(response);
    }

    @Override
    public boolean removeObserver(final OAuthRequestObserver observer) {
        return mObservable.removeObserver(observer);
    }

    /**
     * Gets the display parameter by looking at the screen size of the activity.
     * @return "android_phone" for phones and "android_tablet" for tablets.
     */
    private String getDisplayParameter() {
        final ScreenSize screenSize = ScreenSize.determineScreenSize(mActivity);
        final DeviceType deviceType = screenSize.getDeviceType();

        return deviceType.getDisplayParameter().toString().toLowerCase(Locale.US);
    }

    /**
     * Called when the response uri contains an access_token in the fragment.
     *
     * This method reads the response and calls back the OAuthListener on the UI/main thread,
     * and then dismisses the dialog window.
     *
     * See <a href="http://tools.ietf.org/html/draft-ietf-oauth-v2-22#section-1.3.1">Section
     * 1.3.1</a> of the OAuth 2.0 spec.
     *
     * @param fragmentParameters in the uri
     */
    private void onAccessTokenResponse(final Map<String, String> fragmentParameters) {
        OAuthSuccessfulResponse response;
        try {
            response = OAuthSuccessfulResponse.createFromFragment(fragmentParameters);
        } catch (final AuthException e) {
            this.onException(e);
            return;
        }

        this.onResponse(response);
    }

    /**
     * Called when the response uri contains an authorization code.
     *
     * This method launches an async AccessTokenRequest and dismisses the dialog window.
     *
     * See <a href="http://tools.ietf.org/html/draft-ietf-oauth-v2-22#section-4.1.2">Section
     * 4.1.2</a> of the OAuth 2.0 spec for more information.
     *
     * @param code is the authorization code from the uri
     */
    private void onAuthorizationResponse(final String code) {
        // Since we DO have an authorization code, launch an AccessTokenRequest.
        // We do this asynchronously to prevent the HTTP IO from occupying the
        // UI/main thread (which we are on right now).
        final AccessTokenRequest request = new AccessTokenRequest(mClient,
                                                            mOAuthConfig,
                                                            mClientId,
                                                            mRedirectUri,
                                                            code);

        final TokenRequestAsync requestAsync = new TokenRequestAsync(request);
        // We want to know when this request finishes, because we need to notify our
        // observers.
        requestAsync.addObserver(this);
        requestAsync.execute();
    }

    /**
     * Called when the end uri is loaded.
     *
     * This method will read the uri's query parameters and fragment, and respond with the
     * appropriate action.
     *
     * @param endUri that was loaded
     */
    private void onEndUri(final Uri endUri) {
        // If we are on an end uri, the response could either be in
        // the fragment or the query parameters. The response could
        // either be successful or it could contain an error.
        // Check all situations and call the listener's appropriate callback.
        // Callback the listener on the UI/main thread. We could call it right away since
        // we are on the UI/main thread, but it is probably better that we finish up with
        // the WebView code before we callback on the listener.
        final boolean hasFragment = endUri.getFragment() != null;
        final boolean hasQueryParameters = endUri.getQuery() != null;
        final boolean invalidUri = !hasFragment && !hasQueryParameters;

        // check for an invalid uri, and leave early
        if (invalidUri) {
            this.onInvalidUri();
            return;
        }

        if (hasFragment) {
            final Map<String, String> fragmentParameters =
                    AuthorizationRequest.getFragmentParametersMap(endUri);

            final boolean isSuccessfulResponse =
                    fragmentParameters.containsKey(OAuth.ACCESS_TOKEN)
                    && fragmentParameters.containsKey(OAuth.TOKEN_TYPE);
            if (isSuccessfulResponse) {
                this.onAccessTokenResponse(fragmentParameters);
                return;
            }

            final String error = fragmentParameters.get(OAuth.ERROR);
            if (error != null) {
                final String errorDescription = fragmentParameters.get(OAuth.ERROR_DESCRIPTION);
                final String errorUri = fragmentParameters.get(OAuth.ERROR_URI);
                this.onError(error, errorDescription, errorUri);
                return;
            }
        }

        if (hasQueryParameters) {
            final String code = endUri.getQueryParameter(OAuth.CODE);
            if (code != null) {
                this.onAuthorizationResponse(code);
                return;
            }

            final String error = endUri.getQueryParameter(OAuth.ERROR);
            if (error != null) {
                final String errorDescription = endUri.getQueryParameter(OAuth.ERROR_DESCRIPTION);
                final String errorUri = endUri.getQueryParameter(OAuth.ERROR_URI);
                this.onError(error, errorDescription, errorUri);
                return;
            }
        }

        // if the code reaches this point, the uri was invalid
        // because it did not contain either a successful response
        // or an error in either the queryParameter or the fragment
        this.onInvalidUri();
    }

    /**
     * Called when end uri had an error in either the fragment or the query parameter.
     *
     * This method constructs the proper exception, calls the listener's appropriate callback method
     * on the main/UI thread, and then dismisses the dialog window.
     *
     * @param error containing an error code
     * @param errorDescription optional text with additional information
     * @param errorUri optional uri that is associated with the error.
     */
    private void onError(final String error, final String errorDescription, final String errorUri) {
        final AuthException exception = new AuthException(error,
                                                    errorDescription,
                                                    errorUri);
        this.onException(exception);
    }

    /**
     * Called when an invalid uri (i.e., a uri that does not contain an error or a successful
     * response).
     *
     * This method constructs an exception, calls the listener's appropriate callback on the main/UI
     * thread, and then dismisses the dialog window.
     */
    private void onInvalidUri() {
        final AuthException exception = new AuthException(ErrorMessages.SERVER_ERROR);
        this.onException(exception);
    }
}
