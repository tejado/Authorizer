/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.owncloud.android.lib.resources.files;

import java.util.ArrayList;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONArray;
import org.json.JSONObject;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

/**
 * The SearchOperation searches for files matching a query
 */
@SuppressWarnings("ALL")
public class SearchOperation extends RemoteOperation
{
    /** Information for each search result */
    public static class Result
    {
        public final String itsType;
        public final String itsPath;

        /** Constructor */
        public Result(String type, String path)
        {
            itsType = type;
            itsPath = path;
        }
    }

    private static final String TAG = SearchOperation.class.getSimpleName();

    private static final String URI = "/index.php/search/ajax/search.php";

    private final String itsQuery;

    /** Constructor */
    public SearchOperation(String query)
    {
        itsQuery = query;
    }

    /* (non-Javadoc)
     * @see com.owncloud.android.lib.common.operations.RemoteOperation#run(com.owncloud.android.lib.common.OwnCloudClient)
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client)
    {
        RemoteOperationResult result = null;
        GetMethod get = null;

        try {
            get = new GetMethod(client.getBaseUri() + URI);
            get.setQueryString(new NameValuePair[] {
                    new NameValuePair("query", itsQuery) });
            int status = client.executeMethod(get);
            if (checkSuccess(status, get)) {
                Log_OC.d(TAG, "Successful response len: " +
                         get.getResponseContentLength());

                result = new RemoteOperationResult(
                        true, status, get.getResponseHeaders());
                ArrayList<Object> data = new ArrayList<Object>();

                JSONArray respJson = new JSONArray(
                        get.getResponseBodyAsString());
                for (int i = 0; i < respJson.length(); ++i) {
                    JSONObject item = respJson.getJSONObject(i);
                    data.add(new Result(item.optString("type", null),
                                        item.optString("path", null)));
                }

                result.setData(data);
            } else {
                result = new RemoteOperationResult(
                        false, status, get.getResponseHeaders());
            }
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Exception during search", e);
        } finally {
            if (get != null) {
                get.releaseConnection();
            }
        }

        return result;
    }

    /** Check whether the request was successful */
    private boolean checkSuccess(int status, GetMethod request)
            throws Exception
    {
        if (status != HttpStatus.SC_OK) {
            return false;
        }

        Header header = request.getResponseHeader("content-type");
        if (header == null) {
            throw new Exception("No content-type header");
        }

        //noinspection LoopStatementThatDoesntLoop
        do {
            HeaderElement[] elements = header.getElements();
            if (elements.length != 1) {
                break;
            }
            String type = elements[0].getName();
            if (!type.equalsIgnoreCase("application/json")) {
                break;
            }

            return true;
        } while(false);
        throw new Exception("Unsupported content type: " + header.getValue());
    }
}
