package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxBookmark;
import com.box.androidsdk.content.models.BoxComment;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxListComments;

import java.util.HashMap;

/**
 * Bookmark requests.
 */
public class BoxRequestsBookmark {

    /**
     * Request for retrieving information on a bookmark
     */
    public static class GetBookmarkInfo extends BoxRequestItem<BoxBookmark, GetBookmarkInfo> {

        /**
         * Creates a bookmark information request with the default parameters
         *
         * @param id            id of the bookmark to get information on
         * @param requestUrl    URL of the bookmark information endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public GetBookmarkInfo(String id, String requestUrl, BoxSession session) {
            super(BoxBookmark.class, id, requestUrl, session);
            mRequestMethod = Methods.GET;
        }

        /**
         * Sets the if-none-match header for the request.
         * The bookmark will only be retrieved if the etag does not match the most current etag for the bookmark.
         *
         * @param etag  etag to set in the if-none-match header.
         * @return request with the updated if-none-match header.
         */
        @Override
        public GetBookmarkInfo setIfNoneMatchEtag(String etag) {
            return super.setIfNoneMatchEtag(etag);
        }

        /**
         * Returns the etag currently set in the if-none-match header.
         *
         * @return  etag set in the if-none-match header, or null if none set.
         */
        @Override
        public String getIfNoneMatchEtag() {
            return super.getIfNoneMatchEtag();
        }
    }

    /**
     * Request for creating a new bookmark
     */
    public static class CreateBookmark extends BoxRequestItem<BoxBookmark, CreateBookmark> {

        /**
         * Creates a create bookmark request with the default parameters
         *
         * @param parentId  id of the parent folder for the new bookmark
         * @param url   URL of the new bookmark
         * @param requestUrl    URL of the create bookmark endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public CreateBookmark(String parentId, String url, String requestUrl, BoxSession session) {
            super(BoxBookmark.class, null, requestUrl, session);
            mRequestMethod = Methods.POST;
            setParentId(parentId);
            setUrl(url);
        }

        /**
         * Returns the parent id for the new bookmark currently set in the request.
         *
         * @return  id of the parent for the new bookmark
         */
        public String getParentId() {
            return mBodyMap.containsKey(BoxFolder.FIELD_PARENT) ? (String) mBodyMap.get(BoxFolder.FIELD_ID) : null;
        }

        /**
         * Sets the parent id to use in the create bookmark request.
         *
         * @param id    id of the parent folder for the new bookmark
         * @return  the request with the updated parent id
         */
        public CreateBookmark setParentId(String id) {
            HashMap<String, Object> map = new HashMap<String,Object>();
            map.put(BoxFolder.FIELD_ID, id);
            BoxFolder parentFolder = new BoxFolder(map);
            mBodyMap.put(BoxFolder.FIELD_PARENT, parentFolder);
            return this;
        }

        /**
         * Returns the URL currently set for the create bookmark request.
         *
         * @return  the URL for the new bookmark
         */
        public String getUrl() {
            return (String) mBodyMap.get(BoxBookmark.FIELD_URL);
        }

        /**
         * Sets the URL of the bookmark to be created in the request.
         *
         * @param url   URL of the new bookmark
         * @return  the request with the updated URL
         */
        public CreateBookmark setUrl(String url) {
            mBodyMap.put(BoxBookmark.FIELD_URL, url);
            return this;
        }

        /**
         * Returns the name of the new bookmark currently set in the request. Will return null if not set.
         *
         * @return name of the bookmark that will be created.
         */
        public String getName() {
            return (String) mBodyMap.get(BoxBookmark.FIELD_NAME);
        }

        /**
         * Sets the name of the bookmark to be created in the request.
         *
         * @param name  name of the bookmark to be created
         * @return  the request with the updated name
         */
        public CreateBookmark setName(String name) {
            mBodyMap.put(BoxBookmark.FIELD_NAME, name);
            return this;
        }

        /**
         * Returns the description of the new bookmark currently set in the request. Will return null if not set.
         *
         * @return  description of the bookmark that will be created
         */
        public String getDescription() {
            return (String) mBodyMap.get(BoxBookmark.FIELD_DESCRIPTION);
        }

