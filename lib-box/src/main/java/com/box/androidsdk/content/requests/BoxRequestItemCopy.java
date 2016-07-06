package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.eclipsesource.json.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Abstract class that represents a request to copy a BoxItem.
 *
 * @param <E>   type of BoxItem that will be returned in the response.
 * @param <R>   type of BoxRequest that is being created.
 */
abstract class BoxRequestItemCopy<E extends BoxItem, R extends BoxRequest<E,R>> extends BoxRequestItem<E, R> {

    /**
     * Constructs a box item copy request with the default parameters.
     *
     * @param clazz class of the response object.
     * @param id    id of the item to copy.
     * @param parentId  id of the parent folder for the copy of the item.
     * @param requestUrl    URL for the copy endpoint to use.
     * @param session   the authenticated session that will be used to make the request with.
     */
    public BoxRequestItemCopy(Class<E> clazz, String id, String parentId, String requestUrl, BoxSession session) {
        super(clazz, id, requestUrl, session);
        mRequestMethod = Methods.POST;
        setParentId(parentId);
    }

    /**
     * Returns the name currently set for the item copy.
     * @return  name for the item copy, or null if not set
     */
    public String getName() {
        return mBodyMap.containsKey(BoxItem.FIELD_NAME) ?
                (String) mBodyMap.get(BoxItem.FIELD_NAME) :
                null;
    }

    /**
     * Sets the name used in the request for the item copy.
     *
     * @param name  name for the copy of the item.
     * @return  request with the updated name.
     */
    public R setName(String name) {
        mBodyMap.put(BoxItem.FIELD_NAME, name);
        return (R) this;
    }

    /**
     * Returns the parent id currently set for the item copy.
     *
     * @return  id of the parent folder for the item copy.
     */
    public String getParentId() {
        return mBodyMap.containsKey(BoxItem.FIELD_PARENT) ?
                ((BoxFolder) mBodyMap.get(BoxItem.FIELD_PARENT)).getId() :
                null;
    }

    /**
     * Sets the parent id used in the request for the item copy.
     *
     * @param parentId  id of the parent folder for the item copy.
     * @return  request with the updated parent id.
     */
    public R setParentId(String parentId) {
        LinkedHashMap<String,Object> map = new LinkedHashMap<String, Object>();
        map.put(BoxItem.FIELD_ID, parentId);
        BoxFolder parentFolder = new BoxFolder(map);
        mBodyMap.put(BoxItem.FIELD_PARENT, parentFolder);
        return (R) this;
    }

    @Override
    protected void parseHashMapEntry(JsonObject jsonBody, Map.Entry<String,Object> entry) {
        if (entry.getKey().equals(BoxItem.FIELD_PARENT)) {
            jsonBody.add(entry.getKey(), parseJsonObject(entry.getValue()));
            return;
        }
        super.parseHashMapEntry(jsonBody, entry);
    }
}
