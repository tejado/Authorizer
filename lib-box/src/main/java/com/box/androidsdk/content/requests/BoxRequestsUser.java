package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.models.BoxListUsers;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxUser;
import com.box.androidsdk.content.models.BoxVoid;
import com.eclipsesource.json.JsonObject;

import java.util.Map;

/**
 * User requests.
 */
public class BoxRequestsUser {

    /**
     * Request for retrieving information on the current user
     */
    public static class GetUserInfo extends BoxRequestItem<BoxUser, GetUserInfo> {

        /**
         * Creates a get user information request with the default parameters
         *
         * @param requestUrl URL of the user information endpoint
         * @param session    the authenticated session that will be used to make the request with
         */
        public GetUserInfo(String requestUrl, BoxSession session) {
            super(BoxUser.class, null, requestUrl, session);
            mRequestMethod = Methods.GET;
        }
    }

    /**
     * Request to get users that belong to the admins enterprise
     */
    public static class GetEnterpriseUsers extends BoxRequestItem<BoxListUsers, GetEnterpriseUsers> {
        protected static final String QUERY_FILTER_TERM = "filter_term";
        protected static final String QUERY_LIMIT = "limit";
        protected static final String QUERY_OFFSET = "offset";

        /**
         * Creates a get enterprise users request with the default parameters
         *
         * @param requestUrl URL of the enterprise users endpoint
         * @param session    the authenticated session that will be used to make the request with
         */
        public GetEnterpriseUsers(String requestUrl, BoxSession session) {
            super(BoxListUsers.class, null, requestUrl, session);
            mRequestMethod = Methods.GET;
        }

        /**
         * Gets the string used to filter the results to only users starting with the filter term in either the name or the login
         *
         * @return string used to filter the results to only users starting with the filter term in either the name or the login
         */
        public String getFilterTerm() {
            return mQueryMap.get(QUERY_FILTER_TERM);
        }

        /**
         * Sets the string used to filter the results to only users starting with the filter term in either the name or the login
         *
         * @param filterTerm the string used to filter the results
         * @return The get enterprise users request
         */
        public GetEnterpriseUsers setFilterTerm(String filterTerm) {
            mQueryMap.put(QUERY_FILTER_TERM, filterTerm);
            return this;
        }

        /**
         * Gets the number of records to return.
         *
         * @return the number of records to return.
         */
        public long getLimit() {
            return Long.valueOf(mQueryMap.get(QUERY_LIMIT));
        }

        /**
         * Sets the number of records to return.
         *
         * @param limit the number of records to return.
         * @return The get enterprise users request
         */
        public GetEnterpriseUsers setLimit(long limit) {
            mQueryMap.put(QUERY_LIMIT, Long.toString(limit));
            return this;
        }

        /**
         * Gets the record at which to start
         *
         * @return the record at which to start
         */
        public long getOffset() {
            return Long.valueOf(mQueryMap.get(QUERY_OFFSET));
        }

        /**
         * Sets the record at which to start
         *
         * @param offset the record at which to start
         * @return The get enterprise users request
         */
        public GetEnterpriseUsers setOffset(long offset) {
            mQueryMap.put(QUERY_OFFSET, Long.toString(offset));
            return this;
        }
    }

    /**
     * Request to create an enterprise user
     */
    public static class CreateEnterpriseUser extends BoxRequestUserUpdate<BoxUser, CreateEnterpriseUser> {

        /**
         * Creates a create enterprise user request with the default parameters
         *
         * @param requestUrl URL of the create enterprise user endpoint
         * @param session    the authenticated session that will be used to make the request with
         */
        public CreateEnterpriseUser(String requestUrl, BoxSession session, String login, String name) {
            super(BoxUser.class, null, requestUrl, session);
            mRequestMethod = Methods.POST;
            setLogin(login);
            setName(name);
        }

        /**
         * Gets the email address this user uses to login
         *
         * @return the users login
         */
        public String getLogin() {
            return (String) mBodyMap.get(BoxUser.FIELD_LOGIN);
        }

        /**
         * Sets the email address for the user that will be created
         *
         * @param login the email address this user uses to login
         * @return the create enterprise user request
         */
        public CreateEnterpriseUser setLogin(String login) {
            mBodyMap.put(BoxUser.FIELD_LOGIN, login);
            return this;
        }
    }

