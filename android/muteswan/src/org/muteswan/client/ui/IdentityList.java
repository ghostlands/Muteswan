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

import org.muteswan.client.R;
import org.muteswan.client.data.Identity;
import org.muteswan.client.data.IdentityStore;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.Toast;

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
