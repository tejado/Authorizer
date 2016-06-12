package com.box.androidsdk.content.models;

import android.text.TextUtils;

import com.box.androidsdk.content.utils.BoxDateFormat;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Class that represents a link to a file, folder, or bookmark on Box.
 */
public class BoxSharedLink extends BoxJsonObject {

    private static final long serialVersionUID = -4595593930118314932L;
    public static final String FIELD_URL = "url";
    public static final String FIELD_DOWNLOAD_URL = "download_url";
    public static final String FIELD_VANITY_URL = "vanity_url";
    public static final String FIELD_IS_PASSWORD_ENABLED = "is_password_enabled";
    public static final String FIELD_UNSHARED_AT = "unshared_at";
    public static final String FIELD_DOWNLOAD_COUNT = "download_count";
    public static final String FIELD_PREVIEW_COUNT = "preview_count";
    public static final String FIELD_ACCESS = "access";
    public static final String FIELD_PERMISSIONS = "permissions";
    public static final String FIELD_EFFECTIVE_ACCESS = "effective_access";
    public static final String FIELD_PASSWORD = "password";


    /**
     * Constructs a BoxSharedLink with default settings.
     */
    public BoxSharedLink() {
    }

    /**
     * Constructs a BoxSharedLink with the provided map values.
     *
     * @param map map of keys and values of the object.
     */
    public BoxSharedLink(Map<String, Object> map) {
        super(map);
    }

    /**
     * Get the URL of this shared link.
     *
     * @return the URL of this shared link.
     */
    public String getURL() {
        return (String) mProperties.get(FIELD_URL);
    }

    /**
     * Gets the direct download URL of this shared link.
     *
     * @return the direct download URL of this shared link.
     */
    public String getDownloadURL() {
        return (String) mProperties.get(FIELD_DOWNLOAD_URL);
    }

    /**
     * Gets the vanity URL of this shared link.
     *
     * @return the vanity URL of this shared link.
     */
    public String getVanityURL() {
        return (String) mProperties.get(FIELD_VANITY_URL);
    }

    /**
     * Gets whether or not a password is enabled on this shared link.
     *
     * @return true if there's a password enabled on this shared link; otherwise false.
     */
    public Boolean getIsPasswordEnabled() {
        return (Boolean) mProperties.get(FIELD_IS_PASSWORD_ENABLED);
    }

    /**
     * Gets the time that this shared link will be deactivated.
     *
     * @return the time that this shared link will be deactivated.
     */
    public Date getUnsharedDate() {
        return (Date) mProperties.get(FIELD_UNSHARED_AT);
    }

    /**
     * Gets the number of times that this shared link has been downloaded.
     *
     * @return the number of times that this link has been downloaded.
     */
    public Long getDownloadCount() {
        return (Long) mProperties.get(FIELD_DOWNLOAD_COUNT);
    }

    /**
     * Gets the number of times that this shared link has been previewed.
     *
     * @return the number of times that this link has been previewed.
     */
    public Long getPreviewCount() {
        return (Long) mProperties.get(FIELD_PREVIEW_COUNT);
    }

    /**
     * Gets the access level of this shared link.
     *
     * @return the access level of this shared link.
     */
    public Access getAccess() {
        return (Access) mProperties.get(FIELD_ACCESS);
    }


    /**
     * Gets the password for this shared link.
     *
     * @return the password of this shared link.
     */
    public String getPassword() {
        return (String) mProperties.get(FIELD_PASSWORD);
    }

    /**
     * Gets the effective access level of this shared link, can differ from actual access and this value
     * controls actual behavior of the link.
     *
     * @return the effective access level of this shared link.
     */
    public Access getEffectiveAccess() {
        return (Access) mProperties.get(FIELD_EFFECTIVE_ACCESS);
    }

    /**
     * Gets the permissions associated with this shared link.
     *
     * @return the permissions associated with this shared link.
     */
    public Permissions getPermissions() {
        return (Permissions) mProperties.get(FIELD_PERMISSIONS);
    }

