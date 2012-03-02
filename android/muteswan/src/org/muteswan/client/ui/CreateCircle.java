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
package org.muteswan.client.ui;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.muteswan.client.MuteLog;
import org.muteswan.client.MuteswanHttp;
import org.muteswan.client.R;
import org.muteswan.client.Main;
import org.muteswan.client.data.CircleStore;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;


public class CreateCircle extends Activity implements Runnable {

	public static final String CREATED_CIRCLE_BROADCAST = "CREATEDCIRCLE";

	private EditText serverView;

	private Boolean usePublicServer;

	public void onCreate(Bundle savedInstanceState) {
	       super.onCreate(savedInstanceState);
	       
	       
	       Bundle extras = getIntent().getExtras();
	       String extraNewCircleName = extras.getString("newCircleName");

	       setContentView(R.layout.createcircle);
	       TextView keyTxt = (TextView) findViewById(R.id.newCircleKey);
	       TextView newCircleServerPrompt = (TextView) findViewById(R.id.newCircleServerPrompt);
	       serverView = (EditText) findViewById(R.id.newCircleServer);
	       
	       
	       
	       SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
	       String customServer = defPrefs.getString("customCircleServer", "");
	       if (!customServer.equals("")) {
	    	   serverView.setText(customServer);
	       } else {
	    	   serverView.setVisibility(View.GONE);
	    	   newCircleServerPrompt.setVisibility(View.GONE);
	       }
	       
	       usePublicServer = defPrefs.getBoolean("usePublicServer", true);
	       MuteLog.Log("CreateCircle","Use public server is: " + usePublicServer);
	    
	       
	       final ImageView titleBarImage = (ImageView) findViewById(R.id.titlebarImage);
	       titleBarImage.setOnClickListener(titleBarClicked);
	       
	       String genKeyStr;
	       
	    	//byte[] key = new byte[24];
			SecureRandom sr = null;
			try {
				sr = SecureRandom.getInstance("SHA1PRNG");
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			sr.generateSeed(24);
			genKeyStr = new BigInteger(130,sr).toString(32).substring(0,16);
			
			final Button genCircleButton = (Button) findViewById(R.id.genCircleButton);
	        genCircleButton.setOnClickListener(genCircle);

		    
			keyTxt.setText(genKeyStr);
	       
			
	       if (extraNewCircleName != null) {
	    	   EditText name = (EditText) findViewById(R.id.newCircleName);
	    	   name.setText(extraNewCircleName);
	    	   genCircle.onClick(genCircleButton);
	       }
			
	      		       
	}
	
	public Button.OnClickListener genCircle = new Button.OnClickListener() {
	    public void onClick(View v) {
	    	EditText name = (EditText) findViewById(R.id.newCircleName);
	    	TextView keyTxt = (TextView) findViewById(R.id.newCircleKey);
	    	
	    	String server = serverView.getText().toString();
	    
	    	if (!usePublicServer && server.equals("")) {
	    		server = getString(R.string.defaulthiddencircleserver);
	    	} else if (usePublicServer && server.equals("")) {
	    		server = getString(R.string.defaultcircleserver);
	    	}
	    	
	    	if (name.length() == 0 || server.length() == 0)
	    		return;
	    	
	    	String circleFullText = name.getText().toString() + "+" + keyTxt.getText().toString() + "@" + server;
	    	
	    	//newCircleResult.setText(circleFullText);
	    	
	    	
        	CircleStore newStore = new CircleStore(getApplicationContext(),true,false);
        	newStore.updateStore(circleFullText);
        	
	        Intent createdCircleIntent = new Intent(CreateCircle.CREATED_CIRCLE_BROADCAST);
	        createdCircleIntent.putExtra("circle", Main.genHexHash(circleFullText));
	        sendBroadcast(createdCircleIntent);
	        
	        Intent circleListIntent = new Intent(getApplicationContext(),CircleList.class);
	        circleListIntent.putExtra("newCircle", name.getText().toString());
	        circleListIntent.putExtra("action", CircleList.ANY);
	        startActivity(circleListIntent);
        	
	        //finish();
	 }
	    
	

   };

public View.OnClickListener titleBarClicked = new View.OnClickListener() {
   	public void onClick(View v) {
    		  Intent intent = new Intent(getApplicationContext(),Main.class);
      		  startActivity(intent);
   	}
 };


	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

	
}
