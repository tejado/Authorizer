package com.box.androidsdk.content.models;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.box.androidsdk.content.BoxApiUser;
import com.box.androidsdk.content.BoxConfig;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.auth.BoxAuthentication;
import com.box.androidsdk.content.requests.BoxRequest;
import com.box.androidsdk.content.utils.BoxLogUtils;
import com.box.androidsdk.content.utils.SdkUtils;
import com.box.sdk.android.R;

/**
 * A BoxSession is responsible for maintaining the mapping between user and authentication tokens
 */
public class BoxSession extends BoxObject implements BoxAuthentication.AuthListener, Serializable {

    private static final long serialVersionUID = 8122900496609434013L;

    private static final transient ThreadPoolExecutor AUTH_CREATION_EXECUTOR = SdkUtils.createDefaultThreadPoolExecutor(1, 20, 3600, TimeUnit.SECONDS);
    private String mUserAgent = "com.box.sdk.android";
    private transient Context mApplicationContext;
    private transient BoxAuthentication.AuthListener sessionAuthListener;
    private String mUserId;

    protected String mClientId;
    protected String mClientSecret;
    protected String mClientRedirectUrl;
    protected BoxAuthentication.BoxAuthenticationInfo mAuthInfo;

    protected String mDeviceId;
    protected String mDeviceName;
    protected BoxMDMData mMDMData;
    protected Long mExpiresAt;
    protected String mAccountEmail;

    /**
     * Optional refresh provider.
     */
    protected BoxAuthentication.AuthenticationRefreshProvider mRefreshProvider;

    protected boolean mEnableBoxAppAuthentication = BoxConfig.ENABLE_BOX_APP_AUTHENTICATION;


    /**
     * When using this constructor, if a user has previously been logged in/stored or there is only one user, this user will be authenticated.
     * If no user or multiple users have been stored without knowledge of the last one authenticated, ui will be shown to handle the scenario similar
     * to BoxSession(null, context).
     *
     * @param context current context.
     */
    public BoxSession(Context context) {
        this(context, getBestStoredUserId(context));
    }

    /**
     * @return the user id associated with the only logged in user. If no user is logged in or multiple users are logged in returns null.
     */
    private static String getBestStoredUserId(final Context context) {
        String lastAuthenticatedUserId = BoxAuthentication.getInstance().getLastAuthenticatedUserId(context);
        Map<String, BoxAuthentication.BoxAuthenticationInfo> authInfoMap = BoxAuthentication.getInstance().getStoredAuthInfo(context);
        if (authInfoMap != null) {
            if (!SdkUtils.isEmptyString(lastAuthenticatedUserId) && authInfoMap.get(lastAuthenticatedUserId) != null) {
                return lastAuthenticatedUserId;
            }
            if (authInfoMap.size() == 1) {
                for (String authUserId : authInfoMap.keySet()) {
                    return authUserId;
                }
            }
        }
        return null;
    }

    /**
     * When setting the userId to null ui will be shown to ask which user to authenticate as if at least one user is logged in. If no
     * user has been stored will show login ui. If logging in as a valid user id no ui will be displayed.
     *
     * @param userId  user id to login as or null to login a new user.
     * @param context current context.
     */
    public BoxSession(Context context, String userId) {
        this(context, userId, BoxConfig.CLIENT_ID, BoxConfig.CLIENT_SECRET, BoxConfig.REDIRECT_URL);
        if (!SdkUtils.isEmptyString(BoxConfig.DEVICE_NAME)){
            setDeviceName(BoxConfig.DEVICE_NAME);
        }
        if (!SdkUtils.isEmptyString(BoxConfig.DEVICE_ID)){
            setDeviceName(BoxConfig.DEVICE_ID);
        }
    }

    /**
     * Create a BoxSession using a specific box clientId, secret, and redirectUrl. This constructor is not necessary unless
     * an application uses multiple api keys.
     * Note: When setting the userId to null ui will be shown to ask which user to authenticate as if at least one user is logged in. If no
     * user has been stored will show login ui.
     *
     * @param context      current context.
     * @param clientId     the developer's client id to access the box api.
     * @param clientSecret the developer's secret used to interpret the response coming from Box.
     * @param redirectUrl  the developer's redirect url to use for authenticating via Box.
     * @param userId       user id to login as or null to login as a new user.
     */
    public BoxSession(Context context, String userId, String clientId, String clientSecret, String redirectUrl) {
        mClientId = clientId;
        mClientSecret = clientSecret;
        mClientRedirectUrl = redirectUrl;
        if (SdkUtils.isEmptyString(mClientId) || SdkUtils.isEmptyString(mClientSecret)) {
            throw new RuntimeException("Session must have a valid client id and client secret specified.");
        }
        mApplicationContext = context.getApplicationContext();
        if (!SdkUtils.isEmptyString(userId)) {
            mAuthInfo = BoxAuthentication.getInstance().getAuthInfo(userId, context);
            mUserId = userId;
        }
        if (mAuthInfo == null) {
            mUserId = userId;
            mAuthInfo = new BoxAuthentication.BoxAuthenticationInfo();
        }
        mAuthInfo.setClientId(mClientId);
        setupSession();
    }

