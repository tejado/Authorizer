package com.box.androidsdk.content.requests;

import android.text.TextUtils;

import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxCollection;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxListCollections;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Abstract class that represents a request to update a collection's contents by adding or removing an item.
 * @param <E>   type of BoxItem that is returned in the response.
 * @param <R>   type of BoxRequest that is being created.
 */
abstract class BoxRequestCollectionUpdate<E extends BoxItem, R extends BoxRequest<E,R>> extends BoxRequestItem<E,R> {

    protected static final String FIELD_COLLECTIONS = "collections";

    /**
     * Constructs an update collection request with the default parameters.
     *
     * @param clazz class of the BoxItem returned in the response.
     * @param id    id of the item to add to/remove from a collection.
     * @param requestUrl    URL of the update collection endpoint.
     * @param session   the authenticated session that will be used to make the request with.
     */
    public BoxRequestCollectionUpdate(Class<E> clazz, String id, String requestUrl, BoxSession session) {
        super(clazz, id, requestUrl, session);
        mRequestMethod = Methods.PUT;
    }

    /**
     * Sets the id of the collection to update.
     *
     * @param id    id of the collection to update.
     * @return  request with the updated collection id.
     */
    protected R setCollectionId(String id) {
        BoxListCollections collections = new BoxListCollections();
        if (!TextUtils.isEmpty(id)) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
            map.put(BoxCollection.FIELD_ID, id);
            BoxCollection col = new BoxCollection(map);
            collections.add(col);
        }
        mBodyMap.put(FIELD_COLLECTIONS, collections);
        return (R) this;
    }

    @Override
    protected void parseHashMapEntry(JsonObject jsonBody, Map.Entry<String, Object> entry) {
        if (entry.getKey().equals(FIELD_COLLECTIONS)) {
            if (entry.getValue() != null && entry.getValue() instanceof BoxListCollections) {
                BoxListCollections collections = (BoxListCollections) entry.getValue();
                JsonArray arr = new JsonArray();
                for (BoxCollection col : collections) {
                    arr.add(JsonValue.readFrom(col.toJson()));
                }
                jsonBody.add(entry.getKey(), arr);
            }
            return;
        }
        super.parseHashMapEntry(jsonBody, entry);
    }
}
