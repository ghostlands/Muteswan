package org.aftff.client.data;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.aftff.client.AftffHttp;
import org.aftff.client.Base64;
import org.aftff.client.Crypto;
import org.aftff.client.aftff;
import org.aftff.client.data.RingStore.OpenHelper;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

public class Ring {
     
	Ring.OpenHelper openHelper;
	
	final private String key;
	
	
	
	
	
	
	final private String shortname;
	final private String server;
	final private String notes = null;
	final private String keyHash;
	private AftffHttp aftffHttp;

	
	public Context context;

	private Integer curLastMsgId = 0;

	private SQLiteDatabase rdb;

	public class OpenHelper extends SQLiteOpenHelper {

		public static final int DATABASE_VERSION = 10;
		public String databaseName = "aftffdb";
		public static final String MESSAGESTABLE = "messages";
		public static final String SIGTABLE = "signatures";
		public static final String LASTMESSAGES = "lastmessages";

		
	     
	      public OpenHelper(Context context, String ringHash) {
	    	  super(context, ringHash, null, DATABASE_VERSION);
	    	  databaseName = ringHash;
		}

		@Override
	      public void onCreate(SQLiteDatabase db) {
	         db.execSQL("CREATE TABLE " + MESSAGESTABLE + " (id INTEGER PRIMARY KEY, ringHash TEXT, msgId INTEGER, date DATE, message TEXT)");
	         db.execSQL("CREATE TABLE " + SIGTABLE + " (id INTEGER PRIMARY KEY, msgId INTEGER, ringHash TEXT, signature TEXT)");
	         db.execSQL("CREATE TABLE " + LASTMESSAGES + " (ringHash TEXT PRIMARY KEY, lastMessage INTEGER, lastCheck DATE)");
	      }

	      @Override
	      public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	         db.execSQL("DROP TABLE IF EXISTS " + MESSAGESTABLE);
	         db.execSQL("DROP TABLE IF EXISTS " + SIGTABLE);
	         db.execSQL("DROP TABLE IF EXISTS " + LASTMESSAGES);
	         onCreate(db);
	      }
	      
