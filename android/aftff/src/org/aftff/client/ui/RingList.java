package org.aftff.client.ui;

import java.io.IOException;

import org.aftff.client.R;
import org.aftff.client.aftff;
import org.aftff.client.data.AftffMessage;
import org.aftff.client.data.Ring;
import org.aftff.client.data.RingStore;
import org.apache.http.client.ClientProtocolException;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
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
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
       

        extra = getIntent().getExtras();
        action = extra.getInt("action");
        initialText = extra.getString("initialText");
        
    	store = new RingStore(this,true);

        setContentView(R.layout.ringlist);
        
        TextView txt = (TextView) findViewById(R.id.android_ringlistprompt);
        txt.setText(actionPrompts[action]);
        
        
        ringList = getArray();
        registerForContextMenu(getListView());

      
          ListAdapter listAdapter = new ArrayAdapter<Ring>(this,
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
			intent = new Intent(getApplicationContext(),MsgList.class);
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

	private void deleteRing(int position) {
		RingStore store = new RingStore(getApplicationContext());
		store.deleteRing(ringList[position]);
		Toast.makeText(this,
				"Deleted ring " + ringList[position].getShortname() + " from saved keys.", 
					  Toast.LENGTH_LONG).show();
		
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
		Intent intent = new Intent(getApplicationContext(),MsgList.class);
		intent.putExtra("ring", ringList[position].getFullText());
		startActivity(intent);
	}
}