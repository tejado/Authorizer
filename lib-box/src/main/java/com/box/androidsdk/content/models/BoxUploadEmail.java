package com.box.androidsdk.content.models;

import android.text.TextUtils;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.util.Locale;
import java.util.Map;

/**
 * Represents an email address that can be used to upload files to a folder on Box.
 */
public class BoxUploadEmail extends BoxJsonObject {

    private static final long serialVersionUID = -1707312180661448119L;
    public static final String FIELD_ACCESS = "access";
    public static final String FIELD_EMAIL = "email";

    /**
     * Constructs a BoxUploadEmail with default settings.
     */
    public BoxUploadEmail() {
    }

    /**
     * Constructs a BoxUploadEmail with the provided map values.
     *
     * @param map map of keys and values of the object.
     */
    public BoxUploadEmail(Map<String, Object> map) {
        super(map);
    }

    /**
     * Gets the access level of this upload email.
     *
     * @return the access level of this upload email.
     */
    public Access getAccess() {
        return (Access) mProperties.get(FIELD_ACCESS);
    }

    /**
     * Gets the email address of this upload email.
     *
     * @return the email address of this upload email.
     */
    public String getEmail() {
        return (String) mProperties.get(FIELD_EMAIL);
    }

    @Override
    protected void parseJSONMember(JsonObject.Member member) {
        JsonValue value = member.getValue();
        if (member.getName().equals(FIELD_ACCESS)) {
            this.mProperties.put(FIELD_ACCESS, Access.fromString(value.asString()));
            return;
        } else if (member.getName().equals(FIELD_EMAIL)) {
            this.mProperties.put(FIELD_EMAIL, value.asString());
            return;
        }

        super.parseJSONMember(member);
    }

    /**
     * Enumerates the possible access levels that can be set on an upload email.
     */
    public enum Access {
        /**
         * Anyone can send an upload to this email address.
         */
        OPEN("open"),

        /**
         * Only collaborators can send an upload to this email address.
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
