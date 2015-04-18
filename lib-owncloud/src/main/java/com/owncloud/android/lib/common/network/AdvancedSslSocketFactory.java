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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;

import com.owncloud.android.lib.common.utils.Log_OC;



/**
 * AdvancedSSLProtocolSocketFactory allows to create SSL {@link Socket}s with 
 * a custom SSLContext and an optional Hostname Verifier.
 * 
 * @author David A. Velasco
 */

public class AdvancedSslSocketFactory implements SecureProtocolSocketFactory {

    private static final String TAG = AdvancedSslSocketFactory.class.getSimpleName();
    
    private SSLContext mSslContext = null;
    private AdvancedX509TrustManager mTrustManager = null;
    private X509HostnameVerifier mHostnameVerifier = null;

    public SSLContext getSslContext() {
        return mSslContext;
    }
    
    /**
     * Constructor for AdvancedSSLProtocolSocketFactory.
     */
    public AdvancedSslSocketFactory(
    		SSLContext sslContext, AdvancedX509TrustManager trustManager, X509HostnameVerifier hostnameVerifier
		) {
    	
        if (sslContext == null)
            throw new IllegalArgumentException("AdvancedSslSocketFactory can not be created with a null SSLContext");
        if (trustManager == null && mHostnameVerifier != null)
            throw new IllegalArgumentException(
            		"AdvancedSslSocketFactory can not be created with a null Trust Manager and a " +
            		"not null Hostname Verifier"
    		);
        mSslContext = sslContext;
        mTrustManager = trustManager;
        mHostnameVerifier = hostnameVerifier;
    }

