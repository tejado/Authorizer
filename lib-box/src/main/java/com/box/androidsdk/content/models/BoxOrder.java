package com.box.androidsdk.content.models;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * Class representing an order for a list of objects.
 */
public class BoxOrder extends BoxJsonObject {

    public static final String FIELD_BY = "by";
    public static final String FIELD_DIRECTION = "direction";


    @Override
    protected void parseJSONMember(JsonObject.Member member) {
        String memberName = member.getName();
        JsonValue value = member.getValue();
        if (memberName.equals(FIELD_BY)) {
            this.mProperties.put(FIELD_BY, value.asString());
            return;
        } else if (memberName.equals(FIELD_DIRECTION)) {
            this.mProperties.put(FIELD_DIRECTION, value.asString());
            return;
        }

        super.parseJSONMember(member);
    }
}