    /**
     * Construct a new box session object based off of an existing session.
     *
     * @param session session to use as the base.
     */
    protected BoxSession(BoxSession session) {
        this.mApplicationContext = session.mApplicationContext;
        setAuthInfo(session.getAuthInfo());
        setupSession();
    }


    /**
     * This is an advanced constructor that can be used when implementing an authentication flow that differs from the default oauth 2 flow.
     *
     * @param context         current context.
     * @param authInfo        authentication information that should be used. (Must at the minimum provide an access token).
     * @param refreshProvider the refresh provider to use when the access token expires and needs to be refreshed.
     */
    public BoxSession(Context context, BoxAuthentication.BoxAuthenticationInfo authInfo, BoxAuthentication.AuthenticationRefreshProvider refreshProvider) {
        mApplicationContext = context.getApplicationContext();
        setAuthInfo(authInfo);
        mRefreshProvider = refreshProvider;
        setupSession();
    }

    protected void setAuthInfo(BoxAuthentication.BoxAuthenticationInfo authInfo) {
        if (authInfo != null) {
            mAuthInfo = authInfo;
            if (authInfo.getUser() != null) {
                if (!SdkUtils.isBlank(authInfo.getUser().getId())) {
                    setUserId(authInfo.getUser().getId());
                }
            }
        }
    }

    /**
     * This is a convenience constructor. It is the equivalent of calling BoxSession(context, authInfo, refreshProvider) and creating an authInfo with just
     * an accessToken.
     *
     * @param context         current context
     * @param accessToken     a valid accessToken.
     * @param refreshProvider the refresh provider to use when the access token expires and needs to refreshed.
     */
    public BoxSession(Context context, String accessToken, BoxAuthentication.AuthenticationRefreshProvider refreshProvider) {
        this(context, createSimpleBoxAuthenticationInfo(accessToken), refreshProvider);
    }

    private static BoxAuthentication.BoxAuthenticationInfo createSimpleBoxAuthenticationInfo(final String accessToken) {
        BoxAuthentication.BoxAuthenticationInfo info = new BoxAuthentication.BoxAuthenticationInfo();
        info.setAccessToken(accessToken);
        return info;
    }

    /**
     * Sets whether or not Box App authentication is enabled or not.
     *
     * @param enabled true if the session should try to authenticate via the Box application, false otherwise.
     */
    public void setEnableBoxAppAuthentication(boolean enabled) {
        mEnableBoxAppAuthentication = enabled;
    }

    /**
     * @return true if authentication via the Box App is enabled (this is by default), false otherwise.
     */
    public boolean isEnabledBoxAppAuthentication() {
        return mEnableBoxAppAuthentication;
    }

    /**
     * Set the application context if this session loses it for instance when this object is deserialized.
     */
    public void setApplicationContext(final Context context){
        mApplicationContext = context.getApplicationContext();
    }

    /**
     * @return the application context used to construct this session.
     */
    public Context getApplicationContext() {
        return mApplicationContext;
    }

    /**
     * @param listener listener to notify when authentication events (authentication, refreshing, and their exceptions) occur.
     */
    public void setSessionAuthListener(BoxAuthentication.AuthListener listener) {
        this.sessionAuthListener = listener;
    }

