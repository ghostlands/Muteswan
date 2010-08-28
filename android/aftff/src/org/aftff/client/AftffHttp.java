package org.aftff.client;

import org.apache.http.HttpVersion;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

public class AftffHttp {

	public DefaultHttpClient httpClient;
	
	public AftffHttp() {
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

        ClientConnectionManager	ccm = new MyThreadSafeClientConnManager(params,
                    supportedSchemes);
        

        httpClient = new DefaultHttpClient(ccm, params);
		
	}
	
}
