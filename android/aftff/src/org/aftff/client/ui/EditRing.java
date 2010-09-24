package org.aftff.client.ui;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;

import org.aftff.client.Base64;
import org.aftff.client.R;
import org.aftff.client.aftff;
import org.aftff.client.data.Identity;
import org.aftff.client.data.IdentityStore;
import org.aftff.client.data.Ring;
import org.aftff.client.data.RingStore;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class EditRing extends Activity {

	
	private Identity[] identities;
	private Ring ring;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
	    RingStore rs = new RingStore(getApplicationContext(),true);
	    HashMap<String,Ring> hashMap = rs.asHashMap();
	    ring = hashMap.get(aftff.genHexHash(extras.getString("ring")));
		setContentView(R.layout.editring);
		
		
		Button updateButton = (Button) findViewById(R.id.updateRingButton);
		updateButton.setOnClickListener(updateButtonListener);

        Spinner policySpinner = (Spinner) findViewById(R.id.editRingPostPolicy);
        String[] policyList = new String[] { "NONE", "AUTHKEY", "KEYLIST" };
        ArrayAdapter policyAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, policyList);
        //createFromResource(
         //       this, policyList, android.R.layout.simple_spinner_item);
        //adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        policySpinner.setAdapter(policyAdapter);
        if (ring.getPostPolicy() != null) {
        	for (int i=0; i<policyList.length;i++) {
        		if (policyList[i].equals(ring.getPostPolicy())) {
        			policySpinner.setSelection(i);
        		}
        	}
        }
        
        
		
        IdentityStore idStore = new IdentityStore(getApplicationContext());
	    identities = idStore.asArray(true);
	    CharSequence[] signIdentities = new CharSequence[identities.length+1];
	    signIdentities[0] = "None";
	    int knownIdentity = 0;
	    for (int i=0; i<identities.length;i++) {
	    	  String authkeyEnc = "";
			try {
				if (ring.getAuthKey() != null)
				  authkeyEnc = new String(Base64.decode(ring.getAuthKey()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	    	if (identities[i].publicKeyEnc.equals(authkeyEnc)) {
	    		knownIdentity = i+1;
	    	}
	        signIdentities[i+1] = identities[i].getName();
	    }
	    
	    
	    Spinner authkeySpinner = (Spinner) findViewById(R.id.editRingAuthKey);
	    ArrayAdapter authkeyAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, signIdentities);
        authkeySpinner.setAdapter(authkeyAdapter);
        if (knownIdentity != 0) {
        	authkeySpinner.setSelection(knownIdentity);
        }

        
       // if (ring.getDescription() != null) {
        	EditText txtDesc = (EditText) findViewById(R.id.editRingDescription);
        	txtDesc.setText(ring.getDescription());
        //}
		
        	EditText txtLDesc = (EditText) findViewById(R.id.editRingLongDescription);
        	txtLDesc.setText(ring.getLongDescription());
	}
	
	public Button.OnClickListener updateButtonListener = new Button.OnClickListener() {

		@Override
		public void onClick(View v) {
			final Spinner authKey = (Spinner) findViewById(R.id.editRingAuthKey);
			JSONObject jsonObj = collectManifestData();

			int authkeyId = authKey.getSelectedItemPosition();
			if (authkeyId != 0) {
				try {
				    Signature sig;
				
					sig = Signature.getInstance("MD5WithRSA");
					RSAPrivateKey rsaPrivKey = identities[authkeyId-1].getPrivateKey();
					   
					sig.initSign(rsaPrivKey);
					sig.update(jsonObj.toString().getBytes());
					//sig.update("some random sign data".getBytes("UTF8"));
					byte[] sigBytes = sig.sign();
					
					ring.updateManifest(jsonObj,Base64.encodeBytes(sigBytes));
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidKeySpecException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidKeyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SignatureException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			} else {
				ring.updateManifest(jsonObj);
			}
			
		}
		
	};
	
	
	private JSONObject collectManifestData() {
		final TextView newRingDescription = (TextView) findViewById(R.id.editRingDescription);
		final TextView newRingLongDescription = (TextView) findViewById(R.id.editRingLongDescription);
		final TextView newRingImage = (TextView) findViewById(R.id.editRingImage);
		final Spinner authKey = (Spinner) findViewById(R.id.editRingAuthKey);
		final Spinner postPolicy = (Spinner) findViewById(R.id.editRingPostPolicy);

		JSONObject jsonManifest = new JSONObject();
		JSONObject jsonObj = new JSONObject();

		
		try {
			int authkeyId = authKey.getSelectedItemPosition();
			jsonManifest.put("longdescription",
					Base64.encodeBytes(newRingLongDescription.getText().toString().getBytes("UTF8")));
			jsonManifest.put("description", 
					Base64.encodeBytes(newRingDescription.getText().toString().getBytes("UTF8")));
			//jsonManifest.put("image", newRingImage.getText());
			jsonManifest.put("postpolicy", postPolicy.getSelectedItem());

			if (authkeyId != 0) {
			  jsonManifest.put("authkey",identities[authkeyId-1].publicKeyEnc);

		     
			 
			  //jsonObj.put("signdata",Base64.encodeBytes("some random sign data".getBytes()));
			  //jsonObj.put("signature", Base64.encodeBytes(sigBytes));
			}
			
			
			jsonObj.put("manifest", jsonManifest);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		return jsonObj;
	}
	
	
}