        /**
         * Sets the description of the bookmark to be created in the request.
         *
         * @param description   new description for the bookmark to be created
         * @return  the request with the updated description
         */
        public CreateBookmark setDescription(String description) {
            mBodyMap.put(BoxBookmark.FIELD_DESCRIPTION, description);
            return this;
        }
    }

    /**
     * Request for updating information on a bookmark
     */
    public static class UpdateBookmark extends BoxRequestItemUpdate<BoxBookmark, UpdateBookmark> {

        /**
         * Creates an update file request with the default parameters
         *
         * @param id            id of the file to update information on
         * @param requestUrl    URL of the update file endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public UpdateBookmark(String id, String requestUrl, BoxSession session) {
            super(BoxBookmark.class, id, requestUrl, session);
        }

        /**
         * Returns the URL for the bookmark currently set in the request.
         *
         * @return  URL for the bookmark.
         */
        public String getUrl() {
            return (String) mBodyMap.get(BoxBookmark.FIELD_URL);
        }

        /**
         * Sets the new URL for the bookmark in the request.
         *
         * @param url   new URL for the bookmark.
         * @return  request with the updated bookmark URL.
         */
        public UpdateBookmark setUrl(String url) {
            mBodyMap.put(BoxBookmark.FIELD_URL, url);
            return this;
        }

        @Override
        public UpdateSharedBookmark updateSharedLink() {
            return new UpdateSharedBookmark(this);
        }
    }

    /**
     * Request for updating information on a shared bookmark
     */
    public static class UpdateSharedBookmark extends BoxRequestUpdateSharedItem<BoxBookmark, UpdateSharedBookmark> {

        /**
         * Creates an update shared bookmark request with the default parameters
         *
         * @param id            id of the file to update information on
         * @param requestUrl    URL of the update file endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public UpdateSharedBookmark(String id, String requestUrl, BoxSession session) {
            super(BoxBookmark.class, id, requestUrl, session);
        }


        public UpdateSharedBookmark(UpdateBookmark update) {
            super(update);
        }
    }

    /**
     * Request for copying a bookmark
     */
    public static class CopyBookmark extends BoxRequestItemCopy<BoxBookmark, CopyBookmark> {

        /**
         * Creates a copy bookmark request with the default parameters
         * @param id    id of the bookmark to copy
         * @param parentId  id of the new parent folder
         * @param requestUrl    URL of the copy bookmark endpoint
         * @param session   the authenticated session that will be used to make the request with
         */
        public CopyBookmark(String id, String parentId, String requestUrl, BoxSession session) {
            super(BoxBookmark.class, id, parentId, requestUrl, session);
        }
    }

    /**
     * Request for deleting a bookmark
     */
    public static class DeleteBookmark extends BoxRequestItemDelete<DeleteBookmark> {

        /**
         * Creates a delete bookmark request with the default parameters
         *
         * @param id            id of the bookmark to delete
         * @param requestUrl    URL of the delete bookmark endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public DeleteBookmark(String id, String requestUrl, BoxSession session) {
            super(id, requestUrl, session);
        }
    }

    /**
     * Request for retrieving information on a trashed bookmark
     */
    public static class GetTrashedBookmark extends BoxRequestItem<BoxBookmark, GetTrashedBookmark> {

        /**
         * Creates a request to get a trashed bookmark with the default parameters
         *
         * @param id            id of the bookmark in the trash
         * @param requestUrl    URL of the trashed bookmark endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public GetTrashedBookmark(String id, String requestUrl, BoxSession session) {
            super(BoxBookmark.class, id, requestUrl, session);
            mRequestMethod = Methods.GET;
        }

        /**
         * Sets the if-none-match header for the request.
         * The trashed bookmark will only be retrieved if the etag does not match the most current etag for the bookmark.
         *
         * @param etag  etag to set in the if-none-match header.
         * @return request with the updated if-none-match header.
         */
        @Override
        public GetTrashedBookmark setIfNoneMatchEtag(String etag) {
            return super.setIfNoneMatchEtag(etag);
        }

