package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.models.BoxArray;
import com.box.androidsdk.content.models.BoxJsonObject;
import com.box.androidsdk.content.models.BoxMetadata;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxVoid;

import java.util.Map;

/**
 * Request class that groups all metadata operation requests together
 */
public class BoxRequestsMetadata {

    /**
     * Request for adding metadata to a file
     */
    public static class AddFileMetadata extends BoxRequest<BoxMetadata, AddFileMetadata> {
        /**
         * Creates a add file metadata request with the default parameters
         *
         * @param requestUrl    URL of the file metadata endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public AddFileMetadata(Map<String, Object> values, String requestUrl, BoxSession session) {
            super(BoxMetadata.class, requestUrl, session);
            mRequestMethod = Methods.POST;
            setValues(values);
        }

        /**
         * Sets the values of the item used in the request.
         *
         * @param map    values of the item to add metadata to.
         * @return  request with the updated values.
         */
        protected AddFileMetadata setValues(Map<String,Object> map) {
            mBodyMap.putAll(map);
            return this;
        }
    }

    /**
     * Request for getting metadata on a file
     */
    public static class GetFileMetadata extends BoxRequest<BoxMetadata, GetFileMetadata> {
        /**
         * Creates a get file metadata request with the default parameters
         *
         * @param requestUrl    URL of the file metadata endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public GetFileMetadata(String requestUrl, BoxSession session) {
            super(BoxMetadata.class, requestUrl, session);
            mRequestMethod = Methods.GET;
        }
    }

    /**
     * Request for udpating metadata on a file
     */
    public static class UpdateFileMetadata extends BoxRequest<BoxMetadata, UpdateFileMetadata> {

        /**
         * ENUM that defines all possible update operations.
         */
        public enum Operations {
            ADD("add"),
            REPLACE("replace"),
            REMOVE("remove"),
            TEST("test");

            private String mName;

            private Operations(String name) {
                mName = name;
            }

            @Override
            public String toString() { return mName; }
        }

        private BoxArray<BoxMetadataUpdateTask> mUpdateTasks;

        /**
         * Creates a update file metadata request with the default parameters
         *
         * @param requestUrl    URL of the file metadata endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public UpdateFileMetadata(String requestUrl, BoxSession session) {
            super(BoxMetadata.class, requestUrl, session);
            mRequestMethod = Methods.PUT;
            mContentType = ContentTypes.JSON_PATCH;
            mUpdateTasks = new BoxArray<BoxMetadataUpdateTask>();
        }

        /**
         * Updates the values of the item used in the request.
         *
         * @param updateTasks    task list for metadata update.
         * @return  request with the updated values.
         */
        protected UpdateFileMetadata setUpdateTasks(BoxArray<BoxMetadataUpdateTask> updateTasks) {
            mBodyMap.put(BoxRequest.JSON_OBJECT, updateTasks);
            return this;
        }

        /**
         * Add new update task to request.
         * @param operation The operation to apply.
         * @param key The key.
         * @param value The value for the path (key). Can leave blank if performing REMOVE operation.
         */
        public UpdateFileMetadata addUpdateTask(Operations operation, String key, String value) {
            mUpdateTasks.add(new BoxMetadataUpdateTask(operation, key, value));
            return setUpdateTasks(mUpdateTasks);
        }

        /**
         * Defaults new value to an empty string.
         */
        public UpdateFileMetadata addUpdateTask(Operations operation, String key) {
            return addUpdateTask(operation, key, "");
        }

        /**
         * Represents a single Update Task in the request.
         */
        private class BoxMetadataUpdateTask extends BoxJsonObject {

            /**
             * Operation to perform (add, replace, remove, test).
             */
            public static final String OPERATION = "op";

            /**
             * Path (key) to update.
             */
            public static final String PATH = "path";

            /**
             * Value to use (not required for remove operation).
             */
            public static final String VALUE = "value";

            /**
             * Initializes a BOXMetadataUpdateTask with a given operation to apply to a key/value pair.
             *
             * @param operation The operation to apply.
             * @param key The key.
             * @param value The value for the path (key). Can leave blank if performing REMOVE operation.
             */
            public BoxMetadataUpdateTask (Operations operation, String key, String value) {
                mProperties.put(OPERATION, operation.toString());
                mProperties.put(PATH, "/" + key);
                if (operation != Operations.REMOVE) {
                    mProperties.put(VALUE, value);
                }
            }
        }
    }

    /**
     * Request for deleting metadata on a file
     */
    public static class DeleteFileMetadata extends BoxRequest<BoxVoid, DeleteFileMetadata> {
        /**
         * Creates a delete file metadata request with the default parameters
         *
         * @param requestUrl    URL of the file metadata endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public DeleteFileMetadata(String requestUrl, BoxSession session) {
            super(BoxVoid.class, requestUrl, session);
            mRequestMethod = Methods.DELETE;
        }
    }

    /**
     * Request for getting available metadata templates
     */
    public static class GetMetadataTemplates extends BoxRequest<BoxMetadata, GetMetadataTemplates> {
        /**
         * Creates a delete file metadata request with the default parameters
         *
         * @param requestUrl    URL of the file metadata endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public GetMetadataTemplates(String requestUrl, BoxSession session) {
            super(BoxMetadata.class, requestUrl, session);
            mRequestMethod = Methods.GET;
        }
    }

    /**
     * Request for getting a metadata template schema
     */
    public static class GetMetadataTemplateSchema extends BoxRequest<BoxMetadata, GetMetadataTemplateSchema> {
        /**
         * Creates a delete file metadata request with the default parameters
         *
         * @param requestUrl    URL of the file metadata endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public GetMetadataTemplateSchema(String requestUrl, BoxSession session) {
            super(BoxMetadata.class, requestUrl, session);
            mRequestMethod = Methods.GET;
        }
    }
}
