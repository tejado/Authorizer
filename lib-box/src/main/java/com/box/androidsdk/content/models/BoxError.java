package com.box.androidsdk.content.models;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.util.ArrayList;
import java.util.Map;

/**
 * Class that represents an error from Box.
 */
public class BoxError extends BoxJsonObject {

    //private static final long serialVersionUID = 1626798809346520004L;
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_CODE = "code";
    public static final String FIELD_CONTEXT_INFO = "context_info";
    public static final String FIELD_HELP_URL = "help_url";
    public static final String FIELD_MESSAGE = "message";
    public static final String FIELD_REQUEST_ID = "request_id";
    public static final String FIELD_ERROR = "error";
    public static final String FIELD_ERROR_DESCRIPTION = "error_description";


    /**
     * Constructs an empty BoxError object.
     */
    public BoxError() {
        super();
    }


    /**
     * Constructs a BoxError with the provided map values.
     * @param map   map of keys and values of the object.
     */
    public BoxError(Map<String, Object> map) {
        super(map);
    }


    /**
     * Gets the type of the error.
     *
     * @return the error type.
     */
    public String getType() {
        String type =  (String) mProperties.get(FIELD_TYPE);
        return type;
    }

    /**
     *
     * @return status code of the error.
     */
    public Integer getStatus(){
        return  (Integer) mProperties.get(FIELD_STATUS);
    }

    /**
     *
     * @return the code of the error.
     */
    public String getCode(){
        return  (String) mProperties.get(FIELD_CODE);
    }

    public ErrorContext getContextInfo(){
        return  (ErrorContext) mProperties.get(FIELD_CONTEXT_INFO);
    }

    /**
     *
     * @return a url to get more information about the error.
     */
    public String getFieldHelpUrl(){
        return  (String) mProperties.get(FIELD_HELP_URL);
    }

    /**
     *
     * @return get a human readable string describing the error.
     */
    public String getMessage(){
        return  (String) mProperties.get(FIELD_MESSAGE);
    }

    /**
     *
     * @return the id of the error.
     */
    public String getRequestId(){
        return  (String) mProperties.get(FIELD_REQUEST_ID);
    }

    /**
     *
     * @return the error code.
     */
    public String getError(){
        return (String) mProperties.get(FIELD_ERROR);
    }


    /**
     *
     * @return the error description.
     */
    public String getErrorDescription(){
        return (String) mProperties.get(FIELD_ERROR_DESCRIPTION);
    }

    @Override
    protected void parseJSONMember(JsonObject.Member member) {
        String memberName = member.getName();
        JsonValue value = member.getValue();
        if (memberName.equals(FIELD_TYPE)) {
            this.mProperties.put(FIELD_TYPE, value.asString());
            return;
        } else if (memberName.equals(FIELD_STATUS)) {
            this.mProperties.put(FIELD_STATUS, value.asInt());
            return;
        } else if (memberName.equals(FIELD_CODE)) {
            this.mProperties.put(FIELD_CODE, value.asString());
            return;
        } else if (memberName.equals(FIELD_CONTEXT_INFO)) {
            ErrorContext mapObject = new ErrorContext();
            mapObject.createFromJson(value.asObject());
            this.mProperties.put(FIELD_CONTEXT_INFO, mapObject);
            return;
        } else if (memberName.equals(FIELD_HELP_URL)) {
            this.mProperties.put(FIELD_HELP_URL, value.asString());
            return;
        } else if (memberName.equals(FIELD_MESSAGE)) {
            this.mProperties.put(FIELD_MESSAGE, value.asString());
            return;
        } else if (memberName.equals(FIELD_REQUEST_ID)) {
            this.mProperties.put(FIELD_REQUEST_ID, value.asString());
            return;
        } else if (memberName.equals(FIELD_ERROR)) {
            this.mProperties.put(FIELD_ERROR, value.asString());
            return;
        } else if (memberName.equals(FIELD_ERROR_DESCRIPTION)) {
            this.mProperties.put(FIELD_ERROR_DESCRIPTION, value.asString());
            return;
        }

        super.parseJSONMember(member);
    }

    public static class ErrorContext extends BoxMapJsonObject {

        public static final String FIELD_CONFLICTS = "conflicts";
        @Override
        protected void parseJSONMember(JsonObject.Member member) {
            String memberName = member.getName();
            JsonValue value = member.getValue();
            if (memberName.equals(FIELD_CONFLICTS)) {
                ArrayList<BoxEntity> boxItems = new ArrayList<BoxEntity>();
                if (value.isArray()) {
                    for (JsonValue jv : value.asArray()) {
                        boxItems.add(BoxEntity.createEntityFromJson(jv.asObject()));
                    }
                } else {
                    boxItems.add(BoxEntity.createEntityFromJson(value.asObject()));
                }
                this.mProperties.put(FIELD_CONFLICTS, boxItems);
                return;
            }

            super.parseJSONMember(member);
        }

        /**
         *
         * @return a list of the items that caused a conflict.
         */
        public ArrayList<BoxEntity> getConflicts(){
            return (ArrayList<BoxEntity>)this.mProperties.get(FIELD_CONFLICTS);
        }


    }
}