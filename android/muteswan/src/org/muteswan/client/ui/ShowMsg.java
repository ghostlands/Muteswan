package org.muteswan.client.ui;

import org.muteswan.client.R;
import org.muteswan.client.R.id;
import org.muteswan.client.R.layout;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;


public class ShowMsg extends Activity {

	
	public void onCreate(Bundle savedInstanceState) {
	       super.onCreate(savedInstanceState);

	       setContentView(R.layout.showmsg);
	       TextView msg = (TextView) findViewById(R.id.msgText);
	       
	}

	
}