package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxSharedLink;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class that represents an item update request.
 *
 * @param <E>   type of BoxItem that is being updated.
 * @param <R>   type of BoxRequest that is being created.
 */
public abstract class BoxRequestItemUpdate<E extends BoxItem, R extends BoxRequest<E,R>> extends BoxRequestItem<E, R> {

    /**
     * Creates an update item request with the default parameters.
     *
     * @param clazz class of the item to return in the response.
     * @param id    id of the item being updated.
     * @param requestUrl    URL for the update item endpoint.
     * @param session   authenticated session that will be used to make the request with.
     */
    public BoxRequestItemUpdate(Class<E> clazz, String id, String requestUrl, BoxSession session) {
        super(clazz, id, requestUrl, session);
        mRequestMethod = Methods.PUT;
    }

    protected BoxRequestItemUpdate(BoxRequestItemUpdate r) {
        super(r);
    }

    /**
     * Converts the BoxRequestItemUpdate into a BoxRequestUpdateSharedItem. Using the request you can update shared link of the item.
     * @return  BoxRequestUpdateSharedItem from the BoxRequestItemUpdate.
     */
    public abstract BoxRequestUpdateSharedItem updateSharedLink();

    /**
     * Returns the new name currently set for the item.
     *
     * @return  name for the item, or null if not set.
     */
    public String getName() {
        return mBodyMap.containsKey(BoxItem.FIELD_NAME) ?
                (String) mBodyMap.get(BoxItem.FIELD_NAME) :
                null;
    }

    /**
     * Sets the new name for the item.
     *
     * @param name  new name for the item.
     * @return  request with the updated name.
     */
    public R setName(String name) {
        mBodyMap.put(BoxItem.FIELD_NAME, name);
        return (R) this;
    }

    /**
     * Returns the new description currently set for the item.
     *
     * @return  description for the item, or null if not set.
     */
    public String getDescription() {
        return mBodyMap.containsKey(BoxItem.FIELD_DESCRIPTION) ?
                (String) mBodyMap.get(BoxItem.FIELD_DESCRIPTION) :
                null;
    }

    /**
     * Sets the new description for the item.
     *
     * @param description   description for the item.
     * @return  request with the updated description.
     */
    public R setDescription(String description) {
        mBodyMap.put(BoxItem.FIELD_DESCRIPTION, description);
        return (R) this;
    }

    /**
     * Returns the new parent id currently set for the item.
     *
     * @return  id of the parent folder for the item, or null if not set.
     */
    public String getParentId() {
        return mBodyMap.containsKey(BoxItem.FIELD_PARENT) ?
                ((BoxFolder) mBodyMap.get(BoxItem.FIELD_PARENT)).getId() :
                null;
    }

    /**
     * Sets the new parent id for the item.
     *
     * @param parentId  id of the new parent folder for the item.
     * @return  request with the updated parent id.
     */
    public R setParentId(String parentId) {
        LinkedHashMap<String,Object> map = new LinkedHashMap<String,Object>();
        map.put(BoxItem.FIELD_ID, parentId);
        BoxFolder parentFolder = new BoxFolder(map);
        mBodyMap.put(BoxItem.FIELD_PARENT, parentFolder);
        return (R) this;
    }

    /**
     * Returns the shared link currently set for the item.
     *
     * @return  shared link for the item, or null if not set.
     */
    public BoxSharedLink getSharedLink() {
        return mBodyMap.containsKey(BoxItem.FIELD_SHARED_LINK) ?
                ((BoxSharedLink) mBodyMap.get(BoxItem.FIELD_SHARED_LINK)) :
                null;
    }

    /**
     * Sets the new shared link for the item.
     *
     * @param sharedLink    new shared link for the item.
     * @return  request with the updated shared link.
     */
    public R setSharedLink(BoxSharedLink sharedLink) {
        mBodyMap.put(BoxItem.FIELD_SHARED_LINK, sharedLink);
        return (R) this;
    }


    /**
     * Sets the if-match header for the request.
     * The item will only be updated if the specified etag matches the most current etag for the item.
     *
     * @param etag  etag to set in the if-match header.
     * @return  request with the updated if-match header.
     */
    @Override
    public R setIfMatchEtag(String etag) {
        return super.setIfMatchEtag(etag);
    }

    /**
     * Returns the etag currently set in the if-match header.
     *
     * @return  etag set in the if-match header, or null if none set.
     */
    @Override
    public String getIfMatchEtag() {
        return super.getIfMatchEtag();
    }

    /**
     * Returns the tags currently set for the item.
     *
     * @return  tags for the item, or null if not set.
     */
    public List<String> getTags() {
        return mBodyMap.containsKey(BoxItem.FIELD_TAGS) ?
                (List<String>) mBodyMap.get(BoxItem.FIELD_TAGS) :
                null;
    }

    /**
     * Sets the new tags for the item.
     *
     * @param tags  new tags for the item.
     * @return  request with the updated tags.
     */
    public R setTags(List<String> tags) {
        JsonArray jsonArray = new JsonArray();
        for (String s : tags) {
            jsonArray.add(s);
        }
        mBodyMap.put(BoxItem.FIELD_TAGS, jsonArray);
        return (R) this;
    }

    @Override
    protected void parseHashMapEntry(JsonObject jsonBody, Map.Entry<String,Object> entry) {
        if (entry.getKey().equals(BoxItem.FIELD_PARENT)) {
            jsonBody.add(entry.getKey(), parseJsonObject(entry.getValue()));
            return;
        } else if (entry.getKey().equals(BoxItem.FIELD_SHARED_LINK)) {
            if (entry.getValue() == null) {
                // Adding a null shared link signifies that the link should be disabled
                jsonBody.add(entry.getKey(), (String) null);
            } else {
                jsonBody.add(entry.getKey(), parseJsonObject(entry.getValue()));
            }
            return;
        }
        super.parseHashMapEntry(jsonBody, entry);
    }
}
