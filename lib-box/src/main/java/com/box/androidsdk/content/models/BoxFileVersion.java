package com.box.androidsdk.content.models;

import com.box.androidsdk.content.BoxConstants;
import com.box.androidsdk.content.utils.BoxDateFormat;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;

/**
 * Class that represents a version of a file on Box.
 */
public class BoxFileVersion extends BoxEntity {

    private static final long serialVersionUID = -1013756375421636876L;

    public static final String TYPE = "file_version";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_SHA1 = "sha1";
    public static final String FIELD_DELETED_AT = "deleted_at";
    public static final String FIELD_MODIFIED_BY = "modified_by";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_MODIFIED_AT = "modified_at";
    public static final String FIELD_SIZE = BoxConstants.FIELD_SIZE;

    public static final String[] ALL_FIELDS = new String[]{
            FIELD_NAME,
            FIELD_SIZE,
            FIELD_SHA1,
            FIELD_MODIFIED_BY,
            FIELD_CREATED_AT,
            FIELD_MODIFIED_AT,
            FIELD_DELETED_AT
    };

    /**
     * Constructs an empty BoxFileVersion object.
     */
    public BoxFileVersion() {
        super();
    }

    /**
     * Constructs a BoxFileVersion with the provided map values.
     *
     * @param map map of keys and values of the object.
     */
    public BoxFileVersion(Map<String, Object> map) {
        super(map);
    }

    /**
     * Gets the name of the file version.
     *
     * @return the name of the file version.
     */
    public String getName() {
        return (String) mProperties.get(FIELD_NAME);
    }

    /**
     * Gets the time the file version was created.
     *
     * @return the time the file version was created.
     */
    public Date getCreatedAt() {
        return (Date) mProperties.get(FIELD_CREATED_AT);
    }

    /**
     * Gets the time the file version was last modified.
     *
     * @return the time the file version was last modified.
     */
    public Date getModifiedAt() {
        return (Date) mProperties.get(FIELD_MODIFIED_AT);
    }

    /**
     * Gets the SHA1 hash of the file version.
     *
     * @return the SHA1 hash of the file version.
     */
    public String getSha1() {
        return (String) mProperties.get(FIELD_SHA1);
    }

    /**
     * Gets the time that the file version was/will be deleted.
     *
     * @return the time that the file version was/will be trashed.
     */
    public Date getDeletedAt() {
        return (Date) mProperties.get(FIELD_DELETED_AT);
    }

    /**
     * Gets the size of the file version in bytes.
     *
     * @return the size of the file version in bytes.
     */
    public Long getSize() {
        return (Long) mProperties.get(BoxConstants.FIELD_SIZE);
    }

    /**
     * Gets info about the user who last modified the file version.
     *
     * @return info about the user who last modified the file version.
     */
    public BoxUser getModifiedBy() {
        return (BoxUser) mProperties.get(FIELD_MODIFIED_BY);
    }


    @Override
    protected void parseJSONMember(JsonObject.Member member) {
        try {
            String memberName = member.getName();
            JsonValue value = member.getValue();
            if (memberName.equals(FIELD_NAME)) {
                this.mProperties.put(FIELD_NAME, value.asString());
                return;
            } else if (memberName.equals(FIELD_SHA1)) {
                this.mProperties.put(FIELD_SHA1, value.asString());
                return;
            } else if (memberName.equals(FIELD_DELETED_AT)) {
                this.mProperties.put(FIELD_DELETED_AT, BoxDateFormat.parse(value.asString()));
                return;
            } else if (memberName.equals(FIELD_SIZE)) {
                this.mProperties.put(FIELD_SIZE, Long.valueOf(value.toString()));
                return;
            } else if (memberName.equals(FIELD_MODIFIED_BY)) {
                this.mProperties.put(FIELD_MODIFIED_BY, this.parseUserInfo(value.asObject()));
                return;
            } else if (memberName.equals(FIELD_CREATED_AT)) {
                this.mProperties.put(FIELD_CREATED_AT, BoxDateFormat.parse(value.asString()));
                return;
            } else if (memberName.equals(FIELD_MODIFIED_AT)) {
                this.mProperties.put(FIELD_MODIFIED_AT, BoxDateFormat.parse(value.asString()));
                return;
            }
        } catch (ParseException e) {
            assert false : "A ParseException indicates a bug in the SDK.";
        }

        super.parseJSONMember(member);
    }

    private BoxUser parseUserInfo(JsonObject jsonObject) {
        BoxUser user = new BoxUser();
        user.createFromJson(jsonObject);
        return user;
    }
}