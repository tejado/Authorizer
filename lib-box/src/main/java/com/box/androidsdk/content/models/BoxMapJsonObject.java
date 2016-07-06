package com.box.androidsdk.content.models;

import java.util.Map;

/**
 * A concrete implementation of BoxJsonObject designed for undefined/unknown json objects.
 */
public class BoxMapJsonObject extends BoxJsonObject {
    public BoxMapJsonObject() {
        super();
    }

    public BoxMapJsonObject(Map<String, Object> map) {
        super(map);
    }

    /**
     * Helper method to get values from this object if keys are known. Alternatively this object can be
     * converted into a map using getPropertiesAsHashMap for a similar result.
     * @param key a string that maps to an object in this class.
     * @return an object indexed by the given key, or null if there is no such object.
     */
    public Object getValue(final String key){
        return mProperties.get(key);
    }
}
