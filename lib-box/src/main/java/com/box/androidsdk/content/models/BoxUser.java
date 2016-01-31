package com.box.androidsdk.content.models;

import android.text.TextUtils;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Class that represents a Box user.
 */
public class BoxUser extends BoxCollaborator {

    private static final long serialVersionUID = -9176113409457879123L;

    public static final String TYPE = "user";

    public static final String FIELD_LOGIN = "login";
    public static final String FIELD_ROLE = "role";
    public static final String FIELD_LANGUAGE = "language";
    public static final String FIELD_TIMEZONE = "timezone";
    public static final String FIELD_SPACE_AMOUNT = "space_amount";
    public static final String FIELD_SPACE_USED = "space_used";
    public static final String FIELD_MAX_UPLOAD_SIZE = "max_upload_size";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_JOB_TITLE = "job_title";
    public static final String FIELD_PHONE = "phone";
    public static final String FIELD_ADDRESS = "address";
    public static final String FIELD_AVATAR_URL = "avatar_url";
    public static final String FIELD_TRACKING_CODES = "tracking_codes";
    public static final String FIELD_CAN_SEE_MANAGED_USERS = "can_see_managed_users";
    public static final String FIELD_IS_SYNC_ENABLED = "is_sync_enabled";
    public static final String FIELD_IS_EXTERNAL_COLLAB_RESTRICTED = "is_external_collab_restricted";
    public static final String FIELD_IS_EXEMPT_FROM_DEVICE_LIMITS = "is_exempt_from_device_limits";
    public static final String FIELD_IS_EXEMPT_FROM_LOGIN_VERIFICATION = "is_exempt_from_login_verification";
    public static final String FIELD_ENTERPRISE = "enterprise";
    public static final String FIELD_HOSTNAME = "hostname";
    public static final String FIELD_MY_TAGS = "my_tags";

    public static final String[] ALL_FIELDS = new String[]{
            FIELD_TYPE,
            FIELD_ID,
            FIELD_NAME,
            FIELD_LOGIN,
            FIELD_CREATED_AT,
            FIELD_MODIFIED_AT,
            FIELD_ROLE,
            FIELD_LANGUAGE,
            FIELD_TIMEZONE,
            FIELD_SPACE_AMOUNT,
            FIELD_SPACE_USED,
            FIELD_MAX_UPLOAD_SIZE,
            FIELD_TRACKING_CODES,
            FIELD_CAN_SEE_MANAGED_USERS,
            FIELD_IS_SYNC_ENABLED,
            FIELD_IS_EXTERNAL_COLLAB_RESTRICTED,
            FIELD_STATUS,
            FIELD_JOB_TITLE,
            FIELD_PHONE,
            FIELD_ADDRESS,
            FIELD_AVATAR_URL,
            FIELD_IS_EXEMPT_FROM_DEVICE_LIMITS,
            FIELD_IS_EXEMPT_FROM_LOGIN_VERIFICATION,
            FIELD_ENTERPRISE,
            FIELD_HOSTNAME,
            FIELD_MY_TAGS
    };

    /**
     * Constructs an empty BoxUser object.
     */
    public BoxUser() {
        super();
    }

    /**
     * Constructs a BoxCollaboration with the provided map values.
     *
     * @param map map of keys and values of the object.
     */
    public BoxUser(Map<String, Object> map) {
        super(map);
    }

    /**
     * A convenience method to create an empty user with just the id and type fields set. This allows
     * the ability to interact with the content sdk in a more descriptive and type safe manner
     *
     * @param userId the id of user to create
     * @return an empty BoxUser object that only contains id and type information
     */
    public static BoxUser createFromId(String userId) {
        LinkedHashMap<String, Object> userMap = new LinkedHashMap<String, Object>();
        userMap.put(BoxCollaborator.FIELD_ID, userId);
        userMap.put(BoxCollaborator.FIELD_TYPE, BoxUser.TYPE);
        return new BoxUser(userMap);
    }

    /**
     * Gets the email address the user uses to login.
     *
     * @return the email address the user uses to login.
     */
    public String getLogin() {
        return (String) mProperties.get(FIELD_LOGIN);
    }

    /**
     * Gets the user's enterprise role.
     *
     * @return the user's enterprise role.
     */
    public Role getRole() {
        return (Role) mProperties.get(FIELD_ROLE);
    }

    /**
     * Gets the language of the user.
     *
     * @return the language of the user.
     */
    public String getLanguage() {
        return (String) mProperties.get(FIELD_LANGUAGE);
    }

    /**
     * Gets the timezone of the user.
     *
     * @return the timezone of the user.
     */
    public String getTimezone() {
        return (String) mProperties.get(FIELD_TIMEZONE);
    }

    /**
     * Gets the user's total available space in bytes.
     *
     * @return the user's total available space in bytes.
     */
    public Long getSpaceAmount() {
        return (Long) mProperties.get(FIELD_SPACE_AMOUNT);
    }

    /**
     * Gets the amount of space the user has used in bytes.
     *
     * @return the amount of space the user has used in bytes.
     */
    public Long getSpaceUsed() {
        return (Long) mProperties.get(FIELD_SPACE_USED);
    }

