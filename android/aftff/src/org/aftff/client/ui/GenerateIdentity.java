package org.aftff.client.ui;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import org.aftff.client.R;
import org.aftff.client.data.Identity;
import org.aftff.client.data.IdentityStore;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class GenerateIdentity extends Activity {
	
	
	public void onCreate(Bundle savedInstanceState) {
	       super.onCreate(savedInstanceState);
	       setContentView(R.layout.generateidentity);
    
	       final Button genIdentityButton = (Button) findViewById(R.id.genIdentityButton);
	       genIdentityButton.setOnClickListener(genIdentity);
	       
	}  
	       
	       
	
	public Button.OnClickListener genIdentity = new Button.OnClickListener() {
	    public void onClick(View v) {
	    	EditText txtEdit = (EditText) findViewById(R.id.newIdentityName);
	    	if (txtEdit.length() == 0) 
	    		return;
	    	String name = txtEdit.getText().toString();
	    	Identity identity = new Identity();
	    	identity.setName(name);
	    	try {
				identity.genKeyPair();
				IdentityStore idStore = new IdentityStore(getApplicationContext());
				idStore.addToDb(identity);
				
				TextView txt = new TextView(getApplicationContext());
				txt.setText("Identity created and stored. Formatting for pub is " + identity.formatPub + " and " + identity.formatPriv + " for private.");

				setContentView(txt);
				
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchProviderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	};
	       
	       
//	       //Bundle extras = getIntent().getExtras();
//	       
//	       
//	       
//	       try {
//			genKeyPair();
//		} catch (NoSuchAlgorithmException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (NoSuchProviderException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	    	  
//		
//		TextView txt = new TextView(this);
//		txt.setText("Pub key: " + pubKeyHex + "\nPriv key: " + privKeyHex);
//		setContentView(txt);
//		
//		//Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
//		//intent.putExtra("ENCODE_DATA",pubKeyHex.toString());
//		//intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
////		startActivity(intent);
//			
//		
//		String signData = "8377e7f83018g0f8d83a8ebcefde583";
//		try {
//			Signature sig = Signature.getInstance("MD5WithRSA");
//			sig.initSign(privateKey);
//			sig.update(signData.getBytes("UTF8"));
//			byte[] sigBytes = sig.sign();
//			
//			sig.initVerify(publicKey);
//			sig.update(signData.getBytes("UTF8"));
//			if (sig.verify(sigBytes)) {
//				txt.setText("signData is valid.\n");
//			} else {
//				txt.setText("signData is invalid.\n");
//			}
//			
//		} catch (NoSuchAlgorithmException e) {
//			//get a null pointer
//			String a = null; a.charAt(0);
//
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (InvalidKeyException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (SignatureException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//	}
	
	
	
}
