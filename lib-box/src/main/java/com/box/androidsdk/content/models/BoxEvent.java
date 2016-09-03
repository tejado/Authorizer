package com.box.androidsdk.content.models;

import com.box.androidsdk.content.utils.BoxDateFormat;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.text.ParseException;
import java.util.Map;

/**
 * Class that represents an event fired off by the Box events API.
 */
public class BoxEvent extends BoxEntity {

    private static final long serialVersionUID = -2242620054949669032L;

    public static final String TYPE = "event";

    public static final String FIELD_TYPE = "type";
    public static final String FIELD_EVENT_ID = "event_id";
    public static final String FIELD_CREATED_BY = "created_by";
    public static final String FIELD_EVENT_TYPE = "event_type";
    public static final String FIELD_SESSION_ID = "session_id";
    public static final String FIELD_IS_PACKAGE = "is_package";

    public static final String FIELD_SOURCE = "source";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_RECORDED_AT = "recorded_at";


    @Override
    protected void parseJSONMember(JsonObject.Member member) {
        String memberName = member.getName();

        JsonValue value = member.getValue();

        if (memberName.equals(FIELD_TYPE)) {
            this.mProperties.put(FIELD_TYPE, value.asString());
            return;
        } else if (memberName.equals(FIELD_EVENT_ID)) {
            this.mProperties.put(FIELD_EVENT_ID, value.asString());
            return;
        } else if (memberName.equals(FIELD_CREATED_BY)) {
            this.mProperties.put(FIELD_CREATED_BY, BoxCollaborator.createCollaboratorFromJson(value.asObject()));
            return;
        } else if (memberName.equals(FIELD_EVENT_TYPE)) {
            this.mProperties.put(FIELD_EVENT_TYPE, value.asString());
            return;
        } else if (memberName.equals(FIELD_SESSION_ID)) {
            this.mProperties.put(FIELD_SESSION_ID, value.asString());
            return;
        } else if (memberName.equals(FIELD_IS_PACKAGE)) {
            this.mProperties.put(FIELD_IS_PACKAGE, value.asBoolean());
            return;
        } else if (memberName.equals(FIELD_SOURCE)) {
            this.mProperties.put(FIELD_SOURCE, BoxEntity.createEntityFromJson(value.asObject()));
            return;
        } else if (memberName.equals(FIELD_CREATED_AT)) {
            try {
                this.mProperties.put(FIELD_CREATED_AT, BoxDateFormat.parse(value.asString()));
            } catch (ParseException e) {
                mProperties.put(FIELD_CREATED_AT, null);
            }
            return;
        } else if (memberName.equals(FIELD_RECORDED_AT)) {
            try {
                this.mProperties.put(FIELD_RECORDED_AT, BoxDateFormat.parse(value.asString()));
            } catch (ParseException e) {
                mProperties.put(FIELD_RECORDED_AT, null);
            }
            return;
        }
        super.parseJSONMember(member);
    }

    /**
     * The event type, 'event'
     *
     * @return The event type, 'event'
     */
    public String getType() {
        return (String) mProperties.get(TYPE);
    }


    /**
     * The id of the event, used for de-duplication purposes.
     *
     * @return The id of the event, used for de-duplication purposes.
     */
    public String getEventId() {
        return (String) mProperties.get(FIELD_EVENT_ID);
    }

    /**
     * The user that performed the action.
     *
     * @return The user that performed the action.
     */
    public BoxCollaborator getCreatedBy() {
        return (BoxCollaborator) mProperties.get(FIELD_CREATED_BY);
    }


    /**
     * An event type from either a user event or enterprise event.
     *
     * @return An event type from either a user event or enterprise event.
     */
    public String getEventType() {
        return (String) mProperties.get(FIELD_EVENT_TYPE);
    }

    /**
     * The session of the user that performed the action
     *
     * @return true if the file is an OSX package; otherwise false.
     */
    public String getSessionId() {
        return (String) mProperties.get(FIELD_SESSION_ID);
    }

    /**
     * Gets whether or not the file is an OSX package.
     *
     * @return true if the file is an OSX package; otherwise false.
     */
    public Boolean getIsPackage() {
        return (Boolean) mProperties.get(FIELD_IS_PACKAGE);
    }

    /**
     * The object that was modified. See Object definitions for appropriate object: file, folder, comment, etc. Not all events have a source object.
     *
     * @return The object that was modified.
     */
    public BoxEntity getSource() {
        return (BoxEntity) mProperties.get(FIELD_SOURCE);
    }

    /**
     * Enumerates the possible known types for an event.
     */
    public enum Type {

        /**
         * An file or folder was created.
         */
        ITEM_CREATE,

        /**
         * An file or folder was uploaded.
         */
        ITEM_UPLOAD,

        /**
         * A comment was created on a folder, file, or other comment.
         */
        COMMENT_CREATE,

        /**
         * An file or folder was downloaded.
         */
        ITEM_DOWNLOAD,

        /**
         * A file was previewed.
         */
        ITEM_PREVIEW,

        /**
         * A file or folder was moved.
         */
        ITEM_MOVE,

        /**
         * A file or folder was copied.
         */
        ITEM_COPY,

        /**
         * A task was assigned.
         */
        TASK_ASSIGNMENT_CREATE,

        /**
         * A file was locked.
         */
        LOCK_CREATE,

        /**
         * A file was unlocked.
         */
        LOCK_DESTROY,

        /**
         * A file or folder was deleted.
         */
        ITEM_TRASH,

        /**
         * A file or folder was recovered from the trash.
         */
        ITEM_UNDELETE_VIA_TRASH,

        /**
         * A collaborator was added to a folder.
         */
        COLLAB_ADD_COLLABORATOR,

        /**
         * A collaborator was removed from a folder.
         */
        COLLAB_REMOVE_COLLABORATOR,

        /**
         * A collaborator was invited to a folder.
         */
        COLLAB_INVITE_COLLABORATOR,

        /**
         * A collaborator's role was change in a folder.
         */
        COLLAB_ROLE_CHANGE,

        /**
         * A folder was marked for sync.
         */
        ITEM_SYNC,

        /**
         * A folder was un-marked for sync.
         */
        ITEM_UNSYNC,

        /**
         * A file or folder was renamed.
         */
        ITEM_RENAME,

        /**
         * A file or folder was enabled for sharing.
         */
        ITEM_SHARED_CREATE,

        /**
         * A file or folder was disabled for sharing.
         */
        ITEM_SHARED_UNSHARE,

        /**
         * A folder was shared.
         */
        ITEM_SHARED,

        /**
         * A tag was added to a file or folder.
         */
        TAG_ITEM_CREATE,

        /**
         * A user logged in from a new device.
         */
        ADD_LOGIN_ACTIVITY_DEVICE,

        /**
         * A user session associated with an app was invalidated.
         */
        REMOVE_LOGIN_ACTIVITY_DEVICE,

        /**
         * An admin role changed for a user.
         */
        CHANGE_ADMIN_ROLE;
    }

    /**
     * Constructs an empty BoxEvent object.
     */
    public BoxEvent() {
        super();
    }


    /**
     * Constructs a BoxEvent with the provided map values.
     *
     * @param map map of keys and values of the object.
     */
    public BoxEvent(Map<String, Object> map) {
        super(map);
    }


}