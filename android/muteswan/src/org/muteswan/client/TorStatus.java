/*
Copyright 2011-2012 James Unger,  Chris Churnick.
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

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class TorStatus {

	boolean on = false;
	private MuteswanHttp muteswanHttp;
	private Context ctx;
	

	public TorStatus(MuteswanHttp muteswanHttp, Context ctx) {
		this.muteswanHttp = muteswanHttp;
		this.ctx = ctx;
	}
	
	
	private boolean haveNetworkConnection() {
	    boolean haveConnectedWifi = false;
	    boolean haveConnectedMobile = false;

	    ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo[] netInfo = cm.getAllNetworkInfo();
	    for (NetworkInfo ni : netInfo) {
	        if (ni.getTypeName().equalsIgnoreCase("WIFI"))
	            if (ni.isConnected())
	                haveConnectedWifi = true;
	        if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
	            if (ni.isConnected())
	                haveConnectedMobile = true;
	    }
	    return haveConnectedWifi || haveConnectedMobile;
	}

	
	public boolean checkStatus() {
		//HttpGet httpGet = new HttpGet("http://torcheck.xenobite.eu/");
		HttpGet httpGet = new HttpGet(ctx.getString(R.string.tor_check_url));
	
    	try {
    		
    		if (!haveNetworkConnection()) {
    			Log.v("TorStatus", "Looks like network is down.");
    			return false;
    		}
    		
			HttpResponse resp = muteswanHttp.httpClient.execute(httpGet);
			
			
		
			
			String checkContent = EntityUtils.toString(resp.getEntity());
		
			if (checkContent.contains(ctx.getString(R.string.tor_check_verify_string))) {
				Log.v("TorStatus","Looks like Tor is good.");
				return(true);
			} else {
				Log.v("TorStatus", "Tor failed check.");
				return(false);
			}
			
			//if (checkContent.contains("So you are NOT using Tor to reach the web!")) {
			//	Log.v("TorStatus", "Tor failed check.");
			//	return(false);
			//} else if (checkContent.contains("So you are using Tor successfully to reach the web!")) {
			//	Log.v("TorStatus","Looks like Tor is good.");
			//	return(true);
			//}
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return(false);
		} catch (IOException e) {
			Log.v("TorStatus", "Tor not running.");
			return(false);
		}
    	
	 
	}
	
}