    /**
     * @see ProtocolSocketFactory#createSocket(java.lang.String,int,java.net.InetAddress,int)
     */
    public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort) 
    		throws IOException, UnknownHostException {
    	
        Socket socket = mSslContext.getSocketFactory().createSocket(host, port, clientHost, clientPort);
        enableSecureProtocols(socket);
        verifyPeerIdentity(host, port, socket);
        return socket;
    }

    /*
    private void logSslInfo() {
    	if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
	    	Log_OC.v(TAG, "SUPPORTED SSL PARAMETERS");
	    	logSslParameters(mSslContext.getSupportedSSLParameters());
	    	Log_OC.v(TAG, "DEFAULT SSL PARAMETERS");
	    	logSslParameters(mSslContext.getDefaultSSLParameters());
	    	Log_OC.i(TAG, "CURRENT PARAMETERS");
	    	Log_OC.i(TAG, "Protocol: " + mSslContext.getProtocol());
    	}
    	Log_OC.i(TAG, "PROVIDER");
    	logSecurityProvider(mSslContext.getProvider());
	}
    
    private void logSecurityProvider(Provider provider) {
    	Log_OC.i(TAG, "name: " + provider.getName());
    	Log_OC.i(TAG, "version: " + provider.getVersion());
    	Log_OC.i(TAG, "info: " + provider.getInfo());
    	Enumeration<?> keys = provider.propertyNames();
    	String key;
    	while (keys.hasMoreElements()) {
    		key = (String) keys.nextElement();
        	Log_OC.i(TAG, "  property " + key + " : " + provider.getProperty(key));
    	}
	}

	private void logSslParameters(SSLParameters params) {
    	Log_OC.v(TAG, "Cipher suites: ");
    	String [] elements = params.getCipherSuites();
    	for (int i=0; i<elements.length ; i++) {
    		Log_OC.v(TAG, "  " + elements[i]);
    	}
    	Log_OC.v(TAG, "Protocols: ");
    	elements = params.getProtocols();
    	for (int i=0; i<elements.length ; i++) {
    		Log_OC.v(TAG, "  " + elements[i]);
    	}
	}
	*/

	/**
     * Attempts to get a new socket connection to the given host within the
     * given time limit.
     * 
     * @param host the host name/IP
     * @param port the port on the host
     * @param clientHost the local host name/IP to bind the socket to
     * @param clientPort the port on the local machine
     * @param params {@link HttpConnectionParams Http connection parameters}
     * 
     * @return Socket a new socket
     * 
     * @throws IOException if an I/O error occurs while creating the socket
     * @throws UnknownHostException if the IP address of the host cannot be
     *             determined
     */
    public Socket createSocket(final String host, final int port,
            final InetAddress localAddress, final int localPort,
            final HttpConnectionParams params) throws IOException,
            UnknownHostException, ConnectTimeoutException {
        Log_OC.d(TAG, "Creating SSL Socket with remote " + host + ":" + port + ", local " + localAddress + ":" + 
            localPort + ", params: " + params);
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        } 
        int timeout = params.getConnectionTimeout();
        
        //logSslInfo();
        
        SocketFactory socketfactory = mSslContext.getSocketFactory();
        Log_OC.d(TAG, " ... with connection timeout " + timeout + " and socket timeout " + params.getSoTimeout());
        Socket socket = socketfactory.createSocket();
        enableSecureProtocols(socket);
        SocketAddress localaddr = new InetSocketAddress(localAddress, localPort);
        SocketAddress remoteaddr = new InetSocketAddress(host, port);
        socket.setSoTimeout(params.getSoTimeout());
        socket.bind(localaddr);
        ServerNameIndicator.setServerNameIndication(host, (SSLSocket)socket);
        socket.connect(remoteaddr, timeout);
        verifyPeerIdentity(host, port, socket);
        return socket;
    }

	/**
     * @see ProtocolSocketFactory#createSocket(java.lang.String,int)
     */
    public Socket createSocket(String host, int port) throws IOException,
            UnknownHostException {
    	Log_OC.d(TAG, "Creating SSL Socket with remote " + host + ":" + port);
        Socket socket = mSslContext.getSocketFactory().createSocket(host, port);
        enableSecureProtocols(socket);
        verifyPeerIdentity(host, port, socket);
        return socket; 
    }

    
	@Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException,
    		UnknownHostException {
	    Socket sslSocket = mSslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
	    enableSecureProtocols(sslSocket);
	    verifyPeerIdentity(host, port, sslSocket);
	    return sslSocket;
	}

    
    public boolean equals(Object obj) {
        return ((obj != null) && obj.getClass().equals(
                AdvancedSslSocketFactory.class));
    }

    public int hashCode() {
        return AdvancedSslSocketFactory.class.hashCode();
    }


    public X509HostnameVerifier getHostNameVerifier() {
        return mHostnameVerifier;
    }
    
    
    public void setHostNameVerifier(X509HostnameVerifier hostnameVerifier) {
        mHostnameVerifier = hostnameVerifier;
    }
    
    /**
     * Verifies the identity of the server. 
     * 
     * The server certificate is verified first.
     * 
     * Then, the host name is compared with the content of the server certificate using the current host name verifier,
     *  if any.
     * @param socket
     */
    private void verifyPeerIdentity(String host, int port, Socket socket) throws IOException {
        try {
            CertificateCombinedException failInHandshake = null;
            /// 1. VERIFY THE SERVER CERTIFICATE through the registered TrustManager 
            ///	(that should be an instance of AdvancedX509TrustManager) 
            try {
                SSLSocket sock = (SSLSocket) socket;    // a new SSLSession instance is created as a "side effect" 
                sock.startHandshake();
                
            } catch (RuntimeException e) {
                
                if (e instanceof CertificateCombinedException) {
                    failInHandshake = (CertificateCombinedException) e;
                } else {
                    Throwable cause = e.getCause();
                    Throwable previousCause = null;
                    while (	cause != null && 
                    		cause != previousCause && 
                    		!(cause instanceof CertificateCombinedException)) {
                        previousCause = cause;
                        cause = cause.getCause();
                    }
                    if (cause != null && cause instanceof CertificateCombinedException) {
                        failInHandshake = (CertificateCombinedException)cause;
                    }
                }
                if (failInHandshake == null) {
                    throw e;
                }
                failInHandshake.setHostInUrl(host);
                
            }
            
            /// 2. VERIFY HOSTNAME
            SSLSession newSession = null;
            boolean verifiedHostname = true;
            if (mHostnameVerifier != null) {
                if (failInHandshake != null) {
                    /// 2.1 : a new SSLSession instance was NOT created in the handshake
                    X509Certificate serverCert = failInHandshake.getServerCertificate();
                    try {
                        mHostnameVerifier.verify(host, serverCert);
                    } catch (SSLException e) {
                        verifiedHostname = false;
                    }
                
                } else {
                    /// 2.2 : a new SSLSession instance was created in the handshake
                    newSession = ((SSLSocket)socket).getSession();
                    if (!mTrustManager.isKnownServer((X509Certificate)(newSession.getPeerCertificates()[0]))) {
                        verifiedHostname = mHostnameVerifier.verify(host, newSession); 
                    }
                }
            }

            /// 3. Combine the exceptions to throw, if any
            if (!verifiedHostname) {
                SSLPeerUnverifiedException pue = new SSLPeerUnverifiedException(
                		"Names in the server certificate do not match to " + host + " in the URL"
            		);
                if (failInHandshake == null) {
                    failInHandshake = new CertificateCombinedException(
                    		(X509Certificate) newSession.getPeerCertificates()[0]
    				);
                    failInHandshake.setHostInUrl(host);
                }
                failInHandshake.setSslPeerUnverifiedException(pue);
                pue.initCause(failInHandshake);
                throw pue;
                
            } else if (failInHandshake != null) {
                SSLHandshakeException hse = new SSLHandshakeException("Server certificate could not be verified");
                hse.initCause(failInHandshake);
                throw hse;
            }
            
        } catch (IOException io) {        
            try {
                socket.close();
            } catch (Exception x) {
                // NOTHING - irrelevant exception for the caller 
            }
            throw io;
        }
    }

	/**
	 * Grants that all protocols supported by the Security Provider in mSslContext are enabled in socket.
	 * 
	 * Grants also that no unsupported protocol is tried to be enabled. That would trigger an exception, breaking
	 * the connection process although some protocols are supported.
	 * 
	 * This is not cosmetic: not all the supported protocols are enabled by default. Too see an overview of 
	 * supported and enabled protocols in the stock Security Provider in Android see the tables in
	 * http://developer.android.com/reference/javax/net/ssl/SSLSocket.html.
	 *  
	 * @param socket
	 */
    private void enableSecureProtocols(Socket socket) {
    	SSLParameters params = mSslContext.getSupportedSSLParameters();
    	String [] supportedProtocols = params.getProtocols();
    	((SSLSocket) socket).setEnabledProtocols(supportedProtocols);
    }
	
}
