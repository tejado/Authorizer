package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.BoxConstants;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.utils.IStreamPosition;
import com.box.androidsdk.content.models.BoxJsonObject;
import com.box.androidsdk.content.models.BoxListEvents;
import com.box.androidsdk.content.models.BoxObject;

import java.util.Collection;

/**
 * Abstract class that represents a request to get an event stream.
 *
 * @param <E>   type of BoxJsonObject returned in the response.
 * @param <R>   type of BoxRequest that is being created.
 */
abstract class BoxRequestEvent<E extends BoxJsonObject, R extends BoxRequest<E,R>> extends BoxRequest<E,R> {
    public static final String STREAM_TYPE_ALL = "all";
    public static final String STREAM_TYPE_CHANGES = "changes";
    public static final String STREAM_TYPE_SYNC = "sync";

    public static final String FIELD_STREAM_POSITION = "stream_position";
    public static final String FIELD_STREAM_TYPE = "stream_type";
    public static final String FIELD_LIMIT = "stream_limit";

    private boolean mFilterDuplicates = true;
    private E mListEvents;

    /**
     * Constructs an event request with the default parameters.
     *
     * @param clazz class of event list returned in the response.
     * @param requestUrl    URL of the event stream endpoint.
     * @param session   the authenticated session that will be used to make the request with.
     */
    public BoxRequestEvent(Class<E> clazz, String requestUrl, BoxSession session) {
        super(clazz,requestUrl, session);
        mRequestUrlString = requestUrl;
        mRequestMethod = Methods.GET;
        this.setRequestHandler(new BoxRequestHandler<BoxRequestEvent>(this) {
            public <T extends BoxObject> T onResponse(Class<T> clazz, BoxHttpResponse response) throws IllegalAccessException, InstantiationException, BoxException {
                if (response.getResponseCode() == BoxConstants.HTTP_STATUS_TOO_MANY_REQUESTS) {
                    return retryRateLimited(response);
                }
                String contentType = response.getContentType();
                T entity = clazz.newInstance();
                if (entity instanceof BoxListEvents){
                    ((BoxListEvents)(entity)).setFilterDuplicates(mFilterDuplicates);
                }
                if (entity instanceof BoxJsonObject && contentType.contains(ContentTypes.JSON.toString())) {
                    String json = response.getStringBody();
                    char charA = json.charAt(json.indexOf("event") - 1);
                    char charB = json.charAt(json.indexOf("user") - 1);

                    ((BoxJsonObject) entity).createFromJson(json);
                }
                return entity;
            }
        });

    }


    /**
     * Sets the stream position of items that should be returned. Usually a number in String format, but can be "now".
     *
     * @param streamPosition  stream position to start from for returning events.
     * @return the get events request
     */
    public R setStreamPosition(final String streamPosition){
        mQueryMap.put(FIELD_STREAM_POSITION, streamPosition);
        return (R)this;
    }

    /**
     * Limits the type of events returned. Can be set to:
     * all: returns everything
     * changes: returns tree changes
     * sync: returns tree changes only for sync folders
     * admin_logs: used for events in enterprise.
     * @param streamType  the type of events to get.
     * @return the get events request
     */
    protected R setStreamType(final String streamType){
        mQueryMap.put(FIELD_STREAM_TYPE, streamType);
        return (R)this;
    }

    /**
     * Sets the maximum number of events to return in the list of events.
     *
     * @param limit  max number of events to request.
     * @return the get events request
     */
    public R setLimit(final int limit){
        mQueryMap.put(FIELD_LIMIT, Integer.toString(limit));
        return (R)this;
    }

    /**
     * Sets whether or not the list of events contains duplicates which can be returned due to syncing issues. By default this is true.
     * @param filterDuplicates true to do duplicate removal, false to allow returned list to contain the duplicates.
     * @return the get events request
     */
    public R setFilterDuplicates(final boolean filterDuplicates){
        mFilterDuplicates = filterDuplicates;
        return (R)this;
    }

    /**
     * Convenience method. When set the request will be set to the next stream position from the given event and will will aggregate the new results with the provided list.
     * @param listEvents A list of events to add to.
     * @return A BoxRequestEvent object.
     */
    public R setPreviousListEvents(E listEvents){
        mListEvents = listEvents;
        this.setStreamPosition(((IStreamPosition)mListEvents).getNextStreamPosition().toString());
        return (R)this;
    }

    @Override
    public E send() throws BoxException {
        if (mListEvents != null){
            E nextEvents = super.send();
            ((Collection)mListEvents).addAll((Collection)super.send());
            ((Collection)nextEvents).clear();
            ((Collection)nextEvents).addAll((Collection) mListEvents);
            return nextEvents;
        }
        return super.send();
    }
}
