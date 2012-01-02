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
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.http.HttpHost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.OperatedClientConnection;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.conn.DefaultClientConnectionOperator;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

public class MyDefaultClientConnectionOperator extends
		DefaultClientConnectionOperator {

	public MyDefaultClientConnectionOperator(SchemeRegistry schemes) {
		super(schemes);
	}
	
	@Override
	public void openConnection(OperatedClientConnection conn, HttpHost target,
			InetAddress local, HttpContext context, HttpParams params)
			throws IOException {
		if (conn == null) {
            throw new IllegalArgumentException
                ("Connection must not be null.");
        }
        if (target == null) {
            throw new IllegalArgumentException
                ("Target host must not be null.");
        }
        // local address may be null
        //@@@ is context allowed to be null?
        if (params == null) {
            throw new IllegalArgumentException
                ("Parameters must not be null.");
        }
        if (conn.isOpen()) {
            throw new IllegalArgumentException
                ("Connection must not be open.");
        }

        final Scheme schm = schemeRegistry.getScheme(target.getSchemeName());
        final SocketFactory sf = schm.getSocketFactory();

        Socket sock = sf.createSocket();
        conn.opening(sock, target);

        try {
        	Socket connsock = sf.connectSocket(sock, target.getHostName(),
                    schm.resolvePort(target.getPort()),
                    local, 0, params);
        			if (sock != connsock) {
		                sock = connsock;
		                conn.opening(sock, target);
		            }
        } catch (ConnectException ex) {
            throw new HttpHostConnectException(target, ex);
        }
        prepareSocket(sock, context, params);
        conn.openCompleted(sf.isSecure(sock), params);
	}

}
