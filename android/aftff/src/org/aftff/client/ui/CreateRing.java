package org.aftff.client.ui;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;


import org.aftff.client.R;
import org.aftff.client.aftff;

import org.aftff.client.data.RingStore;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class CreateRing extends Activity {

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
	    	
	    	String ringFullText = name.getText().toString() + "+" + keyTxt.getText().toString() + "@" + server.getText().toString();
	    	
	    	newRingResult.setText(ringFullText);
	    	
	    	
        	RingStore newStore = new RingStore(getApplicationContext());
        	newStore.updateStore(ringFullText);

	    	newRingResult.setText("Created ring " + name.getText().toString());
	    	
	 }
};

	
}
