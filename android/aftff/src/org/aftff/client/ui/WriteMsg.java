package org.aftff.client.ui;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.aftff.client.R;
import org.aftff.client.aftff;
import org.aftff.client.R.id;
import org.aftff.client.R.layout;
import org.aftff.client.data.Identity;
import org.aftff.client.data.IdentityStore;
import org.aftff.client.data.Ring;
import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class WriteMsg extends Activity {

	Ring ring;
	
	public void onCreate(Bundle savedInstanceState) {
	       super.onCreate(savedInstanceState);

	       Bundle extras = getIntent().getExtras();
	       ring = new Ring(this,extras.getString("ring"));
	       
	       setContentView(R.layout.writemsg);
	       
	       IdentityStore idStore = new IdentityStore(getApplicationContext());
	       
	       Spinner selectSignSpinner = (Spinner) findViewById(R.id.keySelectSpinner);
	       ArrayAdapter adapter = new ArrayAdapter<Identity>(
	    		   this, android.R.layout.simple_spinner_item, idStore.asArray());
	       selectSignSpinner.setAdapter(adapter);
	       

	       
	       
	       final Button button = (Button) findViewById(R.id.submitMsg);
	       button.setOnClickListener(submitMsg);
	       
	}
	
	
	public Button.OnClickListener submitMsg = new Button.OnClickListener() {
	    public void onClick(View v) {
	    	EditText newMsgText = (EditText) findViewById(R.id.newMsgText);
	    	Editable txt = newMsgText.getText();
	    	String txtData = txt.toString();
	    	
	    		    	
	    	try {
	    		
	    		Spinner selectSignSpinner = (Spinner) findViewById(R.id.keySelectSpinner);
	    		Identity identity = (Identity) selectSignSpinner.getSelectedItem();
	    		TextView txt2 = new TextView(v.getContext());
	    		if (identity != null) {
				    try {
						ring.postMsg(txtData,new Identity[] { identity });
			            txt2.setText("Posted message to " + ring.getShortname() + " with signature.");

					} catch (InvalidKeyException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
						txt2.setText("Invalid key exception for identity privkey.");
					} catch (SignatureException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
						txt2.setText("Signature exception: " + e.toString());
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
						txt2.setText("UnsupportedEncodingException: " + e.toString());
					} catch (InvalidKeySpecException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
						txt2.setText("Invalid keyspec exception " + e.toString());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    		} else {
				  ring.postMsg(txtData);
			      txt2.setText("Posted message to " + ring.getShortname() + " without signature.");

	    		}
		        setContentView(txt2);

				
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	
	    	//TextView txt2 = new TextView(v.getContext());
	        //txt2.setText("We got this: " + txt);
	        //setContentView(txt2);
	    	
	    	//startActivity(new Intent(v.getContext(), MsgList.class));
	    	
	    	
	    	//Toast.makeText(v.getContext(), "Should post message.", 5);
	    	//TextView txt = new TextView(v.getContext());
    	    //    Intent intent = new Intent("com.google.zxing.client.android.SCAN");
    	    //    intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
    	    //    startActivityForResult(intent, 0);
    	 
	    	
	 }

		
};
	
}
