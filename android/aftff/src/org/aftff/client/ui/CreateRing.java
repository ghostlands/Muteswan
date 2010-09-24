package org.aftff.client.ui;

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


import org.aftff.client.Base64;
import org.aftff.client.Crypto;
import org.aftff.client.R;
import org.aftff.client.aftff;

import org.aftff.client.data.Identity;
import org.aftff.client.data.IdentityStore;
import org.aftff.client.data.RingStore;
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


public class CreateRing extends Activity implements Runnable {

	private Identity[] identities;
	public void onCreate(Bundle savedInstanceState) {
	       super.onCreate(savedInstanceState);

	       setContentView(R.layout.createring);
	       TextView keyTxt = (TextView) findViewById(R.id.newRingKey);
	    
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
			
			final Button genRingButton = (Button) findViewById(R.id.genRingButton);
	        genRingButton.setOnClickListener(genRing);

		    
			keyTxt.setText(genKeyStr);
	       
			
			
	      		       
	}
	
	public Button.OnClickListener genRing = new Button.OnClickListener() {
	    public void onClick(View v) {
	    	EditText name = (EditText) findViewById(R.id.newRingName);
	    	EditText server = (EditText) findViewById(R.id.newRingServer);
	    	TextView keyTxt = (TextView) findViewById(R.id.newRingKey);
	    	TextView newRingResult = (TextView) findViewById(R.id.newRingResult);
	    	
	    	if (name.length() == 0 || server.length() == 0)
	    		return;
	    	
	    	String ringFullText = name.getText().toString() + "+" + keyTxt.getText().toString() + "@" + server.getText().toString();
	    	
	    	newRingResult.setText(ringFullText);
	    	
	    	
        	RingStore newStore = new RingStore(getApplicationContext(),true);
        	newStore.updateStore(ringFullText);

	    	newRingResult.setText("Created ring " + name.getText().toString());
	    	
	    	try {
				Thread.currentThread().sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	
	    	Intent intent = new Intent(getApplicationContext(),EditRing.class);
	    	intent.putExtra("ring",aftff.genHexHash(ringFullText));
	      	startActivity(intent);

	 }


};

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

	
}
