package org.aftff.client.ui;

import org.aftff.client.R;
import org.aftff.client.data.Identity;
import org.aftff.client.data.IdentityStore;
import org.aftff.client.data.Ring;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

public class IdentityList extends ListActivity {

	 public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        
	        setContentView(R.layout.identitylist);
	        
	        IdentityStore idStore = new IdentityStore(getApplicationContext());
	        //Identity[] identityList = (Identity[]) idStore.toArray();
	        
	        Identity[] identityList = idStore.asArray();
	        
	        

	        ListAdapter listAdapter = new ArrayAdapter<Identity>(this,
	                android.R.layout.simple_list_item_1, identityList);
	          setListAdapter(listAdapter);
	        
	 }
	 
	 
	
}
