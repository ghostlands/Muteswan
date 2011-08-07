package org.muteswan.client.ui;

import org.muteswan.client.Base64;
import org.muteswan.client.R;
import org.muteswan.client.data.Identity;
import org.muteswan.client.data.IdentityStore;
import org.muteswan.client.data.Circle;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
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
	private IdentityStore idStore;
	
	 public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        
	        setContentView(R.layout.identitylist);
	        
	        idStore = new IdentityStore(getApplicationContext());
	        //Identity[] identityList = (Identity[]) idStore.toArray();
	        
	        identityList = idStore.asArray();
	        
	        
	        

	        ListAdapter listAdapter = new ArrayAdapter<Identity>(this,
	                android.R.layout.simple_list_item_1, identityList);
	          setListAdapter(listAdapter);
	         

	          registerForContextMenu(getListView());
	 }
	 
	 
	 @Override
	 public boolean onCreateOptionsMenu(Menu menu) {
	    	menu.clear();
	    
	    	
	    	
	    	MenuInflater inflater = getMenuInflater();
	        inflater.inflate(R.menu.idlistmenu, menu);
	        return true;
	 }
	 
	 
	 @Override
	 public boolean onOptionsItemSelected(MenuItem item) {
		 if (item.toString().equals("Create Identity")) {
			 startActivity(new Intent(this,GenerateIdentity.class));
		     return true;
		 }
		 return true;
	 }
	 
	 
	 public void onCreateContextMenu(ContextMenu menu, View v,
	                                 ContextMenuInfo menuInfo) {
	   super.onCreateContextMenu(menu, v, menuInfo);
	   MenuInflater inflater = getMenuInflater();
	  inflater.inflate(R.menu.idlistcontextmenu, menu);
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
		  
		  if (item.getTitle().equals("Delete")) {
			  Toast.makeText(getApplicationContext(), "Deleted identity " + identityList[info.position], Toast.LENGTH_LONG);
			  idStore.delete(identityList[info.position]);
		  }
		  
		  return super.onContextItemSelected(item);
		  
		}
	 
	
}
