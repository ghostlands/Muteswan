package org.aftff.client.data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import org.aftff.client.aftff;


import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

final public class RingStore extends LinkedList<Ring> {
	
	public class OpenHelper extends SQLiteOpenHelper {

			public static final int DATABASE_VERSION = 10;
			public static final String DATABASE_NAME = "aftffdb";
			public static final String RINGTABLE = "rings";

			
		     
		      public OpenHelper(Context context) {
				// TODO Auto-generated constructor stub
		    	  super(context, DATABASE_NAME, null, DATABASE_VERSION);
			}

			@Override
		      public void onCreate(SQLiteDatabase db) {
		         db.execSQL("CREATE TABLE IF NOT EXISTS " + RINGTABLE + " (id INTEGER PRIMARY KEY, shortname TEXT, key TEXT, server TEXT)");
		      }

		      @Override
		      public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		         db.execSQL("DROP TABLE IF EXISTS " + RINGTABLE);
		         onCreate(db);
		      }
		      
		   }
	
    public class ringListAdapter implements ListAdapter {

		@Override
		public boolean areAllItemsEnabled() {
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
		public boolean isEnabled(int arg0) {
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


	
    public Context context;

	private OpenHelper openHelper;

	
	public RingStore(Context applicationContext, boolean readDb) {
		// TODO Auto-generated constructor stub
		context = applicationContext;
	    openHelper = new OpenHelper(context);

		if (readDb) {
          initStore();
		}
	}
	
	public RingStore(Context applicationContext) {
		// TODO Auto-generated constructor stub
		context = applicationContext;
	    openHelper = new OpenHelper(context);

		
        //initStore();
	}

	
	public RingStore.OpenHelper getOpenHelper() {
		return openHelper;
	}
	
	  final public void deleteRing(Ring ring) {
		  SQLiteDatabase db = openHelper.getWritableDatabase();
		  SQLiteStatement delete = db.compileStatement("DELETE FROM " + openHelper.RINGTABLE + " WHERE key = ? AND shortname = ? AND server = ?");
		  delete.bindString(1, ring.getKey());
		  delete.bindString(2, ring.getShortname());
		  delete.bindString(3, ring.getServer());
		  delete.execute();
		  
		  delete = db.compileStatement("DELETE FROM " + Ring.OpenHelper.MESSAGESTABLE + " WHERE ringHash = ?");
		  delete.bindString(1, aftff.genHexHash(ring.getFullText()));
		  delete.execute();
		  db.close();
	  }
	

	  
	final public String getAsString() {
		String returnString = "";
		
		for (Ring r : this) {
			returnString = returnString + r.getFullText() + "---";
		}
		return(returnString);
	}
	  
	  
	  
	  private void initStore() {
		  SQLiteDatabase db = openHelper.getReadableDatabase();
			
		  Cursor cursor = db.query(openHelper.RINGTABLE, new String[] { "shortname", "key", "server"}, null, null, null, null, "shortname desc" );
		  while (cursor.moveToNext()) {
				String shortname = cursor.getString(0);
				String key = cursor.getString(1);
				String server = cursor.getString(2);
				Ring r = new Ring(context,key,shortname,server);
				if (r != null) 
				   add(r);
		  }
		  cursor.close();
		  db.close();
	  }
	  
	  
	 
	  
	  public void updateStore(String contents) {
		  Ring ring = new Ring(context,contents);
		  for (Ring r : this) {
			  if (r.getFullText().equals(contents)) {
				  return;
			  }
		  }
		  addRingToDb(ring);
	  }
	  
	  public void updateStore(String key, String shortname, String server) {
		  Ring ring = new Ring(context,key,shortname,server);
		  for (Ring r : this) {
			  if (r.getKey().equals(key) && r.getShortname().equals(shortname) && r.getServer().equals(server)) {
				  return;
			  }
		  }
		  addRingToDb(ring);
	  }
	  
	  private void addRingToDb(Ring ring) {
		  SQLiteDatabase db = openHelper.getWritableDatabase();
		  SQLiteStatement insrt = db.compileStatement("INSERT INTO " + OpenHelper.RINGTABLE + " (key,shortname,server) VALUES (?,?,?)");
		  insrt.bindString(1, ring.getKey());
		  insrt.bindString(2, ring.getShortname());
		  insrt.bindString(3, ring.getServer());
		  insrt.execute();
		  db.close();
		  
		  add(ring);
	  }
	  

	  
	  public HashMap<String,Ring> asHashMap() {
		HashMap<String,Ring> map = new HashMap<String,Ring>();
		for (Ring r : this) {
			map.put(aftff.genHexHash(r.getFullText()), r);
		}
		
		return map;
		  
	  }

	
	  
	private void updateLatestMessages(ArrayList<AftffMessage> msgs, Ring r,
									Integer start, Integer last) {
		IdentityStore idStore = new IdentityStore(context);
		Integer lastId = r.getLastMessageId();
		
		if (lastId == null || lastId == 0)
			return;
		
		if (start != 0)
			lastId = lastId - start;
		
		if (lastId <= 0)
			return;
		
		RING: for (Integer i = lastId; i>lastId-last; i--) {
		  if (i <= 0)
				break;
		  AftffMessage msg = r.getMsgFromDb(i.toString());

		  if (msg != null) {
			Log.v("RingStore", msg.getId() + " loaded.");
		    msg.verifySignatures(idStore);

			if (msgs.size() == 0) {
				msgs.add(msg);
				continue RING;
			}
			
			
			Integer insertIndex = msgs.size()-1;
			MSGS: for (int j = msgs.size()-1; j>=0; j--) {
			//MSGS: for (AftffMessage omsg : msgs) {
				SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
				AftffMessage omsg = msgs.get(j);
				
				try {
					Date mDate = df.parse(msg.getDate());
					Date oDate = df.parse(omsg.getDate());
					if (mDate.after(oDate)) {
					//	Log.v("RingStore", mDate.toGMTString() + " before " + oDate.toGMTString());
						insertIndex = j;
						
						//break;
					} else {
						
						break;
					}
					
					
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			//Log.v("RingStore", "insertIndex is " + insertIndex);
			if (msgs.size()-1 == insertIndex) {
			  msgs.add(msg);
			} else {
			  msgs.add(insertIndex,msg);
			}
			
		  }
		}
		
		
	}
	
	public ArrayList<AftffMessage> getLatestMessages(String ringHash,
			Integer first, Integer last) {
		ArrayList<AftffMessage> msgs = new ArrayList<AftffMessage>();
		updateLatestMessages(msgs,asHashMap().get(ringHash),first,last);
		return(msgs);
    }
	
	
	
	
	public ArrayList<AftffMessage> getLatestMessages(ArrayList<AftffMessage> msgs, String ringHash,
			Integer first, Integer last) {
		updateLatestMessages(msgs,asHashMap().get(ringHash),first,last);		
		return(msgs);
    }
	

	
	public ArrayList<AftffMessage> getLatestMessages(Integer first, Integer last) {
		ArrayList<AftffMessage> msgs = new ArrayList<AftffMessage>();
		
		for (Ring r : this) {
			updateLatestMessages(msgs,r,first,last);
		}
		
		return(msgs);
	}
	
	public ArrayList<AftffMessage> getLatestMessages(ArrayList<AftffMessage> msgs, Integer first,Integer amount) {
		
		for (Ring r : this) {
			updateLatestMessages(msgs,r,first,amount);
		}
		
		return(msgs);
	}


	
}
