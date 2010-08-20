package org.aftff.client.data;

import java.util.LinkedList;

import org.aftff.client.aftff;


import android.content.Context;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

final public class Store extends LinkedList<Ring> {
	
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

	final public String getAsString() {
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
	  
	  final public void deleteRing(Ring ring, SharedPreferences prefs) {
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
	  
	  
	  
	  
	  public class ringListAdapter implements ListAdapter {

		@Override
		public boolean areAllItemsEnabled() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isEnabled(int arg0) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getItemViewType(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getViewTypeCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean hasStableIds() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isEmpty() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void registerDataSetObserver(DataSetObserver observer) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver observer) {
			// TODO Auto-generated method stub
			
		}
		  
	  }

}
