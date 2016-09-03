package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.models.BoxListCollections;
import com.box.androidsdk.content.models.BoxListItems;
import com.box.androidsdk.content.models.BoxSession;

public class BoxRequestsCollections {

    /**
     * Request to get the available collections.
     */
    public static class GetCollections extends BoxRequestList<BoxListCollections, GetCollections> {

        /**
         * Creates a get collections request with the default parameters.
         *
         * @param collectionsUrl    URL of the collections endpoint.
         * @param session   the authenticated session that will be used to make the request with.
         */
        public GetCollections(String collectionsUrl, BoxSession session) {
            super(BoxListCollections.class, null, collectionsUrl, session);
        }
    }

    /**
     * Request to get a collection's items.
     */
    public static class GetCollectionItems extends BoxRequestList<BoxListItems, GetCollectionItems> {

        /**
         * Creates a get collection items with the default parameters.
         *
         * @param collectionItemsUrl URL of the collection items endpoint.
         * @param session   the authenticated session that will be used to make the request with.
         */
        public GetCollectionItems(String collectionItemsUrl, BoxSession session) {
            super(BoxListItems.class, null, collectionItemsUrl, session);
        }
    }

}
