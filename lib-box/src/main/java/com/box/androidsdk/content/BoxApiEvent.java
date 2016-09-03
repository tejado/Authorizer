package com.box.androidsdk.content;

import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.utils.RealTimeServerConnection;
import com.box.androidsdk.content.requests.BoxRequestsEvent;

/**
 * Represents the API of the event endpoint on Box. This class can be used to generate request objects
 * for each of the APIs exposed endpoints
 */
public class BoxApiEvent extends BoxApi {

    /**
     * Constructs a BoxApiEvent with the provided BoxSession
     *
     * @param session authenticated session to use with the BoxApiEvent
     */
    public BoxApiEvent(BoxSession session) {
        super(session);
    }

    protected String getEventsUrl() { return getBaseUri() + "/events"; }


    /**
     * Gets a request that retrieves user events
     *
     * @return      request to get user events
     */
    public BoxRequestsEvent.GetUserEvents getUserEventsRequest() {
        BoxRequestsEvent.GetUserEvents request = new BoxRequestsEvent.GetUserEvents( getEventsUrl(), mSession);
        return request;
    }

    /**
     * Gets a request that retrieves enterprise events
     *
     * @return      request to retrieve enterprise events
     */
    public BoxRequestsEvent.GetEnterpriseEvents getEnterpriseEventsRequest() {
        BoxRequestsEvent.GetEnterpriseEvents request = new BoxRequestsEvent.GetEnterpriseEvents(getEventsUrl(), mSession);
        return request;
    }

    /**
     * Gets a request that retrieves a RealTimeServerConnection, which is used to create a long poll to check for some generic change
     * to the user's account. This can be combined with syncing logic using getUserEventsRequest or getEnterpriseEventsRequest to discover what has changed.
     *
     * @param changeListener A listener that will get triggered when a change has been made to the user's account or an exception has occurred.
     * @return A RealTimeServerConnection that checks for a change to a user's account.
     */
    public RealTimeServerConnection getLongPollServerConnection(RealTimeServerConnection.OnChangeListener changeListener) {
        BoxRequestsEvent.EventRealTimeServerRequest request = new BoxRequestsEvent.EventRealTimeServerRequest(getEventsUrl(), mSession);
        return new RealTimeServerConnection(request,changeListener, mSession );
    }



}