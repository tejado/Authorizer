package com.microsoft.authenticate;

/**
 * Indicates that an exception occurred during the Auth process.
 */
public class AuthException extends Exception {
    /**
     * The error
     */
    private final String mError;

    /**
     * The error uri
     */
    private final String mErrorUri;

    /**
     * Default constructor
     * @param errorMessage The error message
     */
    AuthException(final String errorMessage) {
        super(errorMessage);
        mError = "";
        mErrorUri = "";
    }

    /**
     * Default constructor
     * @param errorMessage The error message
     * @param throwable The throwable
     */
    AuthException(final String errorMessage, final Throwable throwable) {
        super(errorMessage, throwable);
        mError = "";
        mErrorUri = "";
    }

    /**
     * Default constructor
     * @param error The error
     * @param errorDescription The error description
     * @param errorUri The error uri
     */
    AuthException(final String error, final String errorDescription, final String errorUri) {
        super(errorDescription);

        mError = error;
        mErrorUri = errorUri;
    }

    /**
     * Gets the error
     * @return Returns the authentication error.
     */
    public String getError() {
        return mError;
    }

    /**
     * Gets the error Uri
     * @return Returns the error URI.
     */
    public String getErrorUri() {
        return mErrorUri;
    }
}