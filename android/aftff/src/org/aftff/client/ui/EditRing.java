package org.aftff.client.ui;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;

import org.aftff.client.AftffHttp;
import org.aftff.client.Base64;
import org.aftff.client.R;
import org.aftff.client.aftff;
import org.aftff.client.data.Identity;
import org.aftff.client.data.IdentityStore;
import org.aftff.client.data.Ring;
import org.aftff.client.data.RingStore;
import org.aftff.client.ui.WriteMsg.DialogButtonClickHandler;
import org.aftff.client.ui.WriteMsg.DialogSelectionClickHandler;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

public class EditRing extends Activity {

	
	private Identity[] identities;
	private Ring ring;
	protected byte[] imageBytes;
	private boolean[] keylistIdentitiesSelected;
	private CharSequence[] signIdentities;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
	    RingStore rs = new RingStore(getApplicationContext(),true);
	    HashMap<String,Ring> hashMap = rs.asHashMap();
	    ring = hashMap.get(aftff.genHexHash(extras.getString("ring")));
		setContentView(R.layout.editring);
		

		Button fetchImage = (Button) findViewById(R.id.editRingFetchImageButton);
		fetchImage.setOnClickListener(fetchImageListener);
		
		Button setKeylistButton = (Button) findViewById(R.id.editRingSetKeylistButton);
		setKeylistButton.setOnClickListener(setKeylistButtonListener);
		
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
	    signIdentities = new CharSequence[identities.length+1];
	    keylistIdentitiesSelected = new boolean[identities.length+1];
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
        	
        	TextView editRingName = (TextView) findViewById(R.id.editRingName);
        	editRingName.setText(ring.getShortname());
	}
	
	
	 @Override
     protected Dialog onCreateDialog( int id )
     {
             return
             new AlertDialog.Builder( this )
             .setTitle( "Select identities to add to the keylist." )
             .setMultiChoiceItems(signIdentities, keylistIdentitiesSelected, new DialogSelectionClickHandler() )
             .setPositiveButton( "OK", new DialogButtonClickHandler() )
             .create();
     }

	 public class DialogSelectionClickHandler implements DialogInterface.OnMultiChoiceClickListener
     {
             public void onClick( DialogInterface dialog, int clicked, boolean selected )
             {
            	 keylistIdentitiesSelected[clicked] = selected;
            	 Log.v("WriteMsg", "Set " + clicked + " to " + selected);
             }
     }
	
	 public class DialogButtonClickHandler implements DialogInterface.OnClickListener
     {
             public void onClick( DialogInterface dialog, int clicked )
             {
                     switch( clicked )
                     {
                             case DialogInterface.BUTTON_POSITIVE:
                            	 	 updateKeylist();
                                     break;
                     }
             }

			
     }
	 
   private void updateKeylist() {
			TextView txtView = (TextView) findViewById(R.id.editRingKeylistInfo);
			StringBuilder sBuilder = new StringBuilder();
			for (int i = 0; i<keylistIdentitiesSelected.length; i++) {
				if (signIdentities[i].equals("None"))
				   continue;
				if (keylistIdentitiesSelected[i])
				   sBuilder.append(signIdentities[i] + "\n");
			}
			
			if (sBuilder.toString().equals("")) {
				txtView.setText("None selected.");
   			} else {
			    txtView.setText(sBuilder.toString());
   			}
   }
	 
	public Button.OnClickListener setKeylistButtonListener = new Button.OnClickListener() {
		public void onClick(View v) {
			showDialog(0);
		}
	};
	
	
	public Button.OnClickListener fetchImageListener = new Button.OnClickListener() {
		public void onClick(View v) {
			EditText txtImage = (EditText) findViewById(R.id.editRingImage);
			ImageView imageView = (ImageView) findViewById(R.id.editRingImageView);
			String url = txtImage.getText().toString();
		    AftffHttp aftffHttp = new AftffHttp();
			HttpGet httpGet = new HttpGet(url);
		    try {
				HttpResponse resp = aftffHttp.httpClient.execute(httpGet);
				
				// FIXME seems wrong, dunno what is right
				//imageBytes  = EntityUtils.toString(resp.getEntity()).getBytes();
				imageBytes = new byte[Integer.parseInt(resp.getFirstHeader ("Content-Length").getValue())];
				//resp.getEntity().getContent().read(imageBytes);
				
				InputStream is = resp.getEntity().getContent();
				
				int count=0;
				while ((is.read(imageBytes,count,1)) != -1) {
					count++;
				}
				Log.v("EditRing", "Count is " + count + " and content length is " + imageBytes.length);
				//is.read(imageBytes, 0, imageBytes.length);
				ByteArrayInputStream is2 = new ByteArrayInputStream(imageBytes);
				
				
				
				//Bitmap bitmap = BitmapFactory.decodeStream(resp.getEntity().getContent());
				Bitmap bitmap = BitmapFactory.decodeStream(is2);
				//Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
				imageView.setImageBitmap(bitmap);
				
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};
	
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
			if (imageBytes != null)
			  jsonManifest.put("image", Base64.encodeBytes(imageBytes));
			jsonManifest.put("postpolicy", postPolicy.getSelectedItem());

			if (authkeyId != 0) {
			  jsonManifest.put("authkey",identities[authkeyId-1].publicKeyEnc);
		
			JSONArray keylist = new JSONArray();
			for (int i=0; i<keylistIdentitiesSelected.length; i++) {
				if (keylistIdentitiesSelected[i])
				   keylist.put(identities[i-1].publicKeyEnc);
			}
		    
			if (keylist.length() != 0)
				jsonManifest.put("keylist", keylist);			 
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
