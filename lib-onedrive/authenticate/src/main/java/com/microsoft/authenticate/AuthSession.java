package com.microsoft.authenticate;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Represents an authentication session.
 */
@SuppressWarnings("UnusedDeclaration")
public class AuthSession {

    /**
     * The access token
     */
    private String mAccessToken;

    /**
     * The authentication token
     */
    private String mAuthenticationToken;

    /** Keeps track of all the listeners, and fires the property change events */
    private final PropertyChangeSupport mChangeSupport;

    /**
     * The AuthClient that created this object.
     * This is needed in order to perform a refresh request.
     * There is a one-to-one relationship between the AuthSession and AuthClient.
     */
    private final AuthClient mCreator;

    /**
     * The date when the auth session will expire
     */
    private Date mExpiresIn;

    /**
     * The refresh token
     */
    private String mRefreshToken;

    /**
     * The scopes
     */
    private Set<String> mScopes;

    /**
     * The type of the token
     */
    private String mTokenType;

    /**
     * Constructors a new AuthSession, and sets its creator to the passed in
     * AuthClient. All other member variables are left uninitialized.
     *
     * @param creator The creating auth client
     */
    AuthSession(final AuthClient creator) {
        mCreator = creator;
        mChangeSupport = new PropertyChangeSupport(this);
    }

