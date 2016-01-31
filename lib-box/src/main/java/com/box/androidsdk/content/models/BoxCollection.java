package com.box.androidsdk.content.models;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.util.Map;

/**
 * Class that represents a collection on Box.
 */
public class BoxCollection extends BoxEntity {

    /**
     * Constructs an empty BoxCollection object.
     */
    public BoxCollection() {
        super();
    }

    /**
     * Constructs a BoxCollection with the provided map values
     *
     * @param map - map of keys and values of the object
     */
    public BoxCollection(Map<String, Object> map) {
        super(map);
    }

    public static final String TYPE = "collection";

    public static final String FIELD_NAME = "name";
    public static final String FIELD_COLLECTION_TYPE = "collection_type";


    /**
     * Gets the name of the collection.
     *
     * @return the name of the collection.
     */
    public String getName() {
        return (String) mProperties.get(FIELD_NAME);
    }

    /**
     * Gets the type of the collection. Currently only "favorites" is supported.
     *
     * @return type of collection.
     */
    public String getCollectionType() {
        return (String) mProperties.get(FIELD_COLLECTION_TYPE);
    }

    @Override
    protected void parseJSONMember(JsonObject.Member member) {
        String memberName = member.getName();
        JsonValue value = member.getValue();
        if (memberName.equals(FIELD_NAME)) {
            mProperties.put(FIELD_NAME, value.asString());
            return;
        } else if (memberName.equals(FIELD_COLLECTION_TYPE)) {
            mProperties.put(FIELD_COLLECTION_TYPE, value.asString());
            return;
        }

        super.parseJSONMember(member);
    }
}
