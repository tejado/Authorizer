package com.box.androidsdk.content;

import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFolder;

/**
 * Represents the API of the folder endpoint on Box. This class can be used to generate request objects
 * for each of the APIs exposed endpoints
 */
public class BoxApiFolder extends BoxApi {

    /**
     * Constructs a BoxApiFolder with the provided BoxSession
     *
     * @param session authenticated session to use with the BoxApiFolder
     */
    public BoxApiFolder(BoxSession session) {
        super(session);
    }

    /**
     * Gets the URL for folders
     *
     * @return the folder URL
     */
    protected String getFoldersUrl() { return String.format("%s/folders", getBaseUri()); }

    /**
     * Gets the URL for folder information
     *
     * @param id    id of the folder
     * @return the folder information URL
     */
    protected String getFolderInfoUrl(String id) { return String.format("%s/%s", getFoldersUrl(), id); }

    /**
     * Gets the URL for items
     *
     * @param id    id of the folder
     * @return the folder items URL
     */
    protected String getFolderItemsUrl(String id) { return getFolderInfoUrl(id) + "/items"; }

    /**
     * Gets the URL for the folders collaborations
     *
     * @param id    id of the folder
     * @return the folder collaborations URL
     */
    protected String getFolderCollaborationsUrl(String id) { return getFolderInfoUrl(id) + "/collaborations"; }

    /**
     * Gets the URL for copying a folder
     *
     * @param id    id of the folder
     * @return the copy folder URL
     */
    protected String getFolderCopyUrl(String id) { return getFolderInfoUrl(id) + "/copy"; }

    /**
     * Gets the URL for a trashed folder
     *
     * @param id    id of the folder
     * @return the trashed folder URL
     */
    protected String getTrashedFolderUrl(String id) { return getFolderInfoUrl(id) + "/trash"; }

    /**
     * Gets the URL for items in the trash
     *
     * @return the trashed items URL
     */
    protected String getTrashedItemsUrl() { return getFoldersUrl() + "/trash/items"; }


    /**
     * Gets a request that retrieves information on a folder
     *
     * @param id    id of folder to retrieve info on
     * @return      request to get a folders information
     */
    public BoxRequestsFolder.GetFolderInfo getInfoRequest(String id) {
        BoxRequestsFolder.GetFolderInfo request = new BoxRequestsFolder.GetFolderInfo(id, getFolderInfoUrl(id), mSession);
        return request;
    }


    /**
     * Gets a request that retrieves the items of a folder
     *
     * @param id    id of folder to get children on
     * @return      request to get a folders children
     */
    public BoxRequestsFolder.GetFolderItems getItemsRequest(String id) {
        BoxRequestsFolder.GetFolderItems request = new BoxRequestsFolder.GetFolderItems(id, getFolderItemsUrl(id), mSession);
        return request;
    }


    /**
     * Gets a request that creates a folder in a parent folder
     *
     * @param parentId  id of the parent folder to create the folder in
     * @param name      name of the new folder
     * @return      request to create a folder
     */
    public BoxRequestsFolder.CreateFolder getCreateRequest(String parentId, String name) {
        BoxRequestsFolder.CreateFolder request = new BoxRequestsFolder.CreateFolder(parentId, name, getFoldersUrl(), mSession);
        return request;
    }


