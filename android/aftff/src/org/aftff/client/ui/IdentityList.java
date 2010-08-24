package org.aftff.client.ui;

import org.aftff.client.Base64;
import org.aftff.client.R;
import org.aftff.client.data.Identity;
import org.aftff.client.data.IdentityStore;
import org.aftff.client.data.Ring;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class IdentityList extends ListActivity {

	public Identity[] identityList;
	
	 public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        
	        setContentView(R.layout.identitylist);
	        
	        IdentityStore idStore = new IdentityStore(getApplicationContext());
	        //Identity[] identityList = (Identity[]) idStore.toArray();
	        
	        identityList = idStore.asArray();
	        
	        

	        ListAdapter listAdapter = new ArrayAdapter<Identity>(this,
	                android.R.layout.simple_list_item_1, identityList);
	          setListAdapter(listAdapter);
	         

	          registerForContextMenu(getListView());
	 }
	 
	 
	 
	 public void onCreateContextMenu(ContextMenu menu, View v,
	                                 ContextMenuInfo menuInfo) {
	   super.onCreateContextMenu(menu, v, menuInfo);
	   MenuInflater inflater = getMenuInflater();
	  inflater.inflate(R.menu.idlistcontextmenu, menu);
	  //menu.add("wtf");
	 }
	 
	 public boolean onContextItemSelected(MenuItem item) {
		  AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		  
		  if (item.getTitle().equals("Share")) {
			  Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
			  intent.putExtra("ENCODE_DATA",identityList[info.position].getShareableString());
					  //Base64.encodeBytes(identityList[info.position].publicKeyEnc.getBytes()));
			  intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
			  startActivity(intent);
		  }
		  
		  return super.onContextItemSelected(item);
		  
		}
	 
	
}
