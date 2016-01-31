package com.box.androidsdk.content.models;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

/**
 * Class representing a list of real time servers.
 */
public class BoxListRealTimeServers extends BoxList<BoxRealTimeServer> {

    private static final long serialVersionUID = -4986489348666966126L;
    public static final String FIELD_CHUNK_SIZE = "chunk_size";

    @Override
    protected void parseJSONMember(JsonObject.Member member) {
        String memberName = member.getName();
        JsonValue value = member.getValue();
        if (memberName.equals(FIELD_CHUNK_SIZE)) {
            mProperties.put(FIELD_CHUNK_SIZE, value.asLong());
            return;
        } else if (memberName.equals(FIELD_ENTRIES)) {
            JsonArray entries = value.asArray();
            for (JsonValue entry : entries) {
                BoxRealTimeServer server = new BoxRealTimeServer();
                JsonObject obj = entry.asObject();
                server.createFromJson(obj);
                add(server);
            }
            mProperties.put(FIELD_ENTRIES, collection);
            return;
        }
        super.parseJSONMember(member);
    }

}
