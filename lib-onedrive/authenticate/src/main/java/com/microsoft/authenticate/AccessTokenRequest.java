package com.microsoft.authenticate;

import com.microsoft.authenticate.OAuth.GrantType;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.util.List;
import java.util.Locale;

/**
 * AccessTokenRequest represents a request for an Access Token.
 * It subclasses the abstract class TokenRequest, which does most of the work.
 * This class adds the proper parameters for the access token request via the
 * constructBody() hook.
 */
class AccessTokenRequest extends TokenRequest {

    /**
     * REQUIRED.  The authorization mCode received from the
     * authorization server.
     */
    private final String mCode;

    /** REQUIRED.  Value MUST be set to "authorization_code". */
    private final GrantType mGrantType;

    /**
     * REQUIRED, if the "redirect_uri" parameter was included in the
     * authorization request as described in Section 4.1.1, and their
     * values MUST be identical.
     */
    private final String mRedirectUri;

    /**
     * Constructs a new AccessTokenRequest, and initializes its member variables
     *
     * @param client the HttpClient to make HTTP requests on
     * @param oAuthConfig the OAuth configuration
     * @param clientId the client_id of the calling application
     * @param redirectUri the redirect_uri to be called back
     * @param code the authorization code received from the AuthorizationRequest
     */
    public AccessTokenRequest(final HttpClient client,
                              final OAuthConfig oAuthConfig,
                              final String clientId,
                              final String redirectUri,
                              final String code) {
        super(client, oAuthConfig, clientId);

        mRedirectUri = redirectUri;
        mCode = code;
        mGrantType = GrantType.AUTHORIZATION_CODE;
    }

    /**
     * Adds the "mCode", "redirect_uri", and "grant_type" parameters to the body.
     *
     * @param body the list of NameValuePairs to be placed in the body of the HTTP request
     */
    @Override
    protected void constructBody(final List<NameValuePair> body) {
        body.add(new BasicNameValuePair(OAuth.CODE, this.mCode));
        body.add(new BasicNameValuePair(OAuth.REDIRECT_URI, mRedirectUri));
        body.add(new BasicNameValuePair(OAuth.GRANT_TYPE,
                                        mGrantType.toString().toLowerCase(Locale.US)));
    }
}
