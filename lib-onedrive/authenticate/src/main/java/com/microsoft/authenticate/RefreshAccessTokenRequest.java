package com.microsoft.authenticate;

import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.microsoft.authenticate.OAuth.GrantType;

/**
 * RefreshAccessTokenRequest performs a refresh access token request. Most of the work
 * is done by the parent class, TokenRequest. This class adds in the required body parameters via
 * TokenRequest's hook method, constructBody().
 */
class RefreshAccessTokenRequest extends TokenRequest {

    /** REQUIRED. Value MUST be set to "refresh_token". */
    private final GrantType mGrantType = GrantType.REFRESH_TOKEN;

    /**  REQUIRED. The refresh token issued to the client. */
    private final String mRefreshToken;

    /**
     * The scope
     */
    private final String mScope;

    /**
     * The default constructor
     * @param client The http client
     * @param oAuthConfig The oauth configuration
     * @param clientId The client id
     * @param refreshToken The refresh token
     * @param scope The scopes
     */
    public RefreshAccessTokenRequest(final HttpClient client,
                                     final OAuthConfig oAuthConfig,
                                     final String clientId,
                                     final String refreshToken,
                                     final String scope) {
        super(client, oAuthConfig, clientId);

        mRefreshToken = refreshToken;
        mScope = scope;
    }

    @Override
    protected void constructBody(final List<NameValuePair> body) {
        body.add(new BasicNameValuePair(OAuth.REFRESH_TOKEN, mRefreshToken));
        body.add(new BasicNameValuePair(OAuth.SCOPE, mScope));
        body.add(new BasicNameValuePair(OAuth.GRANT_TYPE, mGrantType.toString()));
    }
}
