package com.box.androidsdk.content.models;

import com.box.androidsdk.content.BoxConstants;
import com.box.androidsdk.content.utils.BoxDateFormat;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Abstract class that represents a BoxItem which is the super class of BoxFolder, BoxFile, and BoxBookmark.
 */
public abstract class BoxItem extends BoxEntity {

    private static final long serialVersionUID = 4876182952337609430L;
    public static final String FIELD_NAME = "name";
    public static final String FIELD_SEQUENCE_ID = "sequence_id";
    public static final String FIELD_ETAG = "etag";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_MODIFIED_AT = "modified_at";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_PATH_COLLECTION = "path_collection";
    public static final String FIELD_CREATED_BY = "created_by";
    public static final String FIELD_MODIFIED_BY = "modified_by";
    public static final String FIELD_TRASHED_AT = "trashed_at";
    public static final String FIELD_PURGED_AT = "purged_at";
    public static final String FIELD_OWNED_BY = "owned_by";
    public static final String FIELD_SHARED_LINK = "shared_link";
    public static final String FIELD_PARENT = "parent";
    public static final String FIELD_ITEM_STATUS = "item_status";
    public static final String FIELD_PERMISSIONS = "permissions";
    public static final String FIELD_SYNCED = "synced";
    public static final String FIELD_ALLOWED_SHARED_LINK_ACCESS_LEVELS = "allowed_shared_link_access_levels";
    public static final String FIELD_TAGS = "tags";

    /**
     * Constructs an empty BoxItem object.
     */
    public BoxItem() {
        super();
    }

    /**
     * Constructs a BoxItem with the provided map values.
     *
     * @param map map of keys and values of the object.
     */
    public BoxItem(Map<String, Object> map) {
        super(map);
    }

    /**
     * Gets a unique string identifying the version of the item.
     *
     * @return a unique string identifying the version of the item.
     */
    public String getEtag() {
        return (String) mProperties.get(FIELD_ETAG);
    }

    /**
     * Gets the name of the item.
     *
     * @return the name of the item.
     */
    public String getName() {
        return (String) mProperties.get(FIELD_NAME);
    }

    /**
     * Gets the time the item was created.
     *
     * @return the time the item was created.
     */
    public Date getCreatedAt() {
        return (Date) mProperties.get(FIELD_CREATED_AT);
    }

    /**
     * Gets the time the item was last modified.
     *
     * @return the time the item was last modified.
     */
    public Date getModifiedAt() {
        return (Date) mProperties.get(FIELD_MODIFIED_AT);
    }

    /**
     * Gets the description of the item.
     *
     * @return the description of the item.
     */
    public String getDescription() {
        return (String) mProperties.get(FIELD_DESCRIPTION);
    }

    /**
     * Gets the size of the item in bytes.
     *
     * @return the size of the item in bytes.
     */
    public Long getSize() {
        return (Long) mProperties.get(BoxConstants.FIELD_SIZE);
    }

    /**
     * Gets the path of folders to the item, starting at the root.
     *
     * @return the path of folders to the item.
     */
    public BoxList<BoxFolder> getPathCollection() {
        return (BoxList<BoxFolder>) mProperties.get(FIELD_PATH_COLLECTION);
    }

    /**
     * Gets info about the user who created the item.
     *
     * @return info about the user who created the item.
     */
    public BoxUser getCreatedBy() {
        return (BoxUser) mProperties.get(FIELD_CREATED_BY);
    }

    /**
     * Gets info about the user who last modified the item.
     *
     * @return info about the user who last modified the item.
     */
    public BoxUser getModifiedBy() {
        return (BoxUser) mProperties.get(FIELD_MODIFIED_BY);
    }

    /**
     * Gets the time that the item was trashed.
     *
     * @return the time that the item was trashed.
     */
    public Date getTrashedAt() {
        return (Date) mProperties.get(FIELD_TRASHED_AT);
    }

    /**
     * Gets the time that the item was purged from the trash.
     *
     * @return the time that the item was purged from the trash.
     */
    public Date getPurgedAt() {
        return (Date) mProperties.get(FIELD_PURGED_AT);
    }

    /**
     * Gets the time that the item was created according to the uploader.
     *
     * @return the time that the item was created according to the uploader.
     */
    protected Date getContentCreatedAt() {
        return (Date) mProperties.get(BoxConstants.FIELD_CONTENT_CREATED_AT);
    }

    /**
     * Gets the time that the item was last modified according to the uploader.
     *
     * @return the time that the item was last modified according to the uploader.
     */
    protected Date getContentModifiedAt() {
        return (Date) mProperties.get(BoxConstants.FIELD_CONTENT_MODIFIED_AT);
    }

    /**
     * Gets info about the user who owns the item.
     *
     * @return info about the user who owns the item.
     */
    public BoxUser getOwnedBy() {
        return (BoxUser) mProperties.get(FIELD_OWNED_BY);
    }