    /**
     * Adds a {@link PropertyChangeListener} to the session that receives notification when any
     * property is changed.
     *
     * @param listener the listener
     */
    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }

        mChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Adds a {@link PropertyChangeListener} to the session that receives notification when a
     * specific property is changed.
     *
     * @param propertyName The property name
     * @param listener The listener
     */
    public void addPropertyChangeListener(final String propertyName, final PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }

        mChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Gets the acces token
     * @return The access token for the signed-in, connected user.
     */
    public String getAccessToken() {
        return mAccessToken;
    }

    /**
     * Gets the authentication token
     * @return A user-specific token that provides information to an app so that it can validate
     *         the user.
     */
    public String getAuthenticationToken() {
        return mAuthenticationToken;
    }

    /**
     * Gets the expiration date for this session
     * @return The exact time when a session expires.
     */
    public Date getExpiresIn() {
        // Defensive copy
        return new Date(mExpiresIn.getTime());
    }

    /**
     * Gets the property change listeners
     * @return An array of all PropertyChangeListeners for this session.
     */
    public PropertyChangeListener[] getPropertyChangeListeners() {
        return mChangeSupport.getPropertyChangeListeners();
    }

    /**
     * Gets ths specific property change listener
     * @param propertyName The property name
     * @return An array of all PropertyChangeListeners for a specific property for this session.
     */
    public PropertyChangeListener[] getPropertyChangeListeners(final String propertyName) {
        return mChangeSupport.getPropertyChangeListeners(propertyName);
    }

    /**
     * The refresh token
     * @return A user-specific refresh token that the app can use to refresh the access token.
     */
    public String getRefreshToken() {
        return mRefreshToken;
    }

    /**
     * The scopes
     * @return The scopes that the user has consented to.
     */
    public Iterable<String> getScopes() {
        // Defensive copy is not necessary, because scopes is an unmodifiableSet
        return mScopes;
    }

    /**
     * The token type
     * @return The type of token.
     */
    public String getTokenType() {
        return mTokenType;
    }

    /**
     * If this session is expired
     * @return {@code true} if the session is expired.
     */
    public boolean isExpired() {
        if (mExpiresIn == null) {
            return true;
        }

        final Date now = new Date();

        return now.after(mExpiresIn);
    }

    /**
     * Removes a PropertyChangeListeners on a session.
     * @param listener The listener
     */
    public void removePropertyChangeListener(final PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }

        mChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Removes a PropertyChangeListener for a specific property on a session.
     * @param propertyName The property name
     * @param listener The listener
     */
    public void removePropertyChangeListener(final String propertyName,
                                             final PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }

        mChangeSupport.removePropertyChangeListener(propertyName, listener);
    }

    @Override
    public String toString() {
        return String.format("AuthSession [accessToken=%s, authenticationToken=%s,"
                    + "expiresIn=%s, refreshToken=%s, scopes=%s, tokenType=%s]",
                mAccessToken,
                mAuthenticationToken,
                mExpiresIn,
                mRefreshToken,
                mScopes,
                mTokenType);
    }

    /**
     * Check if the scopes are found in this session
     * @param scopes The scopes to check for
     * @return If they are all contained within this session
     */
    boolean contains(final Iterable<String> scopes) {
        if (scopes == null) {
            return true;
        } else if (mScopes == null) {
            return false;
        }

        for (final String scope : scopes) {
            if (!mScopes.contains(scope)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Fills in the AuthSession with the OAuthResponse.
     * WARNING: The OAuthResponse must not contain OAuth.ERROR.
     *
     * @param response to load from
     */
    void loadFromOAuthResponse(final OAuthSuccessfulResponse response) {
        mAccessToken = response.getAccessToken();
        mTokenType = response.getTokenType().toString().toLowerCase(Locale.US);

        if (response.hasAuthenticationToken()) {
            mAuthenticationToken = response.getAuthenticationToken();
        }

        if (response.hasExpiresIn()) {
            final Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, response.getExpiresIn());
            setExpiresIn(calendar.getTime());
        }

        if (response.hasRefreshToken()) {
            mRefreshToken = response.getRefreshToken();
        }

        if (response.hasScope()) {
            final String scopeString = response.getScope();
            setScopes(Arrays.asList(scopeString.split(OAuth.SCOPE_DELIMITER)));
        }
    }

    /**
     * Refreshes this AuthSession
     *
     * @return true if it was able to refresh the refresh token.
     */
    boolean refresh() {
        return mCreator.refresh();
    }

    /**
     * Sets the access token
     * @param accessToken The new value
     */
    void setAccessToken(final String accessToken) {
        final String oldValue = mAccessToken;
        mAccessToken = accessToken;

        mChangeSupport.firePropertyChange("accessToken", oldValue, mAccessToken);
    }

    /**
     * Sets the authentication token
     * @param authenticationToken The new value
     */
    void setAuthenticationToken(final String authenticationToken) {
        final String oldValue = mAuthenticationToken;
        mAuthenticationToken = authenticationToken;

        mChangeSupport.firePropertyChange("authenticationToken",
                oldValue,
                mAuthenticationToken);
    }

    /**
     * Sets the expiration time
     * @param expiresIn The new value
     */
    void setExpiresIn(final Date expiresIn) {
        final Date oldValue = mExpiresIn;
        mExpiresIn = new Date(expiresIn.getTime());

        mChangeSupport.firePropertyChange("expiresIn", oldValue, mExpiresIn);
    }

    /**
     * Sets the refresh token
     * @param refreshToken The new value
     */
    void setRefreshToken(final String refreshToken) {
        final String oldValue = mRefreshToken;
        mRefreshToken = refreshToken;

        mChangeSupport.firePropertyChange("refreshToken", oldValue, mRefreshToken);
    }

    /**
     * Sets the scopes
     * @param scopes The new value
     */
    void setScopes(final Iterable<String> scopes) {
        final Iterable<String> oldValue = mScopes;

        // Defensive copy
        mScopes = new HashSet<>();
        if (scopes != null) {
            for (String scope : scopes) {
                mScopes.add(scope);
            }
        }

        mScopes = Collections.unmodifiableSet(mScopes);

        mChangeSupport.firePropertyChange("scopes", oldValue, mScopes);
    }

    /**
     * Sets the token type
     * @param tokenType the new value
     */
    void setTokenType(final String tokenType) {
        final String oldValue = mTokenType;
        mTokenType = tokenType;

        mChangeSupport.firePropertyChange("tokenType", oldValue, mTokenType);
    }

    /**
     * Determine if in the given number of seconds the session will expire
     * @param secs The seconds to check
     * @return If this session will be expired after those number of seconds
     */
    boolean willExpireInSecs(final int secs) {
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, secs);

        final Date future = calendar.getTime();

        // if add secs seconds to the current time and it is after the expired time
        // then it is almost expired.
        return future.after(mExpiresIn);
    }
}