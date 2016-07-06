package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.models.BoxListItems;
import com.box.androidsdk.content.utils.BoxDateFormat;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.utils.SdkUtils;

import java.util.Date;
import java.util.Locale;

/**
 * Search requests.
 */
public class BoxRequestsSearch {

    /**
     * Request for searching.
     */
    public static class Search extends BoxRequest<BoxListItems, Search> {

        /**
         * Only search in names.
         */
        public static String CONTENT_TYPE_NAME = "name";
        /**
         * Only search in descriptions.
         */
        public static String CONTENT_TYPE_DESCRIPTION = "description";
        /**
         * Only search in comments.
         */
        public static String CONTENT_TYPE_COMMENTS = "comments";
        /**
         * Only search in file contents.
         */
        public static String CONTENT_TYPE_FILE_CONTENTS = "file_content";
        /**
         * Only search in tags.
         */
        public static String CONTENT_TYPE_TAGS = "tags";

        /**
         * Field for setting the search query.
         */
        protected static final String FIELD_QUERY = "query";
        /**
         * Field for setting the scope for admins only.
         */
        protected static final String FIELD_SCOPE = "scope";
        /**
         * Field for setting the file extensions.
         */
        protected static final String FIELD_FILE_EXTENSIONS = "file_extensions";
        /**
         * Field for setting the created at range.
         */
        protected static final String FIELD_CREATED_AT_RANGE = "created_at_range";
        /**
         * Field for setting the updated at range.
         */
        protected static final String FIELD_UPDATED_AT_RANGE = "updated_at_range";
        /**
         * Field for setting the size range.
         */
        protected static final String FIELD_SIZE_RANGE = "size_range";
        /**
         * Field for setting the owner user ids.
         */
        protected static final String FIELD_OWNER_USER_IDS = "owner_user_ids";
        /**
         * Field for setting the ancestor folder ids.
         */
        protected static final String FIELD_ANCESTOR_FOLDER_IDS = "ancestor_folder_ids";
        /**
         * Field for setting the content types: name, description, file_content, comments, or tags.
         */
        protected static final String FIELD_CONTENT_TYPES = "content_types";
        /**
         * Field for setting return type: file, folder, or web_link.
         */
        protected static final String FIELD_TYPE = "type";
        /**
         * Field for setting number of search results to return (default=30, max =200)
         */
        protected static final String FIELD_LIMIT = "limit";
        /**
         * The search result at which to start the response. (default=0)
         */
        protected static final String FIELD_OFFSET = "offset";


        public static enum Scope {
            USER_CONTENT,
            /**
             * This scope only works for administrator, and may need special permission. Please check
             * https://developers.box.com/docs/#search for details.
             */
            ENTERPRISE_CONTENT
        }

        /**
         * Creates a search request with the default parameters.
         *
         * @param query      query to search for.
         * @param requestUrl URL of the search endpoint.
         * @param session    the authenticated session that will be used to make the request with
         */
        public Search(String query, String requestUrl, BoxSession session) {
            super(BoxListItems.class, requestUrl, session);
            limitValueForKey(FIELD_QUERY, query);
            mRequestMethod = Methods.GET;
        }

        /**
         * limit key to certain value.
         */
        public Search limitValueForKey(String key, String value) {
            mQueryMap.put(key, value);
            return this;
        }

        /**
         * limit search scope. Please check
         * https://developers.box.com/docs/#search for details.
         */
        public Search limitSearchScope(Scope scope) {
            limitValueForKey(FIELD_SCOPE, scope.name().toLowerCase(Locale.US));
            return this;
        }

        /**
         * limit file search to given file extensions.
         */
        public Search limitFileExtensions(String[] extensions) {
            limitValueForKey(FIELD_FILE_EXTENSIONS, SdkUtils.concatStringWithDelimiter(extensions, ","));
            return this;
        }

        /**
         * Limit the search to creation time between fromDate to toDate.
         *
         * @param fromDate can use null if you don't want to restrict this.
         * @param toDate   can use null if you don't want to restrict this.
         */
        public Search limitCreationTime(Date fromDate, Date toDate) {
            addTimeRange(FIELD_CREATED_AT_RANGE, fromDate, toDate);
            return this;
        }

        /**
         * Limit the search to last update time between fromDate to toDate.
         *
         * @param fromDate can use null if you don't want to restrict this.
         * @param toDate   can use null if you don't want to restrict this.
         */
        public Search limitLastUpdateTime(Date fromDate, Date toDate) {
            addTimeRange(FIELD_UPDATED_AT_RANGE, fromDate, toDate);
            return this;
        }

        /**
         * Limit the search to file size greater than minSize in bytes and less than maxSize in bytes.
         */
        public Search limitSizeRange(long minSize, long maxSize) {
            limitValueForKey(FIELD_SIZE_RANGE, String.format("%d,%d", minSize, maxSize));
            return this;
        }

        /**
         * limit the search to items owned by given users.
         */
        public Search limitOwnerUserIds(String[] userIds) {
            limitValueForKey(FIELD_OWNER_USER_IDS, SdkUtils.concatStringWithDelimiter(userIds, ","));
            return this;
        }

        /**
         * Limit searches to specific ancestor folders.
         */
        public Search limitAncestorFolderIds(String[] folderIds) {
            limitValueForKey(FIELD_ANCESTOR_FOLDER_IDS, SdkUtils.concatStringWithDelimiter(folderIds, ","));
            return this;
        }

