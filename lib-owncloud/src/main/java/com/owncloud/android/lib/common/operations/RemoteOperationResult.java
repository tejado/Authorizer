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

package com.owncloud.android.lib.common.operations;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.net.ssl.SSLException;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.jackrabbit.webdav.DavException;
import org.json.JSONException;

import android.accounts.Account;
import android.accounts.AccountsException;

import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;
import com.owncloud.android.lib.common.network.CertificateCombinedException;
import com.owncloud.android.lib.common.utils.Log_OC;


/**
 * The result of a remote operation required to an ownCloud server.
 * 
 * Provides a common classification of remote operation results for all the
 * application.
 * 
 * @author David A. Velasco
 */
public class RemoteOperationResult implements Serializable {
	
	/** Generated - should be refreshed every time the class changes!! */;
	private static final long serialVersionUID = -9003837206000993465L;
	
	private static final String TAG = "RemoteOperationResult";
    
    public enum ResultCode { 
        OK,
        OK_SSL,
        OK_NO_SSL,
        UNHANDLED_HTTP_CODE,
        UNAUTHORIZED,        
        FILE_NOT_FOUND, 
        INSTANCE_NOT_CONFIGURED, 
        UNKNOWN_ERROR, 
        WRONG_CONNECTION,  
        TIMEOUT, 
        INCORRECT_ADDRESS, 
        HOST_NOT_AVAILABLE, 
        NO_NETWORK_CONNECTION, 
        SSL_ERROR,
        SSL_RECOVERABLE_PEER_UNVERIFIED,
        BAD_OC_VERSION,
        CANCELLED, 
        INVALID_LOCAL_FILE_NAME, 
        INVALID_OVERWRITE,
        CONFLICT, 
        OAUTH2_ERROR,
        SYNC_CONFLICT,
        LOCAL_STORAGE_FULL, 
        LOCAL_STORAGE_NOT_MOVED, 
        LOCAL_STORAGE_NOT_COPIED, 
        OAUTH2_ERROR_ACCESS_DENIED,
        QUOTA_EXCEEDED, 
        ACCOUNT_NOT_FOUND, 
        ACCOUNT_EXCEPTION, 
        ACCOUNT_NOT_NEW, 
        ACCOUNT_NOT_THE_SAME,
        INVALID_CHARACTER_IN_NAME,
        SHARE_NOT_FOUND,
		LOCAL_STORAGE_NOT_REMOVED,
		FORBIDDEN,
		SHARE_FORBIDDEN,
		OK_REDIRECT_TO_NON_SECURE_CONNECTION, 
		INVALID_MOVE_INTO_DESCENDANT, 
		PARTIAL_MOVE_DONE
    }

    private boolean mSuccess = false;
    private int mHttpCode = -1;
    private Exception mException = null;
    private ResultCode mCode = ResultCode.UNKNOWN_ERROR;
    private String mRedirectedLocation;
    private String mAuthenticate;

    private ArrayList<Object> mData;

    public RemoteOperationResult(ResultCode code) {
        mCode = code;
		mSuccess = (code == ResultCode.OK || code == ResultCode.OK_SSL || code == ResultCode.OK_NO_SSL || code == ResultCode.OK_REDIRECT_TO_NON_SECURE_CONNECTION);
        mData = null;
    }

    private RemoteOperationResult(boolean success, int httpCode) {
        mSuccess = success;
        mHttpCode = httpCode;

        if (success) {
            mCode = ResultCode.OK;

        } else if (httpCode > 0) {
            switch (httpCode) {
            case HttpStatus.SC_UNAUTHORIZED:
                mCode = ResultCode.UNAUTHORIZED;
                break;
            case HttpStatus.SC_NOT_FOUND:
                mCode = ResultCode.FILE_NOT_FOUND;
                break;
            case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                mCode = ResultCode.INSTANCE_NOT_CONFIGURED;
                break;
            case HttpStatus.SC_CONFLICT:
                mCode = ResultCode.CONFLICT;
                break;
            case HttpStatus.SC_INSUFFICIENT_STORAGE:
                mCode = ResultCode.QUOTA_EXCEEDED;
                break;
			case HttpStatus.SC_FORBIDDEN:
				mCode = ResultCode.FORBIDDEN;
				break;
            default:
                mCode = ResultCode.UNHANDLED_HTTP_CODE;
                Log_OC.d(TAG, "RemoteOperationResult has processed UNHANDLED_HTTP_CODE: " + httpCode);
            }
        }
    }
    
    public RemoteOperationResult(boolean success, int httpCode, Header[] headers) {
        this(success, httpCode);
        if (headers != null) {
            Header current;
            for (int i=0; i<headers.length; i++) {
                current = headers[i];
                if ("location".equals(current.getName().toLowerCase())) {
                    mRedirectedLocation = current.getValue();
                    continue;
                }
                if ("www-authenticate".equals(current.getName().toLowerCase())) {
                	mAuthenticate = current.getValue();
                	continue;
                }
            }
        }
    }    