    /**
     * Gets the shared link for the item.
     *
     * @return the shared link for the item.
     */
    public BoxSharedLink getSharedLink() {
        return (BoxSharedLink) mProperties.get(FIELD_SHARED_LINK);
    }

    /**
     * Gets a unique ID for use with the EventStreams.
     *
     * @return a unique ID for use with the EventStream.
     */
    public String getSequenceID() {
        return (String) mProperties.get(FIELD_SEQUENCE_ID);
    }


    /**
     * Access level settings for shared links set by administrator. Can be collaborators, open, or company.
     *
     * @return possible access level settings for this item.
     */
    public ArrayList<BoxSharedLink.Access> getAllowedSharedLinkAccessLevels() {
        return (ArrayList<BoxSharedLink.Access>) mProperties.get(FIELD_ALLOWED_SHARED_LINK_ACCESS_LEVELS);
    }

    /**
     * Gets info about the parent folder of the item.
     *
     * @return info about the parent folder of the item.
     */
    public BoxFolder getParent() {
        return (BoxFolder) mProperties.get(FIELD_PARENT);
    }

    /**
     * Gets the status of the item.
     *
     * @return the status of the item.
     */
    public String getItemStatus() {
        return (String) mProperties.get(FIELD_ITEM_STATUS);
    }

    /**
     * Gets whether or not this item is synced.
     *
     * @return true if this item is synced, false otherwise.
     */
    public Boolean getIsSynced() {
        return (Boolean) mProperties.get(FIELD_SYNCED);
    }

    /**
     * Gets the number of comments on the item.
     *
     * @return the number of comments on the item.
     */
    protected Long getCommentCount() {
        return (Long) mProperties.get(BoxConstants.FIELD_COMMENT_COUNT);
    }

    @Override
    protected void parseJSONMember(JsonObject.Member member) {
        try {
            JsonValue value = member.getValue();
            if (member.getName().equals(FIELD_NAME)) {
                this.mProperties.put(FIELD_NAME, value.asString());
                return;
            } else if (member.getName().equals(FIELD_SEQUENCE_ID)) {
                this.mProperties.put(FIELD_SEQUENCE_ID, value.asString());
                return;
            } else if (member.getName().equals(FIELD_ETAG)) {
                this.mProperties.put(FIELD_ETAG, value.asString());
                return;
            } else if (member.getName().equals(FIELD_CREATED_AT)) {
                this.mProperties.put(FIELD_CREATED_AT, BoxDateFormat.parse(value.asString()));
                return;
            } else if (member.getName().equals(FIELD_MODIFIED_AT)) {
                this.mProperties.put(FIELD_MODIFIED_AT, BoxDateFormat.parse(value.asString()));
                return;
            } else if (member.getName().equals(FIELD_DESCRIPTION)) {
                this.mProperties.put(FIELD_DESCRIPTION, value.asString());
                return;
            } else if (member.getName().equals(BoxConstants.FIELD_SIZE)) {
                this.mProperties.put(BoxConstants.FIELD_SIZE, Long.valueOf(value.toString()));
                return;
            } else if (member.getName().equals(FIELD_TRASHED_AT)) {
                this.mProperties.put(FIELD_TRASHED_AT, BoxDateFormat.parse(value.asString()));
                return;
            } else if (member.getName().equals(FIELD_PURGED_AT)) {
                this.mProperties.put(FIELD_PURGED_AT, BoxDateFormat.parse(value.asString()));
                return;
            } else if (member.getName().equals(BoxConstants.FIELD_CONTENT_CREATED_AT)) {
                this.mProperties.put(BoxConstants.FIELD_CONTENT_CREATED_AT, BoxDateFormat.parse(value.asString()));
                return;
            } else if (member.getName().equals(BoxConstants.FIELD_CONTENT_MODIFIED_AT)) {
                this.mProperties.put(BoxConstants.FIELD_CONTENT_MODIFIED_AT, BoxDateFormat.parse(value.asString()));
                return;
            } else if (member.getName().equals(FIELD_PATH_COLLECTION)) {
                JsonObject jsonObject = value.asObject();
                BoxList<BoxFolder> collection = new BoxList<BoxFolder>();
                collection.createFromJson(jsonObject);
                this.mProperties.put(FIELD_PATH_COLLECTION, collection);
                return;
            } else if (member.getName().equals(FIELD_CREATED_BY)) {
                this.mProperties.put(FIELD_CREATED_BY, this.parseUserInfo(value.asObject()));
                return;
            } else if (member.getName().equals(FIELD_MODIFIED_BY)) {
                this.mProperties.put(FIELD_MODIFIED_BY, this.parseUserInfo(value.asObject()));
                return;
            } else if (member.getName().equals(FIELD_OWNED_BY)) {
                this.mProperties.put(FIELD_OWNED_BY, this.parseUserInfo(value.asObject()));
                return;
            } else if (member.getName().equals(FIELD_SHARED_LINK)) {
                BoxSharedLink sl = new BoxSharedLink();
                sl.createFromJson(value.asObject());
                this.mProperties.put(FIELD_SHARED_LINK, sl);
                return;
            } else if (member.getName().equals(FIELD_PARENT)) {
                BoxFolder folder = new BoxFolder();
                folder.createFromJson(value.asObject());
                this.mProperties.put(FIELD_PARENT, folder);
                return;
            } else if (member.getName().equals(FIELD_ITEM_STATUS)) {
                this.mProperties.put(FIELD_ITEM_STATUS, value.asString());
                return;
            } else if (member.getName().equals(FIELD_SYNCED)) {
                this.mProperties.put(FIELD_SYNCED, value.asBoolean());
                return;
            } else if (member.getName().equals(BoxConstants.FIELD_COMMENT_COUNT)) {
                this.mProperties.put(BoxConstants.FIELD_COMMENT_COUNT, value.asLong());
                return;
            } else if (member.getName().equals(FIELD_ALLOWED_SHARED_LINK_ACCESS_LEVELS)) {
                JsonArray accessArr = value.asArray();
                ArrayList<BoxSharedLink.Access> accessLevels = new ArrayList<BoxSharedLink.Access>();
                for (JsonValue val : accessArr) {
                    accessLevels.add(BoxSharedLink.Access.fromString(val.asString()));
                }
                this.mProperties.put(FIELD_ALLOWED_SHARED_LINK_ACCESS_LEVELS, accessLevels);
                return;
            } else if (member.getName().equals(FIELD_TAGS)) {
                this.mProperties.put(FIELD_TAGS, value.asArray());
                return;
            }
        } catch (Exception e) {
            assert false : "A ParseException indicates a bug in the SDK.";
        }
        super.parseJSONMember(member);
    }

