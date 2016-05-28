package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxList;

/**
 * Request for getting a list of items.
 */
abstract class BoxRequestList<E extends BoxList, R extends BoxRequest<E,R>> extends BoxRequestItem<E, R> {
    private static final String LIMIT = "limit";
    private static final String OFFSET = "offset";

    // 1000 is the current max that the API supports
    private static final String DEFAULT_LIMIT = "1000";
    private static final String DEFAULT_OFFSET = "0";


    /**
     * Creates a list request with the default parameters
     *
     * @param id            id of the list to get the items of
     * @param requestUrl    URL of the list items endpoint
     * @param session       the authenticated session that will be used to make the request with
     */
    public BoxRequestList(Class<E> clazz, String id, String requestUrl, BoxSession session) {
        super(clazz, id, requestUrl, session);
        mRequestMethod = Methods.GET;
        mQueryMap.put(LIMIT, DEFAULT_LIMIT);
        mQueryMap.put(OFFSET, DEFAULT_OFFSET);
    }

    /**
     * Sets the limit of items that should be returned
     *
     * @param limit     limit of items to return
     * @return the get folder items request
     */
    public R setLimit(int limit) {
        mQueryMap.put(LIMIT, String.valueOf(limit));
        return (R) this;
    }

    /**
     * Sets the offset of the items that should be returned
     *
     * @param offset    offset of items to return
     * @return the offset of the items to return
     */
    public R setOffset(int offset) {
        mQueryMap.put(OFFSET, String.valueOf(offset));
        return (R) this;
    }
}