    protected void setupSession() {
        // Because BuildConfig.DEBUG is always false when library projects publish their release variants we use ApplicationInfo
        boolean isDebug = false;
        try {
            if (mApplicationContext != null && mApplicationContext.getPackageManager() != null) {
                PackageInfo info = mApplicationContext.getPackageManager().getPackageInfo(mApplicationContext.getPackageName(), 0);
                isDebug = ((info.applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Do nothing -- debug mode will default to false
        }
        BoxConfig.IS_DEBUG = isDebug;
        BoxAuthentication.getInstance().addListener(this);
    }

    /**
     * @return the user associated with this session. May return null if this is a new session before authentication.
     */
    public BoxUser getUser() {
        return mAuthInfo.getUser();
    }

    /**
     * @return the user id associated with this session. This can be null if the session was created without a user id and has
     * not been authenticated.
     */
    public String getUserId() {
        return mUserId;
    }

    protected void setUserId(String userId) {
        mUserId = userId;
    }

    /**
     * @return the auth information associated with this session.
     */
    public BoxAuthentication.BoxAuthenticationInfo getAuthInfo() {
        return mAuthInfo;
    }

    /**
     * @return the custom refresh provider associated with this session. returns null if one is not set.
     */
    public BoxAuthentication.AuthenticationRefreshProvider getRefreshProvider() {
        return mRefreshProvider;
    }

    /**
     * Set the optional unique ID of this device. Used for applications that want to support device-pinning.
     */
    public void setDeviceId(final String deviceId){
        mDeviceId = deviceId;
    }

    /**
     * @return the device id associated with this session. returns null if one is not set.
     */
    public String getDeviceId(){
        return mDeviceId;
    }

    /**
     * Set the optional human readable name for this device.
     */
    public void setDeviceName(final String deviceName){
        mDeviceName = deviceName;
    }

    /**
     * @return the device name associated with this session. returns null if one is not set.
     */
    public String getDeviceName(){
        return mDeviceName;
    }

    /**
     * @return the user agent to use for network requests with this session.
     */
    public String getUserAgent() {
        return mUserAgent;
    }

    /**
     * @param mdmData MDM data object that should be used to authenticate this session if required.
     */
    public void setManagementData(final BoxMDMData mdmData){
        mMDMData = mdmData;
    }

    /**
     * @return mdm data if set.
     */
    public BoxMDMData getManagementData(){
        return mMDMData;
    }

    /**
     * @param expiresAt (optional) set the time as a unix time stamp in seconds when the refresh token should expire. Must be less than the default 60 days if used.
     */
    public void setRefreshTokenExpiresAt(final long expiresAt){
        mExpiresAt = expiresAt;
    }

    /**
     * return the unix time stamp at which refresh token should expire if set, returns null if not set.
     */
    public Long getRefreshTokenExpiresAt(){
        return mExpiresAt;
    }

    /**
     * @param accountName (optional) set email account to prefill into authentication ui if available.
     */
    public void setBoxAccountEmail(final String accountName){
        mAccountEmail = accountName;
    }

    /**
     * @return Box Account email if set.
     */
    public String getBoxAccountEmail(){
        return mAccountEmail;
    }

    /**
     * @return a box future task (already submitted to an executor) that starts the process of authenticating this user.
     * The task can be used to block until the user has completed authentication through whatever ui is necessary(using task.get()).
     */
    public BoxFutureTask<BoxSession> authenticate() {
        BoxSessionAuthCreationRequest req = new BoxSessionAuthCreationRequest(this, mEnableBoxAppAuthentication);
        BoxFutureTask<BoxSession> task = req.toTask();
        AUTH_CREATION_EXECUTOR.submit(task);
        return task;
    }

    /**
     * Logout the currently authenticated user.
     *
     * @return a task that can be used to block until the user associated with this session has been logged out.
     */
    public BoxFutureTask<BoxSession> logout() {
        final BoxFutureTask<BoxSession> task = (new BoxSessionLogoutRequest(this)).toTask();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                task.run();
                return null;
            }
        }.execute();
        return task;
    }

    /**
     * Refresh authentication information associated with this session.
     *
     * @return a task that can be used to block until the information associated with this session has been refreshed.
     */
    public BoxFutureTask<BoxSession> refresh() {

        final BoxFutureTask<BoxSession> task = (new BoxSessionRefreshRequest(this)).toTask();
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            new AsyncTask<Void, Void, Void>()
            {
                @Override
                protected Void doInBackground(Void... params)
                {
                    task.run();
                    return null;
                }
            }.execute();
        } else {
            task.run();
        }
        return task;
    }

    /**
     * Create a shared link session based off of the current session.
     *
     * @param sharedLinkUri The url of the shared link.
     * @return a session that can access a given shared link url and its children.
     */
    public BoxSharedLinkSession getSharedLinkSession(String sharedLinkUri) {
        return new BoxSharedLinkSession(sharedLinkUri, this);
    }

    /**
     * This function gives you the cache location associated with this session. It is
     * preferred to use this method when setting up the location of your cache as it ensures
     * that all data will be cleared upon logout.
     */
    public File getCacheDir() {
        return new File(getApplicationContext().getFilesDir(), getUserId());
    }

