package com.box.androidsdk.content.models;

import java.text.ParseException;
import java.util.Date;

import com.box.androidsdk.content.utils.BoxDateFormat;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.util.Map;

/**
 * Contains information about a BoxCollaborator.
 */
public abstract class BoxCollaborator extends BoxEntity {

    private static final long serialVersionUID = 4995483369186543255L;
    public static final String FIELD_NAME = "name";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_MODIFIED_AT = "modified_at";

    /**
     * Constructs an empty BoxCollaborator object.
     */
    public BoxCollaborator() {
        super();
    }


    /**
     * Constructs a BoxCollaborator with the provided map values.
     *
     * @param map   map of keys and values of the object
     */
    public BoxCollaborator(Map<String, Object> map) {
        super(map);
    }

    /**
     * Gets the name of the collaborator.
     * 
     * @return the name of the collaborator.
     */
    public String getName() {
        return (String) mProperties.get(FIELD_NAME);
    }

    /**
     * Gets the date that the collaborator was created.
     * 
     * @return the date that the collaborator was created.
     */
    public Date getCreatedAt() {
        return (Date) mProperties.get(FIELD_CREATED_AT);
    }

    /**
     * Gets the date that the collaborator was modified.
     * 
     * @return the date that the collaborator was modified.
     */
    public Date getModifiedAt() {
        return (Date) mProperties.get(FIELD_MODIFIED_AT);
    }

    @Override
    protected void parseJSONMember(JsonObject.Member member) {
        try {
            JsonValue value = member.getValue();
            if (member.getName().equals(FIELD_NAME)) {
                this.mProperties.put(FIELD_NAME, value.asString());
                return;
            } else if (member.getName().equals(FIELD_CREATED_AT)) {
                this.mProperties.put(FIELD_CREATED_AT, BoxDateFormat.parse(value.asString()));
                return;
            } else if (member.getName().equals(FIELD_MODIFIED_AT)) {
                this.mProperties.put(FIELD_MODIFIED_AT, BoxDateFormat.parse(value.asString()));
                return;
            }
        } catch (ParseException e) {
            assert false : "A ParseException indicates a bug in the SDK.";
        }

        super.parseJSONMember(member);
    }

    /**
     * Creates a BoxCollaborator object from a JSON string.
     *
     * @return either a {@link BoxUser} or {@link BoxGroup} based on the json passed in, returns null if type in json is invalid for a known BoxCollaborator type.
     */
    public static BoxCollaborator createCollaboratorFromJson(final String json){
        BoxEntity createdByEntity = new BoxEntity();
        createdByEntity.createFromJson(json);
        if (createdByEntity.getType().equals(BoxUser.TYPE)) {
            BoxUser user = new BoxUser();
            user.createFromJson(json);
            return user;
        } else if (createdByEntity.getType().equals(BoxGroup.TYPE)) {
            BoxGroup group = new BoxGroup();
            group.createFromJson(json);
           return group;
        }
        return null;
    }

    /**
     * Creates a BoxCollaborator object from a JsonObject.
     *
     * @return either a {@link BoxUser} or {@link BoxGroup} based on the json passed in, returns null if type in json is invalid for a known BoxCollaborator type.
     */
    public static BoxCollaborator createCollaboratorFromJson(final JsonObject json){
        BoxEntity createdByEntity = new BoxEntity();
        createdByEntity.createFromJson(json);
        if (createdByEntity.getType().equals(BoxUser.TYPE)) {
            BoxUser user = new BoxUser();
            user.createFromJson(json);
            return user;
        } else if (createdByEntity.getType().equals(BoxGroup.TYPE)) {
            BoxGroup group = new BoxGroup();
            group.createFromJson(json);
            return group;
        }
        return null;
    }

}