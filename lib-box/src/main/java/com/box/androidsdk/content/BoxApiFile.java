package com.box.androidsdk.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Locale;

import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFile;

/**
 * Represents the API of the file endpoint on Box. This class can be used to generate request objects
 * for each of the APIs exposed endpoints
 */
public class BoxApiFile extends BoxApi {

    /**
     * Constructs a BoxApiFile with the provided BoxSession
     *
     * @param session authenticated session to use with the BoxApiFile
     */
    public BoxApiFile(BoxSession session) {
        super(session);
    }

    /**
     * Gets the URL for files
     *
     * @return the file URL
     */
    protected String getFilesUrl() { return String.format(Locale.ENGLISH, "%s/files", getBaseUri()); }

    /**
     * Gets the URL for file information
     *
     * @param id    id of the file
     * @return the file information URL
     */
    protected String getFileInfoUrl(String id) { return String.format(Locale.ENGLISH, "%s/%s", getFilesUrl(), id); }

    /**
     * Gets the URL for copying a file
     *
     * @param id    id of the file
     * @return the copy file URL
     */
    protected String getFileCopyUrl(String id) { return String.format(Locale.ENGLISH, getFileInfoUrl(id) + "/copy"); }

    /**
     * Gets the URL for uploading a file
     *
     * @return the file upload URL
     */
    protected String getFileUploadUrl() { return String.format(Locale.ENGLISH, "%s/files/content", getBaseUploadUri() ); }

    /**
     * Gets the URL for uploading a new version of a file
     *
     * @param id    id of the file
     * @return the file version upload URL
     */
    protected String getFileUploadNewVersionUrl(String id) { return String.format(Locale.ENGLISH, "%s/files/%s/content", getBaseUploadUri(), id); }

    /**
     * Gets the URL for comments on a file
     * @param id    id of the file
     * @return  the file comments URL
     */
    protected String getTrashedFileUrl(String id) { return getFileInfoUrl(id) + "/trash"; }

    /**
     * Gets the URL for comments on a file
     * @param id    id of the file
     * @return  the file comments URL
     */
    protected String getFileCommentsUrl(String id) { return getFileInfoUrl(id) + "/comments"; }

    /**
     * Gets the URL for versions of a file
     * @param id    id of the file
     * @return  the file versions URL
     */
    protected String getFileVersionsUrl(String id) { return getFileInfoUrl(id) + "/versions"; }

    /**
     * Gets the URL for promoting the version of a file
     * @param id    id of the file
     * @return  the file version promotion URL
     */
    protected String getPromoteFileVersionUrl(String id) { return getFileVersionsUrl(id) + "/current"; }

    /**
     * Gets the URL for deleting the version of a file
     *
     * @param id    id of the file
     * @return  the file version deletion URL
     */
    protected String getDeleteFileVersionUrl(String id, String versionId) { return String.format(Locale.ENGLISH, "%s/%s", getFileVersionsUrl(id), versionId); }

    /**
     * Gets the URL for downloading a file
     *
     * @param id    id of the file
     * @return  the file download URL
     */
    protected String getFileDownloadUrl(String id) { return getFileInfoUrl(id) + "/content"; }

    /**
     * Gets the URL for downloading the thumbnail of a file
     *
     * @param id    id of the file
     * @return  the thumbnail file download URL
     */
    protected String getThumbnailFileDownloadUrl(String id) { return getFileInfoUrl(id) + "/thumbnail.png"; }

    /**
     * Gets the URL for posting a comment on a file
     *
     * @return  the comments URL
     */
    protected String getCommentUrl() { return getBaseUri() + BoxApiComment.COMMENTS_ENDPOINT; }


