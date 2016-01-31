package com.box.androidsdk.content.models;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.util.Map;

/**
 * Class that represents an enterprise in Box.
 */
public class BoxEnterprise extends BoxEntity {

    private static final long serialVersionUID = -3453999549970888942L;

    public static final String TYPE = "enterprise";

    public static final String FIELD_NAME = "name";

    /**
     * Constructs an empty BoxEnterprise object.
     */
    public BoxEnterprise() {
        super();
    }

    /**
     * Constructs a BoxEnterprise with the provided map values.
     * @param map   map of keys and values of the object.
     */
    public BoxEnterprise(Map<String, Object> map) {
        super(map);
    }

    /**
     * Gets the name of the item.
     * 
     * @return the name of the item.
     */
    public String getName() {
        return (String) mProperties.get(FIELD_NAME);
    }

    @Override
    protected void parseJSONMember(JsonObject.Member member) {
        JsonValue value = member.getValue();
        if (member.getName().equals(FIELD_NAME)) {
            this.mProperties.put(FIELD_NAME, value.asString());
            return;
        }

        super.parseJSONMember(member);
    }

}