    /**
     * Called when this session has been refreshed with new authentication info.
     *
     * @param info the latest info from a successful refresh.
     */
    @Override
    public void onRefreshed(BoxAuthentication.BoxAuthenticationInfo info) {
        if (sameUser(info)) {
            BoxAuthentication.BoxAuthenticationInfo.cloneInfo(mAuthInfo, info);
            if (sessionAuthListener != null) {
                sessionAuthListener.onRefreshed(info);
            }
        }
    }

    /**
     * Called when this user has logged in.
     *
     * @param info the latest info from going through the login flow.
     */
    @Override
    public void onAuthCreated(BoxAuthentication.BoxAuthenticationInfo info) {
        if (sameUser(info)) {
            BoxAuthentication.BoxAuthenticationInfo.cloneInfo(mAuthInfo, info);
            if (sessionAuthListener != null) {
                sessionAuthListener.onAuthCreated(info);
            }
        }
    }

    /**
     * Called when a failure occurs trying to authenticate or refresh.
     *
     * @param info The last authentication information available, before the exception.
     * @param ex   the exception that occurred.
     */
    @Override
    public void onAuthFailure(BoxAuthentication.BoxAuthenticationInfo info, Exception ex) {
        if (sameUser(info) || (info == null && getUserId() == null)) {
            if (sessionAuthListener != null) {
                sessionAuthListener.onAuthFailure(info, ex);
            }
            if (ex instanceof BoxException) {
                BoxException.ErrorType errorType = ((BoxException) ex).getErrorType();
                switch (errorType) {
                    case NETWORK_ERROR:
                        toastString(mApplicationContext, R.string.boxsdk_error_network_connection);
                }

            }
        }
    }

    protected void startAuthenticationUI(){
        BoxAuthentication.getInstance().startAuthenticationUI(this);
    }

    private static AtomicLong mLastToastTime = new AtomicLong();

