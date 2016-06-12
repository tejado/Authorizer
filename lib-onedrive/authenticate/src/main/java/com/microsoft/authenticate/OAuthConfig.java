package com.microsoft.authenticate;

import android.net.Uri;

/**
 * Config is a singleton class that contains the values used throughout the SDK.
 */
public interface OAuthConfig {

    /**
     * The authorization uri
     * @return the value
     */
    Uri getAuthorizeUri();

    /**
     * The desktop uri
     * @return the value
     */
    Uri getDesktopUri();

    /**
     * The logout uri
     * @return the value
     */
    Uri getLogoutUri();

    /**
     * The auth token uri
     * @return the value
     */
    Uri getTokenUri();
}
