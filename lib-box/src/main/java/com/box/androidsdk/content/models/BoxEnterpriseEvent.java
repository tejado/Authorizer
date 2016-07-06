package com.box.androidsdk.content.models;

import com.box.androidsdk.content.utils.BoxDateFormat;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.text.ParseException;
import java.util.Map;

/**
 * Class that represents an event in an enterprise that was fired off by the Box events API.
 */
public class BoxEnterpriseEvent extends BoxEvent {

    private static final long serialVersionUID = -1404872691081072451L;
    public static final String FIELD_IP_ADDRESS = "ip_address";
    public static final String FIELD_ACCESSIBLE_BY = "accessible_by";
    public static final String FIELD_ADDITIONAL_DETAILS = "additional_details";


    @Override
    protected void parseJSONMember(JsonObject.Member member) {
        String memberName = member.getName();

        JsonValue value = member.getValue();
        if (memberName.equals(FIELD_ACCESSIBLE_BY)) {
            this.mProperties.put(FIELD_ACCESSIBLE_BY, BoxCollaborator.createEntityFromJson(value.asObject()));
            return;
        } else if (memberName.equals(FIELD_ADDITIONAL_DETAILS)) {
            //for now just store this as a string. When more defined we can create additional objects based off of the type.
            this.mProperties.put(FIELD_ADDITIONAL_DETAILS, value.toString());
            return;
        } else if (memberName.equals(FIELD_IP_ADDRESS)) {
            this.mProperties.put(FIELD_IP_ADDRESS, value.asString());
            return;
        }
        super.parseJSONMember(member);
    }

    /**
     * The user or group that this event is accessible by.
     *
     * @return The user or group that this event is accessible by.
     */
    public BoxCollaborator getAccessibleBy() {
        return (BoxCollaborator) mProperties.get(FIELD_ACCESSIBLE_BY);
    }


    /**
     * Any additional details that are important for this event. Currently returned as a String.
     *
     * @return additional details important for this sdk.
     */
    public String getAdditionalDetails() {
        return (String) mProperties.get(FIELD_ADDITIONAL_DETAILS);
    }

    /**
     * An address of a user event or enterprise event.
     *
     * @return An ip address of a a user event or enterprise event.
     */
    public String getIpAddress() {
        return (String) mProperties.get(FIELD_IP_ADDRESS);
    }

    /**
     * Enumerates the known possible types for an enterprise event.
     */
    public enum Type {
        /**
         * Added user to group
         */
        GROUP_ADD_USER,
        /**
         * Created user
         */
        NEW_USER,
        /**
         * Created new group
         */
        GROUP_CREATION,
        /**
         * Deleted group
         */
        GROUP_DELETION,
        /**
         * Deleted user
         */
        DELETE_USER,
        /**
         * Edited group
         */
        GROUP_EDITED,
        /**
         * Edited user
         */
        EDIT_USER,
        /**
         * Granted folder access
         */
        GROUP_ADD_FOLDER,
        /**
         * Removed from group
         */
        GROUP_REMOVE_USER,
        /**
         * Removed folder access
         */
        GROUP_REMOVE_FOLDER,
        /**
         * Admin login
         */
        ADMIN_LOGIN,
        /**
         * Added device association
         */
        ADD_DEVICE_ASSOCIATION,
        /**
         * Failed login
         */
        FAILED_LOGIN,
        /**
         * Login
         */
        LOGIN,
        /**
         * OAuth2 token was refreshed for this user
         */
        USER_AUTHENTICATE_OAUTH2_TOKEN_REFRESH,
        /**
         * Removed device association
         */
        REMOVE_DEVICE_ASSOCIATION,
        /**
         * Agreed to terms
         */
        TERMS_OF_SERVICE_AGREE,
        /**
         * Rejected terms
         */
        TERMS_OF_SERVICE_REJECT,
        /**
         * Copied
         */
        COPY,
        /**
         * Deleted
         */
        DELETE,
        /**
         * Downloaded
         */
        DOWNLOAD,
        /**
         * Edited
         */
        EDIT,
        /**
         * Locked
         */
        LOCK,
        /**
         * Moved
         */
        MOVE,
        /**
         * Previewed
         */
        PREVIEW,
        /**
         * Renamed
         */
        RENAME,
        /**
         * Set file auto-delete
         */
        STORAGE_EXPIRATION,
        /**
         * Undeleted
         */
        UNDELETE,
        /**
         * Unlocked
         */
        UNLOCK,
        /**
         * Uploaded
         */
        UPLOAD,
        /**
         * Enabled shared links
         */
        SHARE,
        /**
         * Share links settings updated
         */
        ITEM_SHARED_UPDATE,
        /**
         * Extend shared link expiration
         */
        UPDATE_SHARE_EXPIRATION,
        /**
         * Set shared link expiration
         */
        SHARE_EXPIRATION,
        /**
         * Unshared links
         */
        UNSHARE,
        /**
         * Accepted invites
         */
        COLLABORATION_ACCEPT,
        /**
         * Changed user roles
         */
        COLLABORATION_ROLE_CHANGE,
        /**
         * Extend collaborator expiration
         */
        UPDATE_COLLABORATION_EXPIRATION,
        /**
         * Removed collaborators
         */
        COLLABORATION_REMOVE,
        /**
         * Invited
         */
        COLLABORATION_INVITE,
        /**
         * Set collaborator expiration
         */
        COLLABORATION_EXPIRATION,
        /**
         * Synced folder
         */
        ITEM_SYNC,
        /**
         * Un-synced folder
         */
        ITEM_UNSYNC

    }

    /**
     * Constructs an empty BoxEnterpriseEvent object.
     */
    public BoxEnterpriseEvent() {
        super();
    }


    /**
     * Constructs a BoxEnterpriseEvent with the provided map values.
     *
     * @param map map of keys and values of the object.
     */
    public BoxEnterpriseEvent(Map<String, Object> map) {
        super(map);
    }


}