	      public void deleteData(SQLiteDatabase db) {
	    	  db.execSQL("DELETE FROM " + MESSAGESTABLE);
		      db.execSQL("DELETE FROM " + SIGTABLE);
		      db.execSQL("DELETE FROM " + LASTMESSAGES);
	      }
	      
	   }


  

	
	
	
	
	public Ring(Context context, String contents) {
		Integer plusIndx = contents.indexOf("+");
		Integer atIndx = contents.indexOf("@");
		
		if (plusIndx == -1 || atIndx == -1) {
			this.key = null;
		    this.shortname = null;
		    this.server = null;
		    this.keyHash = null;
			return;
		}
		
		String name = contents.substring(0,plusIndx);
		String key = contents.substring(plusIndx+1,atIndx);
		String srv = contents.substring(atIndx+1,contents.length());
		
		if (name == null || key == null || srv == null) {
			this.key = null;
	        this.shortname = null;
	        this.server = null;
	        this.keyHash = null;
			return;
		}
		
		
		
		this.key = key;
		this.shortname = name;
		this.server = srv;
		this.keyHash = aftff.genHexHash(key);
		this.context = context;
		initHttp();
		this.openHelper = new Ring.OpenHelper(context, aftff.genHexHash(getFullText()));
		
	    aftffHttp = new AftffHttp();
	    curLastMsgId = 0;
		
		//this.add(newRing);
	}
	
	public Ring(Context context, String key, String shortname, String server) {
		super();
		this.key = key;
		this.shortname = shortname;
		this.server = server;
		this.context = context;

		this.keyHash = aftff.genHexHash(key);
		this.openHelper = new Ring.OpenHelper(context, aftff.genHexHash(getFullText()));
	    aftffHttp = new AftffHttp();
	    curLastMsgId = 0;
	}
	
	
	final public String getFullText() {
		return(getShortname()+"+"+getKey()+"@"+getServer());
	}
	
	
	public String getKey() {
		return key;
	}
	
	public Integer getLastMsgId() {
		if (curLastMsgId == 0) {
			curLastMsgId = getLastMessageId();
		}
		return(curLastMsgId);
	}
	
	public Integer getLastMessageId() {
		String ringHash = aftff.genHexHash(getFullText());
		SQLiteDatabase db = this.openHelper.getWritableDatabase();

		Integer lastMessageId = null;
		
		
		//ARGH WTF!!
		//if (!db.isOpen()) {
		//	return(null);
		//}
		
		Cursor cursor = db.query(Ring.OpenHelper.LASTMESSAGES, new String[] { "lastMessage" }, "ringHash = ?", new String[] { ringHash }, null, null, "lastMessage desc" );
		if (cursor.moveToFirst()) {
			lastMessageId = cursor.getInt(0);
		}
		cursor.close();
		//if (db.isOpen()) {
		  db.close();
		//}
		

		
		return(lastMessageId);
		
	}
	
	
	public AftffMessage getMsg(String id) throws ClientProtocolException, IOException {
		AftffMessage msg = null;
		msg = getMsgFromDb(id);
		if (msg == null) {
			
			msg = getMsgFromTor(id);
			
			if (msg == null)
				return null;
			
			if (context != null) {
				if (msg.signatures[0] != null) {
				   saveMsgToDb(Integer.parseInt(id),msg.getDate(),msg.getMsg(),msg.signatures);
				} else {
				   saveMsgToDb(Integer.parseInt(id),msg.getDate(),msg.getMsg());
				}
			}
		}
		
		return(msg);
		
	}
	
	public AftffMessage getMsgFromDb(String id) {
		AftffMessage msg = null;
		
		if (context == null)
			return(null);
		
		
		String ringHash = aftff.genHexHash(this.getFullText());
		
		SQLiteDatabase db = openHelper.getReadableDatabase();
		
		Cursor cursor = db.query(OpenHelper.MESSAGESTABLE, new String[] { "date", "message" }, "msgId = ? and ringHash = ?", new String[] { id, ringHash }, null, null, "id desc" );
		if (cursor.moveToFirst()) {
			String date = cursor.getString(0);
			String msgData = cursor.getString(1);
			
			Cursor cursorSig = db.query(OpenHelper.SIGTABLE, new String[] { "signature" }, "msgId = ? and ringHash = ?", new String[] { id, ringHash }, null, null, "signature desc" );
			
			//FIXME: max signatures?
			String[] signatures = new String[50];
			int i=0;
			SIG: while (cursorSig != null && cursorSig.moveToNext()) {
				String sigTxt = cursorSig.getString(0);
				signatures[i] = sigTxt;
				i++;
				if (cursor.isLast()) 
					break SIG;
			}
			cursorSig.close();
			
			if (signatures[0] != null) {
			   msg = new AftffMessage(this,Integer.parseInt(id),date,msgData,signatures);
			} else {
			   msg = new AftffMessage(this,Integer.parseInt(id),date,msgData);

			}		
			cursor.close();
			db.close();
			return(msg);
		}
		cursor.close();
		
		db.close();
		
		return(msg);
	}
		
	
	public AftffMessage getMsgFromTor(String id) throws ClientProtocolException, IOException {
		HttpGet httpGet = new HttpGet("http://" + server + "/" + keyHash + "/" + id);
		Log.v("Ring", "Fetching message " + id);
    	HttpResponse resp = aftffHttp.httpClient.execute(httpGet);
    	Log.v("Ring", "Fetched message " + id);
    	
    	return(parseMsgFromTor(Integer.parseInt(id),resp));
    	
	}
	
	
	private AftffMessage parseMsgFromTor(Integer id, HttpResponse resp) throws org.apache.http.ParseException, IOException {
		// TODO Auto-generated method stub

		
		String jsonString = EntityUtils.toString(resp.getEntity());
		if (jsonString == null) {
			Log.v("Ring", "WTF, jsonString is null");
			return null;
		}
		
    	Header lastModified = resp.getFirstHeader("Last-Modified");
    	AftffMessage msg = null;
    	String date = null;
    	
    	if (lastModified == null) {
			Log.v("Ring", "WTF, lastModified is null");
			return null;
    	}
    	
		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
		try {
			Date d = format.parse(lastModified.getValue());
			//date = d.getMonth()+1 + "/" + d.getDate() + " " + d.getHours() + ":" + d.getMinutes();
			SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
			//FIXME: hardcoded timezone!
			//TimeZone tz = TimeZone.getTimeZone( "EDT" );
	        //df.setTimeZone( tz );
	        date = df.format(d);
		} catch (ParseException e) {
			return(null);
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
    	
    	try {
			msg = new AftffMessage(id,this,jsonString,date);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
		
    	return(msg);
	}

	public Integer getMsgIndex() {
	  
		
	   HttpGet httpGet = new HttpGet("http://" + server + "/" + keyHash);
	   try {
	    HttpResponse resp = aftffHttp.httpClient.execute(httpGet);
	    Header lastMessage = resp.getFirstHeader("Last-Message");
	    
	    if (lastMessage == null)
	    	return 0;
	    
	    
	    Integer result = Integer.parseInt(lastMessage.getValue());
	    if (result == null)
	    	return 0;
	    	
		return(result);

	} catch (ClientProtocolException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
	}
	
	return(0);
	
	}
	
	public AftffMessage getMsgLongpoll(Integer id) {
		HttpGet httpGet = new HttpGet("http://" + server + "/" + keyHash + "/longpoll/" + id);
    	HttpResponse resp;
    	
    	Log.v("Ring", "getMsgLongpoll called for " + getShortname());
    	
		try {
			resp = aftffHttp.httpClient.execute(httpGet);
			AftffMessage msg = parseMsgFromTor(id,resp);
			if (msg.signatures[0] != null) {
				   saveMsgToDb(id,msg.getDate(),msg.getMsg(),msg.signatures);
			} else {
				   saveMsgToDb(id,msg.getDate(),msg.getMsg());
			}
			return(msg);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return(null);
    	
	}
	
	

	public String getNotes() {
		return notes;
	}
	
	public String getServer() {
		return server;
	}
	
	public String getShortname() {
		return shortname;
	}
	
		
	
	
	private void initHttp() {
		this.aftffHttp = new AftffHttp();
	}
	
	
	public void postMsg(JSONObject jsonObj) {
		
		HttpPost httpPost = new HttpPost("http://" + server + "/" + keyHash);
		ByteArrayEntity entity = new ByteArrayEntity(jsonObj.toString().getBytes());
		httpPost.setEntity(entity);
		
		try {
			aftffHttp.httpClient.execute(httpPost);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return;
		}
		
	}
	
	
	public void postMsg(String msg) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, JSONException {
		Crypto crypto = new Crypto(getKey().getBytes(), msg.getBytes());
		byte[] encData = crypto.encrypt();
		
		String base64EncData = Base64.encodeBytes(encData);
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("message", base64EncData);
		
		postMsg(jsonObj);
	}
	
	public void postMsg(String msg, Identity[] identities) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, JSONException, InvalidKeyException, SignatureException, InvalidKeySpecException, IOException {
		// TODO Auto-generated method stub
		Crypto crypto = new Crypto(getKey().getBytes(), msg.getBytes());
		byte[] encData = crypto.encrypt();
		
		String base64EncData = Base64.encodeBytes(encData);
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("message", base64EncData);
		
		JSONArray jsonArray = new JSONArray();
		
		
		
		for (int i=0; i<identities.length; i++) {
		    Signature sig = Signature.getInstance("MD5WithRSA");
		    
		    if (identities[i] == null) {
		    	Log.v("Ring", "Wtf, identities is null\n");
		    	break;
		    }
		    
		    Log.v("Ring", "Signing with " + identities[i].getName() + "\n");
		
		    RSAPrivateKey rsaPrivKey = identities[i].getPrivateKey();
		   
			sig.initSign(rsaPrivKey);
			sig.update(msg.getBytes("UTF8"));
			byte[] sigBytes = sig.sign();
			jsonArray.put(identities[i].pubKeyHash + ":" + Base64.encodeBytes(sigBytes));
			//encodedSigs[i] = identities[i].pubKeyHash + ":" + Base64.encodeBytes(sigBytes);
		}
		
		//jsonObj.put("signatures", encodedSigs);
		jsonObj.put("signatures", jsonArray);
		
		
		postMsg(jsonObj);
		
	}
	
	public boolean msgExists(SQLiteDatabase db, Integer id) {
		String ringHash = aftff.genHexHash(getFullText());
		Cursor cursor = db.query(OpenHelper.MESSAGESTABLE, new String[] { "date", "message" }, "msgId = ? and ringHash = ?", new String[] { id.toString(), ringHash }, null, null, "id desc" );
		if (cursor.getCount() != 0) {
			cursor.close();
			return(true);
		} else {
			cursor.close();
			return(false);
		}
	}
	
	// FIXME: should refactor
	public void saveMsgToDb(Integer id, String date, String msg) {
		if (context == null) 
			return;	
		
		String ringHash = aftff.genHexHash(getFullText());
		SQLiteDatabase db = openHelper.getWritableDatabase();
		if (msgExists(db,id)) {
			db.close();
			return;
		}

		SQLiteStatement insrt = db.compileStatement("INSERT INTO " + OpenHelper.MESSAGESTABLE + " (ringHash,msgId,date,message) VALUES (?,?,?,?)");
		insrt.bindString(1, ringHash);
		insrt.bindLong(2, id);
		insrt.bindString(3, date);
		insrt.bindString(4, msg);
		insrt.executeInsert();
		db.close();
	}
	
	
	//FIXME: should refactor
	public void saveMsgToDb(Integer id, String date, String msg, String[] signatures) {
		if (context == null) 
			return;	
		
		String ringHash = aftff.genHexHash(getFullText());
		SQLiteDatabase db = openHelper.getWritableDatabase();
		if (msgExists(db,id)) {
			db.close();
			return;
		}
		

		SQLiteStatement insrt = db.compileStatement("INSERT INTO " + OpenHelper.MESSAGESTABLE + " (ringHash,msgId,date,message) VALUES (?,?,?,?)");
		insrt.bindString(1, ringHash);
		insrt.bindLong(2, id);
		insrt.bindString(3, date);
		insrt.bindString(4, msg);
		insrt.executeInsert();
	
		for (int i=0; i<signatures.length; i++) {	
		  //FIXME: length for signatures
		  if (signatures[i] == null)
			  break;
		
		  insrt = db.compileStatement("INSERT INTO " + OpenHelper.SIGTABLE + " (msgId,ringHash,signature) VALUES (?,?,?)");
		  insrt.bindLong(1, id);
		  insrt.bindString(2, ringHash);
		  insrt.bindString(3,signatures[i]);
		  insrt.executeInsert();
		}
		db.close();
	}
	
	
	

	@Override
	public String toString() {
		return getShortname();
	}

	public void createLastMessage(Integer curIndex) {
		
		String ringHash = aftff.genHexHash(getFullText());
		SQLiteDatabase db = openHelper.getWritableDatabase();
		SQLiteStatement insrt = db.compileStatement("INSERT INTO " + OpenHelper.LASTMESSAGES + " (ringHash,lastMessage,lastCheck) VALUES (?,?,datetime('now'))");
		insrt.bindString(1, ringHash);
		insrt.bindLong(2, curIndex);
		insrt.executeInsert();
		db.close();
	}
	
	public void updateLastMessage(Integer curIndex) {
		curLastMsgId = curIndex;
	}
	
	public void saveLastMessage() {
		String ringHash = aftff.genHexHash(getFullText());
		
		SQLiteDatabase db = openHelper.getWritableDatabase();
		SQLiteStatement update = db.compileStatement("UPDATE " + OpenHelper.LASTMESSAGES + " SET lastMessage = ?, lastCheck = datetime('now') WHERE ringHash = ?");
		update.bindLong(1, curLastMsgId);
		update.bindString(2, ringHash);
		if (update.executeInsert() == -1) {
			SQLiteStatement insert = db.compileStatement("INSERT INTO " + OpenHelper.LASTMESSAGES + " (ringHash,lastMessage,lastCheck) VALUES(?,?,datetime('now'))");
			insert.bindString(1,ringHash);
			insert.bindLong(2, curLastMsgId);
			insert.executeInsert();
		}
		db.close();
	}
	
		
	

	



}
