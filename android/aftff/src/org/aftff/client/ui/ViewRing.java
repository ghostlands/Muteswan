package org.aftff.client.ui;

import java.util.HashMap;

import org.aftff.client.R;
import org.aftff.client.aftff;
import org.aftff.client.data.Ring;
import org.aftff.client.data.RingStore;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class ViewRing extends Activity implements Runnable {

	
	private Ring ring;


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle extras = getIntent().getExtras();
	    RingStore rs = new RingStore(getApplicationContext(),true);
	    HashMap<String,Ring> hashMap = rs.asHashMap();
	    ring = hashMap.get(aftff.genHexHash(extras.getString("ring")));
		setContentView(R.layout.viewring);
		
		TextView viewRingServer = (TextView) findViewById(R.id.viewRingServer);
		TextView viewRingName = (TextView) findViewById(R.id.viewRingName);
		TextView viewRingDescription = (TextView) findViewById(R.id.viewRingDescription);
		TextView viewRingLongDescription = (TextView) findViewById(R.id.viewRingLongDescription);
		TextView viewRingPostPolicy = (TextView) findViewById(R.id.viewRingPostPolicy);
		TextView viewRingAuthKey = (TextView) findViewById(R.id.viewRingAuthKey);

		
		viewRingServer.setText("@"+ring.getServer());
		viewRingName.setText(ring.getShortname());
		viewRingDescription.setText(ring.getDescription());
		viewRingLongDescription.setText(ring.getLongDescription());
		viewRingPostPolicy.setText(ring.getPostPolicy());
		viewRingAuthKey.setText(ring.getAuthKey());
		
		
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.viewringmenu, menu);
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Thread thread = new Thread(this);
		thread.start();
		return true;
	}
	
	
	@Override
	public void run() {
		ring.downloadManifest();
		doneDownloading.sendEmptyMessage(0);
	}
	
	final Handler doneDownloading = new Handler() {
		
        @Override
        public void handleMessage(Message msg) {
        	Toast.makeText(getApplicationContext(), "Downloaded manifest.", Toast.LENGTH_LONG);
        }
	};
    
	
}
