/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.microsoft.authenticate;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

/**
 * AuthClient is a class responsible for retrieving an {@link AuthSession}, which
 * can be given to a service in order to make authenticated requests.
 */
@SuppressWarnings({"deprecation", "UnusedReturnValue"})
public class AuthClient {

    /**
     * The OAuth Config
     */
    private final OAuthConfig mOAuthConfig;

    /**
     * Runnable to trigger when auth has completed
     */
    private static class AuthCompleteRunnable extends AuthListenerCaller implements Runnable {

        /**
         * the status of the auth complete action
         */
        private final AuthStatus mStatus;

        /**
         * The backing session for this auth complete action
         */
        private final AuthSession mSession;

        /**
         * Default constructor
         * @param listener The auth listener
         * @param userState The user state
         * @param status The status
         * @param session The session
         */
        public AuthCompleteRunnable(final AuthListener listener,
                                    final Object userState,
                                    final AuthStatus status,
                                    final AuthSession session) {
            super(listener, userState);
            mStatus = status;
            mSession = session;
        }

        @Override
        public void run() {
            getListener().onAuthComplete(mStatus, mSession, getUserState());
        }
    }

    /**
     * The runnable to trigger on auth error
     */
    private static class AuthErrorRunnable extends AuthListenerCaller implements Runnable {

        /**
         * The exception
         */
        private final AuthException mException;

        /**
         * Default constructor
         * @param listener The auth listener
         * @param userState The user state
         * @param exception The exception
         */
        public AuthErrorRunnable(final AuthListener listener,
                                 final Object userState,
                                 final AuthException exception) {
            super(listener, userState);
            mException = exception;
        }

        @Override
        public void run() {
            getListener().onAuthError(mException, getUserState());
        }

    }

    /**
     * Auth Listener caller
     */
    private abstract static class AuthListenerCaller {
        /**
         * The underlying listener
         */
        private final AuthListener mListener;

        /**
         * The user state
         */
        private final Object mUserState;

        /**
         * Default constructor
         * @param listener The auth listener
         * @param userState The user state
         */
        public AuthListenerCaller(final AuthListener listener, final Object userState) {
            mListener = listener;
            mUserState = userState;
        }

        /**
         * Gets the AuthListener
         * @return The auth listener
         */
        protected AuthListener getListener() {
            return mListener;
        }

        /**
         * Gets the UserState
         * @return The user state
         */
        protected Object getUserState() {
            return mUserState;
        }
    }

    /**
     * This class observes an OAuthRequest and calls the appropriate Listener method.
     * On a successful response, it will call the
     * {@link AuthListener#onAuthComplete(AuthStatus, AuthSession, Object)}.
     * On an exception or an unsuccessful response, it will call
     * {@link AuthListener#onAuthError(AuthException, Object)}.
     */
    private class ListenerCallerObserver extends AuthListenerCaller
            implements OAuthRequestObserver,
            OAuthResponseVisitor {

        /**
         * Default constructor
         * @param listener The auth listener
         * @param userState The user state
         */
        public ListenerCallerObserver(final AuthListener listener, final Object userState) {
            super(listener, userState);
        }

        @Override
        public void onException(final AuthException exception) {
            new AuthErrorRunnable(getListener(), getUserState(), exception).run();
        }

        @Override
        public void onResponse(final OAuthResponse response) {
            response.accept(this);
        }

        @Override
        public void visit(final OAuthErrorResponse response) {
            final String error = response.getError().toString().toLowerCase(Locale.US);
            final String errorDescription = response.getErrorDescription();
            final String errorUri = response.getErrorUri();
            final AuthException exception = new AuthException(error,
                    errorDescription,
                    errorUri);

            new AuthErrorRunnable(getListener(), getUserState(), exception).run();
        }

        @Override
        public void visit(final OAuthSuccessfulResponse response) {
            mSession.loadFromOAuthResponse(response);

            new AuthCompleteRunnable(getListener(), getUserState(), AuthStatus.CONNECTED, mSession).run();
        }
    }

    /**
     * Observer that will, depending on the response, save or clear the refresh token.
     */
    private class RefreshTokenWriter implements OAuthRequestObserver, OAuthResponseVisitor {

        @Override
        public void onException(final AuthException exception) {
        }

