package com.box.androidsdk.content;

import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsBookmark;

/**
 * Represents the API of the bookmark endpoint on Box. This class can be used to generate request objects
 * for each of the APIs exposed endpoints
 */
public class BoxApiBookmark extends BoxApi {

    /**
     * Constructs a BoxApiBookmark with the provided BoxSession
     *
     * @param session authenticated session to use with the BoxApiBookmark
     */
    public BoxApiBookmark(BoxSession session) {
        super(session);
    }

    /**
     * Gets the URL for bookmarks
     *
     * @return the bookmark URL
     */
    protected String getBookmarksUrl() { return String.format("%s/web_links", getBaseUri()); }

    /**
     * Gets the URL for bookmark information
     *
     * @param id    id of the bookmark
     * @return the bookmark information URL
     */
    protected String getBookmarkInfoUrl(String id) { return String.format("%s/%s", getBookmarksUrl(), id); }

    /**
     * Gets the URL for copying a bookmark
     *
     * @param id    id of the bookmark
     * @return the copy bookmark URL
     */
    protected String getBookmarkCopyUrl(String id) { return String.format(getBookmarkInfoUrl(id) + "/copy"); }

    /**
     * Gets the URL for a trashed bookmark
     *
     * @param id    id of the bookmark
     * @return the trashed bookmark URL
     */
    protected String getTrashedBookmarkUrl(String id) { return getBookmarkInfoUrl(id) + "/trash"; }

    /**
     * Gets the URL for comments on a bookmark
     *
     * @param id    id of the bookmark
     * @return  the bookmark comments URL
     */
    protected String getBookmarkCommentsUrl(String id) { return getBookmarkInfoUrl(id) + "/comments"; }

    /**
     * Gets the URL for posting a comment on a bookmark
     *
     * @return  the comments URL
     */
    protected String getCommentUrl() { return getBaseUri() + BoxApiComment.COMMENTS_ENDPOINT; }

