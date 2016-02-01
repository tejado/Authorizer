package com.microsoft.onedriveaccess;

import android.util.Log;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Produce Gson instances that can parse OneDrive responses
 */
final class GsonFactory {

    /**
     * List of properties that are not to go back up to OneDrive
     */
    private static final List<String> IGNORED_PROPERTIES_LIST = Arrays.asList("Children", "Thumbnails",
            "Permission");

    /**
     * Default Constructor
     */
    private GsonFactory() {
    }

    /**
     * Creates an instance of Gson
     * @return The new instance
     */
    public static Gson getGsonInstance() {

        final JsonSerializer<Date> dateJsonSerializer = new JsonSerializer<Date>() {
            @Override
            public JsonElement serialize(final Date src,
                                         final Type typeOfSrc,
                                         final JsonSerializationContext context) {
                if (src == null) {
                    return null;
                }
                try {
                    return new JsonPrimitive(ISO8601.fromDate(src));
                } catch (final Exception e) {
                    Log.e("JsonSerializerDate", "Parsing issue on " + src + " ! " + e.toString());
                    return null;
                }
            }
        };

        final JsonDeserializer<Date> dateJsonDeserializer = new JsonDeserializer<Date>() {
            @Override
            public Date deserialize(final JsonElement json,
                                    final Type typeOfT,
                                    final JsonDeserializationContext context) throws JsonParseException {
                if (json == null) {
                    return null;
                }
                try {
                    return ISO8601.toDate(json.getAsString());
                } catch (final ParseException e) {
                    Log.e("JsonDeserializerDate", "Parsing issue on " + json.getAsString() + " ! " + e.toString());
                    return null;
                }
            }
        };

        final ExclusionStrategy ignoreCollections = new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(final FieldAttributes f) {
                for (final String ignoreMe : IGNORED_PROPERTIES_LIST) {
                    if (f.getName().contains(ignoreMe)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean shouldSkipClass(final Class<?> clazz) {
                return false;
            }
        };

        return new GsonBuilder()
                .addSerializationExclusionStrategy(ignoreCollections)
                .registerTypeAdapter(Date.class, dateJsonSerializer)
                .registerTypeAdapter(Date.class, dateJsonDeserializer)
                .create();
    }
}
