package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.BoxConstants;
import com.box.androidsdk.content.models.BoxListCollaborations;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxSharedLinkSession;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxBookmark;
import com.box.androidsdk.content.models.BoxCollaboration;
import com.box.androidsdk.content.models.BoxCollaborator;
import com.box.androidsdk.content.models.BoxEntity;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxFolder;
import com.box.androidsdk.content.models.BoxGroup;
import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxMapJsonObject;
import com.box.androidsdk.content.models.BoxObject;
import com.box.androidsdk.content.models.BoxUser;
import com.box.androidsdk.content.models.BoxVoid;
import com.box.androidsdk.content.utils.SdkUtils;

import java.util.HashMap;

/**
 * Shared link and collaboration requests.
 */
public class BoxRequestsShare {

    /**
     * A request to get an item from a Shared Link
     */
    public static class GetSharedLink extends BoxRequest<BoxItem, GetSharedLink> {

        /**
         * Creates a get item from shared link request with the default parameters
         *
         * @param requestUrl URL of the shared items endpoint
         * @param session    the authenticated session that will be used to make the request with
         */
        public GetSharedLink(String requestUrl, BoxSharedLinkSession session) {
            super(BoxItem.class, requestUrl, session);
            mRequestMethod = Methods.GET;
            setRequestHandler(new BoxRequestHandler<GetSharedLink>(this) {
                @Override
                public <T extends BoxObject> T onResponse(Class<T> clazz, BoxHttpResponse response) throws BoxException {
                    if (response.getResponseCode() == BoxConstants.HTTP_STATUS_TOO_MANY_REQUESTS) {
                        return retryRateLimited(response);
                    }
                    String contentType = response.getContentType();
                    BoxEntity entity = new BoxEntity();
                    if (contentType.contains(ContentTypes.JSON.toString())) {
                        String json = response.getStringBody();
                        entity.createFromJson(json);
                        if (entity.getType().equals(BoxFolder.TYPE)) {
                            entity = new BoxFolder();
                            entity.createFromJson(json);
                        } else if (entity.getType().equals(BoxFile.TYPE)) {
                            entity = new BoxFile();
                            entity.createFromJson(json);
                        } else if (entity.getType().equals(BoxBookmark.TYPE)) {
                            entity = new BoxBookmark();
                            entity.createFromJson(json);
                        }
                    }
                    return (T) entity;
                }
            });
        }

        @Override
        public GetSharedLink setIfNoneMatchEtag(String etag) {
            return super.setIfNoneMatchEtag(etag);
        }

        @Override
        public String getIfNoneMatchEtag() {
            return super.getIfNoneMatchEtag();
        }
    }

    /**
     * Request for retrieving information on a collaboration
     */
    public static class GetCollaborationInfo extends BoxRequest<BoxCollaboration, GetCollaborationInfo> {
        private final String mId;

        /**
         * Creates a request to get a collaboration with the default parameters
         *
         * @param collaborationId id of the collaboration to retrieve information of
         * @param requestUrl      URL of the collaboration endpoint
         * @param session         the authenticated session that will be used to make the request with
         */
        public GetCollaborationInfo(String collaborationId, String requestUrl, BoxSession session) {
            super(BoxCollaboration.class, requestUrl, session);
            mRequestMethod = Methods.GET;
            this.mId = collaborationId;
        }

        /**
         * Returns the id of the collaboration being retrieved.
         *
         * @return the id of the collaboration that this request is attempting to retrieve.
         */
        public String getId() {
            return mId;
        }
    }

    /**
     * Request for retrieving pending collaborations for a user.
     */
    public static class GetPendingCollaborations extends BoxRequest<BoxListCollaborations, GetPendingCollaborations> {

        public GetPendingCollaborations(String requestUrl, BoxSession session) {
            super(BoxListCollaborations.class, requestUrl, session);
            mRequestMethod = Methods.GET;
            mQueryMap.put("status", BoxCollaboration.Status.PENDING.toString());
        }
    }

    /**
     * Request for adding a collaboration
     */
    public static class AddCollaboration extends BoxRequest<BoxCollaboration, AddCollaboration> {

        public static final String ERROR_CODE_USER_ALREADY_COLLABORATOR = "user_already_collaborator";

        private final String mFolderId;

        /**
         * Adds a user by email to a folder as a collaborator.
         *
         * @param url       the url for the add collaboration request
         * @param folderId  id of the folder to be collaborated.
         * @param role      role of the collaboration
         * @param userEmail login email of the user who this collaboration applies to, use null if this is a group or you already supplied a accessibleById.
         * @param session   session to use for the add collaboration request
         */
        public AddCollaboration(String url, String folderId, BoxCollaboration.Role role, String userEmail, BoxSession session) {
            super(BoxCollaboration.class, url, session);
            mRequestMethod = Methods.POST;
            this.mFolderId = folderId;
            setFolder(folderId);
            setAccessibleBy(null, userEmail, BoxUser.TYPE);
            mBodyMap.put(BoxCollaboration.FIELD_ROLE, role.toString());
        }

