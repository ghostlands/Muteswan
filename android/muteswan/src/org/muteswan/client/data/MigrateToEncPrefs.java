package org.muteswan.client.data;

import java.io.File;
import java.util.LinkedList;

import org.muteswan.client.MuteLog;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

public class MigrateToEncPrefs {

	
	public LinkedList<String[]> getOldCircleData() {
		
		LinkedList<String[]> circles = new LinkedList<String[]>();
		
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase("/data/data/org.muteswan.client/databases/muteswandbOld",null);
		Cursor c = db.query("rings", new String[] { "shortname", "key", "server"}, null, null, null, null,"shortname desc");
		
		while (c.moveToNext()) {
			circles.add(new String[] { c.getString(0), c.getString(1), c.getString(2) });
		}
		c.close();
		db.close();
		
		return(circles);
		
	}

	public boolean needsMigration(Context ctx) {
		
		File database = new File("/data/data/org.muteswan.client/databases/muteswandb");
		if (database.exists()) {
			return true;
		}
		
	
		return(false);
	}
}
