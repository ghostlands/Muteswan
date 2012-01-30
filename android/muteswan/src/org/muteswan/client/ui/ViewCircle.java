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

import java.util.HashMap;

import org.muteswan.client.MuteswanHttp;
import org.muteswan.client.R;
import org.muteswan.client.Main;
import org.muteswan.client.data.Circle;
import org.muteswan.client.data.CircleStore;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ViewCircle extends Activity implements Runnable {

	
	private Circle circle;


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle extras = getIntent().getExtras();
	    CircleStore rs = new CircleStore(getApplicationContext(),true,false);
	    HashMap<String,Circle> hashMap = rs.asHashMap();
	    circle = hashMap.get(Main.genHexHash(extras.getString("circle")));
		setContentView(R.layout.viewcircle);
		
		TextView viewCircleServer = (TextView) findViewById(R.id.viewCircleServer);
		TextView viewCircleName = (TextView) findViewById(R.id.viewCircleName);
		TextView viewCircleDescription = (TextView) findViewById(R.id.viewCircleDescription);
		TextView viewCircleLongDescription = (TextView) findViewById(R.id.viewCircleLongDescription);
		TextView viewCirclePostPolicy = (TextView) findViewById(R.id.viewCirclePostPolicy);
		TextView viewCircleAuthKey = (TextView) findViewById(R.id.viewCircleAuthKey);
		ImageView viewCircleImageView = (ImageView) findViewById(R.id.viewCircleImageView);

		
		viewCircleServer.setText("@"+circle.getServer());
		viewCircleName.setText(circle.getShortname());
		viewCircleDescription.setText(circle.getDescription());
		viewCircleLongDescription.setText(circle.getLongDescription());
		viewCirclePostPolicy.setText(circle.getPostPolicy());
		viewCircleAuthKey.setText(circle.getAuthKey());
		
		if (circle.getImage() != null)
		  viewCircleImageView.setImageBitmap(BitmapFactory.decodeByteArray(circle.getImage(), 0, circle.getImage().length));
		
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.viewcirclemenu, menu);
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
		circle.downloadManifest();
		doneDownloading.sendEmptyMessage(0);
	}
	
	final Handler doneDownloading = new Handler() {
		
        @Override
        public void handleMessage(Message msg) {
        	Toast.makeText(getApplicationContext(), "Downloaded manifest.", Toast.LENGTH_LONG);
        }
	};
    
	
}