    /**
     * Gets a request that updates a folders information
     *
     * @param id    id of folder to update information on
     * @return      request to update a folders information
     */
    public BoxRequestsFolder.UpdateFolder getUpdateRequest(String id) {
        BoxRequestsFolder.UpdateFolder request = new BoxRequestsFolder.UpdateFolder(id, getFolderInfoUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that renames a folder
     *
     * @param id        id of folder to rename
     * @param newName   id of folder to retrieve info on
     * @return      request to rename a folder
     */
    public BoxRequestsFolder.UpdateFolder getRenameRequest(String id, String newName) {
        BoxRequestsFolder.UpdateFolder request = new BoxRequestsFolder.UpdateFolder(id, getFolderInfoUrl(id), mSession)
                .setName(newName);
        return request;
    }

    /**
     * Gets a request that moves a folder to another folder
     *
     * @param id        id of folder to move
     * @param parentId  id of parent folder to move folder into
     * @return      request to move a folder
     */
    public BoxRequestsFolder.UpdateFolder getMoveRequest(String id, String parentId) {
        BoxRequestsFolder.UpdateFolder request = new BoxRequestsFolder.UpdateFolder(id, getFolderInfoUrl(id), mSession)
                .setParentId(parentId);
        return request;
    }

    /**
     * Gets a request that copies a folder
     *
     * @param id        id of folder to copy
     * @param parentId  id of parent folder to copy folder into
     * @return      request to copy a folder
     */
    public BoxRequestsFolder.CopyFolder getCopyRequest(String id, String parentId) {
        BoxRequestsFolder.CopyFolder request = new BoxRequestsFolder.CopyFolder(id, parentId, getFolderCopyUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that deletes a folder
     *
     * @param id        id of folder to delete
     * @return      request to delete a folder
     */
    public BoxRequestsFolder.DeleteFolder getDeleteRequest(String id) {
        BoxRequestsFolder.DeleteFolder request = new BoxRequestsFolder.DeleteFolder(id, getFolderInfoUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that gets the collaborations of a folder
     *
     * @param id        id of folder to move
     * @return      request to move a folder
     */
    public BoxRequestsFolder.GetCollaborations getCollaborationsRequest(String id) {
        BoxRequestsFolder.GetCollaborations request = new BoxRequestsFolder.GetCollaborations(id, getFolderCollaborationsUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that creates a shared link for a folder
     *
     * @param id        id of folder to create shared link for
     * @return      request to create a shared link for a folder
     */
    public BoxRequestsFolder.UpdateSharedFolder getCreateSharedLinkRequest(String id) {
        BoxRequestsFolder.UpdateSharedFolder request = new BoxRequestsFolder.UpdateSharedFolder(id, getFolderInfoUrl(id), mSession)
                .setAccess(null);
        return request;
    }

    /**
     * Gets a request that disables a shared link for a folder
     *
     * @param id        id of folder to disable a shared link for
     * @return      request to create a shared link for a folder
     */
    public BoxRequestsFolder.UpdateFolder getDisableSharedLinkRequest(String id) {
        BoxRequestsFolder.UpdateFolder request = new BoxRequestsFolder.UpdateFolder(id, getFolderInfoUrl(id), mSession)
                .setSharedLink(null);
        return request;
    }

    /**
     * Gets a request that adds a folder to a collection
     *
     * @param folderId        id of folder to add to collection
     * @param collectionId    id of collection to add the folder to
     * @return      request to add a folder to a collection
     */
    public BoxRequestsFolder.AddFolderToCollection getAddToCollectionRequest(String folderId, String collectionId) {
        BoxRequestsFolder.AddFolderToCollection request = new BoxRequestsFolder.AddFolderToCollection(folderId, collectionId, getFolderInfoUrl(folderId), mSession);
        return request;
    }

    /**
     * Gets a request that removes a folder from a collection
     *
     * @param id        id of folder to delete from the collection
     * @return request to delete a folder from a collection
     */
    public BoxRequestsFolder.DeleteFolderFromCollection getDeleteFromCollectionRequest(String id) {
        BoxRequestsFolder.DeleteFolderFromCollection request = new BoxRequestsFolder.DeleteFolderFromCollection(id, getFolderInfoUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that returns the items in the trash
     *
     * @return      request to get a folder from the trash
     */
    public BoxRequestsFolder.GetTrashedItems getTrashedItemsRequest() {
        BoxRequestsFolder.GetTrashedItems request = new BoxRequestsFolder.GetTrashedItems(getTrashedItemsUrl(), mSession);
        return request;
    }

    /**
     * Gets a request that returns a folder in the trash
     *
     * @param id        id of folder to get in the trash
     * @return      request to get a folder from the trash
     */
    public BoxRequestsFolder.GetTrashedFolder getTrashedFolderRequest(String id) {
        BoxRequestsFolder.GetTrashedFolder request = new BoxRequestsFolder.GetTrashedFolder(id, getTrashedFolderUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that permanently deletes a folder from the trash
     *
     * @param id        id of folder to delete from the trash
     * @return      request to permanently delete a folder from the trash
     */
    public BoxRequestsFolder.DeleteTrashedFolder getDeleteTrashedFolderRequest(String id) {
        BoxRequestsFolder.DeleteTrashedFolder request = new BoxRequestsFolder.DeleteTrashedFolder(id, getTrashedFolderUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that restores a trashed folder
     *
     * @param id        id of folder to restore
     * @return      request to restore a folder from the trash
     */
    public BoxRequestsFolder.RestoreTrashedFolder getRestoreTrashedFolderRequest(String id) {
        BoxRequestsFolder.RestoreTrashedFolder request = new BoxRequestsFolder.RestoreTrashedFolder(id, getFolderInfoUrl(id), mSession);
        return request;
    }

}