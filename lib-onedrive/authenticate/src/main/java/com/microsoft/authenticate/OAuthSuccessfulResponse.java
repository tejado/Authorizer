package com.microsoft.authenticate;

import java.util.Locale;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.TextUtils;

import com.microsoft.authenticate.OAuth.TokenType;

/**
 * OAuthSuccessfulResponse represents a successful response form an OAuth server.
 */
final class OAuthSuccessfulResponse implements OAuthResponse {

    /**
     * Builder is a utility class that is used to build a new OAuthSuccessfulResponse.
     * It must be constructed with the required fields, and can add on the optional ones.
     */
    public static class Builder {
        /**
         * The access token
         */
        private final String mAccessToken;

        /**
         * The authentication token
         */
        private String mAuthenticationToken;

        /**
         * The expiration time
         */
        private int mExpiresIn = UNINITIALIZED;

        /**
         * The refresh token
         */
        private String mRefreshToken;

        /**
         * The scopes
         */
        private String mScope;

        /**
         * The token type
         */
        private final TokenType mTokenType;

        /**
         * Default constructor
         * @param accessToken The access token
         * @param tokenType The type of the token
         */
        public Builder(final String accessToken, final TokenType tokenType) {
            mAccessToken = accessToken;
            mTokenType = tokenType;
        }

        /**
         * Sets the authentication token
         * @param authenticationToken The value
         * @return The builder
         */
        public Builder authenticationToken(final String authenticationToken) {
            mAuthenticationToken = authenticationToken;
            return this;
        }

        /**
         * Builds an instance of OAuthSuccessfulResponse
         * @return a new instance of an OAuthSuccessfulResponse with the given
         *         parameters passed into the builder.
         */
        public OAuthSuccessfulResponse build() {
            return new OAuthSuccessfulResponse(this);
        }

        /**
         * Sets the expiration
         * @param expiresIn The value
         * @return The builder
         */
        public Builder expiresIn(final int expiresIn) {
            mExpiresIn = expiresIn;
            return this;
        }

        /**
         * Sets the refresh token
         * @param refreshToken The value
         * @return The builder
         */
        public Builder refreshToken(final String refreshToken) {
            mRefreshToken = refreshToken;
            return this;
        }

        /**
         * Sets the scopes
         * @param scope The value
         * @return The builder
         */
        public Builder scope(final String scope) {
            mScope = scope;
            return this;
        }
    }

    /** Used to declare mExpiresIn uninitialized */
    private static final int UNINITIALIZED = -1;

    /**
     * Creates a response object from a set of fragments
     * @param fragmentParameters The parameters
     * @return The OAuthSuccessfulResponse
     * @throws AuthException If the response was not successful
     */
    public static OAuthSuccessfulResponse createFromFragment(
            final Map<String, String> fragmentParameters) throws AuthException {
        final String accessToken = fragmentParameters.get(OAuth.ACCESS_TOKEN);
        final String tokenTypeString = fragmentParameters.get(OAuth.TOKEN_TYPE);

        final TokenType tokenType;
        try {
            tokenType = TokenType.valueOf(tokenTypeString.toUpperCase(Locale.US));
        } catch (final IllegalArgumentException e) {
            throw new AuthException(ErrorMessages.SERVER_ERROR, e);
        }

        final OAuthSuccessfulResponse.Builder builder =
                new OAuthSuccessfulResponse.Builder(accessToken, tokenType);

        final String authenticationToken = fragmentParameters.get(OAuth.AUTHENTICATION_TOKEN);
        if (authenticationToken != null) {
            builder.authenticationToken(authenticationToken);
        }

        final String expiresInString = fragmentParameters.get(OAuth.EXPIRES_IN);
        if (expiresInString != null) {
            final int expiresIn;
            try {
                expiresIn = Integer.parseInt(expiresInString);
            } catch (final NumberFormatException e) {
                throw new AuthException(ErrorMessages.SERVER_ERROR, e);
            }

            builder.expiresIn(expiresIn);
        }

        final String scope = fragmentParameters.get(OAuth.SCOPE);
        if (scope != null) {
            builder.scope(scope);
        }

        return builder.build();
    }

