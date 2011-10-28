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
package org.muteswan.client.ui;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;


import org.muteswan.client.Base64;
import org.muteswan.client.Crypto;
import org.muteswan.client.R;
import org.muteswan.client.muteswan;

import org.muteswan.client.data.Identity;
import org.muteswan.client.data.IdentityStore;
import org.muteswan.client.data.CircleStore;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;


public class CreateCircle extends Activity implements Runnable {

	private Identity[] identities;
	public void onCreate(Bundle savedInstanceState) {
	       super.onCreate(savedInstanceState);

	       setContentView(R.layout.createcircle);
	       TextView keyTxt = (TextView) findViewById(R.id.newCircleKey);
	    
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
	       
			
			
	      		       
	}
	
	public Button.OnClickListener genCircle = new Button.OnClickListener() {
	    public void onClick(View v) {
	    	EditText name = (EditText) findViewById(R.id.newCircleName);
	    	EditText server = (EditText) findViewById(R.id.newCircleServer);
	    	TextView keyTxt = (TextView) findViewById(R.id.newCircleKey);
	    	TextView newCircleResult = (TextView) findViewById(R.id.newCircleResult);
	    	
	    	if (name.length() == 0 || server.length() == 0)
	    		return;
	    	
	    	String circleFullText = name.getText().toString() + "+" + keyTxt.getText().toString() + "@" + server.getText().toString();
	    	
	    	newCircleResult.setText(circleFullText);
	    	
	    	
        	CircleStore newStore = new CircleStore(getApplicationContext(),true);
        	newStore.updateStore(circleFullText);

	    	newCircleResult.setText("Created circle " + name.getText().toString());
	    	
	    	//try {
			//	Thread.currentThread().sleep(1000);
			//} catch (InterruptedException e) {
			//	// TODO Auto-generated catch block
			//	e.printStackTrace();
			//}
	    	
	    	Intent intent = new Intent(getApplicationContext(),EditCircle.class);
	    	intent.putExtra("circle",circleFullText);
	      	startActivity(intent);

	 }


};

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

	
}
