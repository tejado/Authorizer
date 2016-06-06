package com.box.androidsdk.content;

import com.box.androidsdk.content.models.BoxError;
import com.box.androidsdk.content.requests.BoxHttpResponse;

import org.apache.http.HttpStatus;

import java.net.UnknownHostException;

/**
 * Thrown to indicate that an error occurred while communicating with the Box API.
 */
public class BoxException extends Exception {
    private static final long serialVersionUID = 1L;

    private final int responseCode;
    private String response;
    private BoxHttpResponse boxHttpResponse;

    /**
     * Constructs a BoxAPIException with a specified message.
     *
     * @param message a message explaining why the exception occurred.
     */
    public BoxException(String message) {
        super(message);

        this.responseCode = 0;
        this.boxHttpResponse = null;
        this.response = null;
    }

    /**
     * Constructs a BoxAPIException with details about the server's response.
     *
     * @param message  a message explaining why the exception occurred.
     * @param response the response body returned by the Box server.
     */
    public BoxException(String message, BoxHttpResponse response) {
        super(message, (Throwable) null);
        this.boxHttpResponse = response;
        if (response != null) {
            responseCode = response.getResponseCode();
        } else {
            responseCode = 0;
        }
        try {
            this.response = response.getStringBody();
        } catch (Exception e) {
            this.response = null;
        }
    }

    /**
     * Constructs a BoxAPIException that wraps another underlying exception.
     *
     * @param message a message explaining why the exception occurred.
     * @param cause   an underlying exception.
     */
    public BoxException(String message, Throwable cause) {
        super(message, cause);

        this.responseCode = 0;
        this.response = null;
    }

    /**
     * Constructs a BoxAPIException that wraps another underlying exception with details about the server's response.
     *
     * @param message      a message explaining why the exception occurred.
     * @param responseCode the response code returned by the Box server.
     * @param response     the response body returned by the Box server.
     * @param cause        an underlying exception.
     */
    public BoxException(String message, int responseCode, String response, Throwable cause) {
        super(message, cause);

        this.responseCode = responseCode;
        this.response = response;
    }

    /**
     * Gets the response code returned by the server when this exception was thrown.
     *
     * @return the response code returned by the server.
     */
    public int getResponseCode() {
        return this.responseCode;
    }

    /**
     * Gets the body of the response returned by the server when this exception was thrown.
     *
     * @return the body of the response returned by the server.
     */
    public String getResponse() {
        return this.response;
    }