    /**
     * Gets a request that retrieves information on a file
     *
     * @param id    id of file to retrieve info on
     * @return      request to get a files information
     */
    public BoxRequestsFile.GetFileInfo getInfoRequest(final String id) {
        BoxRequestsFile.GetFileInfo request = new BoxRequestsFile.GetFileInfo(id, getFileInfoUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that updates a file's information
     *
     * @param id    id of file to update information on
     * @return      request to update a file's information
     */
    public BoxRequestsFile.UpdateFile getUpdateRequest(String id) {
        BoxRequestsFile.UpdateFile request = new BoxRequestsFile.UpdateFile(id, getFileInfoUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that copies a file
     *
     * @param id    id of the file to copy
     * @param parentId  id of the parent folder to copy the file into
     * @return  request to copy a file
     */
    public BoxRequestsFile.CopyFile getCopyRequest(String id, String parentId) {
        BoxRequestsFile.CopyFile request = new BoxRequestsFile.CopyFile(id, parentId, getFileCopyUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that renames a file
     *
     * @param id        id of file to rename
     * @param newName   id of file to retrieve info on
     * @return      request to rename a file
     */
    public BoxRequestsFile.UpdateFile getRenameRequest(String id, String newName) {
        BoxRequestsFile.UpdateFile request = new BoxRequestsFile.UpdateFile(id, getFileInfoUrl(id), mSession);
        request.setName(newName);
        return request;
    }

    /**
     * Gets a request that moves a file to another folder
     *
     * @param id        id of file to move
     * @param parentId  id of parent folder to move file into
     * @return      request to move a file
     */
    public BoxRequestsFile.UpdateFile getMoveRequest(String id, String parentId) {
        BoxRequestsFile.UpdateFile request = new BoxRequestsFile.UpdateFile(id, getFileInfoUrl(id), mSession);
        request.setParentId(parentId);
        return request;
    }

    /**
     * Gets a request that deletes a folder
     *
     * @param id        id of folder to delete
     * @return      request to delete a folder
     */
    public BoxRequestsFile.DeleteFile getDeleteRequest(String id) {
        BoxRequestsFile.DeleteFile request = new BoxRequestsFile.DeleteFile(id, getFileInfoUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that creates a shared link for a file
     *
     * @param id        id of file to create shared link for
     * @return      request to create a shared link for a file
     */
    public BoxRequestsFile.UpdatedSharedFile getCreateSharedLinkRequest(String id) {
        BoxRequestsFile.UpdatedSharedFile request = new BoxRequestsFile.UpdatedSharedFile(id, getFileInfoUrl(id), mSession)
                .setAccess(null);
        return request;
    }

    /**
     * Gets a request that disables a shared link for a folder
     *
     * @param id        id of folder to disable a shared link for
     * @return      request to create a shared link for a folder
     */
    public BoxRequestsFile.UpdateFile getDisableSharedLinkRequest(String id) {
        BoxRequestsFile.UpdateFile request = new BoxRequestsFile.UpdateFile(id, getFileInfoUrl(id), mSession)
                .setSharedLink(null);
        return request;
    }

    /**
     * Gets a request that adds a comment to a file
     *
     * @param fileId    id of the file to add the comment to
     * @param message   message for the comment that will be added
     * @return  request to add a comment to a file
     */
    public BoxRequestsFile.AddCommentToFile getAddCommentRequest(String fileId, String message) {
        BoxRequestsFile.AddCommentToFile request = new BoxRequestsFile.AddCommentToFile(fileId, message, getCommentUrl(), mSession);
        return request;
    }

    /**
     * Gets a request that uploads a file from an input stream
     *
     * @param fileInputStream   input stream of the file
     * @param fileName  name of the new file
     * @param destinationFolderId   id of the parent folder for the new file
     * @return  request to upload a file from an input stream
     */
    public BoxRequestsFile.UploadFile getUploadRequest(InputStream fileInputStream, String fileName, String destinationFolderId){
        BoxRequestsFile.UploadFile request = new BoxRequestsFile.UploadFile(fileInputStream, fileName, destinationFolderId, getFileUploadUrl(), mSession);
        return request;
    }

    /**
     * Gets a request that uploads a file from an existing file
     * @param file  file to upload
     * @param destinationFolderId   id of the parent folder for the new file
     * @return  request to upload a file from an existing file
     */
    public BoxRequestsFile.UploadFile getUploadRequest(File file, String destinationFolderId) {
            BoxRequestsFile.UploadFile request = new BoxRequestsFile.UploadFile(file,  destinationFolderId, getFileUploadUrl(), mSession);
            return request;
    }

    /**
     * Gets a request that uploads a new file version from an input stream
     *
     * @param fileInputStream   input stream of the new file version
     * @param destinationFileId id of the file to upload a new version of
     * @return  request to upload a new file version from an input stream
     */
    public BoxRequestsFile.UploadNewVersion getUploadNewVersionRequest(InputStream fileInputStream, String destinationFileId){
        BoxRequestsFile.UploadNewVersion request = new BoxRequestsFile.UploadNewVersion(fileInputStream, getFileUploadNewVersionUrl(destinationFileId), mSession);
        return request;
    }

    /**
     * Gets a request that uploads a new file version from an existing file
     *
     * @param file  file to upload as a new version
     * @param destinationFileId id of the file to upload a new version of
     * @return  request to upload a new file version from an existing file
     */
    public BoxRequestsFile.UploadNewVersion getUploadNewVersionRequest(File file, String destinationFileId) {
        try {
            BoxRequestsFile.UploadNewVersion request = getUploadNewVersionRequest(new FileInputStream(file), destinationFileId);
            request.setUploadSize(file.length());
            request.setModifiedDate(new Date(file.lastModified()));
            return request;
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Gets a request that downloads a given file to a target file
     *
     * @param target    target file to download to
     * @param fileId    id of the file to download
     * @return  request to download a file to a target file
     * @throws IOException
     */
    public BoxRequestsFile.DownloadFile getDownloadRequest(File target, String fileId) throws IOException{
            if (!target.exists()){
                throw new FileNotFoundException();
            }
            BoxRequestsFile.DownloadFile request = new BoxRequestsFile.DownloadFile(target, getFileDownloadUrl(fileId),mSession);
            return request;
    }

    /**
     * Gets a request that downloads the given file to the provided outputStream. Developer is responsible for closing the outputStream provided.
     *
     * @param outputStream outputStream to write file contents to.
     * @param fileId the file id to download.
     * @return  request to download a file to an output stream
     */
    public BoxRequestsFile.DownloadFile getDownloadRequest(OutputStream outputStream, String fileId) {
            BoxRequestsFile.DownloadFile request = new BoxRequestsFile.DownloadFile(outputStream, getFileDownloadUrl(fileId),mSession);
            return request;
    }

    /**
     * Gets a request that downloads a thumbnail to a target file
     *
     * @param target    target file to download thumbnail to
     * @param fileId    id of file to download the thumbnail of
     * @return  request to download a thumbnail to a target file
     * @throws IOException
     */
    public BoxRequestsFile.DownloadThumbnail getDownloadThumbnailRequest(File target, String fileId) throws IOException{
        if (!target.exists()){
            throw new FileNotFoundException();
        }
        BoxRequestsFile.DownloadThumbnail request = new BoxRequestsFile.DownloadThumbnail(target, getThumbnailFileDownloadUrl(fileId),mSession);
        return request;
    }

    /**
     * Gets a request that downloads the given file thumbnail to the provided outputStream. Developer is responsible for closing the outputStream provided.
     *
     * @param outputStream outputStream to write file contents to.
     * @param fileId the file id to download.
     * @return  request to download a file thumbnail
     */
    public BoxRequestsFile.DownloadThumbnail getDownloadThumbnailRequest(OutputStream outputStream, String fileId) {
        BoxRequestsFile.DownloadThumbnail request = new BoxRequestsFile.DownloadThumbnail(outputStream, getThumbnailFileDownloadUrl(fileId),mSession);
        return request;
    }

    /**
     * Gets a request that returns a file in the trash
     *
     * @param id        id of file to get in the trash
     * @return      request to get a file from the trash
     */
    public BoxRequestsFile.GetTrashedFile getTrashedFileRequest(String id) {
        BoxRequestsFile.GetTrashedFile request = new BoxRequestsFile.GetTrashedFile(id, getTrashedFileUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that permanently deletes a file from the trash
     *
     * @param id        id of file to delete from the trash
     * @return      request to permanently delete a file from the trash
     */
    public BoxRequestsFile.DeleteTrashedFile getDeleteTrashedFileRequest(String id) {
        BoxRequestsFile.DeleteTrashedFile request = new BoxRequestsFile.DeleteTrashedFile(id, getTrashedFileUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that restores a trashed file
     *
     * @param id        id of file to restore
     * @return      request to restore a file from the trash
     */
    public BoxRequestsFile.RestoreTrashedFile getRestoreTrashedFileRequest(String id) {
        BoxRequestsFile.RestoreTrashedFile request = new BoxRequestsFile.RestoreTrashedFile(id, getFileInfoUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that retrieves the comments on a file
     *
     * @param id    id of the file to retrieve comments for
     * @return  request to retrieve comments on a file
     */
    public BoxRequestsFile.GetFileComments getCommentsRequest(String id) {
        BoxRequestsFile.GetFileComments request = new BoxRequestsFile.GetFileComments(id, getFileCommentsUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that retrieves the versions of a file
     *
     * @param id    id of the file to retrieve file versions for
     * @return  request to retrieve versions of a file
     */
    public BoxRequestsFile.GetFileVersions getVersionsRequest(String id) {
        BoxRequestsFile.GetFileVersions request = new BoxRequestsFile.GetFileVersions(id, getFileVersionsUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that promotes a version to the top of the version stack for a file.
     * This will create a copy of the old version to put on top of the version stack. The file will have the exact same contents, the same SHA1/etag, and the same name as the original.
     * Other properties such as comments do not get updated to their former values.
     *
     * @param id    id of the file to promote the version of
     * @param versionId id of the file version to promote to the top
     * @return  request to promote a version of a file
     */
    public BoxRequestsFile.PromoteFileVersion getPromoteVersionRequest(String id, String versionId) {
        BoxRequestsFile.PromoteFileVersion request = new BoxRequestsFile.PromoteFileVersion(id, versionId, getPromoteFileVersionUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that deletes a version of a file
     *
     * @param id    id of the file to delete a version of
     * @param versionId id of the file version to delete
     * @return  request to delete a file version
     */
    public BoxRequestsFile.DeleteFileVersion getDeleteVersionRequest(String id, String versionId) {
        BoxRequestsFile.DeleteFileVersion request = new BoxRequestsFile.DeleteFileVersion(versionId, getDeleteFileVersionUrl(id, versionId), mSession);
        return request;
    }

    /**
     * Gets a request that adds a file to a collection
     *
     * @param fileId        id of file to add to collection
     * @param collectionId    id of collection to add the file to
     * @return      request to add a file to a collection
     */
    public BoxRequestsFile.AddFileToCollection getAddToCollectionRequest(String fileId, String collectionId) {
        BoxRequestsFile.AddFileToCollection request = new BoxRequestsFile.AddFileToCollection(fileId, collectionId, getFileInfoUrl(fileId), mSession);
        return request;
    }

    /**
     * Gets a request that removes a file from a collection
     *
     * @param id        id of file to delete from the collection
     * @return request to delete a file from a collection
     */
    public BoxRequestsFile.DeleteFileFromCollection getDeleteFromCollectionRequest(String id) {
        BoxRequestsFile.DeleteFileFromCollection request = new BoxRequestsFile.DeleteFileFromCollection(id, getFileInfoUrl(id), mSession);
        return request;
    }
}