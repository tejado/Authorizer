/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud Inc.
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.resources.shares;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpStatus;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;


/** 
 * Get the data from the server to know shares
 * 
 * @author masensio
 *
 */

public class GetRemoteSharesOperation extends RemoteOperation {

	private static final String TAG = GetRemoteSharesOperation.class.getSimpleName();

	private ArrayList<OCShare> mShares;  // List of shares for result

	
	public GetRemoteSharesOperation() {
	}

	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {
		RemoteOperationResult result = null;
		int status = -1;

		// Get Method        
		GetMethod get = null;

		// Get the response
		try{
			get = new GetMethod(client.getBaseUri() + ShareUtils.SHARING_API_PATH);
			get.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);
			status = client.executeMethod(get);
			if(isSuccess(status)) {
				String response = get.getResponseBodyAsString();

				// Parse xml response --> obtain the response in ShareFiles ArrayList
				// convert String into InputStream
				InputStream is = new ByteArrayInputStream(response.getBytes());
				ShareXMLParser xmlParser = new ShareXMLParser();
				mShares = xmlParser.parseXMLResponse(is);
				if (mShares != null) {
					Log_OC.d(TAG, "Got " + mShares.size() + " shares");
					result = new RemoteOperationResult(ResultCode.OK);
					ArrayList<Object> sharesObjects = new ArrayList<Object>();
					for (OCShare share: mShares) {
						sharesObjects.add(share);
					}
					result.setData(sharesObjects);
				}
			} else {
				result = new RemoteOperationResult(false, status, get.getResponseHeaders());
			}
			
		} catch (Exception e) {
			result = new RemoteOperationResult(e);
			Log_OC.e(TAG, "Exception while getting remote shares ", e);
			
		} finally {
			if (get != null) {
				get.releaseConnection();
			}
		}
		return result;
	}

	private boolean isSuccess(int status) {
		return (status == HttpStatus.SC_OK);
	}


}
