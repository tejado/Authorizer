package com.microsoft.authenticate;

/**
 * OAuthRespresent a response from an OAuth server.
 * Known implementors are OAuthSuccessfulResponse and OAuthErrorResponse.
 * Different OAuthResponses can be determined by using the OAuthResponseVisitor.
 */
interface OAuthResponse {

    /**
     * Calls visit() on the visitor.
     * This method is used to determine which OAuthResponse is being returned
     * without using instance of.
     *
     * @param visitor to visit the given OAuthResponse
     */
    void accept(OAuthResponseVisitor visitor);

}
