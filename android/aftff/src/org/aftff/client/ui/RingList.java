package org.aftff.client.ui;

import java.io.IOException;

import org.aftff.client.R;
import org.aftff.client.aftff;
import org.aftff.client.data.AftffMessage;
import org.aftff.client.data.Identity;
import org.aftff.client.data.IdentityStore;
import org.aftff.client.data.Ring;
import org.aftff.client.data.RingStore;
import org.apache.http.client.ClientProtocolException;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class RingList extends ListActivity {


	public static int SHARE = 0;
	public static int READ = 1;
	public static int WRITE = 2;
	public static int ANY = 3;
	public static String[] actionPrompts = new String[] { "Select a ring to share.", 
														  "Select a ring to read messages.", 
														  "Select a ring to write a message.",
														  "Long-press a ring for actions."};
	public Integer action;
	Bundle extra;
	public Ring[] ringList;
	private String initialText;

	
	private RingStore store;
	private ArrayAdapter<Ring> listAdapter;
	
	@Override
	public void onResume() {
		super.onResume();
    	store = new RingStore(this,true);
        ringList = getArray();
        listAdapter = new ArrayAdapter<Ring>(this,
                android.R.layout.simple_list_item_1, ringList);
          setListAdapter(listAdapter);
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
       

        extra = getIntent().getExtras();
        action = extra.getInt("action");
        initialText = extra.getString("initialText");
        
    	store = new RingStore(this,true);

        setContentView(R.layout.ringlist);
        
        
        TextView txt = (TextView) findViewById(R.id.android_ringlistprompt);
        LinearLayout ringlist = (LinearLayout) findViewById(R.id.ringlistButtons);
        txt.setText(actionPrompts[action]);
        
        
        if (action == null || action == ANY) {
          Button addRing = (Button) findViewById(R.id.android_ringlistAddRing);
          Button createRing = (Button) findViewById(R.id.android_ringlistCreateRing);
          addRing.setOnClickListener(addRingListener);
          createRing.setOnClickListener(createRingListener);
          ringlist.setVisibility(View.VISIBLE);
        } else {
          ringlist.setVisibility(View.GONE);
        }

        
        
        ringList = getArray();
        registerForContextMenu(getListView());

      
          listAdapter = new ArrayAdapter<Ring>(this,
                android.R.layout.simple_list_item_1, ringList);
          setListAdapter(listAdapter);
       
        
    }
    
    //FIXME: should be part of store
    private Ring[] getArray() {
    	
    	Ring[] ringList = new Ring[store.size()];
    	int i = 0;
    	for (Ring r : store) {
    		ringList[i] = r;
    		i++;
    	}
    	
    	return(ringList);
    }
    
    
    protected void onListItemClick(ListView parent, View v, int position, long id)
    	 	{
    	
    			dispatchActivity(position);

    			
    	 	}

	private void dispatchActivity(int position) {
		Intent intent = null;
		if (action == WRITE) {
			intent = new Intent(getApplicationContext(),WriteMsg.class);
		} else if (action == READ) {
			intent = new Intent(getApplicationContext(),LatestMessages.class);
	    } else if (action == SHARE) {
			intent = new Intent("com.google.zxing.client.android.ENCODE");
			intent.putExtra("ENCODE_DATA",ringList[position].getFullText());;
			intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");;
	    } else if (action == ANY) {
	    	return;
	    }
		
	
		
		intent.putExtra("ring", ringList[position].getFullText());
		intent.putExtra("initialText", initialText);
		startActivity(intent);

	}
	
	public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
			super.onCreateContextMenu(menu, v, menuInfo);
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.ringlistcontextmenu, menu);
	}

	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
		
		switch (item.getItemId()) {
		  case R.id.ringListDelete:
			  deleteRing(info.position);
			  break;
		  case R.id.ringListRead:
			  showMsgList(info.position);
			  break;
		  case R.id.ringListShare:
			  shareRing(info.position);
			  break;
		  case R.id.ringListWriteMsg:
			  writeMsg(info.position);
			  break;
		}
		return true;
	
	}

	private Button.OnClickListener addRingListener  = new Button.OnClickListener() {
        public void onClick( View v ) {
        	Intent intent = new Intent("com.google.zxing.client.android.SCAN");
	        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
	        startActivityForResult(intent, 0);
         }
     };
	
     
     private Button.OnClickListener createRingListener = new Button.OnClickListener() {
    	 public void onClick(View v) {
    		 startActivity(new Intent(getApplicationContext(),CreateRing.class));
    	     return;
    	 }
     };
     
	
	private void deleteRing(int position) {
		RingStore store = new RingStore(getApplicationContext());
		store.deleteRing(ringList[position]);
		Toast.makeText(this,
				"Deleted ring " + ringList[position].getShortname() + " from saved keys.", 
					  Toast.LENGTH_LONG).show();
		onResume();
		
	}

	private void writeMsg(Integer position) {
		Intent intent = new Intent(getApplicationContext(),WriteMsg.class);
		intent.putExtra("ring", ringList[position].getFullText());
		startActivity(intent);
	}

	private void shareRing(Integer position) {
		Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
		intent.putExtra("ENCODE_DATA",ringList[position].getFullText());
		intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
		startActivity(intent);
	}

	private void showMsgList(Integer position) {
		Intent intent = new Intent(getApplicationContext(),LatestMessages.class);
		intent.putExtra("ring", aftff.genHexHash(ringList[position].getFullText()));
		startActivity(intent);
	}

	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	  if (requestCode == 0) {
    	    if (resultCode == RESULT_OK) {
    	    	    //Handle successful scan
    	            String contents = intent.getStringExtra("SCAN_RESULT");
    	            String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
    	            
    	            int atIndex = contents.indexOf("@");
    	            
    	            // RING
    	            if (atIndex != -1) {
    	            
      	              RingStore store = new RingStore(getApplicationContext(),true);
    	              Ring ring = new Ring(getApplicationContext(),contents);
    	              store.updateStore(contents);
    	               
    	              //this.activeRing = ring;
    	    //          selectMsg(ring);
    	            
    	            // IDENTITY
    	            } else {
    	            	String[] parts = contents.split(":");
    	            	Identity identity = new Identity(parts[0],parts[1],parts[2]);
    	            	IdentityStore idStore = new IdentityStore(getApplicationContext());
    	            	idStore.addToDb(identity);
    	            }
    	            
    	            
    	            
            } else if (resultCode == RESULT_CANCELED) {
            	//final String testSite = "forest+0df46018575f1656@tckwndlytrphlpyo.onion";
            	//final String testSite = "2ndset+1522c03e8b9bae5d@tckwndlytrphlpyo.onion";
            	final String testSite = "testsite+dba4fe6ef22b494d@tckwndlytrphlpyo.onion";

	            RingStore store = new RingStore(getApplicationContext(),true);
            	Ring ring = new Ring(getApplicationContext(),testSite);
 	            //updateStore(testSite);
	            store.updateStore(testSite);

            	            	
            	//this.activeRing = ring;
           // 	selectMsg(ring);
            
            }
    	  }

	}
}