package org.aftff.client.data;

import java.util.LinkedList;

import org.aftff.client.aftff;


import android.content.Context;
import android.content.SharedPreferences;

public class Store extends LinkedList<Ring> {
	
	public Context context;

	
    // FIXME: fix duplication
	public Store(Context applicationContext, SharedPreferences prefs) {
		// TODO Auto-generated constructor stub
		context = applicationContext;
		String storeString = prefs.getString("store", null);
        if (storeString == null || storeString.equals(""))
        	return;
        initStore(storeString);
        
	}

	public Store() {
		// TODO Auto-generated constructor stub
		context = null;
	}

	public Store(SharedPreferences prefs) {
		// TODO Auto-generated constructor stub
		context = null;
		String storeString = prefs.getString("store", null);
        if (storeString == null || storeString.equals(""))
        	return;
        initStore(storeString);
	}
	
	private void initStore(String storeString) {
        String[] storeArr = storeString.split("---");
        
        for (String keyStr : storeArr) {
        	if (keyStr == null)
        		continue;
        	Ring r;
        	if (context == null) {
        		r = new Ring(keyStr);
        	} else {
        		r = new Ring(context,keyStr);
        	}
        	if (r == null)
        		continue;
        	add(r);
        }
	}

	public String getAsString() {
		String returnString = "";
		
		for (Ring r : this) {
			returnString = returnString + r.getFullText() + "---";
		}
		return(returnString);
	}
	
	  public void updateStore(String contents, SharedPreferences prefs) {
	    	boolean haveRing = false;
	        for (Ring r : this) {
	        	if (r.getFullText().equals(contents)) {
	        		haveRing = true;
	        		return;
	        	}
	        }
	  
	        String storeString;
	        if (haveRing == false) {
	        	
	        	if (this.isEmpty()) {
	        		 storeString = contents + "---";
	        	} else {
	        	     storeString = this.getAsString() + contents + "---";
	        	}
	        	
	        	SharedPreferences.Editor prefEd = prefs.edit();
	        	prefEd.putString("store", storeString);
	        	prefEd.commit();
	        }
	    }
	  
	  public void deleteRing(Ring ring, SharedPreferences prefs) {
		  String storeString = "";
		  for (Ring r : this) {
			  if (r.getFullText().equals(ring.getFullText())) {
				  continue;
			  } else {
				  storeString = storeString + r.getFullText() + "---";
			  }
		  }
		  SharedPreferences.Editor prefEd = prefs.edit();
		  prefEd.putString("store",storeString);
		  prefEd.commit();
	  }

}