    /**
     * Gets the maximum individual file size in bytes the user can have.
     *
     * @return the maximum individual file size in bytes the user can have.
     */
    public Long getMaxUploadSize() {
        return (Long) mProperties.get(FIELD_MAX_UPLOAD_SIZE);
    }

    /**
     * Gets the user's current account status.
     *
     * @return the user's current account status.
     */
    public Status getStatus() {
        return (Status) mProperties.get(FIELD_STATUS);
    }

    /**
     * Gets the job title of the user.
     *
     * @return the job title of the user.
     */
    public String getJobTitle() {
        return (String) mProperties.get(FIELD_JOB_TITLE);
    }

    /**
     * Gets the phone number of the user.
     *
     * @return the phone number of the user.
     */
    public String getPhone() {
        return (String) mProperties.get(FIELD_PHONE);
    }

    /**
     * Gets the address of the user.
     *
     * @return the address of the user.
     */
    public String getAddress() {
        return (String) mProperties.get(FIELD_ADDRESS);
    }

    /**
     * Gets the URL of the user's avatar.
     *
     * @return the URL of the user's avatar.
     */
    public String getAvatarURL() {
        return (String) mProperties.get(FIELD_AVATAR_URL);
    }

    /**
     * Gets a list of tracking codes for the user.
     *
     * @return list of tracking codes.
     */
    public List<String> getTrackingCodes() {
        return (List<String>) mProperties.get(FIELD_TRACKING_CODES);
    }

    /**
     * Gets whether or not the user can see managed users.
     *
     * @return whether the user can see managed users.
     */
    public Boolean getCanSeeManagedUsers() {
        return (Boolean) mProperties.get(FIELD_CAN_SEE_MANAGED_USERS);
    }

    /**
     * Gets whether or not sync is enabled for the user.
     *
     * @return whether sync is enabled.
     */
    public Boolean getIsSyncEnabled() {
        return (Boolean) mProperties.get(FIELD_IS_SYNC_ENABLED);
    }

    /**
     * Gets whether or not external collaboration is restricted.
     *
     * @return if external collaboration is restricted.
     */
    public Boolean getIsExternalCollabRestricted() {
        return (Boolean) mProperties.get(FIELD_IS_EXTERNAL_COLLAB_RESTRICTED);
    }

    /**
     * Gets whether or not the user is exempt from Enterprise device limits.
     *
     * @return whether or not the user is exempt from Enterprise device limits.
     */
    public Boolean getIsExemptFromDeviceLimits() {
        return (Boolean) mProperties.get(FIELD_IS_EXEMPT_FROM_DEVICE_LIMITS);
    }

    /**
     * Gets whether or not the user is exempt from two-factor authentication.
     *
     * @return whether or not the user is exempt from two-factor authentication.
     */
    public Boolean getIsExemptFromLoginVerification() {
        return (Boolean) mProperties.get(FIELD_IS_EXEMPT_FROM_LOGIN_VERIFICATION);
    }

    /**
     * Gets the users enterprise.
     *
     * @return the enterprise of the user.
     */
    public BoxEnterprise getEnterprise() {
        return (BoxEnterprise) mProperties.get(FIELD_ENTERPRISE);
    }

    /**
     * Gets the hostname associated with the user.
     *
     * @return the user's hostname.
     */
    public String getHostname() {
        return (String) mProperties.get(FIELD_HOSTNAME);
    }

    /**
     * Gets the user's tags.
     *
     * @return the user's tags.
     */
    public List<String> getMyTags() {
        return (List<String>) mProperties.get(FIELD_MY_TAGS);
    }

