package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxVoid;

/**
 * Abstract class that represents a request to delete a BoxItem.
 *
 * @param <R>   type of BoxRequest that is being created.
 */
abstract class BoxRequestItemDelete<R extends BoxRequest<BoxVoid,R>> extends BoxRequest<BoxVoid, R> {
    protected String mId;

    /**
     * Constructs an item delete request with the default parameters.
     *
     * @param id    id of the Box item to delete.
     * @param requestUrl    URL of the delete endpoint.
     * @param session   the authenticated session that will be used to make the request with.
     */
    public BoxRequestItemDelete(String id, String requestUrl, BoxSession session) {
        super(BoxVoid.class, requestUrl, session);
        mId = id;
        mRequestMethod = Methods.DELETE;
    }

    /**
     * Gets the id of the item to delete.
     *
     * @return id of the item to delete.
     */
    public String getId() {
        return mId;
    }

    /**
     * Sets the if-match header for the request.
     * The item will only be deleted if the specified etag matches the most current etag for the item.
     *
     * @param etag  etag to set in the if-match header.
     * @return  the request with the updated if-match header.
     */
    @Override
    public R setIfMatchEtag(String etag) {
        return super.setIfMatchEtag(etag);
    }

    /**
     * Returns the currently set etag for the if-match header of the request.
     *
     * @return  etag currently set in the if-match header.
     */
    @Override
    public String getIfMatchEtag() {
        return super.getIfMatchEtag();
    }

}