    /**
     * Gets a request that retrieves information on a bookmark
     *
     * @param id    id of bookmark to retrieve info on
     * @return      request to get a bookmarks information
     */
    public BoxRequestsBookmark.GetBookmarkInfo getInfoRequest(final String id) {
        BoxRequestsBookmark.GetBookmarkInfo request = new BoxRequestsBookmark.GetBookmarkInfo(id, getBookmarkInfoUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that creates a bookmark in a parent bookmark
     *
     * @param parentId  id of the parent bookmark to create the bookmark in
     * @param url      URL of the new bookmark
     * @return      request to create a bookmark
     */
    public BoxRequestsBookmark.CreateBookmark getCreateRequest(String parentId, String url) {
        BoxRequestsBookmark.CreateBookmark request = new BoxRequestsBookmark.CreateBookmark(parentId, url, getBookmarksUrl(), mSession);
        return request;
    }

    /**
     * Gets a request that updates a bookmark's information
     *
     * @param id    id of bookmark to update information on
     * @return      request to update a bookmark's information
     */
    public BoxRequestsBookmark.UpdateBookmark getUpdateRequest(String id) {
        BoxRequestsBookmark.UpdateBookmark request = new BoxRequestsBookmark.UpdateBookmark(id, getBookmarkInfoUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that copies a bookmark
     *
     * @param id    id of the bookmark to copy
     * @param parentId  id of the parent folder to copy the bookmark into
     * @return  request to copy a bookmark
     */
    public BoxRequestsBookmark.CopyBookmark getCopyRequest(String id, String parentId) {
        BoxRequestsBookmark.CopyBookmark request = new BoxRequestsBookmark.CopyBookmark(id, parentId, getBookmarkCopyUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that renames a bookmark
     *
     * @param id        id of bookmark to rename
     * @param newName   id of bookmark to retrieve info on
     * @return      request to rename a bookmark
     */
    public BoxRequestsBookmark.UpdateBookmark getRenameRequest(String id, String newName) {
        BoxRequestsBookmark.UpdateBookmark request = new BoxRequestsBookmark.UpdateBookmark(id, getBookmarkInfoUrl(id), mSession);
        request.setName(newName);
        return request;
    }

    /**
     * Gets a request that moves a bookmark to another folder
     *
     * @param id        id of bookmark to move
     * @param parentId  id of parent folder to move bookmark into
     * @return      request to move a bookmark
     */
    public BoxRequestsBookmark.UpdateBookmark getMoveRequest(String id, String parentId) {
        BoxRequestsBookmark.UpdateBookmark request = new BoxRequestsBookmark.UpdateBookmark(id, getBookmarkInfoUrl(id), mSession);
        request.setParentId(parentId);
        return request;
    }

    /**
     * Gets a request that deletes a bookmark
     *
     * @param id        id of bookmark to delete
     * @return      request to delete a bookmark
     */
    public BoxRequestsBookmark.DeleteBookmark getDeleteRequest(String id) {
        BoxRequestsBookmark.DeleteBookmark request = new BoxRequestsBookmark.DeleteBookmark(id, getBookmarkInfoUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that creates a shared link for a bookmark
     *
     * @param id        id of bookmark to create shared link for
     * @return      request to create a shared link for a bookmark
     */
    public BoxRequestsBookmark.UpdateSharedBookmark getCreateSharedLinkRequest(String id) {
        BoxRequestsBookmark.UpdateSharedBookmark request = new BoxRequestsBookmark.UpdateSharedBookmark(id, getBookmarkInfoUrl(id), mSession)
                .setAccess(null);
        return request;
    }

    /**
     * Gets a request that disables a shared link for a bookmark
     *
     * @param id        id of bookmark to disable a shared link for
     * @return      request to create a shared link for a bookmark
     */
    public BoxRequestsBookmark.UpdateBookmark getDisableSharedLinkRequest(String id) {
        BoxRequestsBookmark.UpdateBookmark request = new BoxRequestsBookmark.UpdateBookmark(id, getBookmarkInfoUrl(id), mSession)
                .setSharedLink(null);
        return request;
    }

    /**
     * Gets a request that adds a comment to a bookmark
     *
     * @param bookmarkId    id of the bookmark to add the comment to
     * @param message   message for the comment that will be added
     * @return  request to add a comment to a bookmark
     */
    public BoxRequestsBookmark.AddCommentToBookmark getAddCommentRequest(String bookmarkId, String message) {
        BoxRequestsBookmark.AddCommentToBookmark request = new BoxRequestsBookmark.AddCommentToBookmark(bookmarkId, message, getCommentUrl(), mSession);
        return request;
    }

    /**
     * Gets a request that returns a bookmark in the trash
     *
     * @param id        id of bookmark to get in the trash
     * @return      request to get a bookmark from the trash
     */
    public BoxRequestsBookmark.GetTrashedBookmark getTrashedBookmarkRequest(String id) {
        BoxRequestsBookmark.GetTrashedBookmark request = new BoxRequestsBookmark.GetTrashedBookmark(id, getTrashedBookmarkUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that permanently deletes a bookmark from the trash
     *
     * @param id        id of bookmark to delete from the trash
     * @return      request to permanently delete a bookmark from the trash
     */
    public BoxRequestsBookmark.DeleteTrashedBookmark getDeleteTrashedBookmarkRequest(String id) {
        BoxRequestsBookmark.DeleteTrashedBookmark request = new BoxRequestsBookmark.DeleteTrashedBookmark(id, getTrashedBookmarkUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that restores a trashed bookmark
     *
     * @param id        id of bookmark to restore
     * @return      request to restore a bookmark from the trash
     */
    public BoxRequestsBookmark.RestoreTrashedBookmark getRestoreTrashedBookmarkRequest(String id) {
        BoxRequestsBookmark.RestoreTrashedBookmark request = new BoxRequestsBookmark.RestoreTrashedBookmark(id, getBookmarkInfoUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that retrieves the comments on a bookmark
     *
     * @param id    id of the bookmark to retrieve comments for
     * @return  request to retrieve comments on a bookmark
     */
    public BoxRequestsBookmark.GetBookmarkComments getCommentsRequest(String id) {
        BoxRequestsBookmark.GetBookmarkComments request = new BoxRequestsBookmark.GetBookmarkComments(id, getBookmarkCommentsUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that adds a bookmark to a collection
     *
     * @param bookmarkId        id of bookmark to add to collection
     * @param collectionId    id of collection to add the bookmark to
     * @return      request to add a bookmark to a collection
     */
    public BoxRequestsBookmark.AddBookmarkToCollection getAddToCollectionRequest(String bookmarkId, String collectionId) {
        BoxRequestsBookmark.AddBookmarkToCollection request = new BoxRequestsBookmark.AddBookmarkToCollection(bookmarkId, collectionId, getBookmarkInfoUrl(bookmarkId), mSession);
        return request;
    }

    /**
     * Gets a request that removes a bookmark from a collection
     *
     * @param id        id of bookmark to delete from the collection
     * @return request to delete a bookmark from a collection
     */
    public BoxRequestsBookmark.DeleteBookmarkFromCollection getDeleteFromCollectionRequest(String id) {
        BoxRequestsBookmark.DeleteBookmarkFromCollection request = new BoxRequestsBookmark.DeleteBookmarkFromCollection(id, getBookmarkInfoUrl(id), mSession);
        return request;
    }
}