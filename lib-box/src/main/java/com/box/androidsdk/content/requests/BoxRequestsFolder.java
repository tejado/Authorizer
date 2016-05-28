package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxListCollaborations;
import com.box.androidsdk.content.models.BoxListItems;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxUploadEmail;
import com.box.androidsdk.content.models.BoxUser;
import com.eclipsesource.json.JsonObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class BoxRequestsFolder {

    /**
     * Request for retrieving information on a folder
     */
    public static class GetFolderInfo extends BoxRequestItem<BoxFolder, GetFolderInfo> {

        /**
         * Creates a folder information request with the default parameters
         *
         * @param id         id of the folder to get information on
         * @param requestUrl URL of the folder information endpoint
         * @param session    the authenticated session that will be used to make the request with
         */
        public GetFolderInfo(String id, String requestUrl, BoxSession session) {
            super(BoxFolder.class, id, requestUrl, session);
            mRequestMethod = Methods.GET;
        }

        /**
         * Sets the if-none-match header for the request.
         * The folder will only be retrieved if the etag does not match the most current etag for the folder.
         *
         * @param etag etag to set in the if-none-match header.
         * @return request with the updated if-none-match header.
         */
        @Override
        public GetFolderInfo setIfNoneMatchEtag(String etag) {
            return super.setIfNoneMatchEtag(etag);
        }

        /**
         * Returns the etag currently set in the if-none-match header.
         *
         * @return etag set in the if-none-match header, or null if none set.
         */
        @Override
        public String getIfNoneMatchEtag() {
            return super.getIfNoneMatchEtag();
        }

        /**
         * Sets the limit of items that should be returned
         *
         * @param limit limit of items to return
         * @return the get folder info request
         */
        public GetFolderInfo setLimit(int limit) {
            mQueryMap.put(GetFolderItems.LIMIT, String.valueOf(limit));
            return this;
        }

        /**
         * Sets the offset of the items that should be returned
         *
         * @param offset offset of items to return
         * @return the offset of the items to return
         */
        public GetFolderInfo setOffset(int offset) {
            mQueryMap.put(GetFolderItems.OFFSET, String.valueOf(offset));
            return this;
        }
    }

    /**
     * Request for getting the collaborations of a folder
     */
    public static class GetCollaborations extends BoxRequestItem<BoxListCollaborations, GetCollaborations> {

        /**
         * Creates a request that gets the collaborations of a folder with the default parameters
         *
         * @param id         id of the folder to get collaborations on
         * @param requestUrl URL of the folder collaborations endpoint
         * @param session    the authenticated session that will be used to make the request with
         */
        public GetCollaborations(String id, String requestUrl, BoxSession session) {
            super(BoxListCollaborations.class, id, requestUrl, session);
            mRequestMethod = Methods.GET;
        }
    }

    /**
     * Request for updating information of a folder
     */
    public static class UpdateFolder extends BoxRequestItemUpdate<BoxFolder, UpdateFolder> {

        /**
         * Creates an update folder request with the default parameters
         *
         * @param id         id of the folder to update information on
         * @param requestUrl URL of the update folder endpoint
         * @param session    the authenticated session that will be used to make the request with
         */
        public UpdateFolder(String id, String requestUrl, BoxSession session) {
            super(BoxFolder.class, id, requestUrl, session);
        }

        @Override
        public UpdateSharedFolder updateSharedLink() {
            return new UpdateSharedFolder(this);
        }

        /**
         * Gets the sync state that the folder will be updated with
         *
         * @return updated sync state value
         */
        public BoxFolder.SyncState getSyncState() {
            return mBodyMap.containsKey(BoxFolder.FIELD_SYNC_STATE) ? (BoxFolder.SyncState) mBodyMap.get(BoxFolder.FIELD_SYNC_STATE) : null;
        }

        /**
         * Sets the sync state that the folder will be updated to
         *
         * @param syncState sync state that the folder will be set to
         * @return the update folder request
         */
        public UpdateFolder setSyncState(BoxFolder.SyncState syncState) {
            mBodyMap.put(BoxFolder.FIELD_SYNC_STATE, syncState);
            return this;
        }

        /**
         * Gets the upload email address that the folder will be updated with
         *
         * @return updated upload email address value
         */
        public BoxUploadEmail.Access getUploadEmailAccess() {
            return mBodyMap.containsKey(BoxFolder.FIELD_FOLDER_UPLOAD_EMAIL) ?
                    ((BoxUploadEmail) mBodyMap.get(BoxFolder.FIELD_FOLDER_UPLOAD_EMAIL)).getAccess() :
                    null;
        }

        /**
         * Sets the upload email address that the folder will be updated to
         *
         * @param access folder upload email access that will be set to
         * @return the update request
         */
        public UpdateFolder setFolderUploadEmailAccess(BoxUploadEmail.Access access) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
            map.put(BoxUploadEmail.FIELD_ACCESS, access == null ? "null" : access.toString());
            BoxUploadEmail uploadEmail = new BoxUploadEmail(map);
            mBodyMap.put(BoxFolder.FIELD_FOLDER_UPLOAD_EMAIL, uploadEmail);
            return this;
        }

        /**
         * Gets the id of the user that will own the folder after update
         *
         * @return id of user to change owners to
         */
        public String getOwnedById() {
            return mBodyMap.containsKey(BoxItem.FIELD_OWNED_BY) ?
                    ((BoxUser) mBodyMap.get(BoxItem.FIELD_OWNED_BY)).getId() :
                    null;
        }

        /**
         * Sets the id of the user that will own the folder after update
         *
         * @param userId id of the user that will own the
         * @return the update request
         */
        public UpdateFolder setOwnedById(String userId) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
            map.put(BoxUser.FIELD_ID, userId);
            BoxUser user = new BoxUser(map);
            mBodyMap.put(BoxItem.FIELD_OWNED_BY, user);
            return this;
        }

        @Override
        protected void parseHashMapEntry(JsonObject jsonBody, Map.Entry<String, Object> entry) {
            if (entry.getKey().equals(BoxFolder.FIELD_FOLDER_UPLOAD_EMAIL)) {
                jsonBody.add(entry.getKey(), parseJsonObject(entry.getValue()));
                return;
            } else if (entry.getKey().equals(BoxItem.FIELD_OWNED_BY)) {
                jsonBody.add(entry.getKey(), parseJsonObject(entry.getValue()));
                return;
            } else if (entry.getKey().equals(BoxFolder.FIELD_SYNC_STATE)) {
                BoxFolder.SyncState syncState = (BoxFolder.SyncState) entry.getValue();
                jsonBody.add(entry.getKey(), syncState.toString());
                return;
            }

            super.parseHashMapEntry(jsonBody, entry);
        }
    }


    /**
     * Request for updating information of a shared folder
     */
    public static class UpdateSharedFolder extends BoxRequestUpdateSharedItem<BoxFolder, UpdateSharedFolder> {


        /**
         * Creates an update shared folder request with the default parameters
         *
         * @param id         id of the folder to update information on
         * @param requestUrl URL of the update folder endpoint
         * @param session    the authenticated session that will be used to make the request with
         */
        public UpdateSharedFolder(String id, String requestUrl, BoxSession session) {
            super(BoxFolder.class, id, requestUrl, session);
        }

        protected UpdateSharedFolder(UpdateFolder r) {
            super(r);
        }

        /**
         * Sets whether or not the folder can be downloaded from the shared link.
         *
         * @param canDownload boolean representing whether or not the folder can be downloaded from the shared link.
         * @return request with the updated shared link.
         */
        @Override
        public UpdateSharedFolder setCanDownload(boolean canDownload) {
            return super.setCanDownload(canDownload);
        }

        /**
         * Returns whether or not the folder can be downloaded from the shared link.
         *
         * @return Boolean representing whether or not the folder can be downloaded from the shared link, or null if not set.
         */
        @Override
        public Boolean getCanDownload() {
            return super.getCanDownload();
        }
    }

    /**
     * Request for copying a folder
     */
    public static class CopyFolder extends BoxRequestItemCopy<BoxFolder, CopyFolder> {

        /**
         * Creates a copy folder request with the default parameters
         *
         * @param id         id of the folder to copy
         * @param parentId   id of the new parent folder
         * @param requestUrl URL of the copy folder endpoint
         * @param session    the authenticated session that will be used to make the request with
         */
        public CopyFolder(String id, String parentId, String requestUrl, BoxSession session) {
            super(BoxFolder.class, id, parentId, requestUrl, session);
        }
    }

    /**
     * Request for creating a folder
     */
    public static class CreateFolder extends BoxRequestItem<BoxFolder, CreateFolder> {

        /**
         * Creates a create folder request with the default parameters
         *
         * @param requestUrl URL of the create folder endpoint
         * @param session    the authenticated session that will be used to make the request with
         */
        public CreateFolder(String parentId, String name, String requestUrl, BoxSession session) {
            super(BoxFolder.class, null, requestUrl, session);
            mRequestMethod = Methods.POST;
            setParentId(parentId);
            setName(name);
        }

        /**
         * Gets the parent folder that the folder will be created in
         *
         * @return id of the parent folder
         */
        public String getParentId() {
            return mBodyMap.containsKey(BoxFolder.FIELD_PARENT) ? (String) mBodyMap.get(BoxFolder.FIELD_ID) : null;
        }

        /**
         * Sets the id of the parent folder the folder will be created in
         *
         * @param id id of the parent folder
         * @return the create folder request
         */
        public CreateFolder setParentId(String id) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put(BoxFolder.FIELD_ID, id);
            BoxFolder parentFolder = new BoxFolder(map);
            mBodyMap.put(BoxFolder.FIELD_PARENT, parentFolder);
            return this;
        }

        /**
         * Gets the name that the folder will be created with
         *
         * @return name of the folder to create
         */
        public String getName() {
            return (String) mBodyMap.get(BoxFolder.FIELD_NAME);
        }

        /**
         * Sets the name that the folder will be created with
         *
         * @param name name of the folder to create
         * @return the create folder request
         */
        public CreateFolder setName(String name) {
            mBodyMap.put(BoxFolder.FIELD_NAME, name);
            return this;
        }
    }

    /**
     * Request for deleting a folder
     */
    public static class DeleteFolder extends BoxRequestItemDelete<DeleteFolder> {
        private static final String FIELD_RECURSIVE = "recursive";
        private static final String TRUE = "true";
        private static final String FALSE = "false";


        /**
         * Creates a delete folder request with the default parameters
         *
         * @param id         id of the folder to delete
         * @param requestUrl URL of the delete folder endpoint
         * @param session    the authenticated session that will be used to make the request with
         */
        public DeleteFolder(String id, String requestUrl, BoxSession session) {
            super(id, requestUrl, session);
            setRecursive(true);
        }

        /**
         * Sets whether the folder delete should be recursive and delete all child folders
         *
         * @param recursive whether the delete should be recursive
         * @return the delete folder request
         */
        public DeleteFolder setRecursive(boolean recursive) {
            mQueryMap.put(FIELD_RECURSIVE, recursive ? TRUE : FALSE);
            return this;
        }

        /**
         * Gets whether the folder delete should be recursive and delete all child folders
         *
         * @return whether the delete should be recursive
         */
        public Boolean getRecursive() {
            return TRUE.equals(mQueryMap.get(FIELD_RECURSIVE));
        }
    }

    /**
     * Request for getting a trashed folder
     */
    public static class GetTrashedFolder extends BoxRequestItem<BoxFolder, GetTrashedFolder> {

        /**
         * Creates a request to get a trashed folder with the default parameters
         *
         * @param id         id of the folder in the trash
         * @param requestUrl URL of the trashed folder endpoint
         * @param session    the authenticated session that will be used to make the request with
         */
        public GetTrashedFolder(String id, String requestUrl, BoxSession session) {
            super(BoxFolder.class, id, requestUrl, session);
            mRequestMethod = Methods.GET;
        }

        /**
         * Sets the if-none-match header for the request.
         * The trashed folder will only be retrieved if the etag does not match the most current etag for the folder.
         *
         * @param etag etag to set in the if-none-match header.
         * @return request with the updated if-none-match header.
         */
        @Override
        public GetTrashedFolder setIfNoneMatchEtag(String etag) {
            return super.setIfNoneMatchEtag(etag);
        }

        /**
         * Returns the etag currently set in the if-none-match header.
         *
         * @return etag set in the if-none-match header, or null if none set.
         */
        @Override
        public String getIfNoneMatchEtag() {
            return super.getIfNoneMatchEtag();
        }
    }

    /**
     * Request for permanently deleting a trashed folder
     */
    public static class DeleteTrashedFolder extends BoxRequestItemDelete<DeleteTrashedFolder> {

        /**
         * Creates a delete trashed folder request with the default parameters
         *
         * @param id         id of the trashed folder to permanently delete
         * @param requestUrl URL of the delete trashed folder endpoint
         * @param session    the authenticated session that will be used to make the request with
         */
        public DeleteTrashedFolder(String id, String requestUrl, BoxSession session) {
            super(id, requestUrl, session);
        }
    }

    /**
     * Request to restore a trashed folder
     */
    public static class RestoreTrashedFolder extends BoxRequestItemRestoreTrashed<BoxFolder, RestoreTrashedFolder> {

        /**
         * Creates a restore trashed folder request with the default parameters
         *
         * @param id         id of the trashed folder to restore
         * @param requestUrl URL of the restore trashed folder endpoint
         * @param session    the authenticated session that will be used to make the request with
         */
        public RestoreTrashedFolder(String id, String requestUrl, BoxSession session) {
            super(BoxFolder.class, id, requestUrl, session);
        }
    }

    /**
     * Request for getting a folders items
     */
    public static class GetFolderItems extends BoxRequestItem<BoxListItems, GetFolderItems> {
        private static final String LIMIT = "limit";
        private static final String OFFSET = "offset";

        // 1000 is the current max that the API supports
        private static final String DEFAULT_LIMIT = "1000";
        private static final String DEFAULT_OFFSET = "0";


        /**
         * Creates a folder information request with the default parameters
         *
         * @param id         id of the folder to get the items of
         * @param requestUrl URL of the folder items endpoint
         * @param session    the authenticated session that will be used to make the request with
         */
        public GetFolderItems(String id, String requestUrl, BoxSession session) {
            super(BoxListItems.class, id, requestUrl, session);
            mRequestMethod = Methods.GET;
            mQueryMap.put(LIMIT, DEFAULT_LIMIT);
            mQueryMap.put(OFFSET, DEFAULT_OFFSET);
        }

        /**
         * Sets the limit of items that should be returned
         *
         * @param limit limit of items to return
         * @return the get folder items request
         */
        public GetFolderItems setLimit(int limit) {
            mQueryMap.put(LIMIT, String.valueOf(limit));
            return this;
        }

        /**
         * Sets the offset of the items that should be returned
         *
         * @param offset offset of items to return
         * @return the offset of the items to return
         */
        public GetFolderItems setOffset(int offset) {
            mQueryMap.put(OFFSET, String.valueOf(offset));
            return this;
        }
    }

    /**
     * Request for adding a folder to a collection
     */
    public static class AddFolderToCollection extends BoxRequestCollectionUpdate<BoxFolder, AddFolderToCollection> {

        /**
         * Creates an add folder to collection request with the default parameters
         *
         * @param id           id of the folder to add to the collection
         * @param collectionId id of the collection to add the folder to
         * @param requestUrl   URL of the add to collection endpoint
         * @param session      the authenticated session that will be used to make the request with
         */
        public AddFolderToCollection(String id, String collectionId, String requestUrl, BoxSession session) {
            super(BoxFolder.class, id, requestUrl, session);
            setCollectionId(collectionId);
            mRequestMethod = Methods.PUT;
        }

        /**
         * Sets the collection id in the request to add the folder to.
         *
         * @param id id of the collection to add the folder to.
         * @return request with the updated collection id.
         */
        public AddFolderToCollection setCollectionId(String id) {
            return super.setCollectionId(id);
        }
    }

    /**
     * Request for removing a folder from a collection
     */
    public static class DeleteFolderFromCollection extends BoxRequestCollectionUpdate<BoxFolder, AddFolderToCollection> {

        /**
         * Creates a delete folder from collection request with the default parameters
         *
         * @param id         id of the folder to delete from collection
         * @param requestUrl URL of the delete from collection endpoint
         * @param session    the authenticated session that will be used to make the request with
         */
        public DeleteFolderFromCollection(String id, String requestUrl, BoxSession session) {
            super(BoxFolder.class, id, requestUrl, session);
            setCollectionId(null);
        }
    }


    /**
     * Request for getting trashed items.
     */
    public static class GetTrashedItems extends BoxRequest<BoxListItems, GetTrashedItems> {

        /**
         * Creates a request to get trashed items with the default parameters.
         *
         * @param requestUrl URL of the trashed items endpoint.
         * @param session    the authenticated session that will be used to make the request with.
         */
        public GetTrashedItems(String requestUrl, BoxSession session) {
            super(BoxListItems.class, requestUrl, session);
            mRequestMethod = Methods.GET;
        }
    }
}
