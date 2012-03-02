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
package org.muteswan.client.data;

import java.util.HashMap;
import java.util.LinkedList;

import org.muteswan.client.Main;
import org.muteswan.client.MuteLog;
import org.muteswan.client.MuteswanHttp;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

@SuppressWarnings("serial")
final public class CircleStore extends LinkedList<Circle> {
	
	public class OpenHelper extends SQLiteOpenHelper {

			public static final int DATABASE_VERSION = 10;
			public static final String DATABASE_NAME = "muteswandb";
			public static final String RINGTABLE = "rings";

			
		     
		      public OpenHelper(Context context) {
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
	private MuteswanHttp muteswanHttp;


	
	public CircleStore(Context applicationContext, boolean readDb, boolean initCache, MuteswanHttp muteswanHttp) {
		MuteLog.Log("CircleStore", "Circle store called!");
		context = applicationContext;
	    openHelper = new OpenHelper(context);
	    this.muteswanHttp = muteswanHttp;
	  

		if (readDb && initCache) {
          initStore(muteswanHttp,true);
		} else if (readDb) {
			initStore(muteswanHttp,false);
		}
	}
	
	public CircleStore(Context applicationContext, boolean readDb, boolean initCache) {
		MuteLog.Log("CircleStore", "Circle store called!");
		context = applicationContext;
	    openHelper = new OpenHelper(context);
	  

		if (readDb && initCache) {
          initStore(true);
		} else if (readDb) {
			initStore(false);
		}
	}
	
	public CircleStore(Context applicationContext) {
		context = applicationContext;
	    openHelper = new OpenHelper(context);
	}

	
	public CircleStore.OpenHelper getOpenHelper() {
		return openHelper;
	}
	
	  final public void deleteCircle(Circle circle) {
		  SQLiteDatabase db = openHelper.getWritableDatabase();
		  SQLiteStatement delete = db.compileStatement("DELETE FROM " + OpenHelper.RINGTABLE + " WHERE key = ? AND shortname = ? AND server = ?");
		  delete.bindString(1, circle.getKey());
		  delete.bindString(2, circle.getShortname());
		  delete.bindString(3, circle.getServer());
		  delete.execute();
		  db.close();
		  
		  
		  SQLiteDatabase rdb = circle.getOpenHelper().getWritableDatabase();
		  delete = rdb.compileStatement("DELETE FROM " + Circle.OpenHelper.MESSAGESTABLE + " WHERE ringHash = ?");
		  delete.bindString(1, Main.genHexHash(circle.getFullText()));
		  delete.execute();
		  circle.getOpenHelper().deleteData(rdb);
		  rdb.close();
	  }
	

	  
	final public String getAsString() {
		String returnString = "";
		
		for (Circle r : this) {
			returnString = returnString + r.getFullText() + "---";
		}
		return(returnString);
	}
	  
	  
	  
	  private void initStore(MuteswanHttp muteswanHttp, boolean initCache) {
		  SQLiteDatabase db = openHelper.getReadableDatabase();
			
		  Cursor cursor = db.query(OpenHelper.RINGTABLE, new String[] { "shortname", "key", "server"}, null, null, null, null, "shortname desc" );
		  while (cursor.moveToNext()) {
				String shortname = cursor.getString(0);
				String key = cursor.getString(1);
				String server = cursor.getString(2);
				Circle r = new Circle(context,key,shortname,server,muteswanHttp);
				if (r != null) { 
				   add(r);
				   if (initCache)
				     r.initCache();
				}
		  }
		  cursor.close();
		  db.close();
	  }
	  
	  private void initStore(boolean initCache) {
		  SQLiteDatabase db = openHelper.getReadableDatabase();
			
		  Cursor cursor = db.query(OpenHelper.RINGTABLE, new String[] { "shortname", "key", "server"}, null, null, null, null, "shortname desc" );
		  while (cursor.moveToNext()) {
				String shortname = cursor.getString(0);
				String key = cursor.getString(1);
				String server = cursor.getString(2);
				Circle r = new Circle(context,key,shortname,server);
				if (r != null) { 
				   add(r);
				   if (initCache)
				     r.initCache();
				}
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
		  
		  SQLiteDatabase rdb = circle.getOpenHelper().getWritableDatabase();
		  //muteswan.genHexHash(circle.getFullText()));
 		  SQLiteStatement insert = rdb.compileStatement("INSERT INTO " + Circle.OpenHelper.LASTMESSAGES + " (ringHash,lastMessage,lastCheck) VALUES(?,?,datetime('now'))");
		  insert.bindString(1,Main.genHexHash(circle.getFullText()));
		  insert.bindLong(2, 0);
		  insert.executeInsert();
		  rdb.close();
	  
		  add(circle);
	  }
	  

	  
	  public HashMap<String,Circle> asHashMap() {
		HashMap<String,Circle> map = new HashMap<String,Circle>();
		for (Circle r : this) {
			map.put(Main.genHexHash(r.getFullText()), r);
		}
		
		return map;
		  
	  }

	
	  
	

	
}
