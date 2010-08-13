package org.aftff.client;

import java.util.LinkedList;

import android.content.SharedPreferences;

public class Store extends LinkedList<Ring> {

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

}
