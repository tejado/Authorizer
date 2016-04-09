package com.box.androidsdk.content.auth;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.box.androidsdk.content.BoxApiUser;
import com.box.androidsdk.content.BoxConfig;
import com.box.androidsdk.content.BoxConstants;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.BoxFutureTask;
import com.box.androidsdk.content.models.BoxCollaborator;
import com.box.androidsdk.content.models.BoxJsonObject;
import com.box.androidsdk.content.models.BoxMapJsonObject;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxUser;
import com.box.androidsdk.content.requests.BoxResponse;
import com.box.androidsdk.content.utils.BoxLogUtils;
import com.box.androidsdk.content.utils.SdkUtils;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Handles authentication and stores authentication information.
 */
public class BoxAuthentication {

    // Third parties who are looking to provide their own refresh logic should replace this with the constructor that takea refreshProvider.
    private static BoxAuthentication mAuthentication = new BoxAuthentication();

    private ConcurrentLinkedQueue<WeakReference<AuthListener>> mListeners = new ConcurrentLinkedQueue<WeakReference<AuthListener>>();

    private ConcurrentHashMap<String, BoxAuthenticationInfo> mCurrentAccessInfo;

    private ConcurrentHashMap<String, FutureTask> mRefreshingTasks = new ConcurrentHashMap<String, FutureTask>();

    public static final ThreadPoolExecutor AUTH_EXECUTOR = SdkUtils.createDefaultThreadPoolExecutor(1, 1, 3600, TimeUnit.SECONDS);

    private AuthenticationRefreshProvider mRefreshProvider;
    private static String TAG = BoxAuthentication.class.getName();
    private int EXPIRATION_GRACE = 1000;

    private AuthStorage authStorage = new AuthStorage();

    private BoxAuthentication() {
    }

    private BoxAuthentication(final AuthenticationRefreshProvider refreshProvider) {
        mRefreshProvider = refreshProvider;
    }

    /**
     * Get the BoxAuthenticationInfo for a given user.
     */
    public BoxAuthenticationInfo getAuthInfo(String userId, Context context) {
        return userId == null ? null : getAuthInfoMap(context).get(userId);
    }

    /**
     * Get a map of all stored auth information.
     *
     * @param context current context.
     * @return a map with all stored user information, or null if no user info has been stored.
     */
    public Map<String, BoxAuthenticationInfo> getStoredAuthInfo(final Context context) {
        return getAuthInfoMap(context);
    }

    /**
     * Get the user id that was last authenticated.
     *
     * @param context current context.
     * @return the user id that was last authenticated or null if it does not exist or was removed.
     */
    public String getLastAuthenticatedUserId(final Context context) {
        return authStorage.getLastAuthentictedUserId(context);
    }

    /**
     * Get singleton instance of the BoxAuthentication object.
     */
    public static BoxAuthentication getInstance() {
        return mAuthentication;
    }

    /**
     * Set the storage to store auth information. By default, sharedpref is used. You can use this method to use your own storage class that extends the AuthStorage.
     */
    public void setAuthStorage(AuthStorage storage) {
        this.authStorage = storage;
    }

    /**
     * Get the auth storage used to store auth information.
     */
    public AuthStorage getAuthStorage() {
        return authStorage;
    }

    public synchronized void startAuthenticationUI(BoxSession session) {
        startAuthenticateUI(session);
    }

    /**
     * Callback method to be called when authentication process finishes.
     */
    public synchronized void onAuthenticated(BoxAuthenticationInfo info, Context context) {
        getAuthInfoMap(context).put(info.getUser().getId(), info.clone());
        authStorage.storeLastAuthenticatedUserId(info.getUser().getId(), context);
        authStorage.storeAuthInfoMap(mCurrentAccessInfo, context);
        // if accessToken has not already been refreshed, issue refresh request and cache result
        Set<AuthListener> listeners = getListeners();
        for (AuthListener listener : listeners) {
            listener.onAuthCreated(info);
        }
    }

