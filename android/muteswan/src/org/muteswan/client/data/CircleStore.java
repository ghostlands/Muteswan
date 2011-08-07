package org.muteswan.client.data;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import org.muteswan.client.muteswan;
import org.apache.http.client.ClientProtocolException;


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

final public class CircleStore extends LinkedList<Circle> {
	
	public class OpenHelper extends SQLiteOpenHelper {

			public static final int DATABASE_VERSION = 10;
			public static final String DATABASE_NAME = "muteswandb";
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
	



    public Context context;
	private OpenHelper openHelper;


	
	public CircleStore(Context applicationContext, boolean readDb) {
		context = applicationContext;
	    openHelper = new OpenHelper(context);
	  

		if (readDb) {
          initStore();
		}
	}
	
	public CircleStore(Context applicationContext) {
		// TODO Auto-generated constructor stub
		context = applicationContext;
	    openHelper = new OpenHelper(context);

		
        //initStore();
	}

	
	public CircleStore.OpenHelper getOpenHelper() {
		return openHelper;
	}
	
	  final public void deleteCircle(Circle circle) {
		  SQLiteDatabase db = openHelper.getWritableDatabase();
		  SQLiteStatement delete = db.compileStatement("DELETE FROM " + openHelper.RINGTABLE + " WHERE key = ? AND shortname = ? AND server = ?");
		  delete.bindString(1, circle.getKey());
		  delete.bindString(2, circle.getShortname());
		  delete.bindString(3, circle.getServer());
		  delete.execute();
		  db.close();
		  
		  
		  SQLiteDatabase rdb = circle.openHelper.getWritableDatabase();
		  delete = rdb.compileStatement("DELETE FROM " + Circle.OpenHelper.MESSAGESTABLE + " WHERE circleHash = ?");
		  delete.bindString(1, muteswan.genHexHash(circle.getFullText()));
		  delete.execute();
		  circle.openHelper.deleteData(rdb);
		  rdb.close();
	  }
	

	  
	final public String getAsString() {
		String returnString = "";
		
		for (Circle r : this) {
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
				Circle r = new Circle(context,key,shortname,server);
				if (r != null) 
				   add(r);
		  }
		  cursor.close();
		  db.close();
	  }
	  
	  
	 
	  
	  public void updateStore(String contents) {
		  Circle circle = new Circle(context,contents);
		  for (Circle r : this) {
			  if (r.getFullText().equals(contents)) {
				  return;
			  }
		  }
		  addCircleToDb(circle);
	  }
	  
	  public void updateStore(String key, String shortname, String server) {
		  Circle circle = new Circle(context,key,shortname,server);
		  for (Circle r : this) {
			  if (r.getKey().equals(key) && r.getShortname().equals(shortname) && r.getServer().equals(server)) {
				  return;
			  }
		  }
		  addCircleToDb(circle);
	  }
	  
	  private void addCircleToDb(Circle circle) {
		  SQLiteDatabase db = openHelper.getWritableDatabase();
		  SQLiteStatement insrt = db.compileStatement("INSERT INTO " + OpenHelper.RINGTABLE + " (key,shortname,server) VALUES (?,?,?)");
		  insrt.bindString(1, circle.getKey());
		  insrt.bindString(2, circle.getShortname());
		  insrt.bindString(3, circle.getServer());
		  insrt.execute();
		  db.close();
		  
		  SQLiteDatabase rdb = circle.openHelper.getWritableDatabase();
		  //muteswan.genHexHash(circle.getFullText()));
 		  SQLiteStatement insert = rdb.compileStatement("INSERT INTO " + Circle.OpenHelper.LASTMESSAGES + " (circleHash,lastMessage,lastCheck) VALUES(?,?,datetime('now'))");
		  insert.bindString(1,muteswan.genHexHash(circle.getFullText()));
		  insert.bindLong(2, 0);
		  insert.executeInsert();
		  rdb.close();
	  
		  add(circle);
	  }
	  

	  
	  public HashMap<String,Circle> asHashMap() {
		HashMap<String,Circle> map = new HashMap<String,Circle>();
		for (Circle r : this) {
			map.put(muteswan.genHexHash(r.getFullText()), r);
		}
		
		return map;
		  
	  }

	
	  
	

	
}
