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
				return(false);
			} else if (checkContent.contains("So you are using Tor successfully to reach the web!")) {
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
	
	public void checkButton(Button button) {
		if (checkStatus() == false) {
			button.setClickable(false);
			button.setVisibility(Button.INVISIBLE);
		} else {
			button.setClickable(true);
			button.setVisibility(Button.VISIBLE);
		}
	}
	
	public void checkView(TextView view) {
		if (checkStatus() == false) {
			view.setClickable(false);
			view.setVisibility(Button.INVISIBLE);
		} else {
			view.setClickable(true);
			view.setVisibility(Button.VISIBLE);
		}
	}
	
}
