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
import org.aftff.client.data.RingStore;
import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class WriteMsg extends Activity {

	Ring ring;
	boolean[] signSelections;
	CharSequence[] signIdentities;
	Identity[] identities;
	String initialText;
	
	public void onCreate(Bundle savedInstanceState) {
	       super.onCreate(savedInstanceState);

	       Bundle extras = getIntent().getExtras();
	       RingStore rs = new RingStore(getApplicationContext());
	       ring = new Ring(this,rs.getOpenHelper(),extras.getString("ring"));
	       initialText = extras.getString("initialText");
	       
	       setContentView(R.layout.writemsg);
	       
	       IdentityStore idStore = new IdentityStore(getApplicationContext());
	       identities = idStore.asArray(true);
	       signIdentities = new CharSequence[identities.length];
	       for (int i=0; i<identities.length;i++) {
	    	   signIdentities[i] = identities[i].getName();
	       }
	       signSelections = new boolean[signIdentities.length];
	       for(int i=0; i<signSelections.length; i++) {
	    	   signSelections[i] = false;
	       }
	       
	       //Spinner selectSignSpinner = (Spinner) findViewById(R.id.keySelectSpinner);
	       //ArrayAdapter adapter = new ArrayAdapter<Identity>(
	       //	   this, android.R.layout.simple_spinner_item, idStore.asArray());
	       //selectSignSpinner.setAdapter(adapter);
	       

	       
	       final Button button = (Button) findViewById(R.id.submitMsg);
	       button.setOnClickListener(submitMsg);
	       
	       final Button selectSigButton = (Button) findViewById(R.id.selectSigButton);
	       selectSigButton.setOnClickListener(selectSigButtonHandler);
	       
	       if (initialText != null) {
	    	EditText newMsgText = (EditText) findViewById(R.id.newMsgText);
	    	newMsgText.setText(initialText);
	       }
	       
	}
	
	 @Override
     protected Dialog onCreateDialog( int id )
     {
             return
             new AlertDialog.Builder( this )
             .setTitle( "Sign message with identity" )
             .setMultiChoiceItems(signIdentities, signSelections, new DialogSelectionClickHandler() )
             .setPositiveButton( "OK", new DialogButtonClickHandler() )
             .create();
     }

	 public class DialogSelectionClickHandler implements DialogInterface.OnMultiChoiceClickListener
     {
             public void onClick( DialogInterface dialog, int clicked, boolean selected )
             {
            	 signSelections[clicked] = selected;
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
                                     break;
                     }
             }
     }


   
	 public Button.OnClickListener selectSigButtonHandler  = new View.OnClickListener() {
        public void onClick( View v ) {
        		Log.v("WriteMsg", "select sig button clicked.\n");
                 showDialog( 0 );
         }
     };

	
	
	public Button.OnClickListener submitMsg = new Button.OnClickListener() {
	    public void onClick(View v) {
	    	EditText newMsgText = (EditText) findViewById(R.id.newMsgText);
	    	Editable txt = newMsgText.getText();
	    	String txtData = txt.toString();
	    	
	    		    	
	    	try {
	    		
	    		//Spinner selectSignSpinner = (Spinner) findViewById(R.id.keySelectSpinner);
	    		//Identity identity = (Identity) selectSignSpinner.getSelectedItem();
	    		
	    		//FIXME: max sigs?
	    		Identity[] signIds = new Identity[50];
	    		int j = 0;
	    		for(int i=0; i<signSelections.length; i++) {
	    			if (signSelections[i] == true) {
	    				signIds[j] = identities[i];
	    				j++;
	    			}
	    		}
	    		
	    		TextView txt2 = new TextView(v.getContext());
	    		if (signIds[0] != null) {
				    try {
				    	Log.v("WriteMsg", "Posting with signatures...");
						ring.postMsg(txtData,signIds);
			            txt2.setText("Posted message to " + ring.getShortname() + " with signature(s).");

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
	    	
	    	
    	 
	    	
	 }

		
};
	
}
