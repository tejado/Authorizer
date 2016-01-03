package com.microsoft.authenticate;

import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import static com.microsoft.authenticate.OAuth.*;

/**
 * OAuthErrorResponse represents the an Error Response from the OAuth server.
 */
final class OAuthErrorResponse implements OAuthResponse {

    /**
     * Builder is a helper class to create a OAuthErrorResponse.
     * An OAuthResponse must contain an error, but an error_description and
     * error_uri are optional
     */
    @SuppressWarnings("UnusedReturnValue")
    public static class Builder {
        /**
         * The error
         */
        private final ErrorType mError;

        /**
         * The description of the error
         */
        private String mErrorDescription;

        /**
         * The error uri
         */
        private String mErrorUri;

        /**
         * Default constructor
         * @param error The error
         */
        public Builder(final ErrorType error) {
            mError = error;
        }

        /**
         * Builds an instance of OAuthErrorResponse
         * @return a new instance of an OAuthErrorResponse containing
         *         the values called on the builder.
         */
        public OAuthErrorResponse build() {
            return new OAuthErrorResponse(this);
        }

        /**
         * Sets the error description
         * @param errorDescription The description of the error
         * @return The builder
         */
        public Builder errorDescription(final String errorDescription) {
            mErrorDescription = errorDescription;
            return this;
        }

        /***
         * Sets the error uri
         * @param errorUri The error uri
         * @return The builder
         */
        public Builder errorUri(final String errorUri) {
            mErrorUri = errorUri;
            return this;
        }
    }

    /**
     * Static constructor that creates an OAuthErrorResponse from the given OAuth server's
     * JSONObject response
     * @param response from the OAuth server
     * @return A new instance of an OAuthErrorResponse from the given response
     * @throws AuthException if there is an JSONException, or the error type cannot be found.
     */
    public static OAuthErrorResponse createFromJson(final JSONObject response) throws AuthException {
        final String errorString;
        try {
            errorString = response.getString(ERROR);
        } catch (final JSONException e) {
            throw new AuthException(ErrorMessages.SERVER_ERROR, e);
        }

        final ErrorType error;
        try {
            error = ErrorType.valueOf(errorString.toUpperCase(Locale.US));
        } catch (final IllegalArgumentException | NullPointerException e) {
            throw new AuthException(ErrorMessages.SERVER_ERROR, e);
        }

        final Builder builder = new Builder(error);
        if (response.has(ERROR_DESCRIPTION)) {
            final String errorDescription;
            try {
                errorDescription = response.getString(ERROR_DESCRIPTION);
            } catch (final JSONException e) {
                throw new AuthException(ErrorMessages.CLIENT_ERROR, e);
            }
            builder.errorDescription(errorDescription);
        }

        if (response.has(ERROR_URI)) {
            final String errorUri;
            try {
                errorUri = response.getString(ERROR_URI);
            } catch (final JSONException e) {
                throw new AuthException(ErrorMessages.CLIENT_ERROR, e);
            }
            builder.errorUri(errorUri);
        }

        return builder.build();
    }

    /**
     * Check if the error response is valid
     * @param response to check
     * @return true if the given JSONObject is a valid OAuth response
     */
    public static boolean validOAuthErrorResponse(final JSONObject response) {
        return response.has(ERROR);
    }

    /**
     * The error type
     */
    private final ErrorType mError;

    /**
     * OPTIONAL.  A human-readable UTF-8 encoded text providing
     * additional information, used to assist the client developer in
     * understanding the error that occurred.
     */
    private final String mErrorDescription;

    /**
     * OPTIONAL.  A URI identifying a human-readable web page with
     * information about the error, used to provide the client
     * developer with additional information about the error.
     */
    private final String mErrorUri;

    /**
     * OAuthErrorResponse constructor. It is private to enforce
     * the use of the Builder.
     *
     * @param builder to use to construct the object.
     */
    private OAuthErrorResponse(final Builder builder) {
        mError = builder.mError;
        mErrorDescription = builder.mErrorDescription;
        mErrorUri = builder.mErrorUri;
    }

    @Override
    public void accept(final OAuthResponseVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * error is a required field.
     * @return the error
     */
    public ErrorType getError() {
        return mError;
    }

    /**
     * error_description is an optional field
     * @return error_description
     */
    public String getErrorDescription() {
        return mErrorDescription;
    }

    /**
     * error_uri is an optional field
     * @return error_uri
     */
    public String getErrorUri() {
        return mErrorUri;
    }

    @Override
    public String toString() {
        return String.format("OAuthErrorResponse [error=%s, errorDescription=%s, errorUri=%s]",
                mError.toString().toLowerCase(Locale.US), mErrorDescription, mErrorUri);
    }
}
