package com.microsoft.onedriveaccess;

import retrofit.converter.Converter;
import retrofit.converter.GsonConverter;

/**
 * Abstract connection type using {@link com.google.gson.Gson} as its parsing delegate
 */
public abstract class GsonODConnection extends AbstractODConnection {

    @Override
    protected Converter getConverter() {
        return new GsonConverter(GsonFactory.getGsonInstance());
    }
}
