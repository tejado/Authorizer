package com.box.androidsdk.content.requests;

import java.util.Locale;

import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxJsonObject;

/**
 * Abstract class that represents a request which returns an item in the response.
 *
 * @param <E>   type of BoxJsonObject that is returned in the response.
 * @param <R>   type of BoxRequest being created.
 */
public abstract class BoxRequestItem<E extends BoxJsonObject, R extends BoxRequest<E,R>> extends BoxRequest<E,R> {

    private static String QUERY_FIELDS = "fields";

    protected String mId = null;

    /**
     * Constructs a BoxRequestItem with the default parameters.
     *
     * @param clazz class of the object returned in the response.
     * @param id    id of the object.
     * @param requestUrl    URL of the endpoint for the request.
     * @param session   the authenticated session that will be used to make the request with.
     */
    public BoxRequestItem(Class<E> clazz, String id, String requestUrl, BoxSession session) {
        super(clazz, requestUrl, session);
        mContentType = ContentTypes.JSON;
        mId = id;
    }

    protected BoxRequestItem(BoxRequestItem r) {
        super(r);
    }

    /**
     * Sets the fields to return in the response.
     *
     * @param fields    fields to return in the response.
     * @return  request with the updated fields.
     */
    public R setFields(String... fields) {
        if (fields.length > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(fields[0]);
            for (int i = 1; i < fields.length; ++i) {
                sb.append(String.format(Locale.ENGLISH, ",%s", fields[i]));
            }
            mQueryMap.put(QUERY_FIELDS, sb.toString());
        }

        return (R) this;
    }

    /**
     * Returns the id of the Box item being modified.
     *
     * @return the id of the Box item that this request is attempting to modify.
     */
    public String getId(){
        return mId;
    }
}