    private List<BoxFolder> parsePathCollection(JsonObject jsonObject) {
        int count = jsonObject.get("total_count").asInt();
        List<BoxFolder> pathCollection = new ArrayList<BoxFolder>(count);
        JsonArray entries = jsonObject.get("entries").asArray();
        for (JsonValue value : entries) {
            JsonObject entry = value.asObject();
            BoxFolder folder = new BoxFolder();
            folder.createFromJson(entry);
            pathCollection.add(folder);
        }

        return pathCollection;
    }

    private BoxUser parseUserInfo(JsonObject jsonObject) {
        BoxUser user = new BoxUser();
        user.createFromJson(jsonObject);
        return user;
    }

    private List<String> parseTags(JsonArray jsonArray) {
        List<String> tags = new ArrayList<String>(jsonArray.size());
        for (JsonValue value : jsonArray) {
            tags.add(value.asString());
        }

        return tags;
    }

    @Override
    protected JsonValue parseJsonObject(Map.Entry<String, Object> entry) {
        return super.parseJsonObject(entry);
    }

    /**
     * Creates a BoxItem object from a JSON string.
     *
     * @param json JSON string to convert to a BoxItem.
     * @return BoxItem object representing information in the JSON string.
     */
    public static BoxItem createBoxItemFromJson(final String json) {
        BoxEntity createdByEntity = new BoxEntity();
        createdByEntity.createFromJson(json);
        if (createdByEntity.getType().equals(BoxFile.TYPE)) {
            BoxFile file = new BoxFile();
            file.createFromJson(json);
            return file;
        } else if (createdByEntity.getType().equals(BoxBookmark.TYPE)) {
            BoxBookmark bookmark = new BoxBookmark();
            bookmark.createFromJson(json);
            return bookmark;
        } else if (createdByEntity.getType().equals(BoxFolder.TYPE)) {
            BoxFolder folder = new BoxFolder();
            folder.createFromJson(json);
            return folder;

        }
        return null;
    }

    /**
     * Creates a BoxItem object from a JsonObject.
     *
     * @param json JsonObject to convert to a BoxItem.
     * @return BoxItem object representing information in the JsonObject.
     */
    public static BoxItem createBoxItemFromJson(final JsonObject json) {
        BoxEntity createdByEntity = new BoxEntity();
        createdByEntity.createFromJson(json);
        if (createdByEntity.getType().equals(BoxFile.TYPE)) {
            BoxFile file = new BoxFile();
            file.createFromJson(json);
            return file;
        } else if (createdByEntity.getType().equals(BoxBookmark.TYPE)) {
            BoxBookmark bookmark = new BoxBookmark();
            bookmark.createFromJson(json);
            return bookmark;
        } else if (createdByEntity.getType().equals(BoxFolder.TYPE)) {
            BoxFolder folder = new BoxFolder();
            folder.createFromJson(json);
            return folder;

        }
        return null;
    }

}