    /**
     * Request for updating a users information
     */
    public static class UpdateUserInformation extends BoxRequestUserUpdate<BoxUser, UpdateUserInformation> {

        protected static final String FIELD_IS_PASSWORD_RESET_REQUIRED = "is_password_reset_required";

        /**
         * Creates an update user information request with the default parameters
         *
         * @param requestUrl URL of the update user information endpoint
         * @param session    the authenticated session that will be used to make the request with
         */
        public UpdateUserInformation(String requestUrl, BoxSession session, String login, String name) {
            super(BoxUser.class, null, requestUrl, session);
            mRequestMethod = Methods.PUT;
        }

        public String getEnterprise() {
            return (String) mBodyMap.get(BoxUser.FIELD_ENTERPRISE);
        }

        /**
         * Sets the enterprise the user belongs to. Setting this to null will roll the user out of the enterprise
         * and make them a free user.
         *
         * @param enterprise the enterprise the user will be associated with.
         * @return the update user information request.
         */
        public UpdateUserInformation setEnterprise(String enterprise) {
            mBodyMap.put(BoxUser.FIELD_ENTERPRISE, enterprise);
            return this;
        }

        /**
         * Gets whether or not the user is required to reset password
         *
         * @return whether or not the user is required to reset password
         */
        public String getIsPasswordResetRequired() {
            return (String) mBodyMap.get(FIELD_IS_PASSWORD_RESET_REQUIRED);
        }

        /**
         * Sets whether or not the user is required to reset password
         *
         * @param isPasswordResetRequired whether or not the user is required to reset password
         * @return the update user information request
         */
        public UpdateUserInformation setIsPasswordResetRequired(boolean isPasswordResetRequired) {
            mBodyMap.put(FIELD_IS_PASSWORD_RESET_REQUIRED, isPasswordResetRequired);
            return this;
        }


        @Override
        protected void parseHashMapEntry(JsonObject jsonBody, Map.Entry<String, Object> entry) {
            if (entry.getKey().equals(BoxUser.FIELD_ENTERPRISE)) {
                jsonBody.add(entry.getKey(), entry.getValue() == null ?
                        null : // setting an enterprise to null removes the user from the enterprise
                        (String) entry.getValue());
                return;
            }
            super.parseHashMapEntry(jsonBody, entry);
        }

    }

    /**
     * Request for deleting an enterprise user
     */
    public static class DeleteEnterpriseUser extends BoxRequest<BoxVoid, DeleteEnterpriseUser> {

        protected static final String QUERY_NOTIFY = "notify";
        protected static final String QUERY_FORCE = "force";

        protected String mId;

        /**
         * Creates an update user information request with the default parameters
         *
         * @param requestUrl URL of the update user information endpoint
         * @param session    the authenticated session that will be used to make the request with
         */
        public DeleteEnterpriseUser(String requestUrl, BoxSession session, String userId) {
            super(BoxVoid.class, requestUrl, session);
            mRequestMethod = Methods.DELETE;
            mId = userId;
        }

        /**
         * Gets the id of the user to delete
         *
         * @return id of the user to delete
         */
        public String getId() {
            return mId;
        }

        /**
         * Gets whether the destination user should receive email notification of the delete.
         *
         * @return whether or not the user should be notified
         */
        public Boolean getShouldNotify() {
            return Boolean.valueOf(mQueryMap.get(QUERY_NOTIFY));
        }

        /**
         * Sets whether or not the destination user should receive email notification of the delete.
         *
         * @param shouldNotify whether or not the user should receive an email notification
         * @return the delete enterprise user request
         */
        public DeleteEnterpriseUser setShouldNotify(Boolean shouldNotify) {
            mQueryMap.put(QUERY_NOTIFY, Boolean.toString(shouldNotify));
            return this;
        }

        /**
         * Gets whether or not the user should be deleted even if this user still owns files
         *
         * @return whether or not the user should be deleted even if this user still owns files
         */
        public Boolean getShouldForce() {
            return Boolean.valueOf(mQueryMap.get(QUERY_FORCE));
        }

        /**
         * Sets whether or not the user should be deleted even if this user still owns files
         *
         * @param shouldForce whether or not the user should be deleted even if this user still owns files
         * @return the delete enterprise user request
         */
        public DeleteEnterpriseUser setShouldForce(Boolean shouldForce) {
            mQueryMap.put(QUERY_FORCE, Boolean.toString(shouldForce));
            return this;
        }
    }

}