    @Override
    protected void parseJSONMember(JsonObject.Member member) {
        JsonValue value = member.getValue();
        try {
            if (member.getName().equals(FIELD_URL)) {
                this.mProperties.put(FIELD_URL, value.asString());
                return;
            } else if (member.getName().equals(FIELD_DOWNLOAD_URL)) {
                this.mProperties.put(FIELD_DOWNLOAD_URL, value.asString());
                return;
            } else if (member.getName().equals(FIELD_VANITY_URL)) {
                this.mProperties.put(FIELD_VANITY_URL, value.asString());
                return;
            } else if (member.getName().equals(FIELD_IS_PASSWORD_ENABLED)) {
                this.mProperties.put(FIELD_IS_PASSWORD_ENABLED, value.asBoolean());
                return;
            } else if (member.getName().equals(FIELD_UNSHARED_AT)) {
                this.mProperties.put(FIELD_UNSHARED_AT, BoxDateFormat.parse(value.asString()));
                return;
            } else if (member.getName().equals(FIELD_DOWNLOAD_COUNT)) {
                this.mProperties.put(FIELD_DOWNLOAD_COUNT, Double.valueOf(value.toString()).longValue());
                return;
            } else if (member.getName().equals(FIELD_PREVIEW_COUNT)) {
                this.mProperties.put(FIELD_PREVIEW_COUNT, Double.valueOf(value.toString()).longValue());
                return;
            } else if (member.getName().equals(FIELD_ACCESS)) {
                this.mProperties.put(FIELD_ACCESS, Access.fromString(value.asString()));
                return;
            } else if (member.getName().equals(FIELD_EFFECTIVE_ACCESS)) {
                this.mProperties.put(FIELD_EFFECTIVE_ACCESS, Access.fromString(value.asString()));
                return;
            } else if (member.getName().equals(FIELD_PERMISSIONS)) {
                Permissions permissions = new Permissions();
                permissions.createFromJson(value.asObject());
                this.mProperties.put(FIELD_PERMISSIONS, permissions);
                return;
            }
        } catch (ParseException e) {
            assert false : "A ParseException indicates a bug in the SDK.";
        }

        super.parseJSONMember(member);
    }

    /**
     * Contains permissions fields that can be set on a shared link.
     */
    public static class Permissions extends BoxJsonObject {

        public static final String FIELD_CAN_DOWNLOAD = "can_download";
        // we do not want to expose this field since it has been deprecated. If returned we will parse it, but do nothing with it.
        private static final String FIELD_CAN_PREVIEW = "can_preview";

        /**
         * Constructs a Permissions object with all permissions disabled.
         */
        public Permissions() {
        }


        /**
         * Constructs Permssions with the provided map values.
         *
         * @param map map of keys and values of the object.
         */
        public Permissions(Map<String, Object> map) {
            super(map);
        }

        /**
         * Gets whether or not the shared link can be downloaded.
         *
         * @return true if the shared link can be downloaded; otherwise false.
         */
        public Boolean getCanDownload() {
            return (Boolean) mProperties.get(FIELD_CAN_DOWNLOAD);
        }

        @Override
        protected void parseJSONMember(JsonObject.Member member) {
            JsonValue value = member.getValue();
            if (member.getName().equals(FIELD_CAN_DOWNLOAD)) {
                this.mProperties.put(FIELD_CAN_DOWNLOAD, value.asBoolean());
            } else if (member.getName().equals(FIELD_CAN_PREVIEW)) {
                this.mProperties.put(FIELD_CAN_PREVIEW, value.asBoolean());
            }
        }
    }

    /**
     * Enumerates the possible access levels that can be set on a shared link.
     */
    public enum Access {
        /**
         * The default access level for the user or enterprise.
         */
        DEFAULT(null),

        /**
         * The link can be accessed by anyone.
         */
        OPEN("open"),

        /**
         * The link can be accessed by other users within the company.
         */
        COMPANY("company"),

        /**
         * The link can be accessed by other collaborators.
         */
        COLLABORATORS("collaborators");

        private final String mValue;

        public static Access fromString(String text) {
            if (!TextUtils.isEmpty(text)) {
                for (Access e : Access.values()) {
                    if (text.equalsIgnoreCase(e.toString())) {
                        return e;
                    }
                }
            }
            throw new IllegalArgumentException(String.format(Locale.ENGLISH, "No enum with text %s found", text));
        }

        private Access(String value) {
            this.mValue = value;
        }

        @Override
        public String toString() {
            return this.mValue;
        }
    }
}