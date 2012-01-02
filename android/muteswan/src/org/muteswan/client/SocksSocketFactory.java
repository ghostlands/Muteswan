/*
Copyright 2011-2012 James Unger, Rob Wolffe, Chris Churnick.
This file is part of Muteswan.

Muteswan is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Muteswan is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with Muteswan.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.muteswan.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import uk.ac.cam.cl.dtg.android.tor.TorProxyLib.SocksProxy;

/**
 * Provides sockets for an HttpClient connection.
 * @author cmg47
 *
 */
public class SocksSocketFactory implements SocketFactory {

        SocksProxy mSocksProxy = null;

        /**
         * Construct a SocksSocketFactory that uses the provided SOCKS proxy.
         * @param proxyaddress the IP address of the SOCKS proxy
         * @param proxyport the port of the SOCKS proxy
         */
        public SocksSocketFactory(String proxyaddress, int proxyport) {
                mSocksProxy = new SocksProxy(proxyaddress, proxyport);
        }

        @Override
        public Socket connectSocket(Socket sock, String host, int port,
                        InetAddress localAddress, int localPort, HttpParams params) throws
 IOException,
                        UnknownHostException, ConnectTimeoutException {

                if (host == null) {
            throw new IllegalArgumentException("Target host may not be null.");
        }
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null.");
        }

        if (sock == null)
            sock = createSocket();
        
        //DataOutputStream fhe = null;
		//try {
		//	fhe = new DataOutputStream(new FileOutputStream("/sdcard/sockdata.txt"));
		//} catch (FileNotFoundException e1) {
		//	// TODO Auto-generated catch block
		//	e1.printStackTrace();
		//}
		
        //try {
        //	fhe.writeChars("sock " + sock + " host " + host + " port " + port + " localAddress " + localAddress + " localPort " + localPort);
		//} catch (IOException e1) {
		//	// TODO Auto-generated catch block
		//	e1.printStackTrace();
		//}

        if ((localAddress != null) || (localPort > 0)) {

            // we need to bind explicitly
            if (localPort < 0)
                localPort = 0; // indicates "any"

            InetSocketAddress isa =
                new InetSocketAddress(localAddress, localPort);
            sock.bind(isa);
        }

        int timeout = HttpConnectionParams.getConnectionTimeout(params);

        // Pipe this socket over the proxy
        sock = mSocksProxy.connectSocksProxy(sock, host, port, timeout);
        return sock;

        }



        @Override
        public Socket createSocket() throws IOException {
                return new Socket();
        }

        @Override
        public boolean isSecure(Socket sock) throws IllegalArgumentException {
                return false;
        }

}
