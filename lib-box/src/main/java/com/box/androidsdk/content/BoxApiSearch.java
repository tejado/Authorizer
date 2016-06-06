package com.box.androidsdk.content;

import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsSearch;

/**
 * Represents the API of the search endpoint on Box. This class can be used to generate request objects
 * for each of the APIs exposed endpoints
 */
public class BoxApiSearch extends BoxApi {

    public BoxApiSearch(BoxSession session) {
        super(session);
    }

    /**
     * Gets a request to search
     *
     * @param query query to use for search
     * @return request to search
     */
    public BoxRequestsSearch.Search getSearchRequest(String query) {
        BoxRequestsSearch.Search request = new BoxRequestsSearch.Search(query, getSearchUrl(), mSession);
        return request;
    }

    protected String getSearchUrl() { return String.format("%s/search", getBaseUri()); }

}