        @Override
        public void onResponse(final OAuthResponse response) {
            response.accept(this);
        }

        @Override
        public void visit(final OAuthErrorResponse response) {
            if (response.getError() == OAuth.ErrorType.INVALID_GRANT) {
                AuthClient.this.clearRefreshTokenFromPreferences();
            }
        }

        @Override
        public void visit(final OAuthSuccessfulResponse response) {
            final String refreshToken = response.getRefreshToken();
            if (!TextUtils.isEmpty(refreshToken)) {
                this.saveRefreshTokenToPreferences(refreshToken);
            }
        }

        /**
         * Saves the refresh token
         * @param refreshToken The refresh token
         */
        private void saveRefreshTokenToPreferences(final String refreshToken) {
            final SharedPreferences settings =
                    mApplicationContext.getSharedPreferences(PreferencesConstants.FILE_NAME,
                            Context.MODE_PRIVATE);
            final Editor editor = settings.edit();
            editor.putString(PreferencesConstants.REFRESH_TOKEN_KEY, refreshToken);

            editor.apply();
        }
    }

    /**
     * An {@link OAuthResponseVisitor} that checks the {@link OAuthResponse} and if it is a
     * successful response, it loads the response into the given mSession.
     */
    private static class SessionRefresher implements OAuthResponseVisitor {

        /**
         * The session
         */
        private final AuthSession mSession;

        /**
         * IF the visit responsed with success
         */
        private boolean mVisitedSuccessfulResponse;

        /**
         * Default constructor
         * @param session The session to refresh
         */
        public SessionRefresher(final AuthSession session) {
            mSession = session;
            mVisitedSuccessfulResponse = false;
        }

        @Override
        public void visit(final OAuthErrorResponse response) {
            mVisitedSuccessfulResponse = false;
        }

        @Override
        public void visit(final OAuthSuccessfulResponse response) {
            mSession.loadFromOAuthResponse(response);
            mVisitedSuccessfulResponse = true;
        }

        /**
         * Gets the visited success response
         * @return If successful
         */
        public boolean visitedSuccessfulResponse() {
            return mVisitedSuccessfulResponse;
        }
    }

    /**
     * A AuthListener that does nothing on each of the call backs.
     * This is used so when a null listener is passed in, this can be used, instead of null,
     * to avoid if (listener == null) checks.
     */
    private static final AuthListener NULL_LISTENER = new AuthListener() {
        @Override
        public void onAuthComplete(final AuthStatus status, final AuthSession session, final Object sender) {
        }

        @Override
        public void onAuthError(final AuthException exception, final Object sender) {
        }
    };

    /**
     * The application context
     */
    private final Context mApplicationContext;

    /**
     * The client id
     */
    private final String mClientId;

    /**
     * Is there a pending login happening
     */
    private boolean mHasPendingLoginRequest;

    /**
     * Responsible for all network (i.e., HTTP) calls.
     * Tests will want to change this to mock the network and HTTP responses.
     *
     * @see #setHttpClient(HttpClient)
     */
    private HttpClient mHttpClient;

    /**
     * saved from initialize and used in the login call if login's scopes are null.
     */
    private Set<String> mScopesFromInitialize;

    /**
     * One-to-one relationship between AuthClient and AuthSession.
     */
    private final AuthSession mSession;
    {
        mHttpClient = new DefaultHttpClient();
        mHasPendingLoginRequest = false;
        mSession = new AuthSession(this);
    }

    /**
     * Constructs a new {@code AuthClient} instance and initializes its member variables.
     *
     * @param context  Context of the Application used to save any refresh_token.
     * @param oAuthConfig the OAuth configuration
     * @param clientId The client_id of the application to login to.
     */
    public AuthClient(final Context context, final OAuthConfig oAuthConfig, final String clientId) {
        this.mApplicationContext = context.getApplicationContext();
        this.mClientId = clientId;
        this.mOAuthConfig = oAuthConfig;
    }

