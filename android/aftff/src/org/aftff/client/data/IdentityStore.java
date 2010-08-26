package org.aftff.client.data;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.LinkedList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

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
		openHelper.close();
		
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
		
		openHelper.close();

		
		return(true);
	}

	public Identity[] asArray(LinkedList<Identity> identityList) {
		Identity[] identityArr = new Identity[identityList.size()];
        int i = 0;
        for (Identity id : identityList) {
        	Log.v("IdentityStore", "id is " + id.name);
        	identityArr[i] = id;
        	i++;
        }
		return identityArr;
	}
	
	
	// only return identities with both public/private key pairs
	public Identity[] asArray(boolean signable) {
		if (signable) {
			LinkedList<Identity> signedIds = new LinkedList<Identity>();
			for (Identity id : this) {
				if (id.privateKeyEnc != null && id.privateKeyEnc.length() > 1) {
					signedIds.add(id);
				}
			}
			return asArray(signedIds);
		} else {
		  return asArray(this);
	    }
	}
	
	public Identity[] asArray() {
		return asArray(this);
	}
	
	public void delete(Identity identity) {
		OpenHelper openHelper = new OpenHelper(context);
		SQLiteDatabase db = openHelper.getWritableDatabase();
		
		SQLiteStatement delete = db.compileStatement("DELETE FROM " + OpenHelper.TABLE + " WHERE name=?");
		//(name,publicKey,privateKey,pubKeyHash,privKeyHash) VALUES (?,?,?,?,?)");
		delete.bindString(1, identity.name);
		delete.execute();
		this.remove(identity);
	}

	

	//public void save() {
	//	
	//}
	
}