    /**
     * Static constructor used to create a new OAuthSuccessfulResponse from an
     * OAuth server's JSON response.
     *
     * @param response from an OAuth server that is used to create the object.
     * @return a new instance of OAuthSuccessfulResponse that is created from the given JSONObject
     * @throws AuthException if there is a JSONException or the token_type is unknown.
     */
    public static OAuthSuccessfulResponse createFromJson(final JSONObject response)
            throws AuthException {
        final String accessToken;
        try {
            accessToken = response.getString(OAuth.ACCESS_TOKEN);
        } catch (final JSONException e) {
            throw new AuthException(ErrorMessages.SERVER_ERROR, e);
        }

        final String tokenTypeString;
        try {
            tokenTypeString = response.getString(OAuth.TOKEN_TYPE);
        } catch (final JSONException e) {
            throw new AuthException(ErrorMessages.SERVER_ERROR, e);
        }

        final TokenType tokenType;
        try {
            tokenType = TokenType.valueOf(tokenTypeString.toUpperCase(Locale.US));
        } catch (final IllegalArgumentException | NullPointerException e) {
            throw new AuthException(ErrorMessages.SERVER_ERROR, e);
        }

        final Builder builder = new Builder(accessToken, tokenType);

        if (response.has(OAuth.AUTHENTICATION_TOKEN)) {
            final String authenticationToken;
            try {
                authenticationToken = response.getString(OAuth.AUTHENTICATION_TOKEN);
            } catch (final JSONException e) {
                throw new AuthException(ErrorMessages.CLIENT_ERROR, e);
            }
            builder.authenticationToken(authenticationToken);
        }

        if (response.has(OAuth.REFRESH_TOKEN)) {
            final String refreshToken;
            try {
                refreshToken = response.getString(OAuth.REFRESH_TOKEN);
            } catch (final JSONException e) {
                throw new AuthException(ErrorMessages.CLIENT_ERROR, e);
            }
            builder.refreshToken(refreshToken);
        }

        if (response.has(OAuth.EXPIRES_IN)) {
            final int expiresIn;
            try {
                expiresIn = response.getInt(OAuth.EXPIRES_IN);
            } catch (final JSONException e) {
                throw new AuthException(ErrorMessages.CLIENT_ERROR, e);
            }
            builder.expiresIn(expiresIn);
        }

        if (response.has(OAuth.SCOPE)) {
            final String scope;
            try {
                scope = response.getString(OAuth.SCOPE);
            } catch (final JSONException e) {
                throw new AuthException(ErrorMessages.CLIENT_ERROR, e);
            }
            builder.scope(scope);
        }

        return builder.build();
    }

    /**
     * Validates the AuthSuccessful response
     * @param response The json contents of the response
     * @return true if the given JSONObject has the required fields to construct an
     *         OAuthSuccessfulResponse (i.e., has access_token and token_type)
     */
    public static boolean validOAuthSuccessfulResponse(final JSONObject response) {
        return response.has(OAuth.ACCESS_TOKEN)
               && response.has(OAuth.TOKEN_TYPE);
    }

    /** REQUIRED. The access token issued by the authorization server. */
    private final String mAccessToken;

    /**
     * The authentication token
     */
    private final String mAuthenticationToken;

    /**
     * OPTIONAL.  The lifetime in seconds of the access token.  For
     * example, the value "3600" denotes that the access token will
     * expire in one hour from the time the response was generated.
     */
    private final int mExpiresIn;

    /**
     * OPTIONAL.  The refresh token which can be used to obtain new
     * access tokens using the same authorization grant.
     */
    private final String mRefreshToken;

    /** OPTIONAL. */
    private final String mScope;

    /** REQUIRED. */
    private final TokenType mTokenType;

    /**
     * Private constructor to enforce the user of the builder.
     * @param builder to use to construct the object from.
     */
    private OAuthSuccessfulResponse(final Builder builder) {
        mAccessToken = builder.mAccessToken;
        mAuthenticationToken = builder.mAuthenticationToken;
        mTokenType = builder.mTokenType;
        mRefreshToken = builder.mRefreshToken;
        mExpiresIn = builder.mExpiresIn;
        mScope = builder.mScope;
    }

    @Override
    public void accept(final OAuthResponseVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * Gets the access token
     * @return The value
     */
    public String getAccessToken() {
        return mAccessToken;
    }

    /**
     * Gets the authentication token
     * @return The value
     */
    public String getAuthenticationToken() {
        return mAuthenticationToken;
    }

    /**
     * Gets the expiration time
     * @return The value
     */
    public int getExpiresIn() {
        return mExpiresIn;
    }

    /**
     * Gets the refresh token
     * @return The value
     */
    public String getRefreshToken() {
        return mRefreshToken;
    }

    /**
     * Gets the scope
     * @return The value
     */
    public String getScope() {
        return mScope;
    }

    /**
     * Gets the token type
     * @return The value
     */
    public TokenType getTokenType() {
        return mTokenType;
    }

    /**
     * Is there an authentication token
     * @return The value
     */
    public boolean hasAuthenticationToken() {
        return mAuthenticationToken != null && !TextUtils.isEmpty(mAuthenticationToken);
    }

    /**
     * Gets the access token
     * @return The value
     */
    public boolean hasExpiresIn() {
        return mExpiresIn != UNINITIALIZED;
    }

    /**
     * Is there a refresh token
     * @return The value
     */
    public boolean hasRefreshToken() {
        return mRefreshToken != null && !TextUtils.isEmpty(mRefreshToken);
    }

    /**
     * Is there a scope
     * @return The value
     */
    public boolean hasScope() {
        return mScope != null && !TextUtils.isEmpty(mScope);
    }

    @Override
    public String toString() {
        return String.format("OAuthSuccessfulResponse [mAccessToken=%s, mAuthenticationToken=%s, mTokenType=%s,"
                        + "mRefreshToken=%s, mExpiresIn=%s, mScope=%s]",
                             mAccessToken,
                             mAuthenticationToken,
                             mTokenType,
                             mRefreshToken,
                             mExpiresIn,
                             mScope);
    }
}
