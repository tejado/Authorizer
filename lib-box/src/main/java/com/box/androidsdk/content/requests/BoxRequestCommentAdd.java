package com.box.androidsdk.content.requests;


import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxComment;
import com.box.androidsdk.content.models.BoxEntity;
import com.box.androidsdk.content.models.BoxItem;

import java.util.LinkedHashMap;

/**
 * Abstract class that represents a request to add a comment to an item on Box.
 * @param <E>   type of BoxComment object returned in the response.
 * @param <R>   type of BoxRequest to return.
 */
abstract class BoxRequestCommentAdd<E extends BoxComment, R extends BoxRequest<E,R>> extends BoxRequestItem<E, R> {

    /**
     * Creates an add comment request with the default parameters.
     *
     * @param clazz class of the BoxComment to return.
     * @param requestUrl    URL of the add comments endpoint.
     * @param session   the authenticated session that will be used to make the request with.
     */
    public BoxRequestCommentAdd(Class<E> clazz, String requestUrl, BoxSession session) {
        super(clazz, null, requestUrl, session);
        mRequestMethod = Methods.POST;
    }

    /**
     * Returns the message that is currently set in the request for the new comment.
     *
     * @return  message for the new comment.
     */
    public String getMessage() {
        return (String) mBodyMap.get(BoxComment.FIELD_MESSAGE);
    }

    /**
     * Sets the message used in the request to create a new comment.
     *
     * @param message   message for the new comment.
     * @return  request with the updated message.
     */
    public R setMessage(String message) {
        mBodyMap.put(BoxComment.FIELD_MESSAGE, message);
        return (R) this;
    }

    /**
     * Returns the id of the item currently set in the request to add a comment to.
     *
     * @return  id of the item to add a comment to.
     */
    public String getItemId() {
        return mBodyMap.containsKey(BoxComment.FIELD_ITEM) ?
                (String) mBodyMap.get(BoxItem.FIELD_ID) :
                null;
    }

    /**
     * Sets the id of the item used in the request to add a comment to.
     *
     * @param id    id of the item to add a comment to.
     * @return  request with the updated item id.
     */
    protected R setItemId(String id) {
        LinkedHashMap<String,Object> itemMap = new LinkedHashMap<String, Object>();
        if (mBodyMap.containsKey(BoxComment.FIELD_ITEM)) {
            BoxEntity item = (BoxEntity) mBodyMap.get(BoxComment.FIELD_ITEM);
            itemMap = new LinkedHashMap(item.getPropertiesAsHashMap());
        }
        itemMap.put(BoxEntity.FIELD_ID, id);
        BoxEntity item = new BoxEntity(itemMap);
        mBodyMap.put(BoxComment.FIELD_ITEM, item);
        return (R) this;
    }

    /**
     * Returns the type of item used in the request to add a comment to.
     *
     * @return  type of item to add a comment to.
     */
    public String getItemType() {
        return mBodyMap.containsKey(BoxComment.FIELD_ITEM) ?
                (String) mBodyMap.get(BoxItem.FIELD_TYPE) :
                null;
    }

    /**
     * Sets the type of item used in the request to add a comment to.
     *
     * @param type  type of item used in the request. Must be "file", "comment", or "web_link".
     * @return  request with the updated item type.
     */
    protected R setItemType(String type) {
        LinkedHashMap<String,Object> itemMap = new LinkedHashMap<String, Object>();
        if (mBodyMap.containsKey(BoxComment.FIELD_ITEM)) {
            BoxEntity item = (BoxEntity) mBodyMap.get(BoxComment.FIELD_ITEM);
            itemMap = new LinkedHashMap(item.getPropertiesAsHashMap());
        }
        itemMap.put(BoxEntity.FIELD_TYPE, type);
        BoxEntity item = new BoxEntity(itemMap);
        mBodyMap.put(BoxComment.FIELD_ITEM, item);
        return (R) this;
    }
}
