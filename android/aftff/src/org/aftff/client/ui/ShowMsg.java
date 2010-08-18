package org.aftff.client.ui;

import org.aftff.client.R;
import org.aftff.client.R.id;
import org.aftff.client.R.layout;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;


public class ShowMsg extends Activity {

	
	public void onCreate(Bundle savedInstanceState) {
	       super.onCreate(savedInstanceState);

	       setContentView(R.layout.showmsg);
	       TextView msg = (TextView) findViewById(R.id.msgText);
	       //msg.setText(getIntent().getStringExtra("msg"));
	       
	}

	
}