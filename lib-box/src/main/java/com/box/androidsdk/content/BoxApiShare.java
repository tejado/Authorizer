package com.box.androidsdk.content;

import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxSharedLinkSession;
import com.box.androidsdk.content.requests.BoxRequestsShare;

/**
 * Represents the API of the share endpoint on Box. This class can be used to generate request objects
 * for each of the APIs exposed endpoints
 */
public class BoxApiShare extends BoxApi {

    protected String getSharedItemsUrl() { return String.format("%s/shared_items", getBaseUri()); }

    /**
     * Constructs a BoxApiShare with the provided BoxSession
     *
     * @param session authenticated session to use with the BoxApiShare
     */
    public BoxApiShare(BoxSession session) {
        super(session);
    }

    /**
     * Returns a request to get a BoxItem from a shared link.
     *
     * @param sharedLink    shared link of the item to retrieve.
     * @return  request to get a BoxItem from a shared link.
     */
    public BoxRequestsShare.GetSharedLink getSharedLinkRequest(String sharedLink) {
        BoxSharedLinkSession session = new BoxSharedLinkSession(sharedLink, mSession);
        session.setSharedLink(sharedLink);
        BoxRequestsShare.GetSharedLink request = new BoxRequestsShare.GetSharedLink(getSharedItemsUrl(), session);
        return request;
    }
}