        /**
         * Returns the etag currently set in the if-none-match header.
         *
         * @return  etag set in the if-none-match header, or null if none set.
         */
        @Override
        public String getIfNoneMatchEtag() {
            return super.getIfNoneMatchEtag();
        }
    }

    /**
     * Request for permanently deleting a trashed bookmark
     */
    public static class DeleteTrashedBookmark extends BoxRequestItemDelete<DeleteTrashedBookmark> {

        /**
         * Creates a delete trashed bookmark request with the default parameters
         *
         * @param id            id of the trashed bookmark to permanently delete
         * @param requestUrl    URL of the delete trashed bookmark endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public DeleteTrashedBookmark(String id, String requestUrl, BoxSession session) {
            super(id, requestUrl, session);
        }
    }

    /**
     * Request for restoring a trashed bookmark
     */
    public static class RestoreTrashedBookmark extends BoxRequestItemRestoreTrashed<BoxBookmark, RestoreTrashedBookmark> {

        /**
         * Creates a restore trashed bookmark request with the default parameters
         *
         * @param id            id of the trashed bookmark to restore
         * @param requestUrl    URL of the restore trashed bookmark endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public RestoreTrashedBookmark(String id, String requestUrl, BoxSession session) {
            super(BoxBookmark.class, id, requestUrl, session);
        }
    }

    /**
     * Request for getting the comments on a bookmark
     */
    public static class GetBookmarkComments extends BoxRequestItem<BoxListComments, GetBookmarkComments> {

        /**
         * Creates a get bookmark comments request with the default parameters
         *
         * @param id    id of the bookmark to get comments of
         * @param requestUrl    URL of the bookmark comments endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public GetBookmarkComments(String id, String requestUrl, BoxSession session) {
            super(BoxListComments.class, id, requestUrl, session);
            mRequestMethod = Methods.GET;
        }
    }

    /**
     * Request for adding a comment to a bookmark
     */
    public static class AddCommentToBookmark extends BoxRequestCommentAdd<BoxComment, AddCommentToBookmark> {

        /**
         * Creates an add comment to bookmark request with the default parameters
         *
         * @param bookmarkId    id of the bookmark to add a comment to
         * @param message   message of the new comment
         * @param requestUrl    URL of the add comment endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public AddCommentToBookmark(String bookmarkId, String message, String requestUrl, BoxSession session) {
            super(BoxComment.class, requestUrl, session);
            setItemId(bookmarkId);
            setItemType(BoxBookmark.TYPE);
            setMessage(message);
        }
    }

    /**
     * Request for adding a bookmark to a collection
     */
    public static class AddBookmarkToCollection extends BoxRequestCollectionUpdate<BoxBookmark, AddBookmarkToCollection> {

        /**
         * Creates an add bookmark to collection request with the default parameters
         *
         * @param id    id of the bookmark to add to the collection
         * @param collectionId  id of the collection to add the bookmark to
         * @param requestUrl    URL of the add to collection endpoint
         * @param session   the authenticated session that will be used to make the request with
         */
        public AddBookmarkToCollection(String id, String collectionId, String requestUrl, BoxSession session) {
            super(BoxBookmark.class, id, requestUrl, session);
            setCollectionId(collectionId);
        }

        /**
         * Sets the collection id to add the bookmark to in the request.
         *
         * @param id    id of the collection to add the bookmark to.
         * @return  request with the updated collection id.
         */
        @Override
        public AddBookmarkToCollection setCollectionId(String id) {
            return super.setCollectionId(id);
        }
    }

    /**
     * Request for removing a bookmark from a collection
     */
    public static class DeleteBookmarkFromCollection extends BoxRequestCollectionUpdate<BoxBookmark, DeleteBookmarkFromCollection> {

        /**
         * Creates a delete bookmark from collection request with the default parameters
         *
         * @param id    id of the bookmark to delete from collection
         * @param requestUrl    URL of the delete from collection endpoint
         * @param session   the authenticated session that will be used to make the request with
         */
        public DeleteBookmarkFromCollection(String id, String requestUrl, BoxSession session) {
            super(BoxBookmark.class, id, requestUrl, session);
            setCollectionId(null);
        }
    }
}