        /**
         * Adds a user or group to a folder as a collaborator.
         *
         * @param url          the url for the add collaboration request
         * @param folderId     id of the folder to be collaborated.
         * @param role         role of the collaboration
         * @param collaborator the user or group to add to the folder as a collaborator
         * @param session      session to use for the add collaboration request
         */
        public AddCollaboration(String url, String folderId, BoxCollaboration.Role role, BoxCollaborator collaborator, BoxSession session) {
            super(BoxCollaboration.class, url, session);
            mRequestMethod = Methods.POST;
            this.mFolderId = folderId;
            setFolder(folderId);
            setAccessibleBy(collaborator.getId(), null, collaborator.getType());
            mBodyMap.put(BoxCollaboration.FIELD_ROLE, role.toString());
        }

        /**
         * Determines if the user, (or all the users in the group) should receive email notification of the collaboration.
         *
         * @param notify whether or not to notify the collaborators via email about the collaboration.
         */
        public AddCollaboration notifyCollaborators(boolean notify) {
            mQueryMap.put("notify", Boolean.toString(notify));
            return this;
        }

        /**
         * Returns the id of the folder collaborations are being added to.
         *
         * @return the id of the folder that this request is attempting to add collaborations to.
         */
        public String getFolderId() {
            return mFolderId;
        }

        private void setFolder(String id) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            map.put(BoxItem.FIELD_ID, id);
            map.put(BoxItem.FIELD_TYPE, BoxFolder.TYPE);
            mBodyMap.put(BoxCollaboration.FIELD_ITEM, new BoxMapJsonObject(map));

        }

        private void setAccessibleBy(String accessibleById, String accessibleByEmail, String accessibleByType) {
            HashMap<String, Object> map = new HashMap<String, Object>();
            if (!SdkUtils.isEmptyString(accessibleById)) {
                map.put(BoxCollaborator.FIELD_ID, accessibleById);
            }
            if (!SdkUtils.isEmptyString(accessibleByEmail)) {
                map.put(BoxUser.FIELD_LOGIN, accessibleByEmail);
            }
            map.put(BoxCollaborator.FIELD_TYPE, accessibleByType);
            BoxCollaborator collaborator;
            if (accessibleByType.equals(BoxUser.TYPE)) {
                collaborator = new BoxUser(map);
            } else if (accessibleByType.equals(BoxGroup.TYPE)) {
                collaborator = new BoxGroup(map);
            } else {
                throw new IllegalArgumentException("AccessibleBy property can only be set with type BoxUser.TYPE or BoxGroup.TYPE");
            }
            mBodyMap.put(BoxCollaboration.FIELD_ACCESSIBLE_BY, collaborator);
        }

        /**
         * Gets the collaborator that the folder will be accessible by
         *
         * @return collaborator that can access the folder
         */
        public BoxCollaborator getAccessibleBy() {
            return mBodyMap.containsKey(BoxCollaboration.FIELD_ACCESSIBLE_BY) ?
                    (BoxCollaborator) mBodyMap.get(BoxCollaboration.FIELD_ACCESSIBLE_BY) :
                    null;
        }
    }


    /**
     * Request for deleting a collaboration
     */
    public static class DeleteCollaboration extends BoxRequest<BoxVoid, DeleteCollaboration> {
        private String mId;

        /**
         * Creates a request to delete a collaboration with the default parameters
         *
         * @param collaborationId id of the collaboration to delete
         * @param requestUrl      URL of the delete collaboration endpoint
         * @param session         the authenticated session that will be used to make the request with
         */
        public DeleteCollaboration(String collaborationId, String requestUrl, BoxSession session) {
            super(BoxVoid.class, requestUrl, session);
            this.mId = collaborationId;
            mRequestMethod = Methods.DELETE;
        }


        /**
         * Returns the id of the collaboration being deleted.
         *
         * @return the id of the collaboration that this request is attempting to delete.
         */
        public String getId() {
            return mId;
        }
    }

    /**
     * Request for updating a collaboration
     */
    public static class UpdateCollaboration extends BoxRequest<BoxCollaboration, UpdateCollaboration> {
        private String mId;

        /**
         * Creates a request to update the collaboration with the default parameters
         *
         * @param collaborationId id of the collaboration to update
         * @param requestUrl      URL of the update collaboration endpoint
         * @param session         the authenticated session that will be used to make the request with
         */
        public UpdateCollaboration(String collaborationId, String requestUrl, BoxSession session) {
            super(BoxCollaboration.class, requestUrl, session);
            this.mId = collaborationId;
            mRequestMethod = Methods.PUT;
        }

        /**
         * Returns the id of the collaboration being modified.
         *
         * @return the id of the collaboration that this request is attempting to modify.
         */
        public String getId() {
            return mId;
        }

        /**
         * Sets the new role for the collaboration to be updated in the request
         *
         * @param newRole role to update the collaboration to
         * @return request with the updated collaboration role
         */
        public UpdateCollaboration setNewRole(BoxCollaboration.Role newRole) {
            mBodyMap.put(BoxCollaboration.FIELD_ROLE, newRole.toString());
            return this;
        }

        /**
         * Sets the status of the collaboration to be updated in the request
         *
         * @param status new status for the collaboration. This can be set to 'accepted' or 'rejected' by the 'accessible_by' user if the status is 'pending'
         * @return request with the updated collaboration status
         */
        public UpdateCollaboration setNewStatus(String status) {
            mBodyMap.put(BoxCollaboration.FIELD_STATUS, status);
            return this;
        }
    }

}
