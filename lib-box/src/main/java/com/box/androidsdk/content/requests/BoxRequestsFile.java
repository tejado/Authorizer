package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxComment;
import com.box.androidsdk.content.models.BoxDownload;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFileVersion;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxListComments;
import com.box.androidsdk.content.models.BoxListFileVersions;
import com.box.androidsdk.content.models.BoxVoid;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * Request class that groups all file operation requests together
 */
public class BoxRequestsFile {

    /**
     * Request for retrieving information on a file
     */
    public static class GetFileInfo extends BoxRequestItem<BoxFile, GetFileInfo> {

        /**
         * Creates a file information request with the default parameters
         *
         * @param id            id of the file to get information on
         * @param requestUrl    URL of the file information endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public GetFileInfo(String id, String requestUrl, BoxSession session) {
            super(BoxFile.class, id, requestUrl, session);
            mRequestMethod = Methods.GET;
        }

        /**
         * Sets the if-none-match header for the request.
         * The file will only be retrieved if the etag does not match the most current etag for the file.
         *
         * @param etag  etag to set in the if-none-match header.
         * @return request with the updated if-none-match header.
         */
        @Override
        public GetFileInfo setIfNoneMatchEtag(String etag) {
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
     * Request for updating information on a file
     */
    public static class UpdateFile extends BoxRequestItemUpdate<BoxFile, UpdateFile> {

        /**
         * Creates an update file request with the default parameters
         *
         * @param id            id of the file to update information on
         * @param requestUrl    URL of the update file endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public UpdateFile(String id, String requestUrl, BoxSession session) {
            super(BoxFile.class, id, requestUrl, session);
        }

        @Override
        public UpdatedSharedFile updateSharedLink() {
            return new UpdatedSharedFile(this);
        }

    }

    public static class UpdatedSharedFile extends BoxRequestUpdateSharedItem<BoxFile, UpdatedSharedFile> {
        /**
         * Creates an update shared file request with the default parameters
         *
         * @param id            id of the file to update information on
         * @param requestUrl    URL of the update file endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public UpdatedSharedFile(String id, String requestUrl, BoxSession session) {
            super(BoxFile.class, id, requestUrl, session);
        }

        protected UpdatedSharedFile(UpdateFile r) {
            super(r);
        }

        /**
         * Sets whether or not the file can be downloaded from the shared link.
         *
         * @param canDownload   boolean representing whether or not the file can be downloaded from the shared link.
         * @return  request with the updated shared link.
         */
        @Override
        public UpdatedSharedFile setCanDownload(boolean canDownload) {
            return super.setCanDownload(canDownload);
        }

        /**
         * Returns whether or not the file can be downloaded from the shared link.
         *
         * @return  Boolean representing whether or not the file can be downloaded from the shared link, or null if not set.
         */
        @Override
        public Boolean getCanDownload() {
            return super.getCanDownload();
        }
    }

    /**
     * Request for copying a file
     */
    public static class CopyFile extends BoxRequestItemCopy<BoxFile, CopyFile> {

        /**
         * Creates a copy file request with the default parameters
         * @param id    id of the file to copy
         * @param parentId  id of the new parent folder
         * @param requestUrl    URL of the copy file endpoint
         * @param session   the authenticated session that will be used to make the request with
         */
        public CopyFile(String id, String parentId, String requestUrl, BoxSession session) {
            super(BoxFile.class, id, parentId, requestUrl, session);
        }
    }

    /**
     * Request for deleting a file
     */
    public static class DeleteFile extends BoxRequestItemDelete<DeleteFile> {
        /**
         * Creates a delete file request with the default parameters
         *
         * @param id            id of the file to delete
         * @param requestUrl    URL of the delete file endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public DeleteFile(String id, String requestUrl, BoxSession session) {
            super(id, requestUrl, session);
        }
    }

    /**
     * Request for retrieving information on a trashed file
     */
    public static class GetTrashedFile extends BoxRequestItem<BoxFile, GetTrashedFile> {

        /**
         * Creates a request to get a trashed file with the default parameters
         *
         * @param id            id of the file in the trash
         * @param requestUrl    URL of the trashed file endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public GetTrashedFile(String id, String requestUrl, BoxSession session) {
            super(BoxFile.class, id, requestUrl, session);
            mRequestMethod = Methods.GET;
        }

        /**
         * Sets the if-none-match header for the request.
         * The trashed file will only be retrieved if the etag does not match the most current etag for the file.
         *
         * @param etag  etag to set in the if-none-match header.
         * @return request with the updated if-none-match header.
         */
        @Override
        public GetTrashedFile setIfNoneMatchEtag(String etag) {
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
     * Request for permanently deleting a trashed file
     */
    public static class DeleteTrashedFile extends BoxRequestItemDelete<DeleteTrashedFile> {

        /**
         * Creates a delete trashed file request with the default parameters
         *
         * @param id            id of the trashed file to permanently delete
         * @param requestUrl    URL of the delete trashed file endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public DeleteTrashedFile(String id, String requestUrl, BoxSession session) {
            super(id, requestUrl, session);
        }
    }

    /**
     * Request for restoring a trashed file
     */
    public static class RestoreTrashedFile extends BoxRequestItemRestoreTrashed<BoxFile, RestoreTrashedFile> {

        /**
         * Creates a restore trashed file request with the default parameters
         *
         * @param id            id of the trashed file to restore
         * @param requestUrl    URL of the restore trashed file endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public RestoreTrashedFile(String id, String requestUrl, BoxSession session) {
            super(BoxFile.class, id, requestUrl, session);
        }
    }

    /**
     * Request for getting comments on a file
     */
    public static class GetFileComments extends BoxRequestItem<BoxListComments, GetFileComments> {

        /**
         * Creates a get file comments request with the default parameters
         *
         * @param id    id of the file to get comments of
         * @param requestUrl    URL of the file comments endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public GetFileComments(String id, String requestUrl, BoxSession session) {
            super(BoxListComments.class, id, requestUrl, session);
            mRequestMethod = Methods.GET;
        }
    }

    /**
     * Request for adding a comment to a file
     */
    public static class AddCommentToFile extends BoxRequestCommentAdd<BoxComment, AddCommentToFile> {

        /**
         * Creates an add comment to file request with the default parameters
         *
         * @param fileId    id of the file to add a comment to
         * @param message   message of the new comment
         * @param requestUrl    URL of the add comment endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public AddCommentToFile(String fileId, String message, String requestUrl, BoxSession session) {
            super(BoxComment.class, requestUrl, session);
            setItemId(fileId);
            setItemType(BoxFile.TYPE);
            setMessage(message);
        }
    }

    /**
     * Request for getting versions of a file
     */
    public static class GetFileVersions extends BoxRequestItem<BoxListFileVersions, GetFileVersions> {

        /**
         * Creates a get file versions request with the default parameters
         *
         * @param id    id of the file to get versions of
         * @param requestUrl    URL of the file versions endpoint
         * @param session   the authenticated session that will be used to make the request with
         */
        public GetFileVersions(String id, String requestUrl, BoxSession session) {
            super(BoxListFileVersions.class, id, requestUrl, session);
            mRequestMethod = Methods.GET;
            // this call will by default set all fields as we need the deleted_at to know which are actual versions.
            setFields(BoxFileVersion.ALL_FIELDS);
        }
    }

    /**
     * Request for promoting an old version to the top of the version stack for a file
     */
    public static class PromoteFileVersion extends BoxRequestItem<BoxFileVersion, PromoteFileVersion> {

        /**
         * Creates a promote file version request with the default parameters
         *
         * @param id    id of the file to promote a version of
         * @param versionId id of the version to promote to the top
         * @param requestUrl    URL of the promote file version endpoint
         * @param session   the authenticated session that will be used to make the request with
         */
        public PromoteFileVersion(String id, String versionId, String requestUrl, BoxSession session) {
            super(BoxFileVersion.class, id, requestUrl, session);
            mRequestMethod = Methods.POST;
            setVersionId(versionId);
        }

        /**
         * Sets the id of the version to promote.
         *
         * @param versionId id of the version to promote.
         * @return  request with the updated version id.
         */
        public PromoteFileVersion setVersionId(String versionId) {
            mBodyMap.put(BoxFileVersion.FIELD_TYPE, BoxFileVersion.TYPE);
            mBodyMap.put(BoxFolder.FIELD_ID, versionId);
            return this;
        }
    }

    /**
     * Request for deleting an old version of a file
     */
    public static class DeleteFileVersion extends BoxRequest<BoxVoid, DeleteFileVersion> {
        private final String mVersionId;

        /**
         * Creates a delete old file version request with the default parameters
         *
         * @param versionId    id of the version to delete
         * @param requestUrl    URL of the delete file version endpoint
         * @param session   the authenticated session that will be used to make the request with
         */
        public DeleteFileVersion(String versionId, String requestUrl, BoxSession session) {
            super(BoxVoid.class, requestUrl, session);
            mRequestMethod = Methods.DELETE;
            mVersionId = versionId;
        }

        /**
         * Returns the id of the file version to delete.
         *
         * @return  id of the file version to delete.
         */
        public String getVersionId() {
            return mVersionId;
        }
    }

    /**
     * Request for uploading a new file
     */
    public static class UploadFile extends BoxRequestUpload<BoxFile, UploadFile> {
        String mDestinationFolderId;

        /**
         * Creates an upload file from input stream request with the default parameters
         *
         * @param inputStream   input stream of the file
         * @param fileName  name of the new file
         * @param destinationFolderId   id of the parent folder for the new file
         * @param requestUrl    URL of the upload file endpoint
         * @param session   the authenticated session that will be used to make the request with
         */
        public UploadFile(InputStream inputStream, String fileName, String destinationFolderId, String requestUrl, BoxSession session) {
            super(BoxFile.class, inputStream, requestUrl, session);
            mRequestUrlString = requestUrl;
            mRequestMethod = Methods.POST;
            mFileName = fileName;
            mStream = inputStream;
            mDestinationFolderId = destinationFolderId;
        }

        /**
         * Creates an upload file from file request with the default parameters
         *
         * @param file  file to upload
         * @param destinationFolderId   id of the parent folder for the new file
         * @param requestUrl    URL of the upload file endpoint
         * @param session   the authenticated session that will be used to make the request with
         */
        public UploadFile(File file, String destinationFolderId, String requestUrl, BoxSession session) {
            super(BoxFile.class, null, requestUrl, session);
            mRequestUrlString = requestUrl;
            mRequestMethod = Methods.POST;
            mDestinationFolderId = destinationFolderId;
            mFileName = file.getName();
            mFile = file;
            mUploadSize = file.length();
            mModifiedDate = new Date(file.lastModified());
        }


        @Override
        protected BoxRequestMultipart createMultipartRequest() throws IOException, BoxException {
            BoxRequestMultipart request = super.createMultipartRequest();
            request.putField("parent_id", mDestinationFolderId);
            return request;
        }

        /**
         * Returns the name of the file to upload.
         *
         * @return  name of the file to upload.
         */
        public String getFileName() {
            return mFileName;
        }

        /**
         * Sets the name of the file to upload.
         *
         * @param mFileName name of the file to upload.
         * @return  request with the updated file to upload.
         */
        public UploadFile setFileName(String mFileName) {
            this.mFileName = mFileName;
            return this;
        }

        /**
         * Returns the destination folder id for the uploaded file.
         *
         * @return  id of the destination folder for the uploaded file.
         */
        public String getDestinationFolderId() {
            return mDestinationFolderId;
        }
    }

    /**
     * Request for uploading a new version of a file
     */
    public static class UploadNewVersion extends BoxRequestUpload<BoxFile, UploadNewVersion> {

        /**
         * Creates an upload new file version request with the default parameters
         *
         * @param fileInputStream   input stream of the new file version
         * @param requestUrl    URL of the upload new version endpoint
         * @param session   the authenticated session that will be used to make the request with
         */
        public UploadNewVersion(InputStream fileInputStream, String requestUrl, BoxSession session) {
            super(BoxFile.class, fileInputStream, requestUrl, session);
        }

        /**
         * Sets the if-match header for the request.
         * The new version will only be uploaded if the specified etag matches the most current etag for the file.
         *
         * @param etag  etag to set in the if-match header.
         * @return  request with the updated if-match header.
         */
        @Override
        public UploadNewVersion setIfMatchEtag(String etag) {
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
    }

    /**
     * Request for downloading a file
     */
    public static class DownloadFile extends BoxRequestDownload<BoxDownload, DownloadFile> {

        /**
         * Creates a download file to output stream request with the default parameters
         *
         * @param outputStream  output stream to download the file to
         * @param requestUrl    URL of the download file endpoint
         * @param session   the authenticated session that will be used to make the request with
         */
        public DownloadFile(final OutputStream outputStream, String requestUrl, BoxSession session) {
            super(BoxDownload.class, outputStream, requestUrl, session);
        }

        /**
         * Creates a download file to file request with the default parameters
         *
         * @param target    target file to download to
         * @param requestUrl    URL of the download file endpoint
         * @param session   the authenticated session that will be used to make the request with
         */
        public DownloadFile(final File target, String requestUrl, BoxSession session) {
            super(BoxDownload.class, target, requestUrl, session);
        }
    }

    /**
     * Request for downloading a thumbnail
     */
    public static class DownloadThumbnail extends BoxRequestDownload<BoxDownload, DownloadThumbnail> {

        private static final String FIELD_MIN_WIDTH = "min_width";
        private static final String FIELD_MIN_HEIGHT = "min_height";
        private static final String FIELD_MAX_WIDTH = "max_width";
        private static final String FIELD_MAX_HEIGHT = "max_height";

        public static int SIZE_32 = 32;
        public static int SIZE_64 = 64;
        public static int SIZE_128 = 128;
        public static int SIZE_256 = 256;

        /**
         * Creates a download thumbnail to output stream request with the default parameters
         *
         * @param outputStream  output stream to download the thumbnail to
         * @param requestUrl    URL of the download thumbnail endpoint
         * @param session   the authenticated session that will be used to make the request with
         */
        public DownloadThumbnail(final OutputStream outputStream, String requestUrl, BoxSession session) {
            super(BoxDownload.class, outputStream, requestUrl, session);
        }

        /**
         * Creates a download thumbnail to file request with the default parameters
         *
         * @param target    target file to download thumbnail to
         * @param requestUrl    URL of the download thumbnail endpoint
         * @param session   the authenticated session that will be used to make the request with
         */
        public DownloadThumbnail(final File target, String requestUrl, BoxSession session) {
            super(BoxDownload.class, target, requestUrl, session);
        }

        /**
         * Sets the minimum width for the thumbnail in the request.
         *
         * @param width int for the minimum width.
         * @return  request with the updated minimum width.
         */
        public DownloadThumbnail setMinWidth(int width){
            mQueryMap.put(FIELD_MIN_WIDTH, Integer.toString(width));
            return this;
        }

        /**
         * Sets the maximum width for the thumbnail in the request.
         *
         * @param width int for the maximum width.
         * @return  request with the updated maximum width.
         */
        public DownloadThumbnail setMaxWidth(int width){
            mQueryMap.put(FIELD_MAX_WIDTH, Integer.toString(width));
            return this;
        }

        /**
         * Sets the minimum height for the thumbnail in the request.
         *
         * @param height int for the minimum height.
         * @return  request with the updated minimum height.
         */
        public DownloadThumbnail setMinHeight(int height){
            mQueryMap.put(FIELD_MIN_HEIGHT, Integer.toString(height));
            return this;
        }

        /**
         * Sets the maximum height for the thumbnail in the request.
         *
         * @param height int for the maximum height.
         * @return  request with the updated maximum height.
         */
        public DownloadThumbnail setMaxHeight(int height){
            mQueryMap.put(FIELD_MAX_HEIGHT, Integer.toString(height));
            return this;
        }

        /**
         * Sets both the minimum height and minimum width for the thumbnail in the request.
         *
         * @param size int for the minimum height and minimum width.
         * @return  request with the updated minimum size.
         */
        public DownloadThumbnail setMinSize(int size){
            setMinWidth(size);
            setMinHeight(size);
            return this;
        }
    }

    /**
     * Request for adding a file to a collection
     */
    public static class AddFileToCollection extends BoxRequestCollectionUpdate<BoxFile, AddFileToCollection> {

        /**
         * Creates an add file to collection request with the default parameters
         *
         * @param id    id of the file to add to the collection
         * @param collectionId  id of the collection to add the file to
         * @param requestUrl    URL of the add to collection endpoint
         * @param session   the authenticated session that will be used to make the request with
         */
        public AddFileToCollection(String id, String collectionId, String requestUrl, BoxSession session) {
            super(BoxFile.class, id, requestUrl, session);
            setCollectionId(collectionId);
        }

        /**
         * Sets the collection id in the request to add the file to.
         *
         * @param id    id of the collection to add the file to.
         * @return  request with the updated collection id.
         */
        @Override
        public AddFileToCollection setCollectionId(String id) {
            return super.setCollectionId(id);
        }
    }

    /**
     * Request for removing a file from a collection
     */
    public static class DeleteFileFromCollection extends BoxRequestCollectionUpdate<BoxFile, DeleteFileFromCollection> {

        /**
         * Creates a delete file from collection request with the default parameters
         *
         * @param id    id of the file to delete from collection
         * @param requestUrl    URL of the delete from collection endpoint
         * @param session   the authenticated session that will be used to make the request with
         */
        public DeleteFileFromCollection(String id, String requestUrl, BoxSession session) {
            super(BoxFile.class, id, requestUrl, session);
            setCollectionId(null);
        }
    }

}
