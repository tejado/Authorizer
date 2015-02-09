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

package com.owncloud.android.lib.common.network;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLSocket;

import com.owncloud.android.lib.common.utils.Log_OC;


/**
 * Enables the support of Server Name Indication if existing 
 * in the underlying network implementation.
 * 
 * Build as a singleton.
 * 
 * @author David A. Velasco
 */
public class ServerNameIndicator {
	
	private static final String TAG = ServerNameIndicator.class.getSimpleName();
	
	private static final AtomicReference<ServerNameIndicator> mSingleInstance = new AtomicReference<ServerNameIndicator>();
	
	private static final String METHOD_NAME = "setHostname";
	
	private final WeakReference<Class<?>> mSSLSocketClassRef;
	private final WeakReference<Method> mSetHostnameMethodRef;
	
	
	/**
	 * Private constructor, class is a singleton.
	 * 
	 * @param sslSocketClass		Underlying implementation class of {@link SSLSocket} used to connect with the server. 
	 * @param setHostnameMethod		Name of the method to call to enable the SNI support.
	 */
	private ServerNameIndicator(Class<?> sslSocketClass, Method setHostnameMethod) {
		mSSLSocketClassRef = new WeakReference<Class<?>>(sslSocketClass);
		mSetHostnameMethodRef = (setHostnameMethod == null) ? null : new WeakReference<Method>(setHostnameMethod);
	}
	
	
	/**
	 * Calls the {@code #setHostname(String)} method of the underlying implementation 
	 * of {@link SSLSocket} if exists.
	 * 
	 * Creates and initializes the single instance of the class when needed
	 *
	 * @param hostname 		The name of the server host of interest.
	 * @param sslSocket 	Client socket to connect with the server.
	 */
	public static void setServerNameIndication(String hostname, SSLSocket sslSocket) {
		final Method setHostnameMethod = getMethod(sslSocket);
		if (setHostnameMethod != null) {
			try {
				setHostnameMethod.invoke(sslSocket, hostname);
				Log_OC.i(TAG, "SNI done, hostname: " + hostname);
				
			} catch (IllegalArgumentException e) {
				Log_OC.e(TAG, "Call to SSLSocket#setHost(String) failed ", e);
				
			} catch (IllegalAccessException e) {
				Log_OC.e(TAG, "Call to SSLSocket#setHost(String) failed ", e);
				
			} catch (InvocationTargetException e) {
				Log_OC.e(TAG, "Call to SSLSocket#setHost(String) failed ", e);
			}
		} else {
			Log_OC.i(TAG, "SNI not supported");
		}
	}

	
	/**
	 * Gets the method to invoke trying to minimize the effective 
	 * application of reflection.
	 * 
	 * @param 	sslSocket		Instance of the SSL socket to use in connection with server.
	 * @return					Method to call to indicate the server name of interest to the server.
	 */
	private static Method getMethod(SSLSocket sslSocket) {
		final Class<?> sslSocketClass = sslSocket.getClass();
		final ServerNameIndicator instance = mSingleInstance.get();
		if (instance == null) {
			return initFrom(sslSocketClass);
			
		} else if (instance.mSSLSocketClassRef.get() != sslSocketClass) {
			// the underlying class changed
			return initFrom(sslSocketClass);
				
		} else if (instance.mSetHostnameMethodRef == null) {
			// SNI not supported
			return null;
				
		} else {
			final Method cachedSetHostnameMethod = instance.mSetHostnameMethodRef.get();
			return (cachedSetHostnameMethod == null) ? initFrom(sslSocketClass) : cachedSetHostnameMethod;
		}
	}


	/**
	 * Singleton initializer.
	 * 
	 * Uses reflection to extract and 'cache' the method to invoke to indicate the desited host name to the server side.
	 *  
	 * @param 	sslSocketClass		Underlying class providing the implementation of {@link SSLSocket}.
	 * @return						Method to call to indicate the server name of interest to the server.
	 */
	private static Method initFrom(Class<?> sslSocketClass) {
        Log_OC.i(TAG, "SSLSocket implementation: " + sslSocketClass.getCanonicalName());
		Method setHostnameMethod = null;
		try {
			setHostnameMethod = sslSocketClass.getMethod(METHOD_NAME, String.class);
		} catch (SecurityException e) {
			Log_OC.e(TAG, "Could not access to SSLSocket#setHostname(String) method ", e);
			
		} catch (NoSuchMethodException e) {
			Log_OC.i(TAG, "Could not find SSLSocket#setHostname(String) method - SNI not supported");
		}
		mSingleInstance.set(new ServerNameIndicator(sslSocketClass, setHostnameMethod));
		return setHostnameMethod;
	}

}
