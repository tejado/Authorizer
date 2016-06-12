package com.box.androidsdk.content.models;

import com.box.androidsdk.content.utils.BoxDateFormat;
import com.box.androidsdk.content.utils.SdkUtils;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The abstract base class for all types that contain JSON data returned by the Box API.
 */
public abstract class BoxJsonObject extends BoxObject implements Serializable {

    private static final long serialVersionUID = 7174936367401884790L;
    // Map that holds all the properties of the entity. LinkedHashMap was chosen to preserve ordering when outputting json
    protected final LinkedHashMap<String, Object> mProperties;

    /**
     * Constructs an empty BoxJSONObject.
     */
    public BoxJsonObject() {
        mProperties= new LinkedHashMap<String, Object>();
    }


    /**
     * Constructs a BoxJsonObject with the provided map values.
     * @param map   map of keys and values that will populate the object.
     */
    public BoxJsonObject(Map<String, Object> map) {
        mProperties = new LinkedHashMap<String, Object>(map);
    }

    /**
     * Serializes a json blob into a BoxJsonObject.
     *
     * @param json  json blob to deserialize.
     */
    public void createFromJson(String json) {
        createFromJson(JsonObject.readFrom(json));
    }

    /**
     * Creates the BoxJsonObject from a JsonObject
     * 
     * @param object    json object to parse.
     */
    public void createFromJson(JsonObject object) {
        for (JsonObject.Member member : object) {
            if (member.getValue().isNull()) {
                parseNullJsonMember(member);
                continue;
            }

            this.parseJSONMember(member);
        }
    }

    /**
     * Handle parsing of null member objects from createFromJson method.
     * @param member a member where getValue returns null.
     */
    public void parseNullJsonMember(final JsonObject.Member member){
        if (!SdkUtils.isEmptyString(member.getName())) {
            mProperties.put(member.getName(), null);
        }
    }

    /**
     * Invoked with a JSON member whenever this object is updated or created from a JSON object.
     * 
     * <p>
     * Subclasses should override this method in order to parse any JSON members it knows about. This method is a no-op by default.
     * </p>
     * 
     * @param member
     *            the JSON member to be parsed.
     */
    protected void parseJSONMember(JsonObject.Member member) {
        String memberName = member.getName();
        JsonValue value = member.getValue();
        // TODO: remove this check once we are confident are SDK is handling all fields. BoxEntity is an exception since we are using it for preprocessing.
        if (! (this instanceof BoxEntity)) {
            System.out.println("unhandled json member '" + memberName + "' xxx  " + value + " current object " + this.getClass());
      //      throw new RuntimeException("unhandled json member '" + memberName + "' xxx  " + value + " current object " + this.getClass());
        }
        try{
            mProperties.put(memberName, value.asString());
        } catch (UnsupportedOperationException e){
            this.mProperties.put(memberName, value. toString());
        }


    }

    /**
     * Returns a JSON string representing the object.
     *
     * @return  JSON string representation of the object.
     */
    public String toJson() {
        return toJsonObject().toString();
    }

    protected JsonObject toJsonObject() {
        JsonObject jsonObj = new JsonObject();
        for (Map.Entry<String, Object> entry : mProperties.entrySet()) {
            JsonValue value = parseJsonObject(entry);
            jsonObj.add(entry.getKey(), value);
        }
        return jsonObj;
    }

    protected JsonValue parseJsonObject(Map.Entry<String, Object> entry) {
        Object obj = entry.getValue();
        return obj instanceof BoxJsonObject ? ((BoxJsonObject) obj).toJsonObject() :
                obj instanceof Integer ? JsonValue.valueOf((Integer) obj) :
                obj instanceof Long ? JsonValue.valueOf((Long) obj) :
                obj instanceof Float ? JsonValue.valueOf((Float) obj) :
                obj instanceof Double ? JsonValue.valueOf((Double) obj) :
                obj instanceof Boolean ? JsonValue.valueOf((Boolean) obj) :
                obj instanceof Enum ? JsonValue.valueOf(obj.toString()) :
                obj instanceof Date ? JsonValue.valueOf((BoxDateFormat.format((Date) obj))) :
                obj instanceof String ? JsonValue.valueOf((String) obj) :
                obj instanceof Collection ? parseJsonArray((Collection) obj) :
                JsonValue.valueOf(null);
    }

    private JsonArray parseJsonArray(Collection collection) {
        JsonArray arr = new JsonArray();
        for (Object o : collection) {
            arr.add(JsonValue.valueOf(o.toString()));
        }
        return arr;
    }

    /**
     * Gets properties of the BoxJsonObject as a HashMap.
     *
     * @return  HashMap representing the object's properties.
     */
    public HashMap<String, Object> getPropertiesAsHashMap() {
        return SdkUtils.cloneSerializable(mProperties);
    }
}
