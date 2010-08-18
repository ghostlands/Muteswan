package org.aftff.client.ui;

import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.aftff.client.R;
import org.aftff.client.aftff;
import org.aftff.client.R.id;
import org.aftff.client.R.layout;
import org.aftff.client.data.Ring;
import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class WriteMsg extends Activity {

	
	public void onCreate(Bundle savedInstanceState) {
	       super.onCreate(savedInstanceState);

	       setContentView(R.layout.writemsg);
	       final Button button = (Button) findViewById(R.id.submitMsg);
	       button.setOnClickListener(submitMsg);
	       
	}
	
	
	public Button.OnClickListener submitMsg = new Button.OnClickListener() {
	    public void onClick(View v) {
	    	EditText newMsgText = (EditText) findViewById(R.id.newMsgText);
	    	Editable txt = newMsgText.getText();
	    	String txtData = txt.toString();
	    	
	    	
	    	Ring ring = aftff.activeRing;
	    	
	    	try {
				ring.postMsg(txtData);
				TextView txt2 = new TextView(v.getContext());
		        txt2.setText("Posted message to " + ring.getShortname() + ".");
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