        /**
         * Limit search to certain content types. The allowed content type strings are defined as final static in this class.
         * e.g. Search.CONTENT_TYPE_NAME, Search.CONTENT_TYPE_DESCRIPTION...
         */
        public Search limitContentTypes(String[] contentTypes) {
            limitValueForKey(FIELD_CONTENT_TYPES, SdkUtils.concatStringWithDelimiter(contentTypes, ","));
            return this;
        }

        /**
         * The type you want to return in your search. Can be BoxFile.TYPE, BoxFolder.TYPE, BoxBookmark.TYPE.
         */
        public Search limitType(String type) {
            limitValueForKey(FIELD_TYPE, type);
            return this;
        }

        /**
         * Sets the limit of items that should be returned
         *
         * @param limit limit of items to return
         * @return the get folder items request
         */
        public Search setLimit(int limit) {
            limitValueForKey(FIELD_LIMIT, String.valueOf(limit));
            return this;
        }

        /**
         * Sets the offset of the items that should be returned
         *
         * @param offset offset of items to return
         * @return the offset of the items to return
         */
        public Search setOffset(int offset) {
            limitValueForKey(FIELD_OFFSET, String.valueOf(offset));
            return this;
        }

        /**
         * @return the minimum last updated at date set in this request if this request was limited, null otherwise.
         */
        public Date getLastUpdatedAtDateRangeFrom() {
            return returnFromDate(FIELD_UPDATED_AT_RANGE);
        }

        /**
         * @return the maximum last updated at date set in this request if this request was limited, null otherwise.
         */
        public Date getLastUpdatedAtDateRangeTo() {
            return returnToDate(FIELD_UPDATED_AT_RANGE);
        }

        /**
         * @return the minimum created at date set in this request if this request was limited, null otherwise.
         */
        public Date getCreatedAtDateRangeFrom() {
            return returnFromDate(FIELD_CREATED_AT_RANGE);
        }

        /**
         * @return the maximum created at date set in this request if this request was limited, null otherwise.
         */
        public Date getCreatedAtDateRangeTo() {
            return returnToDate(FIELD_CREATED_AT_RANGE);
        }

        /**
         * @return the minimum size set in this request if this request was limited, null otherwise.
         */
        public Long getSizeRangeFrom() {
            String range = mQueryMap.get(FIELD_SIZE_RANGE);
            if (SdkUtils.isEmptyString(range)) {
                return null;
            }
            String[] ranges = range.split(",");
            return Long.parseLong(ranges[0]);
        }

        /**
         * @return the maximum size set in this request if this request was limited, null otherwise.
         */
        public Long getSizeRangeTo() {
            String range = mQueryMap.get(FIELD_SIZE_RANGE);
            if (SdkUtils.isEmptyString(range)) {
                return null;
            }
            String[] ranges = range.split(",");
            return Long.parseLong(ranges[1]);
        }

        /**
         * @return the owner user ids this request is limited to.
         */
        public String[] getOwnerUserIds() {
            return getStringArray(FIELD_OWNER_USER_IDS);
        }

        /**
         * @return the ancestor folder ids this request is limited to.
         */
        public String[] getAncestorFolderIds() {
            return getStringArray(FIELD_ANCESTOR_FOLDER_IDS);
        }

        /**
         * @return the content types this request is limited to.
         */
        public String[] getContentTypes() {
            return getStringArray(FIELD_CONTENT_TYPES);
        }

        /**
         * @return the type this search is limited to.
         */
        public String getType() {
            return mQueryMap.get(FIELD_TYPE);
        }

        /**
         * @return the number of searches to return.
         */
        public Integer getLimit() {
            String limit = mQueryMap.get(FIELD_LIMIT);
            if (limit != null){
                try {
                    return Integer.parseInt(limit);
                } catch (NumberFormatException e){
                    return null;
                }
            } else {
                return null;
            }
        }

        /**
         * @return the offset set for this search query.
         */
        public Integer getOffset() {
            String offset = mQueryMap.get(FIELD_OFFSET);
            if (offset != null){
                try {
                    return Integer.parseInt(offset);
                } catch (NumberFormatException e){
                    return null;
                }
            } else {
                return null;
            }
        }

        /**
         * @return the query this request will search for.
         */
        public String getQuery() {
            return mQueryMap.get(FIELD_QUERY);
        }

        /**
         * @return the search scope limitation of this request
         */
        public String getScope() {
            return mQueryMap.get(FIELD_SCOPE);
        }

        /**
         * @return the file extensions this search is limited to separated by commas.
         */
        public String[] getFileExtensions() {
            return getStringArray(FIELD_FILE_EXTENSIONS);
        }

        private String[] getStringArray(final String key){
            String types = mQueryMap.get(key);
            if (SdkUtils.isEmptyString(types)) {
                return null;
            }
            return types.split(",");
        }


        private Date returnFromDate(final String timeRangeKey) {
            String range = mQueryMap.get(timeRangeKey);
            if (!SdkUtils.isEmptyString(range)) {
                return BoxDateFormat.getTimeRangeDates(range)[0];
            }
            return null;
        }

        private Date returnToDate(final String timeRangeKey) {
            String range = mQueryMap.get(timeRangeKey);
            if (!SdkUtils.isEmptyString(range)) {
                return BoxDateFormat.getTimeRangeDates(range)[1];
            }
            return null;
        }

        private void addTimeRange(String key, Date fromDate, Date toDate) {
            String range = BoxDateFormat.getTimeRangeString(fromDate, toDate);
            if (!SdkUtils.isEmptyString(range)) {
                limitValueForKey(key, range);
            }
        }
    }

}
