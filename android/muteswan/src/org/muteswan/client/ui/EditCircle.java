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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.muteswan.client.Base64;
import org.muteswan.client.MuteswanHttp;
import org.muteswan.client.R;
import org.muteswan.client.Main;
import org.muteswan.client.data.Circle;
import org.muteswan.client.data.CircleStore;
import org.muteswan.client.data.Identity;
import org.muteswan.client.data.IdentityStore;
import org.muteswan.client.MuteLog;


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

public class EditCircle extends Activity {

	
	private Identity[] identities;
	private Circle circle;
	protected byte[] imageBytes;
	private boolean[] keylistIdentitiesSelected;
	private CharSequence[] signIdentities;
	private MuteswanHttp muteswanHttp;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
		muteswanHttp = new MuteswanHttp();
	    CircleStore rs = new CircleStore(null,getApplicationContext(),true,false);
	    HashMap<String,Circle> hashMap = rs.asHashMap();
	    circle = hashMap.get(Main.genHexHash(extras.getString("circle")));
		setContentView(R.layout.editcircle);
		

		Button fetchImage = (Button) findViewById(R.id.editCircleFetchImageButton);
		fetchImage.setOnClickListener(fetchImageListener);
		
		Button setKeylistButton = (Button) findViewById(R.id.editCircleSetKeylistButton);
		setKeylistButton.setOnClickListener(setKeylistButtonListener);
		
		Button updateButton = (Button) findViewById(R.id.updateCircleButton);
		updateButton.setOnClickListener(updateButtonListener);

        Spinner policySpinner = (Spinner) findViewById(R.id.editCirclePostPolicy);
        String[] policyList = new String[] { "NONE", "AUTHKEY", "KEYLIST" };
        ArrayAdapter<String> policyAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, policyList);
        //createFromResource(
         //       this, policyList, android.R.layout.simple_spinner_item);
        //adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        policySpinner.setAdapter(policyAdapter);
        if (circle.getPostPolicy() != null) {
        	for (int i=0; i<policyList.length;i++) {
        		if (policyList[i].equals(circle.getPostPolicy())) {
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
				if (circle.getAuthKey() != null)
				  authkeyEnc = new String(Base64.decode(circle.getAuthKey()));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	    	if (identities[i].publicKeyEnc.equals(authkeyEnc)) {
	    		knownIdentity = i+1;
	    	}
	        signIdentities[i+1] = identities[i].getName();
	    }
	    
	    
	    Spinner authkeySpinner = (Spinner) findViewById(R.id.editCircleAuthKey);
	    ArrayAdapter<CharSequence> authkeyAdapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_dropdown_item, signIdentities);
        authkeySpinner.setAdapter(authkeyAdapter);
        if (knownIdentity != 0) {
        	authkeySpinner.setSelection(knownIdentity);
        }

        
       // if (circle.getDescription() != null) {
        	EditText txtDesc = (EditText) findViewById(R.id.editCircleDescription);
        	txtDesc.setText(circle.getDescription());
        //}
		
        	EditText txtLDesc = (EditText) findViewById(R.id.editCircleLongDescription);
        	txtLDesc.setText(circle.getLongDescription());
        	
        	TextView editCircleName = (TextView) findViewById(R.id.editCircleName);
        	editCircleName.setText(circle.getShortname());
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
            	 MuteLog.Log("WriteMsg", "Set " + clicked + " to " + selected);
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
			TextView txtView = (TextView) findViewById(R.id.editCircleKeylistInfo);
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
			EditText txtImage = (EditText) findViewById(R.id.editCircleImage);
			ImageView imageView = (ImageView) findViewById(R.id.editCircleImageView);
			String url = txtImage.getText().toString();
		    MuteswanHttp muteswanHttp = new MuteswanHttp();
			HttpGet httpGet = new HttpGet(url);
		    try {
				HttpResponse resp = muteswanHttp.execute(httpGet);
				
				// FIXME seems wrong, dunno what is right
				//imageBytes  = EntityUtils.toString(resp.getEntity()).getBytes();
				imageBytes = new byte[Integer.parseInt(resp.getFirstHeader ("Content-Length").getValue())];
				//resp.getEntity().getContent().read(imageBytes);
				
				InputStream is = resp.getEntity().getContent();
				
				int count=0;
				while ((is.read(imageBytes,count,1)) != -1) {
					count++;
				}
				MuteLog.Log("EditCircle", "Count is " + count + " and content length is " + imageBytes.length);
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
			final Spinner authKey = (Spinner) findViewById(R.id.editCircleAuthKey);
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
					
					circle.updateManifest(jsonObj,Base64.encodeBytes(sigBytes));
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
				circle.updateManifest(jsonObj);
			}
			
		}
		
	};
	
	
	private JSONObject collectManifestData() {
		final TextView newCircleDescription = (TextView) findViewById(R.id.editCircleDescription);
		final TextView newCircleLongDescription = (TextView) findViewById(R.id.editCircleLongDescription);
		final Spinner authKey = (Spinner) findViewById(R.id.editCircleAuthKey);
		final Spinner postPolicy = (Spinner) findViewById(R.id.editCirclePostPolicy);

		JSONObject jsonManifest = new JSONObject();
		JSONObject jsonObj = new JSONObject();

		
		try {
			int authkeyId = authKey.getSelectedItemPosition();
			jsonManifest.put("longdescription",
					Base64.encodeBytes(newCircleLongDescription.getText().toString().getBytes("UTF8")));
			jsonManifest.put("description", 
					Base64.encodeBytes(newCircleDescription.getText().toString().getBytes("UTF8")));
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
