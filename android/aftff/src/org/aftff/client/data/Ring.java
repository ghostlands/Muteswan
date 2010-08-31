package org.aftff.client.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.aftff.client.AftffHttp;
import org.aftff.client.Base64;
import org.aftff.client.Crypto;
import org.aftff.client.MyThreadSafeClientConnManager;
import org.aftff.client.SocksSocketFactory;
import org.aftff.client.aftff;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Entity;

import uk.ac.cam.cl.dtg.android.tor.TorProxyLib.SocksProxy;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class Ring {
     
	RingStore.OpenHelper openHelper;
	
	public Ring(Context context, RingStore.OpenHelper openHelper, String key, String shortname, String server) {
		super();
		this.key = key;
		this.shortname = shortname;
		this.server = server;
		this.context = context;

		this.keyHash = aftff.genHexHash(key);
	    this.openHelper = openHelper;
	    aftffHttp = new AftffHttp();
	}
	
	
	
	
	
	
	final private String key;
	final private String shortname;
	final private String server;
	final private String notes = null;
	final private String keyHash;

	
	private AftffHttp aftffHttp;
	public Context context;
	
	public String getKey() {
		return key;
	}
	
	
	public String getShortname() {
		return shortname;
	}
	
	
	public String getServer() {
		return server;
	}
	
	
	public String getNotes() {
		return notes;
	}
	
	
	public String toString() {
		return getShortname();
	}
	
	public Ring(Context context, RingStore.OpenHelper openHelper, String contents) {
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
		this.openHelper = openHelper;
		
	    aftffHttp = new AftffHttp();

		
		//this.add(newRing);
	}
		
	
	final public String getFullText() {
		return(getShortname()+"+"+getKey()+"@"+getServer());
	}
	
	
	private void initHttp() {
		
//		SocksSocketFactory socksFactory = new SocksSocketFactory("127.0.0.1",9050); 
//		
//		SchemeRegistry supportedSchemes = new SchemeRegistry();
//		supportedSchemes.register(new Scheme("http", socksFactory, 80));
//		
//		HttpParams params = new BasicHttpParams();
//        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
//        HttpProtocolParams.setContentCharset(params, "UTF-8");
//        HttpProtocolParams.setUseExpectContinue(params,false);
//
//        ClientConnectionManager	ccm = new MyThreadSafeClientConnManager(params,
//                    supportedSchemes);
//        

		this.aftffHttp = new AftffHttp();
       // this.httpClient = new DefaultHttpClient(ccm, params);	
		
	}
	
	

	public void postMsg(String msg, Identity[] identities) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, JSONException, InvalidKeyException, SignatureException, InvalidKeySpecException, IOException {
		// TODO Auto-generated method stub
		Crypto crypto = new Crypto(getKey().getBytes(), msg.getBytes());
		byte[] encData = crypto.encrypt();
		
		String base64EncData = Base64.encodeBytes(encData);
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("message", base64EncData);
		
		String[] encodedSigs = new String[identities.length];
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
	
	public void postMsg(String msg) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, JSONException {
		Crypto crypto = new Crypto(getKey().getBytes(), msg.getBytes());
		byte[] encData = crypto.encrypt();
		
		String base64EncData = Base64.encodeBytes(encData);
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("message", base64EncData);
		
		postMsg(jsonObj);
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
	
		
	
	
	public Integer getMsgIndex() {
	   //String[] msgs = {};
		//String[] msgs = null;
		
		
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
//	return(msgs);
	
	}
	
	
	public AftffMessage getMsgFromDb(String id) {
		AftffMessage msg = null;
		
		if (context == null)
			return(null);
		
		
		String ringHash = "r" + aftff.genHexHash(this.getFullText());
		
		SQLiteDatabase db = openHelper.getReadableDatabase();
		
		Cursor cursor = db.query(openHelper.MESSAGESTABLE, new String[] { "date", "message" }, "id = ? and ringHash = ?", new String[] { id, ringHash }, null, null, "id desc" );
		if (cursor.moveToFirst()) {
			String date = cursor.getString(0);
			String msgData = cursor.getString(1);
			
			Cursor cursorSig = db.query(openHelper.SIGTABLE, new String[] { "signature" }, "msgId = ? and ringHash = ?", new String[] { id, ringHash }, null, null, "signature desc" );
			
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
		openHelper.close();
		
		return(msg);
	}
	
	
	// FIXME: should refactor
	public void saveMsgToDb(Integer id, String date, String msg) {
		if (context == null) 
			return;	
		
		String ringHash = "r" + aftff.genHexHash(getFullText());
		SQLiteDatabase db = openHelper.getWritableDatabase();
		//db.execSQL("CREATE TABLE if not exists r" + table + " (id INTEGER PRIMARY KEY, date TEXT, message TEXT)");
		SQLiteStatement insrt = db.compileStatement("INSERT INTO " + openHelper.MESSAGESTABLE + " (ringHash,id,date,message) VALUES (?,?,?,?)");
		insrt.bindString(1, ringHash);
		insrt.bindLong(2, id);
		insrt.bindString(3, date);
		insrt.bindString(4, msg);
		insrt.executeInsert();
		db.close();
		openHelper.close();
	}
	
	//FIXME: should refactor
	public void saveMsgToDb(Integer id, String date, String msg, String[] signatures) {
		if (context == null) 
			return;	
		
		String ringHash = "r" + aftff.genHexHash(getFullText());
		SQLiteDatabase db = openHelper.getWritableDatabase();
		//db.execSQL("CREATE TABLE if not exists r" + table + " (id INTEGER PRIMARY KEY, date TEXT, message TEXT)");
		SQLiteStatement insrt = db.compileStatement("INSERT INTO " + openHelper.MESSAGESTABLE + " (ringHash,id,date,message) VALUES (?,?,?,?)");
		insrt.bindString(1, ringHash);
		insrt.bindLong(2, id);
		insrt.bindString(3, date);
		insrt.bindString(4, msg);
		insrt.executeInsert();
	
		for (int i=0; i<signatures.length; i++) {	
		  //FIXME: length for signatures
		  if (signatures[i] == null)
			  break;
		
		  insrt = db.compileStatement("INSERT INTO " + openHelper.SIGTABLE + " (msgId,ringHash,signature) VALUES (?,?,?)");
		  insrt.bindLong(1, id);
		  insrt.bindString(2, ringHash);
		  insrt.bindString(3,signatures[i]);
		  insrt.executeInsert();
		}
		db.close();
	}
	
	
	
	public AftffMessage getMsg(String id) throws ClientProtocolException, IOException {
		AftffMessage msg = null;
		msg = getMsgFromDb(id);
		if (msg == null) {
			
			msg = getMsgFromTor(id);
			
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
	
	
	public AftffMessage getMsgFromTor(String id) throws ClientProtocolException, IOException {
		HttpGet httpGet = new HttpGet("http://" + server + "/" + keyHash + "/" + id);
    	HttpResponse resp = aftffHttp.httpClient.execute(httpGet);
    	
    	String jsonString = EntityUtils.toString(resp.getEntity());
    	Header lastModified = resp.getFirstHeader("Last-Modified");
    	AftffMessage msg = null;
    	String date = null;
    	
    	SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
		try {
			Date d = format.parse(lastModified.getValue());
			date = d.getMonth()+1 + "/" + d.getDate() + " " + d.getHours() + ":" + d.getMinutes();
		} catch (ParseException e) {
			return(null);
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
    	
    	try {
			msg = new AftffMessage(this,jsonString,date);
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
	
	
	

	public void updateLastMessage(Integer curIndex) {
		
		String ringHash = aftff.genHexHash(getFullText());
		Integer lastMsgId = getLastMessage();
		if (lastMsgId == null) {
		  SQLiteDatabase db = openHelper.getWritableDatabase();
		  SQLiteStatement insrt = db.compileStatement("INSERT INTO " + openHelper.LASTMESSAGES + " (ringHash,lastMessage) VALUES (?,?)");
		  insrt.bindString(1, ringHash);
		  insrt.bindLong(2, curIndex);
		  insrt.executeInsert();
		  db.close();
		} else {
			SQLiteDatabase db = openHelper.getWritableDatabase();
			SQLiteStatement update = db.compileStatement("UPDATE " + openHelper.LASTMESSAGES + " SET lastMessage = ? WHERE ringHash = ?");
			update.bindLong(1, curIndex);
			update.bindString(2, ringHash);
			update.executeInsert();
			db.close();
			
		}
		
		
		
		//SharedPreferences prefs = context.getSharedPreferences(aftff.PREFS,0);
		//SharedPreferences.Editor ed = prefs.edit();
		//ed.putInt("lastMessage" + getFullText(), curIndex);
		//ed.commit();
		
		
	}

	public Integer getLastMessage() {
		String ringHash = aftff.genHexHash(getFullText());
		SQLiteDatabase db = openHelper.getReadableDatabase();

		Integer lastMessage = null;
		
		Cursor cursor = db.query(openHelper.LASTMESSAGES, new String[] { "lastMessage" }, "ringHash = ?", new String[] { ringHash }, null, null, "lastMessage desc" );
		if (cursor.moveToFirst()) {
			lastMessage = cursor.getInt(0);
		}
		cursor.close();
		db.close();
		
//		SharedPreferences prefs = context.getSharedPreferences(aftff.PREFS,0);
//		Integer lastMessage = prefs.getInt("lastMessage" + getFullText(), 0);
//		Log.v("Ring", "lastMesagePref: " + lastMessage);
//		return(lastMessage);
		
		return(lastMessage);
		
	}

	

//	public boolean isMsgRead(Integer i) {
//		// TODO Auto-generated method stub
//		if (context == null)
//			return false;
//		
//		Message 
//		
//		return false;
//	}	

}
