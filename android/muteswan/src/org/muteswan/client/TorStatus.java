/*
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
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import android.content.ServiceConnection;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

public class TorStatus {

	boolean on = false;
	private MuteswanHttp muteswanHttp = new MuteswanHttp();
	
	
	
	public boolean checkStatus() {
		HttpGet httpGet = new HttpGet("http://torcheck.xenobite.eu/");
	
    	try {
			HttpResponse resp = muteswanHttp.httpClient.execute(httpGet);
			
			
			
			String checkContent = EntityUtils.toString(resp.getEntity());
			if (checkContent.contains("So you are NOT using Tor to reach the web!")) {
				Log.v("TorStatus", "Tor failed check.");
				return(false);
			} else if (checkContent.contains("So you are using Tor successfully to reach the web!")) {
				Log.v("TorStatus","Looks like Tor is good.");
				return(true);
			}
			
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
   	 	return(false);
	 
	}
	
}
