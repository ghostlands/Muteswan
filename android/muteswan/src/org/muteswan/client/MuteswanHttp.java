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

import org.apache.http.HttpVersion;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

public class MuteswanHttp {

	public DefaultHttpClient httpClient;
	
	public MuteswanHttp() {
		initHttp();
	}
    
	private void initHttp() {

		SocksSocketFactory socksFactory = new SocksSocketFactory("127.0.0.1",9050); 
		
		SchemeRegistry supportedSchemes = new SchemeRegistry();
		supportedSchemes.register(new Scheme("http", socksFactory, 80));
	
		
		HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUseExpectContinue(params,false);
        params.setIntParameter("http.socket.timeout", 45000);

        ClientConnectionManager	ccm = new MyThreadSafeClientConnManager(params,
                    supportedSchemes);
        

        httpClient = new DefaultHttpClient(ccm, params);
		
	}
	
}
