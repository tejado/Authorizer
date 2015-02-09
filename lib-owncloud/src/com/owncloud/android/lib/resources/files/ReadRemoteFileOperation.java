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
package com.owncloud.android.lib.resources.files;

import java.util.ArrayList;

import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.WebdavEntry;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;


/**
 * Remote operation performing the read a file from the ownCloud server.
 * 
 * @author David A. Velasco
 * @author masensio
 */

public class ReadRemoteFileOperation extends RemoteOperation {

    private static final String TAG = ReadRemoteFileOperation.class.getSimpleName();
    private static final int SYNC_READ_TIMEOUT = 40000;
    private static final int SYNC_CONNECTION_TIMEOUT = 5000;
    
    private String mRemotePath;
    
	
    /**
     * Constructor
     * 
     * @param remotePath		Remote path of the file. 
     */
    public ReadRemoteFileOperation(String remotePath) {
    	mRemotePath = remotePath;
    }

    /**
     * Performs the read operation.
     * 
     * @param client		Client object to communicate with the remote ownCloud server.
     */
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
    	PropFindMethod propfind = null;
    	RemoteOperationResult result = null;

    	/// take the duty of check the server for the current state of the file there
    	try {
    		propfind = new PropFindMethod(client.getWebdavUri() + WebdavUtils.encodePath(mRemotePath),
    				DavConstants.PROPFIND_ALL_PROP,
    				DavConstants.DEPTH_0);
    		int status;
    		status = client.executeMethod(propfind, SYNC_READ_TIMEOUT, SYNC_CONNECTION_TIMEOUT);

    		boolean isSuccess = (
    				status == HttpStatus.SC_MULTI_STATUS ||
    				status == HttpStatus.SC_OK
			);
    		if (isSuccess) {
    			// Parse response
    			MultiStatus resp = propfind.getResponseBodyAsMultiStatus();
				WebdavEntry we = new WebdavEntry(resp.getResponses()[0], client.getWebdavUri().getPath());
				RemoteFile remoteFile = new RemoteFile(we);
				ArrayList<Object> files = new ArrayList<Object>();
				files.add(remoteFile);

    			// Result of the operation
    			result = new RemoteOperationResult(true, status, propfind.getResponseHeaders());
    			result.setData(files);
    			
    		} else {
    			client.exhaustResponse(propfind.getResponseBodyAsStream());
    			result = new RemoteOperationResult(false, status, propfind.getResponseHeaders());
    		}

    	} catch (Exception e) {
    		result = new RemoteOperationResult(e);
    		e.printStackTrace();
    		Log_OC.e(TAG, "Synchronizing  file " + mRemotePath + ": " + result.getLogMessage(), result.getException());
    	} finally {
    		if (propfind != null)
    			propfind.releaseConnection();
    	}
    	return result;
    }

}