    public RemoteOperationResult(Exception e) {
        mException = e;

        if (e instanceof OperationCancelledException) {
            mCode = ResultCode.CANCELLED;

        } else if (e instanceof SocketException) {
            mCode = ResultCode.WRONG_CONNECTION;

        } else if (e instanceof SocketTimeoutException) {
            mCode = ResultCode.TIMEOUT;

        } else if (e instanceof ConnectTimeoutException) {
            mCode = ResultCode.TIMEOUT;

        } else if (e instanceof MalformedURLException) {
            mCode = ResultCode.INCORRECT_ADDRESS;

        } else if (e instanceof UnknownHostException) {
            mCode = ResultCode.HOST_NOT_AVAILABLE;

        } else if (e instanceof AccountNotFoundException) {
            mCode = ResultCode.ACCOUNT_NOT_FOUND;
            
        } else if (e instanceof AccountsException) {
            mCode = ResultCode.ACCOUNT_EXCEPTION;
            
        } else if (e instanceof SSLException || e instanceof RuntimeException) {
            CertificateCombinedException se = getCertificateCombinedException(e);
            if (se != null) {
                mException = se;
                if (se.isRecoverable()) {
                    mCode = ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED;
                }
            } else if (e instanceof RuntimeException) {
                mCode = ResultCode.HOST_NOT_AVAILABLE;

            } else {
                mCode = ResultCode.SSL_ERROR;
            }

        } else {
            mCode = ResultCode.UNKNOWN_ERROR;
        }

    }


    public void setData(ArrayList<Object> files){
    	mData = files;
    }
    
	public ArrayList<Object> getData(){
		return mData;
	}
    
    public boolean isSuccess() {
        return mSuccess;
    }

    public boolean isCancelled() {
        return mCode == ResultCode.CANCELLED;
    }

    public int getHttpCode() {
        return mHttpCode;
    }

    public ResultCode getCode() {
        return mCode;
    }

    public Exception getException() {
        return mException;
    }

    public boolean isSslRecoverableException() {
        return mCode == ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED;
    }

	public boolean isRedirectToNonSecureConnection() {
		return mCode == ResultCode.OK_REDIRECT_TO_NON_SECURE_CONNECTION;
	}

    private CertificateCombinedException getCertificateCombinedException(Exception e) {
        CertificateCombinedException result = null;
        if (e instanceof CertificateCombinedException) {
            return (CertificateCombinedException) e;
        }
        Throwable cause = mException.getCause();
        Throwable previousCause = null;
        while (cause != null && cause != previousCause && !(cause instanceof CertificateCombinedException)) {
            previousCause = cause;
            cause = cause.getCause();
        }
        if (cause != null && cause instanceof CertificateCombinedException) {
            result = (CertificateCombinedException) cause;
        }
        return result;
    }

    public String getLogMessage() {

        if (mException != null) {
            if (mException instanceof OperationCancelledException) {
                return "Operation cancelled by the caller";

            } else if (mException instanceof SocketException) {
                return "Socket exception";

            } else if (mException instanceof SocketTimeoutException) {
                return "Socket timeout exception";

            } else if (mException instanceof ConnectTimeoutException) {
                return "Connect timeout exception";

            } else if (mException instanceof MalformedURLException) {
                return "Malformed URL exception";

            } else if (mException instanceof UnknownHostException) {
                return "Unknown host exception";

            } else if (mException instanceof CertificateCombinedException) {
                if (((CertificateCombinedException) mException).isRecoverable())
                    return "SSL recoverable exception";
                else
                    return "SSL exception";

            } else if (mException instanceof SSLException) {
                return "SSL exception";

            } else if (mException instanceof DavException) {
                return "Unexpected WebDAV exception";

            } else if (mException instanceof HttpException) {
                return "HTTP violation";

            } else if (mException instanceof IOException) {
                return "Unrecovered transport exception";

            } else if (mException instanceof AccountNotFoundException) {
                Account failedAccount = ((AccountNotFoundException)mException).getFailedAccount();
                return mException.getMessage() + " (" + (failedAccount != null ? failedAccount.name : "NULL") + ")";
                
            } else if (mException instanceof AccountsException) {
                return "Exception while using account";
                
            } else if (mException instanceof JSONException) {
            	return "JSON exception";
            	
            } else {
                return "Unexpected exception";
            }
        }

        if (mCode == ResultCode.INSTANCE_NOT_CONFIGURED) {
            return "The ownCloud server is not configured!";

        } else if (mCode == ResultCode.NO_NETWORK_CONNECTION) {
            return "No network connection";

        } else if (mCode == ResultCode.BAD_OC_VERSION) {
            return "No valid ownCloud version was found at the server";

        } else if (mCode == ResultCode.LOCAL_STORAGE_FULL) {
            return "Local storage full";

        } else if (mCode == ResultCode.LOCAL_STORAGE_NOT_MOVED) {
            return "Error while moving file to final directory";

        } else if (mCode == ResultCode.ACCOUNT_NOT_NEW) {
            return "Account already existing when creating a new one";

        } else if (mCode == ResultCode.ACCOUNT_NOT_THE_SAME) {
            return "Authenticated with a different account than the one updating";
        } else if (mCode == ResultCode.INVALID_CHARACTER_IN_NAME) {
                return "The file name contains an forbidden character";
        }

        return "Operation finished with HTTP status code " + mHttpCode + " (" + (isSuccess() ? "success" : "fail") + ")";

    }

    public boolean isServerFail() {
        return (mHttpCode >= HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    public boolean isException() {
        return (mException != null);
    }

    public boolean isTemporalRedirection() {
        return (mHttpCode == 302 || mHttpCode == 307);
    }

    public String getRedirectedLocation() {
        return mRedirectedLocation;
    }
    
    public boolean isIdPRedirection() {
        return (mRedirectedLocation != null &&
                (mRedirectedLocation.toUpperCase().contains("SAML") || 
                mRedirectedLocation.toLowerCase().contains("wayf")));
    }
    
	/**
	 * Checks if is a non https connection
	 * 
	 * @return boolean true/false
	 */
	public boolean isNonSecureRedirection() {
		return (mRedirectedLocation != null && !(mRedirectedLocation.toLowerCase().startsWith("https://")));
	}

    public String getAuthenticateHeader() {
    	return mAuthenticate;
    }

}
