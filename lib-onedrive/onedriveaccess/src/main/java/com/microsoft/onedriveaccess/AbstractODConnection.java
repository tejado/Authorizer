package com.microsoft.onedriveaccess;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.Converter;

/**
 * Abstract type defining configurable aspects of the ODConnection
 */
public abstract class AbstractODConnection {

    /**
     * Creates an instance of the IOneDriveService
     *
     * @return The IOneDriveService
     */
    public IOneDriveService getService() {
        final RestAdapter adapter = new RestAdapter.Builder()
                .setLogLevel(getLogLevel())
                .setEndpoint(getEndpoint())
                .setConverter(getConverter())
                .setRequestInterceptor(getInterceptor())
                .build();

        return adapter.create(IOneDriveService.class);
    }

    /**
     * The {@link retrofit.RequestInterceptor} to use for this connection
     *
     * @return the interceptor
     */
    protected abstract RequestInterceptor getInterceptor();

    /**
     * The {@link retrofit.RestAdapter.LogLevel} to use for this connection
     *
     * @return the log level
     */
    protected abstract RestAdapter.LogLevel getLogLevel();

    /**
     * The endpoint used by this connection
     *
     * @return the url to use
     */
    protected abstract String getEndpoint();

    /**
     * The {@link retrofit.converter.Converter} to use for parsing webservice responses
     *
     * @return the converter
     */
    protected abstract Converter getConverter();
}
