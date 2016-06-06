package com.box.androidsdk.content;

import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsComment;

/**
 * Represents the API of the comment endpoint on Box. This class can be used to generate request objects
 * for each of the APIs exposed endpoints
 */
public class BoxApiComment extends BoxApi {

    /**
     * Constructs a BoxApiComment with the provided BoxSession
     *
     * @param session authenticated session to use with the BoxApiComment
     */
    public BoxApiComment(BoxSession session) {
        super(session);
    }

    public static final String COMMENTS_ENDPOINT = "/comments";

    protected String getCommentsUrl() { return getBaseUri() + COMMENTS_ENDPOINT; }
    protected String getCommentInfoUrl(String id) { return String.format("%s/%s", getCommentsUrl(), id); }

    /**
     * Gets a request that retrieves information on a comment
     *
     * @param id    id of comment to retrieve info on
     * @return      request to get a comment's information
     */
    public BoxRequestsComment.GetCommentInfo getInfoRequest(String id) {
        BoxRequestsComment.GetCommentInfo request = new BoxRequestsComment.GetCommentInfo(id, getCommentInfoUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that adds a reply comment to a comment
     *
     * @param commentId    id of the comment to reply to
     * @param message   message for the comment that will be added
     * @return  request to add a reply comment to a comment
     */
    public BoxRequestsComment.AddReplyComment getAddCommentReplyRequest(String commentId, String message) {
        BoxRequestsComment.AddReplyComment request = new BoxRequestsComment.AddReplyComment(commentId, message, getCommentsUrl(), mSession);
        return request;
    }

    /**
     * Gets a request that updates a comment's information
     *
     * @param id    id of comment to update information on
     * @param newMessage    new message for the comment
     * @return      request to update a comment's information
     */
    public BoxRequestsComment.UpdateComment getUpdateRequest(String id, String newMessage) {
        BoxRequestsComment.UpdateComment request = new BoxRequestsComment.UpdateComment(id, newMessage, getCommentInfoUrl(id), mSession);
        return request;
    }

    /**
     * Gets a request that deletes a comment
     *
     * @param id        id of comment to delete
     * @return      request to delete a comment
     */
    public BoxRequestsComment.DeleteComment getDeleteRequest(String id) {
        BoxRequestsComment.DeleteComment request = new BoxRequestsComment.DeleteComment(id, getCommentInfoUrl(id), mSession);
        return request;
    }


}