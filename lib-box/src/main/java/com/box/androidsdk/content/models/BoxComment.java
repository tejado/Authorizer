package com.box.androidsdk.content.models;

import com.box.androidsdk.content.utils.BoxDateFormat;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Class that represents a comment on Box.
 */
public class BoxComment extends BoxEntity {

    private static final long serialVersionUID = 8873984774699405343L;

    public static final String TYPE = "comment";

    public static final String FIELD_IS_REPLY_COMMENT = "is_reply_comment";
    public static final String FIELD_MESSAGE = "message";
    public static final String FIELD_TAGGED_MESSAGE = "tagged_message";
    public static final String FIELD_CREATED_BY = "created_by";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_ITEM = "item";
    public static final String FIELD_MODIFIED_AT = "modified_at";

    public static final String[] ALL_FIELDS = new String[]{
            FIELD_TYPE,
            FIELD_ID,
            FIELD_IS_REPLY_COMMENT,
            FIELD_MESSAGE,
            FIELD_TAGGED_MESSAGE,
            FIELD_CREATED_BY,
            FIELD_CREATED_AT,
            FIELD_ITEM,
            FIELD_MODIFIED_AT
    };

    /**
     * Constructs an empty BoxComment object.
     */
    public BoxComment() {
        super();
    }


    /**
     * Constructs a BoxComment with the provided map values.
     *
     * @param map map of keys and values of the object
     */
    public BoxComment(Map<String, Object> map) {
        super(map);
    }

    /**
     * Gets whether or not the comment is a reply to another comment.
     *
     * @return true if this comment is a reply to another comment; otherwise false.
     */
    public Boolean getIsReplyComment() {
        return (Boolean) mProperties.get(FIELD_IS_REPLY_COMMENT);
    }

    /**
     * Gets the comment's message.
     *
     * @return the comment's message.
     */
    public String getMessage() {
        return (String) mProperties.get(FIELD_MESSAGE);
    }

    /**
     * Gets info about the user who created the comment.
     *
     * @return info about the user who created the comment.
     */
    public BoxUser getCreatedBy() {
        return (BoxUser) mProperties.get(FIELD_CREATED_BY);
    }

    /**
     * Gets the time the comment was created.
     *
     * @return the time the comment was created.
     */
    public Date getCreatedAt() {
        return (Date) mProperties.get(FIELD_CREATED_AT);
    }

    /**
     * Gets info about the item this comment is attached to. If the comment is a reply, then the item will be another BoxComment. Otherwise, the item will be a
     * {@link BoxFile} or {@link BoxBookmark}.
     *
     * @return the item this comment is attached to.
     */
    public BoxItem getItem() {
        return (BoxItem) mProperties.get(FIELD_ITEM);
    }

    /**
     * Gets the time the comment was last modified.
     *
     * @return the time the comment was last modified.
     */
    public Date getModifiedAt() {
        return (Date) mProperties.get(FIELD_MODIFIED_AT);
    }

    @Override
    protected void parseJSONMember(JsonObject.Member member) {
        try {
            String memberName = member.getName();
            JsonValue value = member.getValue();
            if (memberName.equals(FIELD_IS_REPLY_COMMENT)) {
                this.mProperties.put(FIELD_IS_REPLY_COMMENT, value.asBoolean());
                return;
            } else if (memberName.equals(FIELD_MESSAGE)) {
                this.mProperties.put(FIELD_MESSAGE, value.asString());
                return;
            } else if (memberName.equals(FIELD_TAGGED_MESSAGE)) {
                this.mProperties.put(FIELD_TAGGED_MESSAGE, value.asString());
                return;
            } else if (memberName.equals(FIELD_CREATED_BY)) {
                BoxUser createdBy = new BoxUser();
                createdBy.createFromJson(value.asObject());
                this.mProperties.put(FIELD_CREATED_BY, createdBy);
                return;
            } else if (memberName.equals(FIELD_CREATED_AT)) {
                this.mProperties.put(FIELD_CREATED_AT, BoxDateFormat.parse(value.asString()));
                return;
            } else if (memberName.equals(FIELD_MODIFIED_AT)) {
                this.mProperties.put(FIELD_MODIFIED_AT, BoxDateFormat.parse(value.asString()));
                return;
            } else if (memberName.equals(FIELD_ITEM)) {
                JsonObject itemObj = value.asObject();
                String itemType = itemObj.get(BoxEntity.FIELD_TYPE).asString();
                BoxEntity entity;
                if (itemType.equals(BoxFile.TYPE)) {
                    entity = new BoxFile();
                    entity.createFromJson(itemObj);
                } else if (itemType.equals(BoxComment.TYPE)) {
                    entity = new BoxComment();
                    entity.createFromJson(itemObj);
                } else if (itemType.equals(BoxBookmark.TYPE)) {
                    entity = new BoxBookmark();
                    entity.createFromJson(itemObj);
                } else {
                    throw new IllegalArgumentException(String.format(Locale.ENGLISH, "Unsupported type \"%s\" for comment found", itemType));
                }
                this.mProperties.put(FIELD_ITEM, entity);
                return;
            }
        } catch (ParseException e) {
            assert false : "A ParseException indicates a bug in the SDK.";
        }

        super.parseJSONMember(member);
    }
}
