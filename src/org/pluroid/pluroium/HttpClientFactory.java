/*
 * Copyright (C) 2011 The Pluroium Development Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pluroid.pluroium;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.RequestExpectContinue;

public class HttpClientFactory {
    
    private static final SchemeRegistry supportedSchemes = new SchemeRegistry();
    static {
        // Setup the schemes
        // Register the "http" and "https" protocol schemes, they are required by the default operator to look up socket factories.
    	supportedSchemes.register( new Scheme( "http", PlainSocketFactory.getSocketFactory(), 80));
    	supportedSchemes.register( new Scheme( "https", new MySSLSocketFactory(), 443));
    }
    
    private final static int CONNECT_TIMEOUT = 20000;
    private final static int READ_TIMEOUT = 45000;

    public static ClientConnectionManager createConnectionManager() {
    	HttpParams params = new BasicHttpParams();
    	params.setParameter("http.socket.timeout", new Integer(READ_TIMEOUT));
    	params.setParameter("http.connection.timeout", new Integer(CONNECT_TIMEOUT));
    	return new ThreadSafeClientConnManager(params, supportedSchemes);
    }
    
    public static DefaultHttpClient createHttpClient(ClientConnectionManager connMgr) {
        DefaultHttpClient httpClient;
        HttpParams params = new BasicHttpParams();
        params.setParameter("http.socket.timeout", new Integer(READ_TIMEOUT));
        params.setParameter("http.connection.timeout", new Integer(CONNECT_TIMEOUT));
        connMgr = new ThreadSafeClientConnManager(params, supportedSchemes);

        httpClient = new DefaultHttpClient(connMgr, params);
        httpClient.removeRequestInterceptorByClass(RequestExpectContinue.class);
        return httpClient;
    }
}

/**
 * @author ericsk
 */
class MySSLSocketFactory implements SocketFactory, LayeredSocketFactory {
    /**
     * Constructor
     */
    public MySSLSocketFactory() {
        if( m_sslSocketFactory == null) {
            try {
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, null, null);
                m_sslSocketFactory = sc.getSocketFactory();
            } catch( Exception ex) {
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.http.conn.scheme.SocketFactory#connectSocket(java.net.Socket, java.lang.String, int, java.net.InetAddress, int, org.apache.http.params.HttpParams)
     */
    public Socket connectSocket( Socket socket, String host, int port, InetAddress localAddress, int localPort, HttpParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
        if(socket == null) {
            socket = createSocket();
        }
        if( (localAddress != null) || (localPort > 0)) {
            if( localPort < 0) {
                localPort = 0; // indicates "any"
            }
            socket.bind( new InetSocketAddress( localAddress, localPort));
        }
        socket.setSoTimeout( HttpConnectionParams.getSoTimeout( params));
        socket.connect( new InetSocketAddress( host, port), HttpConnectionParams.getConnectionTimeout( params));
        return socket;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.http.conn.scheme.SocketFactory#createSocket()
     */
    public Socket createSocket() throws IOException {
        return m_sslSocketFactory.createSocket();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.http.conn.scheme.LayeredSocketFactory#createSocket(java.net.Socket, java.lang.String, int, boolean)
     */
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
        return m_sslSocketFactory.createSocket();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.http.conn.scheme.SocketFactory#isSecure(java.net.Socket)
     */
    public boolean isSecure(Socket socket) throws IllegalArgumentException {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        return((obj != null) && obj.getClass().equals( MySSLSocketFactory.class));
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return MySSLSocketFactory.class.hashCode();
    }

    /**
     * Member variables
     */
    private static SSLSocketFactory m_sslSocketFactory;
}

