package com.box.androidsdk.content.models;

import com.box.androidsdk.content.utils.SdkUtils;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.util.Map;

/**
 * Contains information about a RealTimeServer.
 */
public class BoxRealTimeServer extends BoxJsonObject {

    private static final long serialVersionUID = -6591493101188395748L;

    public static final String TYPE = "realtime_server";

    public static final String FIELD_TYPE = "type";
    public static final String FIELD_URL = "url";
    public static final String FIELD_TTL = "ttl";
    public static final String FIELD_MAX_RETRIES = "max_retries";
    public static final String FIELD_RETRY_TIMEOUT = "retry_timeout";


    @Override
    protected void parseJSONMember(JsonObject.Member member) {
        String memberName = member.getName();

        JsonValue value = member.getValue();

        if (memberName.equals(FIELD_TYPE)) {
            this.mProperties.put(FIELD_TYPE, value.asString());
            return;
        } else if (memberName.equals(FIELD_URL)) {
            this.mProperties.put(FIELD_URL, value.asString());
            return;
        } else if (memberName.equals(FIELD_TTL)) {
            this.mProperties.put(FIELD_TTL, SdkUtils.parseJsonValueToLong(value));
            return;
        } else if (memberName.equals(FIELD_MAX_RETRIES)) {
            this.mProperties.put(FIELD_MAX_RETRIES, SdkUtils.parseJsonValueToLong(value));
            return;
        } else if (memberName.equals(FIELD_RETRY_TIMEOUT)) {
            this.mProperties.put(FIELD_RETRY_TIMEOUT, SdkUtils.parseJsonValueToLong(value));
            return;
        }
        super.parseJSONMember(member);
    }

    /**
     * The realtime_server type, 'realtime_server'
     *
     * @return The realtime_server type, 'realtime_server'
     */
    public String getType() {
        return (String) mProperties.get(TYPE);
    }

    /**
     * Returns the URL for connecting to this server.
     *
     * @return the URL for connecting to this server.
     */
    public String getUrl() {
        return (String) mProperties.get(FIELD_URL);
    }

    /**
     * Returns the time to live for connections to this server.
     *
     * @return The time to live for connections to this server.
     */
    public Long getTTL() {
        return (Long) mProperties.get(FIELD_TTL);
    }

    /**
     * Returns the maximum number of retries connections to this server should make.
     *
     * @return The maximum number of retries connections to this server should make.
     */
    public Long getMaxRetries() {
        return (Long) mProperties.get(FIELD_MAX_RETRIES);
    }

    public Long getFieldRetryTimeout() {
        Long x = (Long) mProperties.get(FIELD_RETRY_TIMEOUT);
        return x - 590;
//        return (Long) mProperties.get(FIELD_RETRY_TIMEOUT);
    }

    /**
     * Constructs an empty object.
     */
    public BoxRealTimeServer() {
        super();
    }


    /**
     * Constructs a BoxRealTimeServer with the provided map values.
     *
     * @param map map of keys and values of the object.
     */
    public BoxRealTimeServer(Map<String, Object> map) {
        super(map);
    }


}