    /**
     * Initializes a new {@link AuthSession} with the given scopes.
     * <p/>
     * The {@link AuthSession} will be returned by calling
     * {@link AuthListener#onAuthComplete(AuthStatus, AuthSession, Object)}.
     * Otherwise, the {@link AuthListener#onAuthError(AuthException, Object)} will be
     * called. These methods will be called on the main/UI thread.
     * <p/>
     * If the wl.offline_access scope is used, a refresh_token is stored in the given
     * {@link Activity}'s {@link SharedPreferences}.
     *
     * @param scopes       to initialize the {@link AuthSession} with.
     * @param listener     called on either completion or error during the initialize process
     * @param userState    arbitrary object that is used to determine the caller of the method.
     * @param refreshToken optional previously saved token to be used by this client.
     */
    public void initialize(final Iterable<String> scopes, final AuthListener listener, final Object userState,
                           final String refreshToken) {

        final AuthListener activeListener;
        if (listener == null) {
            activeListener = NULL_LISTENER;
        } else {
            activeListener = listener;
        }

        // copy scopes for login
        mScopesFromInitialize = new HashSet<>();
        if (scopes != null) {
            for (String scope : scopes) {
                mScopesFromInitialize.add(scope);
            }
        }
        mScopesFromInitialize = Collections.unmodifiableSet(mScopesFromInitialize);

        //if no token is provided, try to get one from SharedPreferences
        final String activeRefreshToken;
        if (refreshToken == null) {
            activeRefreshToken = getRefreshTokenFromPreferences();
        } else {
            activeRefreshToken = refreshToken;
        }

        if (activeRefreshToken == null) {
            activeListener.onAuthComplete(AuthStatus.UNKNOWN, null, userState);
            return;
        }

        RefreshAccessTokenRequest request =
                new RefreshAccessTokenRequest(this.mHttpClient,
                        mOAuthConfig,
                        this.mClientId,
                        activeRefreshToken,
                        TextUtils.join(OAuth.SCOPE_DELIMITER, mScopesFromInitialize));
        TokenRequestAsync asyncRequest = new TokenRequestAsync(request);

        asyncRequest.addObserver(new ListenerCallerObserver(activeListener, userState));
        asyncRequest.addObserver(new RefreshTokenWriter());

        asyncRequest.execute();
    }

    /**
     * Initializes a new {@link AuthSession} with the given scopes.
     * <p/>
     * The {@link AuthSession} will be returned by calling
     * {@link AuthListener#onAuthComplete(AuthStatus, AuthSession, Object)}.
     * Otherwise, the {@link AuthListener#onAuthError(AuthException, Object)} will be
     * called. These methods will be called on the main/UI thread.
     * <p/>
     * If the wl.offline_access scope is used, a refresh_token is stored in the given
     * {@link Activity}'s {@link SharedPreferences}.
     * <p/>
     * This initialize will use the last successfully used scopes from either a login or initialize.
     *
     * @param listener called on either completion or error during the initialize process.
     */
    public void initialize(final AuthListener listener) {
        this.initialize(listener, null);
    }

    /**
     * Initializes a new {@link AuthSession} with the given scopes.
     * <p/>
     * The {@link AuthSession} will be returned by calling
     * {@link AuthListener#onAuthComplete(AuthStatus, AuthSession, Object)}.
     * Otherwise, the {@link AuthListener#onAuthError(AuthException, Object)} will be
     * called. These methods will be called on the main/UI thread.
     * <p/>
     * If the wl.offline_access scope is used, a refresh_token is stored in the given
     * {@link Activity}'s {@link SharedPreferences}.
     * <p/>
     * This initialize will use the last successfully used scopes from either a login or initialize.
     *
     * @param listener  called on either completion or error during the initialize process.
     * @param userState arbitrary object that is used to determine the caller of the method.
     */
    public void initialize(final AuthListener listener, final Object userState) {
        this.initialize(null, listener, userState, null);
    }

    /**
     * Logs in an user with the given scopes.
     * <p/>
     * login displays a Dialog that will prompt the
     * user for a username and password, and ask for consent to use the given scopes.
     * A {@link AuthSession} will be returned by calling
     * {@link AuthListener#onAuthComplete(AuthStatus, AuthSession, Object)}.
     * Otherwise, the {@link AuthListener#onAuthError(AuthException, Object)} will be
     * called. These methods will be called on the main/UI thread.
     *
     * @param activity {@link Activity} instance to display the Login dialog on.
     * @param scopes   to initialize the {@link AuthSession} with.
     * @param listener called on either completion or error during the login process.
     */
    public void login(final Activity activity, final Iterable<String> scopes, final AuthListener listener) {
        this.login(activity, scopes, listener, null);
    }

