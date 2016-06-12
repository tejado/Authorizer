package com.box.androidsdk.content.requests;


import com.box.androidsdk.content.utils.BoxDateFormat;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxListEnterpriseEvents;
import com.box.androidsdk.content.models.BoxListEvents;
import com.box.androidsdk.content.models.BoxListRealTimeServers;
import com.box.androidsdk.content.models.BoxSimpleMessage;


import java.util.Date;

/**
 * Event requests.
 */
public class BoxRequestsEvent {

    /**
     * Request for retrieving a stream of user events.
     */
    public static class GetUserEvents extends BoxRequestEvent<BoxListEvents, GetUserEvents> {

        /**
         * Creates a user event stream request with the default parameters.
         *
         * @param requestUrl    URL for the events endpoint.
         * @param session   the authenticated session that will be used to make the request with.
         */
        public GetUserEvents(String requestUrl, BoxSession session) {
            super(BoxListEvents.class,requestUrl, session);
        }

        /**
         * Sets the stream type to limit the type of events in the response.
         *
         * @param streamType    type of events to return. Can be "all", "changes", or "sync".
         * @return  request with the updated stream type.
         */
        public GetUserEvents setStreamType(final String streamType){
            return super.setStreamType(streamType);
        }

    }

    /**
     * Request for retrieving a stream of all events in an enterprise.
     */
    public static class GetEnterpriseEvents extends BoxRequestEvent<BoxListEnterpriseEvents, GetEnterpriseEvents> {

        public static final String FIELD_CREATED_AFTER = "created_after";
        public static final String FIELD_CREATED_BEFORE = "created_before";
        protected static final String STREAM_TYPE = "admin_logs";

        /**
         * Creates an enterprise event stream with the default parameters.
         *
         * @param requestUrl    URL for the events endpoint.
         * @param session   the authenticated session that will be used to make the request with.
         */
        public GetEnterpriseEvents(String requestUrl, BoxSession session) {
            super(BoxListEnterpriseEvents.class, requestUrl, session);
            setStreamType(STREAM_TYPE);
        }

        /**
         * Sets the lower bound on the timestamp of events returned in the request.
         *
         * @param date  date events should be created after.
         * @return  request with the updated created after date.
         */
        public GetEnterpriseEvents setCreatedAfter(final Date date){
            mQueryMap.put(FIELD_CREATED_AFTER, BoxDateFormat.format(date));
            return this;
        }

        /**
         * Sets the upper bound on the timestamp of events returned in the request.
         *
         * @param date  date events should be created before.
         * @return  request with the updated created before date.
         */
        public GetEnterpriseEvents setCreatedBefore(final Date date){
            mQueryMap.put(FIELD_CREATED_BEFORE, BoxDateFormat.format(date));
            return this;
        }

    }

    /**
     * Request to get long poll URL for real-time event notifications.
     */
    public static class EventRealTimeServerRequest extends BoxRequest<BoxListRealTimeServers, EventRealTimeServerRequest>{

        /**
         * Creates an event real time server request with the default parameters.
         *
         * @param requestUrl    URL of the events endpoint.
         * @param session   the authenticated session that will be used to make the request with.
         */
        public EventRealTimeServerRequest(String requestUrl, BoxSession session) {
            super(BoxListRealTimeServers.class, requestUrl, session);
            mRequestUrlString = requestUrl;
            mRequestMethod = Methods.OPTIONS;
        }
    }

    /**
     * Request to begin listening to real-time event notifications.
     * If an event occurs within an account you are monitoring, you will receive a response with the value new_change. It’s important to note that this response will not come with any other details, but should serve as a prompt to take further action such as calling the /events endpoint with your last known stream_position. After sending this response, the server will close the connection and you will need to repeat the long poll process to begin listening for events again.
     * If no events occur for a period of time after you make the GET request to the long poll URL, you will receive a response with the value reconnect. When you receive this response, you’ll make another OPTIONS call to the /events endpoint and repeat the long poll process.
     * If you receive no events in retry_timeout seconds, you should make another GET request to the real time server (i.e. URL in the response). This might be necessary in case you do not receive the reconnect message in the face of network errors.
     * If you receive max_retries error when making GET requests to the real time server, you should make another OPTIONS request.
     */
    public static class LongPollMessageRequest extends BoxRequest<BoxSimpleMessage, LongPollMessageRequest>{

        /**
         * Creates a long poll request with the default parameters.
         *
         * @param requestUrl    URL to use for long polling.
         * @param session   the authenticated session that will be used to make the request with.
         */
        public LongPollMessageRequest(String requestUrl, BoxSession session) {
            super(BoxSimpleMessage.class, requestUrl, session);
            mRequestUrlString = requestUrl;
            mRequestMethod = Methods.GET;
        }
    }

}
