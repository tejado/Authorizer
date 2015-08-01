package com.microsoft.onedriveaccess;

import com.microsoft.authenticate.AuthClient;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;

/**
 * Concrete object to interface with the OneDrive service
 */
public class ODConnection extends GsonODConnection {

    /**
     * The credentials for this connection
     */
    private final AuthClient mAuthClient;

    /**
     * The verbose logcat setting
     */
    private boolean mVerboseLogcatOutput;

    /**
     * Default Constructor
     *
     * @param authClient The credentials to use for this connection
     */
    public ODConnection(final AuthClient authClient) {
        mAuthClient = authClient;
        mVerboseLogcatOutput = true;
    }

    /**
     * Changes the verbosity of the logcat output while requests are issued.
     *
     * @param value <b>True</b> to enable verbose logging, <b>False</b> for minimal logging.
     */
    public void setVerboseLogcatOutput(final boolean value) {
        mVerboseLogcatOutput = value;
    }

    @Override
    protected RequestInterceptor getInterceptor() {
        return InterceptorFactory.getRequestInterceptor(mAuthClient);
    }

    /**
     * Gets the RestAdapter.LogLevel to use for this connection
     *
     * @return The logging level
     */
    @Override
    public RestAdapter.LogLevel getLogLevel() {
        if (mVerboseLogcatOutput) {
            return RestAdapter.LogLevel.FULL;
        }
        return RestAdapter.LogLevel.NONE;
    }

    @Override
    protected String getEndpoint() {
        return "https://api.onedrive.com";
    }
}
