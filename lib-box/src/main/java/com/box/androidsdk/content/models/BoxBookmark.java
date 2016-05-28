package com.box.androidsdk.content.models;

import android.text.TextUtils;

import com.box.androidsdk.content.BoxConstants;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Class that represents a bookmark on Box.
 */
public class BoxBookmark extends BoxItem {
    public static final String TYPE = "web_link";
    public static final String FIELD_URL = "url";
    public static final String FIELD_COMMENT_COUNT = BoxConstants.FIELD_COMMENT_COUNT;

    public static final String[] ALL_FIELDS = new String[]{
            FIELD_TYPE,
            FIELD_ID,
            FIELD_SEQUENCE_ID,
            FIELD_ETAG,
            FIELD_NAME,
            FIELD_URL,
            FIELD_CREATED_AT,
            FIELD_MODIFIED_AT,
            FIELD_DESCRIPTION,
            FIELD_PATH_COLLECTION,
            FIELD_CREATED_BY,
            FIELD_MODIFIED_BY,
            FIELD_TRASHED_AT,
            FIELD_PURGED_AT,
            FIELD_OWNED_BY,
            FIELD_SHARED_LINK,
            FIELD_PARENT,
            FIELD_ITEM_STATUS,
            FIELD_PERMISSIONS,
            FIELD_COMMENT_COUNT
    };

    /**
     * Constructs an empty BoxBookmark object.
     */
    public BoxBookmark() {
        super();
    }

    /**
     * Constructs a BoxBookmark with the provided map values.
     *
     * @param map map of keys and values of the object.
     */
    public BoxBookmark(Map<String, Object> map) {
        super(map);
    }

    /**
     * A convenience method to create an empty bookmark with just the id and type fields set. This allows
     * the ability to interact with the content sdk in a more descriptive and type safe manner
     *
     * @param bookmarkId the id of folder to create
     * @return an empty BoxBookmark object that only contains id and type information
     */
    public static BoxBookmark createFromId(String bookmarkId) {
        LinkedHashMap<String, Object> bookmarkMap = new LinkedHashMap<String, Object>();
        bookmarkMap.put(BoxItem.FIELD_ID, bookmarkId);
        bookmarkMap.put(BoxItem.FIELD_TYPE, BoxBookmark.TYPE);
        return new BoxBookmark(bookmarkMap);
    }

    /**
     * Gets the URL of the bookmark.
     *
     * @return the URL of the bookmark.
     */
    public String getUrl() {
        return (String) mProperties.get(FIELD_URL);
    }

    @Override
    public Long getCommentCount() {
        return super.getCommentCount();
    }

    /**
     * This always returns null as size doesn't make sense for bookmarks.
     *
     * @return null.
     */
    public Long getSize() {
        return null;
    }

    /**
     * Gets the permissions that the current user has on the bookmark.
     *
     * @return the permissions that the current user has on the bookmark.
     */
    public EnumSet<Permission> getPermissions() {
        return (EnumSet<Permission>) mProperties.get(FIELD_PERMISSIONS);
    }

    @Override
    protected void parseJSONMember(JsonObject.Member member) {
        String memberName = member.getName();
        JsonValue value = member.getValue();
        if (memberName.equals(FIELD_URL)) {
            this.mProperties.put(FIELD_URL, value.asString());
            return;
        } else if (memberName.equals(FIELD_PERMISSIONS)) {
            this.mProperties.put(FIELD_PERMISSIONS, this.parsePermissions(value.asObject()));
            return;
        }
        super.parseJSONMember(member);
    }

    private EnumSet<Permission> parsePermissions(JsonObject jsonObject) {
        EnumSet<Permission> permissions = EnumSet.noneOf(Permission.class);
        for (JsonObject.Member member : jsonObject) {
            JsonValue value = member.getValue();
            if (value.isNull() || !value.asBoolean()) {
                continue;
            }

            String memberName = member.getName();
            if (memberName.equals("can_rename")) {
                permissions.add(Permission.CAN_RENAME);
            } else if (memberName.equals("can_delete")) {
                permissions.add(Permission.CAN_DELETE);
            } else if (memberName.equals("can_share")) {
                permissions.add(Permission.CAN_SHARE);
            } else if (memberName.equals("can_set_share_access")) {
                permissions.add(Permission.CAN_SET_SHARE_ACCESS);
            } else if (memberName.equals("can_comment")) {
                permissions.add(Permission.CAN_COMMENT);
            }
        }

        return permissions;
    }

    /**
     * Enumerates the possible permissions that a user can have on a bookmark.
     */
    public enum Permission {
        /**
         * The user can rename the bookmark.
         */
        CAN_RENAME("can_rename"),

        /**
         * The user can delete the bookmark.
         */
        CAN_DELETE("can_delete"),

        /**
         * The user can share the bookmark.
         */
        CAN_SHARE("can_share"),

        /**
         * The user can set the access level for shared links to the bookmark.
         */
        CAN_SET_SHARE_ACCESS("can_set_share_access"),

        /**
         * The user can comment on the bookmark.
         */
        CAN_COMMENT("can_comment");

        private final String value;

        private Permission(String value) {
            this.value = value;
        }

        /**
         * Returns Permission enum based on a string.
         *
         * @param text text to convert to a Permission enum.
         * @return enum that corresponds to the text.
         */
        public static Permission fromString(String text) {
            if (!TextUtils.isEmpty(text)) {
                for (Permission a : Permission.values()) {
                    if (text.equalsIgnoreCase(a.name())) {
                        return a;
                    }
                }
            }
            throw new IllegalArgumentException(String.format(Locale.ENGLISH, "No enum with text %s found", text));
        }

        @Override
        public String toString() {
            return this.value;
        }
    }
}