    private static void toastString(final Context context, final int id) {
        Handler handler = new Handler(Looper.getMainLooper());
        long currentTime = System.currentTimeMillis();
        long lastToastTime = mLastToastTime.get();
        if (currentTime - 3000 < lastToastTime) {
            return;
        }
        mLastToastTime.set(currentTime);
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, id, Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public void onLoggedOut(BoxAuthentication.BoxAuthenticationInfo info, Exception ex) {
        if (sameUser(info)) {
            info.wipeOutAuth();
            getAuthInfo().wipeOutAuth();
            setUserId(null);
            if (sessionAuthListener != null) {
                sessionAuthListener.onLoggedOut(info, ex);
            }
        }
    }

    /**
     * Returns the associated client id. If none is set, the one defined in BoxConfig will be used
     *
     * @return the client id this session is associated with.
     */
    public String getClientId() {
        return mClientId;
    }

    /**
     * Returns the associated client secret. Because client secrets are not managed by the SDK, if another
     * client id/secret is used aside from the one defined in the BoxConfig
     *
     * @return the client secret this session is associated with
     */
    public String getClientSecret() {
        return mClientSecret;
    }

    /**
     * @return the redirect url this session is using. By default comes from BoxConstants.
     */
    public String getRedirectUrl() {
        return mClientRedirectUrl;
    }

    private boolean sameUser(BoxAuthentication.BoxAuthenticationInfo info) {
        return info != null && info.getUser() != null && getUserId() != null && getUserId().equals(info.getUser().getId());
    }

    private static class BoxSessionLogoutRequest extends BoxRequest<BoxSession, BoxSessionLogoutRequest> {
        private BoxSession mSession;

        public BoxSessionLogoutRequest(BoxSession session) {
            super(null, " ", null);
            this.mSession = session;
        }

        public BoxSession send() throws BoxException {
            synchronized (mSession) {
                if (mSession.getUser() != null) {
                    BoxAuthentication.getInstance().logout(mSession);
                    mSession.getAuthInfo().wipeOutAuth();
                    mSession.setUserId(null);
                }
            }
            return mSession;
        }
    }


    private static class BoxSessionRefreshRequest extends BoxRequest<BoxSession, BoxSessionRefreshRequest> {
        private BoxSession mSession;

        public BoxSessionRefreshRequest(BoxSession session) {
            super(null, " ", null);
            this.mSession = session;
        }

        public BoxSession send() throws BoxException {
            try {
                // block until this session is finished refreshing.
                BoxAuthentication.BoxAuthenticationInfo refreshedInfo = BoxAuthentication.getInstance().refresh(mSession).get();
            } catch (Exception e) {
                BoxException r = (BoxException) e.getCause();
                if (e.getCause() instanceof BoxException) {
                    throw (BoxException) e.getCause();
                } else {
                    throw new BoxException("BoxSessionRefreshRequest failed", e);
                }
            }
            BoxAuthentication.BoxAuthenticationInfo.cloneInfo(mSession.mAuthInfo,
                    BoxAuthentication.getInstance().getAuthInfo(mSession.getUserId(), mSession.getApplicationContext()));
            return mSession;
        }
    }

    private static class BoxSessionAuthCreationRequest extends BoxRequest<BoxSession, BoxSessionAuthCreationRequest> implements BoxAuthentication.AuthListener {
        private final BoxSession mSession;
        private CountDownLatch authLatch;

        public BoxSessionAuthCreationRequest(BoxSession session, boolean viaBoxApp) {
            super(null, " ", null);
            this.mSession = session;
        }

        public BoxSession send() throws BoxException {
            synchronized (mSession) {
                if (mSession.getUser() == null) {
                    if (mSession.getAuthInfo() != null && !SdkUtils.isBlank(mSession.getAuthInfo().accessToken())) {

                        // if we have an access token, but no user try to repair by making the call to user endpoint.
                        try {
                            //TODO: show some ui while requestion user info
                            BoxApiUser apiUser = new BoxApiUser(mSession);
                            BoxUser user = apiUser.getCurrentUserInfoRequest().send();

                            mSession.setUserId(user.getId());
                            mSession.getAuthInfo().setUser(user);
                            mSession.onAuthCreated(mSession.getAuthInfo());
                            return mSession;

                        } catch (BoxException e) {
                            BoxLogUtils.e("BoxSession", "Unable to repair user", e);
                            if (e instanceof BoxException.RefreshFailure && ((BoxException.RefreshFailure) e).isErrorFatal()) {
                                // if the refresh failure is unrecoverable have the user login again.
                                toastString(mSession.getApplicationContext(), R.string.boxsdk_error_fatal_refresh);
                            } else if (e.getErrorType() == BoxException.ErrorType.TERMS_OF_SERVICE_REQUIRED) {
                                toastString(mSession.getApplicationContext(), R.string.boxsdk_error_terms_of_service);
                            } else {
                                mSession.onAuthFailure(null, e);
                                throw e;
                            }

                        }
                        // at this point we were unable to repair.

                    }
                    BoxAuthentication.getInstance().addListener(this);
                    launchAuthUI();
                    return mSession;
                } else {
                    BoxAuthentication.BoxAuthenticationInfo info = BoxAuthentication.getInstance().getAuthInfo(mSession.getUserId(), mSession.getApplicationContext());
                    if (info != null) {
                        BoxAuthentication.BoxAuthenticationInfo.cloneInfo(mSession.mAuthInfo, info);
                        mSession.onAuthCreated(mSession.getAuthInfo());
                    } else {
                        // Fail to get information of current user. current use no longer valid.
                        mSession.mAuthInfo.setUser(null);
                        launchAuthUI();
                    }
                }

                return mSession;
            }
        }

        private void launchAuthUI() {
            authLatch = new CountDownLatch(1);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {

                    if (mSession.getRefreshProvider() != null && mSession.getRefreshProvider().launchAuthUi(mSession.getUserId(), mSession)) {
                        // Do nothing authentication ui will be handled by developer.
                    } else {
                        mSession.startAuthenticationUI();
                    }
                }
            });
            try {
                authLatch.await();
            } catch (InterruptedException e) {
                authLatch.countDown();
            }
        }

        @Override
        public void onRefreshed(BoxAuthentication.BoxAuthenticationInfo info) {
            // Do not implement, this class itself only handles auth creation, regardless success or not, failure should be handled by caller.
        }

        @Override
        public void onAuthCreated(BoxAuthentication.BoxAuthenticationInfo info) {
            BoxAuthentication.BoxAuthenticationInfo.cloneInfo(mSession.mAuthInfo, info);
            mSession.setUserId(info.getUser().getId());
            mSession.onAuthCreated(info);
            authLatch.countDown();
        }

        @Override
        public void onAuthFailure(BoxAuthentication.BoxAuthenticationInfo info, Exception ex) {
            authLatch.countDown();
            // Do not implement, this class itself only handles auth creation, regardless success or not, failure should be handled by caller.
        }

        @Override
        public void onLoggedOut(BoxAuthentication.BoxAuthenticationInfo info, Exception ex) {
            // Do not implement, this class itself only handles auth creation, regardless success or not, failure should be handled by caller.
        }
    }
}