    /**
     * Logs in an user with the given scopes.
     * <p/>
     * login displays a Dialog that will prompt the
     * user for a username and password, and ask for consent to use the given scopes.
     * A {@link AuthSession} will be returned by calling
     * {@link AuthListener#onAuthComplete(AuthStatus, AuthSession, Object)}.
     * Otherwise, the {@link AuthListener#onAuthError(AuthException, Object)} will be
     * called. These methods will be called on the main/UI thread.
     *
     * @param activity  {@link Activity} instance to display the Login dialog on
     * @param scopes    to initialize the {@link AuthSession} with.
     * @param listener  called on either completion or error during the login process.
     * @param userState arbitrary object that is used to determine the caller of the method.
     */
    public void login(final Activity activity,
                      final Iterable<String> scopes,
                      final AuthListener listener,
                      final Object userState) {
        final AuthListener activeListener;
        if (listener == null) {
            activeListener = NULL_LISTENER;
        } else {
            activeListener = listener;
        }

        if (this.mHasPendingLoginRequest) {
            throw new IllegalStateException(ErrorMessages.LOGIN_IN_PROGRESS);
        }

        final List<String> activeScopes = new LinkedList<>();
        if (scopes == null && mScopesFromInitialize != null) {
            activeScopes.addAll(mScopesFromInitialize);
        }
        if (scopes != null) {
            for (final String scope : scopes) {
                activeScopes.add(scope);
            }
        }


        // if the mSession is valid and contains all the scopes, do not display the login ui.
        boolean showDialog = mSession.isExpired() || !mSession.contains(scopes);
        if (!showDialog) {
            activeListener.onAuthComplete(AuthStatus.CONNECTED, mSession, userState);
            return;
        }

        final String scope = TextUtils.join(OAuth.SCOPE_DELIMITER, activeScopes);
        final String redirectUri = mOAuthConfig.getDesktopUri().toString();
        final AuthorizationRequest request = new AuthorizationRequest(activity,
                this.mHttpClient,
                mOAuthConfig,
                this.mClientId,
                redirectUri,
                scope);

        request.addObserver(new ListenerCallerObserver(activeListener, userState));
        request.addObserver(new RefreshTokenWriter());
        request.addObserver(new OAuthRequestObserver() {
            @Override
            public void onException(final AuthException exception) {
                AuthClient.this.mHasPendingLoginRequest = false;
            }

            @Override
            public void onResponse(final OAuthResponse response) {
                AuthClient.this.mHasPendingLoginRequest = false;
            }
        });

        this.mHasPendingLoginRequest = true;

        request.execute();
    }

    /**
     * Logs out the given user.
     * <p/>
     * Also, this method clears the previously created {@link AuthSession}.
     * {@link AuthListener#onAuthComplete(AuthStatus, AuthSession, Object)} will be
     * called on completion. Otherwise,
     * {@link AuthListener#onAuthError(AuthException, Object)} will be called.
     *
     * @param listener called on either completion or error during the logout process.
     */
    public void logout(final AuthListener listener) {
        this.logout(listener, null);
    }

    /**
     * Logs out the given user.
     * <p/>
     * Also, this method clears the previously created {@link AuthSession}.
     * {@link AuthListener#onAuthComplete(AuthStatus, AuthSession, Object)} will be
     * called on completion. Otherwise,
     * {@link AuthListener#onAuthError(AuthException, Object)} will be called.
     *
     * @param listener  called on either completion or error during the logout process.
     * @param userState arbitrary object that is used to determine the caller of the method.
     */
    @SuppressWarnings("deprecation")
    public void logout(final AuthListener listener, final Object userState) {
        final AuthListener activeListener;
        if (listener == null) {
            activeListener = NULL_LISTENER;
        } else {
            activeListener = listener;
        }

        mSession.setAccessToken(null);
        mSession.setAuthenticationToken(null);
        mSession.setRefreshToken(null);
        mSession.setScopes(null);
        mSession.setTokenType(null);
        mSession.setExpiresIn(new Date(0));

        clearRefreshTokenFromPreferences();

        final CookieSyncManager cookieSyncManager =
                CookieSyncManager.createInstance(this.mApplicationContext);
        final CookieManager manager = CookieManager.getInstance();
        final Uri logoutUri = mOAuthConfig.getLogoutUri();
        final String url = logoutUri.toString();
        final String domain = logoutUri.getHost();

        final List<String> cookieKeys = this.getCookieKeysFromPreferences();
        for (final String cookieKey : cookieKeys) {
            final String value = TextUtils.join("", new String[]{
                    cookieKey,
                    "=; expires=Thu, 30-Oct-1980 16:00:00 GMT;domain=",
                    domain,
                    ";path=/;version=1"
            });

            manager.setCookie(url, value);
        }

        cookieSyncManager.sync();
        activeListener.onAuthComplete(AuthStatus.UNKNOWN, null, userState);
    }

