package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.eclipsesource.json.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Abstract class that represents a request to restore a trashed item.
 *
 * @param <E>   type of BoxItem to restore from the trash.
 * @param <R>   type of BoxRequest that is being created.
 */
abstract class BoxRequestItemRestoreTrashed<E extends BoxItem, R extends BoxRequest<E,R>> extends BoxRequestItem<E, R> {
    public BoxRequestItemRestoreTrashed(Class<E> clazz, String id, String requestUrl, BoxSession session) {
        super(clazz, id, requestUrl, session);
        mRequestMethod = Methods.POST;
    }

    /**
     * Returns the name currently set for the restored item, or null if not set.
     *
     * @return  new name for the restored item.
     */
    public String getName() {
        return mBodyMap.containsKey(BoxItem.FIELD_NAME) ?
                (String) mBodyMap.get(BoxItem.FIELD_NAME) :
                null;
    }

    /**
     * Sets the new name for the restored item. If null, the restored item will keep its original name.
     *
     * @param name  name for the restored item.
     * @return  request with the updated name for the restored item.
     */
    public R setName(String name) {
        mBodyMap.put(BoxItem.FIELD_NAME, name);
        return (R) this;
    }

    /**
     * Returns the parent id currently set for the restored item, or null if not set.
     *
     * @return  parent id for the restored item.
     */
    public String getParentId() {
        return mBodyMap.containsKey(BoxItem.FIELD_PARENT) ?
                ((BoxFolder) mBodyMap.get(BoxItem.FIELD_PARENT)).getId() :
                null;
    }

    /**
     * Sets the id of the parent folder for the restored item. If null, the restored item will be restored to its original parent folder.
     *
     * @param parentId  id of the parent folder for the restored item.
     * @return  request with the updated parent folder id.
     */
    public R setParentId(String parentId) {
        LinkedHashMap<String,Object> map = new LinkedHashMap<String,Object>();
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
