package org.aftff.client.ui;

import java.io.IOException;

import org.aftff.client.R;
import org.aftff.client.aftff;
import org.aftff.client.data.Message;
import org.aftff.client.data.Ring;
import org.aftff.client.data.Store;
import org.apache.http.client.ClientProtocolException;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class RingList extends ListActivity {


	public static int SHARE = 0;
	public static int READ = 1;
	public static int WRITE = 2;
	public static String[] actionPrompts = new String[] { "Select a ring to share.", "Select a ring to read messages.", "Select a ring to write a message." };
	public Integer action;
	Bundle extra;
	public Ring[] ringList;

	
	private Store store;
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //String ringStr = savedInstanceState.getString("ring");
        //action = savedInstanceState.getInt("action");

        extra = getIntent().getExtras();
        
        
    	SharedPreferences prefs = getSharedPreferences(aftff.PREFS,0);
    	store = new Store(this,prefs);

        setContentView(R.layout.ringlist);
        
        TextView txt = (TextView) findViewById(R.id.android_ringlistprompt);
        txt.setText(actionPrompts[extra.getInt("action")]);
        
        
        ringList = getArray();
        //String[] ringList = new String[] { "first", "second" };
        
        
        
        //if (extra.getInt("action") == READ) {
          ListAdapter listAdapter = new ArrayAdapter<Ring>(this,
                android.R.layout.simple_list_item_1, ringList);
          setListAdapter(listAdapter);
        //}
    	
        //Toast.makeText(this, "action is " + extra.getInt("action"), Toast.LENGTH_LONG);
        
    }
    
    // should be part of store
    private Ring[] getArray() {
    	//return((Ring[])store.toArray());
    	//String[] ringList = new String[store.size()];
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
		// TODO Auto-generated method stub
		Intent intent = null;
		if (extra.getInt("action") == RingList.WRITE) {
			intent = new Intent(getApplicationContext(),WriteMsg.class);
		} else if (extra.getInt("action") == RingList.READ) {
			intent = new Intent(getApplicationContext(),MsgList.class);
	    } else if (extra.getInt("action") == RingList.SHARE) {
			intent = new Intent("com.google.zxing.client.android.ENCODE");
			intent.putExtra("ENCODE_DATA",ringList[position].getFullText());;
			intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");;
	    }
		
	
		//Bundle extr = intent.getExtras();
		//extr.putString("ring", ringList[position].getFullText());
		intent.putExtra("ring", ringList[position].getFullText());
		startActivity(intent);
	}
    
}
