package com.microsoft.authenticate;

import android.net.Uri;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class that represents an OAuth token request.
 * Known subclasses include AccessTokenRequest and RefreshAccessTokenRequest
 */
abstract class TokenRequest {

    /**
     * The content type of the request
     */
    private static final String CONTENT_TYPE =
            URLEncodedUtils.CONTENT_TYPE + ";charset=" + HTTP.UTF_8;

    /**
     * The http client
     */
    private final HttpClient mClient;

    /**
     * The client id
     */
    private final String mClientId;

    /**
     * The oauth configuration
     */
    private final OAuthConfig mOAuthConfig;

    /**
     * Constructs a new TokenRequest instance and initializes its parameters.
     *
     * @param client the HttpClient to make HTTP requests on
     * @param oAuthConfig the oauth configuration
     * @param clientId the client_id of the calling application
     */
    public TokenRequest(final HttpClient client, final OAuthConfig oAuthConfig, final String clientId) {
        mClient = client;
        mClientId = clientId;
        mOAuthConfig = oAuthConfig;
    }

    /**
     * Performs the Token Request and returns the OAuth server's response.
     *
     * @return The OAuthResponse from the server
     * @throws AuthException if there is any exception while executing the request
     *                           (e.g., IOException, JSONException)
     */
    public OAuthResponse execute() throws AuthException {
        final Uri requestUri = mOAuthConfig.getTokenUri();

        final HttpPost request = new HttpPost(requestUri.toString());

        final List<NameValuePair> body = new ArrayList<>();
        body.add(new BasicNameValuePair(OAuth.CLIENT_ID, mClientId));

        // constructBody allows subclasses to add to body
        constructBody(body);

        try {
            final UrlEncodedFormEntity entity = new UrlEncodedFormEntity(body, HTTP.UTF_8);
            entity.setContentType(CONTENT_TYPE);
            request.setEntity(entity);
        } catch (final UnsupportedEncodingException e) {
            throw new AuthException(ErrorMessages.CLIENT_ERROR, e);
        }

        final HttpResponse response;
        try {
            response = mClient.execute(request);
        } catch (final IOException e) {
            throw new AuthException(ErrorMessages.SERVER_ERROR, e);
        }

        final HttpEntity entity = response.getEntity();
        final String stringResponse;
        try {
            stringResponse = EntityUtils.toString(entity);
        } catch (final IOException e) {
            throw new AuthException(ErrorMessages.SERVER_ERROR, e);
        }

        final JSONObject jsonResponse;
        try {
            jsonResponse = new JSONObject(stringResponse);
        } catch (final JSONException e) {
            throw new AuthException(ErrorMessages.SERVER_ERROR, e);
        }

        if (OAuthErrorResponse.validOAuthErrorResponse(jsonResponse)) {
            return OAuthErrorResponse.createFromJson(jsonResponse);
        } else if (OAuthSuccessfulResponse.validOAuthSuccessfulResponse(jsonResponse)) {
            return OAuthSuccessfulResponse.createFromJson(jsonResponse);
        } else {
            throw new AuthException(ErrorMessages.SERVER_ERROR);
        }
    }

    /**
     * This method gives a hook in the execute process, and allows subclasses
     * to add to the HttpRequest's body.
     * NOTE: The content type has already been added
     *
     * @param body of NameValuePairs to add to
     */
    protected abstract void constructBody(List<NameValuePair> body);
}