    /**
     * Callback method to be called if authentication process fails.
     */
    public synchronized void onAuthenticationFailure(BoxAuthenticationInfo info, Exception ex) {
        Set<AuthListener> listeners = getListeners();
        for (AuthListener listener : listeners) {
            listener.onAuthFailure(info, ex);
        }
    }

    /**
     * Callback method to be called on logout.
     */
    public synchronized void onLoggedOut(BoxAuthenticationInfo info, Exception ex) {
        Set<AuthListener> listeners = getListeners();
        for (AuthListener listener : listeners) {
            listener.onLoggedOut(info, ex);
        }
    }

    public Set<AuthListener> getListeners() {
        Set<AuthListener> listeners = new LinkedHashSet<AuthListener>();
        for (WeakReference<AuthListener> reference : mListeners) {
            AuthListener rc = reference.get();
            if (rc != null) {
                listeners.add(rc);
            }
        }
        if (mListeners.size() > listeners.size()) {
            //clean up mListeners
            mListeners = new ConcurrentLinkedQueue<WeakReference<AuthListener>>();
            for (AuthListener listener : listeners) {
                mListeners.add(new WeakReference<AuthListener>(listener));
            }
        }
        return listeners;
    }

    private void clearCache(BoxSession session) {
        File cacheDir = session.getCacheDir();
        if (cacheDir.exists()) {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteFilesRecursively(child);
                }
            }
        }
    }

    private void deleteFilesRecursively(File fileOrDirectory) {
        if (fileOrDirectory != null) {
            if (fileOrDirectory.isDirectory()) {
                File[] files = fileOrDirectory.listFiles();
                if (files != null) {
                    for (File child : files) {
                        deleteFilesRecursively(child);
                    }
                }
            }
            fileOrDirectory.delete();
        }
    }

    /**
     * Log out current BoxSession. After logging out, the authentication information related to the Box user in this session will be gone.
     */
    public synchronized void logout(final BoxSession session) {
        BoxUser user = session.getUser();
        if (user == null) {
            return;
        }
        clearCache(session);

        Context context = session.getApplicationContext();
        String userId = user.getId();

        getAuthInfoMap(session.getApplicationContext());
        BoxAuthenticationInfo info = mCurrentAccessInfo.get(userId);
        Exception ex = null;

        try {
            BoxApiAuthentication api = new BoxApiAuthentication(session);
            BoxApiAuthentication.BoxRevokeAuthRequest request = api.revokeOAuth(info.refreshToken(), session.getClientId(), session.getClientSecret());
            request.send();
        } catch (Exception e) {
            ex = e;
            BoxLogUtils.e(TAG, "logout", e);
            // Do nothing as we want to continue wiping auth info
        }
        mCurrentAccessInfo.remove(userId);
        String lastUserId = authStorage.getLastAuthentictedUserId(context);
        if (lastUserId != null && userId.equals(userId)) {
            authStorage.storeLastAuthenticatedUserId(null, context);
        }
        authStorage.storeAuthInfoMap(mCurrentAccessInfo, context);
        onLoggedOut(info, ex);
    }

    /**
     * Log out all users. After logging out, all authentication information will be gone.
     */
    public synchronized void logoutAllUsers(Context context) {
        getAuthInfoMap(context);
        for (String userId : mCurrentAccessInfo.keySet()) {
            BoxSession session = new BoxSession(context, userId);
            logout(session);
        }
    }

    /**
     * Create Oauth for the first time. This method should be called by ui to authenticate the user for the first time.
     * @param session a box session with all the necessary information to authenticate the user for the first time.
     * @param code the code returned by web page necessary to authenticate.
     * @return a future task allowing monitoring of the api call.
     */
    public synchronized FutureTask<BoxAuthenticationInfo> create(BoxSession session, final String code) throws BoxException{
        FutureTask<BoxAuthenticationInfo> task = doCreate(session,code);
        BoxAuthentication.AUTH_EXECUTOR.submit(task);
        return task;
    }

    /**
     * Refresh the OAuth in the given BoxSession. This method is called when OAuth token expires.
     */
    public synchronized FutureTask<BoxAuthenticationInfo> refresh(BoxSession session) throws BoxException {
        BoxUser user = session.getUser();
        if (user == null) {
            return doRefresh(session, session.getAuthInfo());
        }
        // Fetch auth info map from storage if not present.
        getAuthInfoMap(session.getApplicationContext());
        BoxAuthenticationInfo info = mCurrentAccessInfo.get(user.getId());

        if (info == null) {
            // session has info that we do not. ? is there any other situation we want to update our info based on session info? we can do checks against
            // refresh time.
            mCurrentAccessInfo.put(user.getId(), session.getAuthInfo());
            info = mCurrentAccessInfo.get(user.getId());
        }

        if (!session.getAuthInfo().accessToken().equals(info.accessToken())) {
            final BoxAuthenticationInfo latestInfo = info;
            // this session is probably using old information. Give it our information.
            BoxAuthenticationInfo.cloneInfo(session.getAuthInfo(), info);
            return new FutureTask<BoxAuthenticationInfo>(new Callable<BoxAuthenticationInfo>() {
                @Override
                public BoxAuthenticationInfo call() throws Exception {
                    return latestInfo;
                }
            });
        }

        FutureTask task = mRefreshingTasks.get(user.getId());
        if (task != null) {
            // We already have a refreshing task for this user. No need to do anything.
            return task;
        }

        // long currentTime = System.currentTimeMillis();
        // if ((currentTime - info.refreshTime) > info.refreshTime - EXPIRATION_GRACE) {
        // this access info is close to expiration or has passed expiration time needs to be refreshed before usage.
        // }
        // create the task to do the refresh and put it in mRefreshingTasks and execute it.
        return doRefresh(session, info);

    }


    private FutureTask<BoxAuthenticationInfo> doCreate(final BoxSession session, final String code)  {
        FutureTask<BoxAuthenticationInfo> task = new FutureTask<BoxAuthenticationInfo>(new Callable<BoxAuthenticationInfo>() {
            @Override
            public BoxAuthenticationInfo call() throws Exception {
                BoxApiAuthentication api = new BoxApiAuthentication(session);
                BoxApiAuthentication.BoxCreateAuthRequest request = api.createOAuth(code, session.getClientId(), session.getClientSecret());
                BoxAuthenticationInfo info = new BoxAuthenticationInfo();
                BoxAuthenticationInfo.cloneInfo(info, session.getAuthInfo());
                BoxAuthenticationInfo authenticatedInfo = request.send();
                info.setAccessToken(authenticatedInfo.accessToken());
                info.setRefreshToken(authenticatedInfo.refreshToken());

                info.setRefreshTime(System.currentTimeMillis());

                BoxSession tempSession = new BoxSession(session.getApplicationContext(), info, null);
                BoxApiUser userApi = new BoxApiUser(tempSession);
                BoxUser user = userApi.getCurrentUserInfoRequest().send();
                info.setUser(user);

                BoxAuthentication.getInstance().onAuthenticated(info, session.getApplicationContext());
                return info;
            }
        });
        return task;


    }



    /**
     * Add listener to listen to the authentication process for this BoxSession.
     */
    public synchronized void addListener(AuthListener listener) {
        mListeners.add(new WeakReference<AuthListener>(listener));
    }

    /**
     * Start authentication UI.
     *
     * @param session the session to authenticate.
     */
    protected synchronized void startAuthenticateUI(BoxSession session) {
        Context context = session.getApplicationContext();
        Intent intent = OAuthActivity.createOAuthActivityIntent(context, session, BoxAuthentication.isBoxAuthAppAvailable(context) && session.isEnabledBoxAppAuthentication());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private BoxException.RefreshFailure handleRefreshException(final BoxException e, final BoxAuthenticationInfo info) {
        BoxException.RefreshFailure refreshFailure = new BoxException.RefreshFailure(e);
        BoxAuthentication.getInstance().onAuthenticationFailure(info, refreshFailure);
        return refreshFailure;
    }

    private FutureTask<BoxAuthenticationInfo> doRefresh(final BoxSession session, final BoxAuthenticationInfo info) throws BoxException {
        final boolean userUnknown = (info.getUser() == null && session.getUser() == null);
        final String taskKey = SdkUtils.isBlank(session.getUserId()) && userUnknown ? info.accessToken() : session.getUserId();
        FutureTask<BoxAuthenticationInfo> task = new FutureTask<BoxAuthenticationInfo>(new Callable<BoxAuthenticationInfo>() {
            @Override
            public BoxAuthenticationInfo call() throws Exception {
                BoxAuthenticationInfo refreshInfo = null;
                if (session.getRefreshProvider() != null) {
                    try {
                        refreshInfo = session.getRefreshProvider().refreshAuthenticationInfo(info);
                    } catch (BoxException e) {
                        throw handleRefreshException(e, info);
                    }
                } else if (mRefreshProvider != null) {
                    try {
                        refreshInfo = mRefreshProvider.refreshAuthenticationInfo(info);
                    } catch (BoxException e) {
                        throw handleRefreshException(e, info);
                    }
                } else {
                    String refreshToken = info.refreshToken() != null ? info.refreshToken() : "";
                    String clientId = session.getClientId() != null ? session.getClientId() : BoxConfig.CLIENT_ID;
                    String clientSecret = session.getClientSecret() != null ? session.getClientSecret() : BoxConfig.CLIENT_SECRET;
                    if (SdkUtils.isBlank(clientId) || SdkUtils.isBlank(clientSecret)) {
                        BoxException badRequest = new BoxException("client id or secret not specified", 400, "{\"error\": \"bad_request\",\n" +
                                "  \"error_description\": \"client id or secret not specified\"}", null);
                        throw handleRefreshException(badRequest, info);
                    }

                    BoxApiAuthentication.BoxRefreshAuthRequest request = new BoxApiAuthentication(session).refreshOAuth(refreshToken, clientId, clientSecret);
                    try {
                        refreshInfo = request.send();
                    } catch (BoxException e) {
                        throw handleRefreshException(e, info);
                    }
                }
                if (refreshInfo != null) {
                    refreshInfo.setRefreshTime(System.currentTimeMillis());
                }
                BoxAuthenticationInfo.cloneInfo(session.getAuthInfo(), refreshInfo);
                // if we using a custom refresh provider ensure we check the user, otherwise do this only if we don't know who the user is.
                if (userUnknown || session.getRefreshProvider() != null || mRefreshProvider != null) {
                    BoxApiUser userApi = new BoxApiUser(session);
                    info.setUser(userApi.getCurrentUserInfoRequest().send());
                }

                getAuthInfoMap(session.getApplicationContext()).put(info.getUser().getId(), refreshInfo);
                authStorage.storeAuthInfoMap(mCurrentAccessInfo, session.getApplicationContext());
                // call notifyListeners() with results.
                for (WeakReference<AuthListener> reference : mListeners) {
                    AuthListener rc = reference.get();
                    if (rc != null) {
                        rc.onRefreshed(refreshInfo);
                    }
                }
                if (!session.getUserId().equals(info.getUser().getId())) {
                    session.onAuthFailure(info, new BoxException("Session User Id has changed!"));
                }

                mRefreshingTasks.remove(taskKey);

                return info;
            }
        });
        mRefreshingTasks.put(taskKey, task);
        AUTH_EXECUTOR.execute(task);
        return task;


    }

    private ConcurrentHashMap<String, BoxAuthenticationInfo> getAuthInfoMap(Context context) {
        if (mCurrentAccessInfo == null) {
            mCurrentAccessInfo = authStorage.loadAuthInfoMap(context);
        }
        return mCurrentAccessInfo;
    }

    /**
     * Interface of a listener to listen to authentication events.
     */
    public interface AuthListener {

        void onRefreshed(BoxAuthenticationInfo info);

        void onAuthCreated(BoxAuthenticationInfo info);

        void onAuthFailure(BoxAuthentication.BoxAuthenticationInfo info, Exception ex);

        void onLoggedOut(BoxAuthentication.BoxAuthenticationInfo info, Exception ex);
    }

    /**
     * An interface that should be implemented if using a custom authentication scheme and not the default oauth 2 based token refresh logic.
     */
    public static interface AuthenticationRefreshProvider {

        /**
         * This method should return a refreshed authentication info object given one that is expired or nearly expired.
         *
         * @param info the expired authentication information.
         * @return a refreshed BoxAuthenticationInfo object. The object must include the valid access token.
         * @throws BoxException Exception that should be thrown if there was a problem fetching the information.
         */
        public BoxAuthenticationInfo refreshAuthenticationInfo(BoxAuthenticationInfo info) throws BoxException;

        /**
         * This method should launch an activity or perform necessary logic in order to authenticate a user for the first time or re-authenticate a user if necessary.
         * Implementers should call BoxAuthenciation.getInstance().onAuthenticated(BoxAuthenticationInfo info, Context context) to complete authentication.
         *
         * @param userId  the user id that needs re-authentication if known. For a new user this will be null.
         * @param session the session that is attempting to launch authentication ui.
         * @return true if the ui is handled, if false the default authentication ui will be shown to authenticate the user.
         */
        public boolean launchAuthUi(String userId, BoxSession session);

    }

    /**
     * Object holding authentication info.
     */
    public static class BoxAuthenticationInfo extends BoxJsonObject {

        private static final long serialVersionUID = 2878150977399126399L;

        private static final String FIELD_REFRESH_TIME = "refresh_time";

        public static final String FIELD_CLIENT_ID = "client_id";

        public static final String FIELD_ACCESS_TOKEN = "access_token";
        public static final String FIELD_REFRESH_TOKEN = "refresh_token";
        public static final String FIELD_EXPIRES_IN = "expires_in";
        public static final String FIELD_USER = "user";

        public static final String FIELD_BASE_DOMAIN = "base_domain";

        /**
         * Constructs an empty BoxAuthenticationInfo object.
         */
        public BoxAuthenticationInfo() {
            super();
        }

        /**
         * Creates a clone of a BoxAuthenticationInfo object.
         *
         * @return clone of BoxAuthenticationInfo object.
         */
        public BoxAuthenticationInfo clone() {
            BoxAuthenticationInfo cloned = new BoxAuthenticationInfo();
            cloneInfo(cloned, this);
            return cloned;
        }

        /**
         * Clone BoxAuthenticationInfo from source object into target object. Note that this method assumes the two objects have same user.
         * Otherwise it would not make sense to do a clone operation.
         */
        public static void cloneInfo(BoxAuthenticationInfo targetInfo, BoxAuthenticationInfo sourceInfo) {
            targetInfo.setAccessToken(sourceInfo.accessToken());
            targetInfo.setRefreshToken(sourceInfo.refreshToken());
            targetInfo.setRefreshTime(sourceInfo.getRefreshTime());
            targetInfo.setClientId(sourceInfo.getClientId());
            targetInfo.setBaseDomain(sourceInfo.getBaseDomain());
            if (targetInfo.getUser() == null) {
                targetInfo.setUser(sourceInfo.getUser());
            }
        }

        public String getClientId() {
            return (String) mProperties.get(FIELD_CLIENT_ID);
        }

        /**
         * OAuth access token.
         */
        public String accessToken() {
            return (String) mProperties.get(FIELD_ACCESS_TOKEN);
        }

        /**
         * OAuth refresh token.
         */
        public String refreshToken() {
            return (String) mProperties.get(FIELD_REFRESH_TOKEN);
        }

        /**
         * Time the oauth is going to expire (in ms).
         */
        public Long expiresIn() {
            return (Long) mProperties.get(FIELD_EXPIRES_IN);
        }

        /**
         * Time the OAuth last refreshed.
         *
         * @return time the OAuth last refreshed.
         */
        public Long getRefreshTime() {
            return (Long) mProperties.get(FIELD_REFRESH_TIME);
        }

        /**
         * Set the refresh time. Called when refresh happened.
         */
        public void setRefreshTime(Long refreshTime) {
            mProperties.put(FIELD_REFRESH_TIME, refreshTime);
        }

        public void setClientId(String clientId) {
            mProperties.put(FIELD_CLIENT_ID, clientId);
        }

        /**
         * Setter for access token.
         */
        public void setAccessToken(String access) {
            mProperties.put(FIELD_ACCESS_TOKEN, access);
        }

        /**
         * Setter for refresh token
         */
        public void setRefreshToken(String refresh) {
            mProperties.put(FIELD_REFRESH_TOKEN, refresh);
        }

        /**
         * Setter for base domain.
         */
        public void setBaseDomain(String baseDomain) {
            mProperties.put(FIELD_BASE_DOMAIN, baseDomain);
        }

        /**
         * Get the base domain associated with this user.
         */
        public String getBaseDomain() {
            return (String) mProperties.get(FIELD_BASE_DOMAIN);
        }

        /**
         * Setter for BoxUser corresponding to this authentication info.
         */
        public void setUser(BoxUser user) {
            mProperties.put(FIELD_USER, user);
        }

        /**
         * Get the BoxUser related to this authentication info.
         */
        public BoxUser getUser() {
            return (BoxUser) mProperties.get(FIELD_USER);
        }

        /**
         * Wipe out all the information in this object.
         */
        public void wipeOutAuth() {
            setUser(null);
            setClientId(null);
            setAccessToken(null);
            setRefreshToken(null);
        }

        @Override
        protected void parseJSONMember(JsonObject.Member member) {
            String memberName = member.getName();
            JsonValue value = member.getValue();
            if (memberName.equals(FIELD_ACCESS_TOKEN)) {
                mProperties.put(FIELD_ACCESS_TOKEN, value.asString());
                return;
            } else if (memberName.equals(FIELD_REFRESH_TOKEN)) {
                mProperties.put(FIELD_REFRESH_TOKEN, value.asString());
                return;
            } else if (memberName.equals(FIELD_USER)) {
                mProperties.put(FIELD_USER, BoxCollaborator.createCollaboratorFromJson(value.asObject()));
                return;
            } else if (memberName.equals(FIELD_EXPIRES_IN)) {
                this.mProperties.put(FIELD_EXPIRES_IN, value.asLong());
                return;
            } else if (memberName.equals(FIELD_REFRESH_TIME)) {
                this.mProperties.put(FIELD_REFRESH_TIME, SdkUtils.parseJsonValueToLong(value));
                return;
            } else if (memberName.equals(FIELD_CLIENT_ID)) {
                this.mProperties.put(FIELD_CLIENT_ID, value.asString());
                return;
            }

            super.parseJSONMember(member);
        }
    }

    /**
     * Storage class to store auth info. Note this class uses shared pref as storage. You can extend this class and use your own
     * preferred storage and call setAuthStorage() method in BoxAuthentication class to use your own storage.
     */
    public static class AuthStorage {
        private static final String AUTH_STORAGE_NAME = AuthStorage.class.getCanonicalName() + "_SharedPref";
        private static final String AUTH_MAP_STORAGE_KEY = AuthStorage.class.getCanonicalName() + "_authInfoMap";
        private static final String AUTH_STORAGE_LAST_AUTH_USER_ID_KEY = AuthStorage.class.getCanonicalName() + "_lastAuthUserId";

        /**
         * Store the auth info into storage.
         *
         * @param authInfo auth info to store.
         * @param context  context here is only used to load shared pref. In case you don't need shared pref, you can ignore this
         *                 argument in your implementation.
         */
        protected void storeAuthInfoMap(Map<String, BoxAuthenticationInfo> authInfo, Context context) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            for (String key : authInfo.keySet()) {
                map.put(key, authInfo.get(key));
            }
            BoxMapJsonObject infoMapObj = new BoxMapJsonObject(map);
            context.getSharedPreferences(AUTH_STORAGE_NAME, Context.MODE_PRIVATE).edit().putString(AUTH_MAP_STORAGE_KEY, infoMapObj.toJson()).apply();
        }

        /**
         * Removes auth info from storage.
         *
         * @param context context here is only used to load shared pref. In case you don't need shared pref, you can ignore this
         *                argument in your implementation.
         */
        protected void clearAuthInfoMap(Context context) {
            context.getSharedPreferences(AUTH_STORAGE_NAME, Context.MODE_PRIVATE).edit().remove(AUTH_MAP_STORAGE_KEY).apply();
        }

        /**
         * Store out the last user id that the user authenticated as. This will be the one that is restored if no user is specified for a BoxSession.
         *
         * @param userId  user id of the last authenticated user. null if this data should be removed.
         * @param context context here is only used to load shared pref. In case you don't need shared pref, you can ignore this
         *                argument in your implementation.
         */
        protected void storeLastAuthenticatedUserId(String userId, Context context) {
            if (SdkUtils.isEmptyString(userId)) {
                context.getSharedPreferences(AUTH_STORAGE_NAME, Context.MODE_PRIVATE).edit().remove(AUTH_STORAGE_LAST_AUTH_USER_ID_KEY).apply();
            } else {
                context.getSharedPreferences(AUTH_STORAGE_NAME, Context.MODE_PRIVATE).edit().putString(AUTH_STORAGE_LAST_AUTH_USER_ID_KEY, userId).apply();
            }
        }

        /**
         * Return the last user id associated with the last authentication.
         *
         * @param context context here is only used to load shared pref. In case you don't need shared pref, you can ignore this
         *                argument in your implementation.
         * @return the user id of the last authenticated user or null if not stored or the user has since been logged out.
         */
        protected String getLastAuthentictedUserId(Context context) {
            return context.getSharedPreferences(AUTH_STORAGE_NAME, Context.MODE_PRIVATE).getString(AUTH_STORAGE_LAST_AUTH_USER_ID_KEY, null);
        }

        /**
         * Load auth info from storage.
         *
         * @param context context here is only used to load shared pref. In case you don't need shared pref, you can ignore this
         *                argument in your implementation.
         */
        protected ConcurrentHashMap<String, BoxAuthenticationInfo> loadAuthInfoMap(Context context) {
            ConcurrentHashMap<String, BoxAuthenticationInfo> map = new ConcurrentHashMap<String, BoxAuthenticationInfo>();
            String json = context.getSharedPreferences(AUTH_STORAGE_NAME, 0).getString(AUTH_MAP_STORAGE_KEY, "");
            if (json.length() > 0) {
                BoxMapJsonObject obj = new BoxMapJsonObject();
                obj.createFromJson(json);
                HashMap<String, Object> parsed = obj.getPropertiesAsHashMap();
                for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                    BoxAuthenticationInfo info = null;
                    if (entry.getValue() instanceof String) {
                        info = new BoxAuthenticationInfo();
                        info.createFromJson((String) entry.getValue());
                    } else if (entry.getValue() instanceof BoxAuthenticationInfo) {
                        info = (BoxAuthenticationInfo) entry.getValue();
                    }
                    map.put(entry.getKey(), info);
                }
            }
            return map;
        }
    }


    /**
     * A check to see if an official box application supporting third party authentication is available.
     * This lets users authenticate without re-entering credentials.
     *
     * @param context current context
     * @returntrue if an official box application that supports third party authentication is installed.
     */
    public static boolean isBoxAuthAppAvailable(final Context context) {
        Intent intent = new Intent(BoxConstants.REQUEST_BOX_APP_FOR_AUTH_INTENT_ACTION);
        List<ResolveInfo> infos = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY | PackageManager.GET_RESOLVED_FILTER);
        if (infos.size() > 0) {
            return true;
        }
        return false;
    }
}
