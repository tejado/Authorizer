package com.microsoft.authenticate;

/**
 * ErrorMessages is a non-instantiable class that contains all the String constants
 * used in for errors.
 */
final class ErrorMessages {

    /**
     * Message if there was a client size error
     */
    public static final String CLIENT_ERROR =
            "An error occurred on the client during the operation.";

    /**
     * Message if there is already a login in process
     */
    public static final String LOGIN_IN_PROGRESS =
            "Another login operation is already in progress.";

    /**
     * Message if there was a problem communicating with the server
     */
    public static final String SERVER_ERROR =
            "An error occurred while communicating with the server during the operation. Please try again later.";

    /**
     * Message if the user canceled the signin operation
     */
    public static final String SIGNIN_CANCEL = "The user cancelled the login operation.";

    /**
     * Utility class, don't allow construct
     */
    private ErrorMessages() {
    }
}
