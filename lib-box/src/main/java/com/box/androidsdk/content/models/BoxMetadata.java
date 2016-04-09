package com.box.androidsdk.content.models;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.util.List;
import java.util.Map;

/**
 * Represents metadata information from a template.
 */
public class BoxMetadata extends BoxJsonObject {

    /**
     * The file ID that metadata belongs to.
     */
    public static final String FIELD_PARENT = "parent";

    /**
     * The template that the metadata information belongs to.
     */
    public static final String FIELD_TEMPLATE = "template";

    /**
     * The scope that the metadata's template belongs to.
     */
    public static final String FIELD_SCOPE = "scope";

    private List<String> mMetadataKeys;

    /**
     * Constructs an empty BoxMetadata object.
     */
    public BoxMetadata() {
        super();
    }

    /**
     *  Initialize with a Map from Box API response JSON.
     *
     *  @param JSONData from Box API response JSON.
     *
     *  @return The model object.
     */
    public BoxMetadata(Map<String, Object> JSONData) {
        super(JSONData);
    }

    /**
     * Gets the metadata's parent.
     *
     * @return the metadata's parent.
     */
    public String getParent() {
        return (String) mProperties.get(FIELD_PARENT);
    }

    /**
     * Gets the metadata's template.
     *
     * @return the metadata's template.
     */
    public String getTemplate() {
        return (String) mProperties.get(FIELD_TEMPLATE);
    }

    /**
     * Gets the metadata's scope.
     *
     * @return the metadata's scope.
     */
    public String getScope() {
        return (String) mProperties.get(FIELD_SCOPE);
    }

    @Override
    protected void parseJSONMember(JsonObject.Member member) {
        try {
            String memberName = member.getName();
            JsonValue value = member.getValue();
            if (memberName.equals(FIELD_PARENT)) {
                this.mProperties.put(FIELD_PARENT, value.asString());
                return;
            } else if (memberName.equals(FIELD_TEMPLATE)) {
                this.mProperties.put(FIELD_TEMPLATE, value.asString());
                return;
            } else if (memberName.equals(FIELD_SCOPE)) {
                this.mProperties.put(FIELD_SCOPE, value.asString());
                return;
            } else if (!mMetadataKeys.contains(memberName)){
                this.mProperties.put(memberName, value.asString());
                mMetadataKeys.add(memberName);
                return;
            }
        } catch (Exception e) {
            assert false : "A ParseException indicates a bug in the SDK.";
        }

        super.parseJSONMember(member);
    }
}
