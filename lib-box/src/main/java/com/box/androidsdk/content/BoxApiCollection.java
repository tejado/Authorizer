package com.box.androidsdk.content;

import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsCollections;

/**
 * Represents the API of the collections endpoint on Box. This class can be used to generate request objects
 * for each of the APIs exposed endpoints
 */
public class BoxApiCollection extends BoxApi {

    /**
     * Constructs a BoxApiCollection with the provided BoxSession
     *
     * @param session authenticated session to use with the BoxApiCollection
     */
    public BoxApiCollection(BoxSession session) {
        super(session);
    }

    /**
     * Gets the URL for getting the available collections
     *
     * @return the URL string for getting the available collections
     */
    protected String getCollectionsUrl() {
        return String.format("%s/collections", getBaseUri());
    }

    /**
     * Gets the URL for getting the items of a collection
     *
     * @return the URL string for getting a collections items
     */
    protected String getCollectionItemsUrl(String id) {
        return String.format("%s/%s/items", getCollectionsUrl(), id);
    }

    /**
     * Gets a request that gets the collections belonging to the user
     *
     * @return request to get collections belonging to the user
     */
    public BoxRequestsCollections.GetCollections getCollectionsRequest() {
        BoxRequestsCollections.GetCollections request = new BoxRequestsCollections.GetCollections(getCollectionsUrl(), mSession);
        return request;
    }

    /**
     * Gets a request that gets the items of a collection
     *
     * @param id id of collection to retrieve items of
     * @return request to get a collections items
     */
    public BoxRequestsCollections.GetCollectionItems getItemsRequest(String id) {
        BoxRequestsCollections.GetCollectionItems request = new BoxRequestsCollections.GetCollectionItems(getCollectionItemsUrl(id), mSession);
        return request;
    }

}