    /**
     * Gets the current session
     * @return The {@link AuthSession} instance that this {@code AuthClient} created.
     */
    public AuthSession getSession() {
        return mSession;
    }

    /**
     * Gets whether the client has a refresh token
     */
    public boolean hasRefreshToken()
    {
        return !TextUtils.isEmpty(getRefreshTokenFromPreferences());
    }

    /**
     * Does the client have a pending login request
     */
    public boolean hasPendingLogin()
    {
        return mHasPendingLoginRequest;
    }

    /**
     * Refreshes the previously created mSession.
     *
     * @return true if the mSession was successfully refreshed.
     */
    boolean refresh() {
        Iterable<String> scopes = mSession.getScopes();
        if (scopes == null) {
            scopes = mScopesFromInitialize;
        }
        final String scope = TextUtils.join(OAuth.SCOPE_DELIMITER, scopes);

        String sessionRefreshToken = mSession.getRefreshToken();
        final String refreshToken =
                (sessionRefreshToken != null) ?
                        sessionRefreshToken : getRefreshTokenFromPreferences();

        if (TextUtils.isEmpty(refreshToken)) {
            return false;
        }

        final RefreshAccessTokenRequest request =
                new RefreshAccessTokenRequest(this.mHttpClient, mOAuthConfig, this.mClientId, refreshToken, scope);

        final OAuthResponse response;
        try {
            response = request.execute();
        } catch (final AuthException e) {
            return false;
        }

        final SessionRefresher refresher = new SessionRefresher(mSession);
        response.accept(refresher);
        response.accept(new RefreshTokenWriter());

        return refresher.visitedSuccessfulResponse();
    }

    /**
     * Sets the {@link HttpClient} that is used for HTTP requests by this {@code AuthClient}.
     * Tests will want to change this to mock the network/HTTP responses.
     *
     * @param client The new HttpClient to be set.
     */
    void setHttpClient(final HttpClient client) {
        this.mHttpClient = client;
    }

    /**
     * Clears the refresh token from this {@code AuthClient}'s
     * {@link Activity#getPreferences(int)}.
     *
     * @return true if the refresh token was successfully cleared.
     */
    private boolean clearRefreshTokenFromPreferences() {
        final SharedPreferences settings = getSharedPreferences();
        final Editor editor = settings.edit();
        editor.remove(PreferencesConstants.REFRESH_TOKEN_KEY);

        return editor.commit();
    }

    /**
     * Gets the shared preferences
     * @return The current preferences instance
     */
    private SharedPreferences getSharedPreferences() {
        return mApplicationContext.getSharedPreferences(PreferencesConstants.FILE_NAME,
                Context.MODE_PRIVATE);
    }

    /**
     * Gets the cookie key from shared preferences
     * @return The list of cookies
     */
    private List<String> getCookieKeysFromPreferences() {
        final SharedPreferences settings = getSharedPreferences();
        final String cookieKeys = settings.getString(PreferencesConstants.COOKIES_KEY, "");

        return Arrays.asList(TextUtils.split(cookieKeys, PreferencesConstants.COOKIE_DELIMITER));
    }

    /**
     * Retrieves the refresh token from this {@code AuthClient}'s
     * {@link Activity#getPreferences(int)}.
     *
     * @return the refresh token from persistent storage.
     */
    private String getRefreshTokenFromPreferences() {
        final SharedPreferences settings = getSharedPreferences();
        return settings.getString(PreferencesConstants.REFRESH_TOKEN_KEY, null);
    }
}
