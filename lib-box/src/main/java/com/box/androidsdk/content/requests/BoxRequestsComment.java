package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxComment;
import com.box.androidsdk.content.models.BoxVoid;

/**
 * Comment requests.
 */
public class BoxRequestsComment {

    /**
     * Request for retrieving information on a comment
     */
    public static class GetCommentInfo extends BoxRequestItem<BoxComment, GetCommentInfo> {

        /**
         * Creates a comment information request with the default parameters
         *
         * @param id            id of the comment to get information on
         * @param requestUrl    URL of the comment information endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public GetCommentInfo(String id, String requestUrl, BoxSession session) {
            super(BoxComment.class, id, requestUrl, session);
            mRequestMethod = Methods.GET;
        }
    }

    /**
     * Request for adding a reply comment to a comment
     */
    public static class AddReplyComment extends BoxRequestCommentAdd<BoxComment, AddReplyComment> {

        /**
         * Creates an add reply comment request with the default parameters
         *
         * @param itemId    id of the comment to add a comment to
         * @param message   message of the new comment
         * @param requestUrl    URL of the add comment endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public AddReplyComment(String itemId, String message, String requestUrl, BoxSession session) {
            super(BoxComment.class, requestUrl, session);
            setItemId(itemId);
            setItemType(BoxComment.TYPE);
            setMessage(message);
        }
    }

    /**
     * Request for updating the message on a comment
     */
    public static class UpdateComment extends BoxRequest<BoxComment, UpdateComment> {
        String mId;

        /**
         * Creates an update comment request with the default parameters
         *
         * @param id            id of the comment to update information on
         * @param requestUrl    URL of the update comment endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public UpdateComment(String id, String message, String requestUrl, BoxSession session) {
            super(BoxComment.class, requestUrl, session);
            mId = id;
            mRequestMethod = Methods.PUT;
            setMessage(message);
        }

        /**
         * Gets the id of the comment to be updated in the request
         *
         * @return  id of the comment that will be updated
         */
        public String getId() {
            return mId;
        }

        /**
         * Gets the message currently set as the new message for the comment in the update request
         *
         * @return  message to update the comment to
         */
        public String getMessage() {
            return (String) mBodyMap.get(BoxComment.FIELD_MESSAGE);
        }

        public UpdateComment setMessage(String message) {
            mBodyMap.put(BoxComment.FIELD_MESSAGE, message);
            return this;
        }
    }

    /**
     * Request for deleting a comment
     */
    public static class DeleteComment extends BoxRequest<BoxVoid, DeleteComment> {
        private final String mId;

        /**
         * Creates a delete comment request with the default parameters
         *
         * @param id            id of the comment to delete
         * @param requestUrl    URL of the delete comment endpoint
         * @param session       the authenticated session that will be used to make the request with
         */
        public DeleteComment(String id, String requestUrl, BoxSession session) {
            super(BoxVoid.class, requestUrl, session);
            mRequestMethod = Methods.DELETE;
            mId = id;
        }

        /**
         * Gets the id for the comment that will be deleted in the request
         *
         * @return  id of the comment to be deleted
         */
        public String getId() {
            return mId;
        }
    }

}
