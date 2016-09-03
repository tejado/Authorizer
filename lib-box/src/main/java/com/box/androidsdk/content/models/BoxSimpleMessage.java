package com.box.androidsdk.content.models;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * Class representing a response from a real time server.
 */
public class BoxSimpleMessage extends BoxJsonObject {

    private static final long serialVersionUID = 1626798809346520004L;
    public static final String FIELD_MESSAGE = "message";

    public static final String MESSAGE_NEW_CHANGE = "new_change";
    public static final String MESSAGE_RECONNECT = "reconnect";


    @Override
    protected void parseJSONMember(JsonObject.Member member) {
        JsonValue value = member.getValue();
        if (member.getName().equals(FIELD_MESSAGE)) {
            this.mProperties.put(FIELD_MESSAGE, value.asString());
            return;
        }
        super.parseJSONMember(member);
    }

    /**
     * Returns the message from the server.
     *
     * @return message from the server.
     */
    public String getMessage() {
        return (String) mProperties.get(FIELD_MESSAGE);
    }

    /**
     * Constructs an empty BoxSimpleMessage object.
     */
    public BoxSimpleMessage() {

    }
}