    /**
     * Gets the server response as a BoxError.
     *
     * @return the response as a BoxError, or null if the response cannot be converted.
     */
    public BoxError getAsBoxError() {
        try {
            BoxError error = new BoxError();
            error.createFromJson(getResponse());
            return error;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @return a known error type that corresponds to a given response and code.
     */
    public ErrorType getErrorType() {
        if (getCause() instanceof UnknownHostException){
            return ErrorType.NETWORK_ERROR;
        }
        BoxError error = this.getAsBoxError();
        String errorString = error != null ? error.getError() : null;
        return ErrorType.fromErrorInfo(errorString, getResponseCode());
    }

    public enum ErrorType {
        /*
         * Refresh token has expired
         */
        INVALID_GRANT_TOKEN_EXPIRED("invalid_grant", HttpStatus.SC_BAD_REQUEST),
        /*
         * Invalid refresh token
         */
        INVALID_GRANT_INVALID_TOKEN("invalid_grant", HttpStatus.SC_BAD_REQUEST),
        /*
         * Access denied
         */
        ACCESS_DENIED("access_denied", HttpStatus.SC_FORBIDDEN),
        /*
         * No refresh token parameter found
         */
        INVALID_REQUEST("invalid_request", HttpStatus.SC_BAD_REQUEST),
        /*
         * The client credentials are invalid
         */
        INVALID_CLIENT("invalid_client", HttpStatus.SC_BAD_REQUEST),
        /*
         * Refresh token has expired
         */
        PASSWORD_RESET_REQUIRED("password_reset_required", HttpStatus.SC_BAD_REQUEST),
        /*
         * User needs to accept terms of service
         */
        TERMS_OF_SERVICE_REQUIRED("terms_of_service_required", HttpStatus.SC_BAD_REQUEST),
        /*
         * Free trial expired for this account
         */
        NO_CREDIT_CARD_TRIAL_ENDED("no_credit_card_trial_ended", HttpStatus.SC_BAD_REQUEST),
        /*
         * The server is currently unable to handle the request due to a temporary overloading of the server
         */
        TEMPORARILY_UNAVAILABLE("temporarily_unavailable", 429),
        /*
         * The application is blocked by your administrator
         */
        SERVICE_BLOCKED("service_blocked", HttpStatus.SC_BAD_REQUEST),
        /*
         * Device not authorized to request an access token
         */
        UNAUTHORIZED_DEVICE("unauthorized_device", HttpStatus.SC_BAD_REQUEST),
        /*
         * The account grace period has expired
         */
        GRACE_PERIOD_EXPIRED("grace_period_expired", HttpStatus.SC_FORBIDDEN),
        /**
         * Could not connect to Box API due to a network error
         */
        NETWORK_ERROR("bad_connection_network_error", 0),
        /**
         * Location accessed from is not authorized.
         */
        LOCATION_BLOCKED("access_from_location_blocked", HttpStatus.SC_FORBIDDEN),
        /**
         * An unknown exception has occurred.
         */
        OTHER("", 0);

        private final String mValue;
        private final int mStatusCode;

        private ErrorType(String value, int statusCode) {
            mValue = value;
            mStatusCode = statusCode;
        }

        public static ErrorType fromErrorInfo(final String errorCode, final int statusCode) {
            for (ErrorType type : ErrorType.values()) {
                if (type.mStatusCode == statusCode && type.mValue.equals(errorCode)) {
                    return type;
                }
            }
            return OTHER;
        }


    }

    /**
     * An exception that indicates the RealTimeServerConnection has exceeded the recommended number of retries.
     */
    public static class MaxAttemptsExceeded extends BoxException {
        private final int mTimesTried;

        /**
         * @param message    message for this exception.
         * @param timesTried number of times tried before failing.
         */
        public MaxAttemptsExceeded(String message, int timesTried) {
            this(message, timesTried, null);
        }

        public MaxAttemptsExceeded(String message, int timesTried, BoxHttpResponse response) {
            super(message + timesTried, response);
            mTimesTried = timesTried;
        }

        /**
         * @return the number of times tried specified from constructor.
         */
        public int getTimesTried() {
            return mTimesTried;
        }
    }

    public static class RateLimitAttemptsExceeded extends MaxAttemptsExceeded {
        public RateLimitAttemptsExceeded(String message, int timesTried, BoxHttpResponse response) {
            super(message, timesTried, response);
        }
    }

    public static class RefreshFailure extends BoxException {


        public RefreshFailure(BoxException exception) {
            super(exception.getMessage(), exception.responseCode, exception.getResponse(), exception);
        }

        public boolean isErrorFatal() {
            ErrorType type = getErrorType();
            ErrorType[] fatalTypes = new ErrorType[]{ErrorType.INVALID_GRANT_INVALID_TOKEN,
                    ErrorType.INVALID_GRANT_TOKEN_EXPIRED, ErrorType.ACCESS_DENIED, ErrorType.NO_CREDIT_CARD_TRIAL_ENDED,
                    ErrorType.SERVICE_BLOCKED, ErrorType.INVALID_CLIENT, ErrorType.UNAUTHORIZED_DEVICE,
                    ErrorType.GRACE_PERIOD_EXPIRED, ErrorType.OTHER};
            for (ErrorType fatalType : fatalTypes) {
                if (type == fatalType) {
                    return true;
                }
            }
            return false;
        }


    }

}