    @Override
    protected void parseJSONMember(JsonObject.Member member) {
        String memberName = member.getName();
        JsonValue value = member.getValue();
        if (memberName.equals(FIELD_LOGIN)) {
            this.mProperties.put(FIELD_LOGIN, value.asString());
            return;
        } else if (memberName.equals(FIELD_ROLE)) {
            this.mProperties.put(FIELD_ROLE, this.parseRole(value));
            return;
        } else if (memberName.equals(FIELD_LANGUAGE)) {
            this.mProperties.put(FIELD_LANGUAGE, value.asString());
            return;
        } else if (memberName.equals(FIELD_TIMEZONE)) {
            this.mProperties.put(FIELD_TIMEZONE, value.asString());
            return;
        } else if (memberName.equals(FIELD_SPACE_AMOUNT)) {
            this.mProperties.put(FIELD_SPACE_AMOUNT, Double.valueOf(value.toString()).longValue());
            return;
        } else if (memberName.equals(FIELD_SPACE_USED)) {
            this.mProperties.put(FIELD_SPACE_USED, Double.valueOf(value.toString()).longValue());
            return;
        } else if (memberName.equals(FIELD_MAX_UPLOAD_SIZE)) {
            this.mProperties.put(FIELD_MAX_UPLOAD_SIZE, Double.valueOf(value.toString()).longValue());
            return;
        } else if (memberName.equals(FIELD_STATUS)) {
            this.mProperties.put(FIELD_STATUS, this.parseStatus(value));
            return;
        } else if (memberName.equals(FIELD_JOB_TITLE)) {
            this.mProperties.put(FIELD_JOB_TITLE, value.asString());
            return;
        } else if (memberName.equals(FIELD_PHONE)) {
            this.mProperties.put(FIELD_PHONE, value.asString());
            return;
        } else if (memberName.equals(FIELD_ADDRESS)) {
            this.mProperties.put(FIELD_ADDRESS, value.asString());
            return;
        } else if (memberName.equals(FIELD_AVATAR_URL)) {
            this.mProperties.put(FIELD_AVATAR_URL, value.asString());
            return;
        } else if (memberName.equals(FIELD_TRACKING_CODES)) {
            this.mProperties.put(FIELD_TRACKING_CODES, this.parseJsonArray(value.asArray()));
            return;
        } else if (memberName.equals(FIELD_CAN_SEE_MANAGED_USERS)) {
            this.mProperties.put(FIELD_CAN_SEE_MANAGED_USERS, value.asBoolean());
            return;
        } else if (memberName.equals(FIELD_IS_SYNC_ENABLED)) {
            this.mProperties.put(FIELD_IS_SYNC_ENABLED, value.asBoolean());
            return;
        } else if (memberName.equals(FIELD_IS_EXTERNAL_COLLAB_RESTRICTED)) {
            this.mProperties.put(FIELD_IS_EXTERNAL_COLLAB_RESTRICTED, value.asBoolean());
            return;
        } else if (memberName.equals(FIELD_IS_EXEMPT_FROM_DEVICE_LIMITS)) {
            this.mProperties.put(FIELD_IS_EXEMPT_FROM_DEVICE_LIMITS, value.asBoolean());
            return;
        } else if (memberName.equals(FIELD_IS_EXEMPT_FROM_LOGIN_VERIFICATION)) {
            this.mProperties.put(FIELD_IS_EXEMPT_FROM_LOGIN_VERIFICATION, value.asBoolean());
            return;
        } else if (memberName.equals(FIELD_ENTERPRISE)) {
            BoxEnterprise enterprise = new BoxEnterprise();
            enterprise.createFromJson(value.asObject());
            this.mProperties.put(FIELD_ENTERPRISE, enterprise);
            return;
        } else if (memberName.equals(FIELD_HOSTNAME)) {
            this.mProperties.put(FIELD_HOSTNAME, value.asString());
            return;
        } else if (memberName.equals(FIELD_MY_TAGS)) {
            this.mProperties.put(FIELD_MY_TAGS, this.parseJsonArray(value.asArray()));
            return;
        }
        super.parseJSONMember(member);
    }

    private Role parseRole(JsonValue value) {
        String roleString = value.asString().toUpperCase();
        return Role.valueOf(roleString);
    }

    private Status parseStatus(JsonValue value) {
        String statusString = value.asString().toUpperCase();
        return Status.valueOf(statusString);
    }

    private List<String> parseJsonArray(JsonArray jsonArray) {
        List<String> tags = new ArrayList<String>(jsonArray.size());
        for (JsonValue value : jsonArray) {
            tags.add(value.asString());
        }

        return tags;
    }

    /**
     * Enumerates the possible roles that a user can have within an enterprise.
     */
    public enum Role {
        /**
         * The user is an administrator of their enterprise.
         */
        ADMIN("admin"),

        /**
         * The user is a co-administrator of their enterprise.
         */
        COADMIN("coadmin"),

        /**
         * The user is a regular user within their enterprise.
         */
        USER("user");

        private final String mValue;

        private Role(String value) {
            this.mValue = value;
        }

        public static Role fromString(String text) {
            if (!TextUtils.isEmpty(text)) {
                for (Role e : Role.values()) {
                    if (text.equalsIgnoreCase(e.toString())) {
                        return e;
                    }
                }
            }
            throw new IllegalArgumentException(String.format(Locale.ENGLISH, "No enum with text %s found", text));
        }

        @Override
        public String toString() {
            return this.mValue;
        }
    }

    /**
     * Enumerates the possible statuses that a user's account can have.
     */
    public enum Status {
        /**
         * The user's account is active.
         */
        ACTIVE("active"),

        /**
         * The user's account is inactive.
         */
        INACTIVE("inactive"),

        /**
         * The user's account cannot delete or edit content.
         */
        CANNOT_DELETE_EDIT("cannot_delete_edit"),

        /**
         * The user's account cannot delete, edit, or upload content.
         */
        CANNOT_DELETE_EDIT_UPLOAD("cannot_delete_edit_upload");

        private final String mValue;

        private Status(String value) {
            this.mValue = value;
        }

        public static Status fromString(String text) {
            if (!TextUtils.isEmpty(text)) {
                for (Status e : Status.values()) {
                    if (text.equalsIgnoreCase(e.toString())) {
                        return e;
                    }
                }
            }
            throw new IllegalArgumentException(String.format(Locale.ENGLISH, "No enum with text %s found", text));
        }

        @Override
        public String toString() {
            return this.mValue;
        }
    }
}