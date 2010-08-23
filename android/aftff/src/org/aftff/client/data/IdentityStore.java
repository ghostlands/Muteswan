package org.aftff.client.data;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.LinkedList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

public class IdentityStore extends LinkedList<Identity> {
	
	
	private class OpenHelper extends SQLiteOpenHelper {

		private static final int DATABASE_VERSION = 1;
		private static final String DATABASE_NAME = "identities";
		private static final String TABLE = "identities";

		
	     
	      public OpenHelper(Context context) {
			// TODO Auto-generated constructor stub
	    	  super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
	      public void onCreate(SQLiteDatabase db) {
	         db.execSQL("CREATE TABLE " + TABLE + " (name TEXT PRIMARY KEY, privateKey TEXT, publicKey TEXT, privKeyHash TEXT, pubKeyHash TEXT)");
	      }

	      @Override
	      public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	         db.execSQL("DROP TABLE IF EXISTS " + TABLE);
	         onCreate(db);
	      }
	      
	   }

	private Context context;
	
	
	public IdentityStore(Context context) {
		this.context = context;
		OpenHelper openHelper = new OpenHelper(context);
		SQLiteDatabase db = openHelper.getWritableDatabase();
		
		Cursor cursor = db.query(openHelper.TABLE, new String[] { "name", "privateKey", "publicKey", "privKeyHash", "pubKeyHash" }, null, null, null, null, "name desc" );
		
		while (cursor.moveToNext()) {
			
			String name = cursor.getString(0);
			String privateKeyEnc = cursor.getString(1);
			String publicKeyEnc = cursor.getString(2);
			String privKeyHash = cursor.getString(3);
			String pubKeyHash = cursor.getString(4);
			
			Identity identity = new Identity(name,publicKeyEnc,privateKeyEnc,pubKeyHash,privKeyHash);
			this.add(identity);
			
			if (cursor.isLast()) 
				break;
			
		}
		cursor.close();
		
	}

	public boolean addToDb(Identity identity) {
		add(identity);
		OpenHelper openHelper = new OpenHelper(context);
		SQLiteDatabase db = openHelper.getWritableDatabase();
		
		SQLiteStatement insrt = db.compileStatement("INSERT INTO " + OpenHelper.TABLE + " (name,publicKey,privateKey,pubKeyHash,privKeyHash) VALUES (?,?,?,?,?)");
		insrt.bindString(1, identity.name);
		insrt.bindString(2, identity.publicKeyEnc);
		insrt.bindString(3, identity.privateKeyEnc);
		insrt.bindString(4, identity.getPubKeyHash());
		insrt.bindString(5, identity.getPrivKeyHash());
		insrt.execute();
		
		
		
		return(true);
	}

	public Identity[] asArray() {
		// TODO Auto-generated method stub
		Identity[] identityList = new Identity[this.size()];
        int i = 0;
        for (Identity id : this) {
        	identityList[i] = id;
        	i++;
        }
		return identityList;
	}

	//public void save() {
	//	
	//}
	
}
