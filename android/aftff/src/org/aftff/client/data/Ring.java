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
     
	
	
	
	public Ring(Context context, String key, String shortname, String server) {
		super();
		this.key = key;
		this.shortname = shortname;
		this.server = server;
		this.context = context;

		this.keyHash = aftff.genHexHash(key);
	}
	
	public Ring(String key, String shortname, String server) {
		super();
		this.key = key;
		this.shortname = shortname;
		this.server = server;

		this.keyHash = aftff.genHexHash(key);
	}
	
	
	private class OpenHelper extends SQLiteOpenHelper {

		private static final int DATABASE_VERSION = 3;
		private static final String DATABASE_NAME = "aftffdb";
		private static final String TABLE = "messages";
		private static final String SIGTABLE = "signatures";

		
	     
	      public OpenHelper(Context context) {
			// TODO Auto-generated constructor stub
	    	  super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
	      public void onCreate(SQLiteDatabase db) {
	         db.execSQL("CREATE TABLE " + TABLE + " (ringHash TEXT, id INTEGER, date TEXT, message TEXT)");
	         db.execSQL("CREATE TABLE " + SIGTABLE + " (id INTEGER PRIMARY KEY, msgId INTEGER, ringHash TEXT, signature TEXT)");
	      }

	      @Override
	      public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	         db.execSQL("DROP TABLE IF EXISTS " + TABLE);
	         db.execSQL("DROP TABLE IF EXISTS " + SIGTABLE);
	         onCreate(db);
	      }
	      
	   }

	
	
	final private String key;
	final private String shortname;
	final private String server;
	final private String notes = null;
	final private String keyHash;

	
	private DefaultHttpClient httpClient;
	public Context context;
	
	public String getKey() {
		return key;
	}
	//public void setKey(String key) {
	//	this.key = key;
	//}
	public String getShortname() {
		return shortname;
	}
	//public void setShortname(String shortname) {
	//	this.shortname = shortname;
	//}
	public String getServer() {
		return server;
	}
	//public void setServer(String server) {
	//	this.server = server;
	//}
	public String getNotes() {
		return notes;
	}
	//public void setNotes(String notes) {
	//	this.notes = notes;
	//}
	public String toString() {
		return getShortname();
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
		
		
		//this.add(newRing);
	}
	
	// yuck fix this duplication
	public Ring(String contents) {
		// TODO Auto-generated constructor stub
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
		this.context = null;
		initHttp();
		
	
	}
	
	
	
	final public String getFullText() {
		return(getShortname()+"+"+getKey()+"@"+getServer());
	}
	
	
	private void initHttp() {
		
		SocksSocketFactory socksFactory = new SocksSocketFactory("127.0.0.1",9050); 
		
		SchemeRegistry supportedSchemes = new SchemeRegistry();
		supportedSchemes.register(new Scheme("http", socksFactory, 80));
		
		HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUseExpectContinue(params,false);

        ClientConnectionManager	ccm = new MyThreadSafeClientConnManager(params,
                    supportedSchemes);
        

        this.httpClient = new DefaultHttpClient(ccm, params);
		
		
		
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
			httpClient.execute(httpPost);
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
		
	  // initHttp();
		
	   HttpGet httpGet = new HttpGet("http://" + server + "/" + keyHash);
	   try {
	    HttpResponse resp = httpClient.execute(httpGet);
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
	
	
	public Message getMsgFromDb(String id) {
		Message msg = null;
		
		if (context == null)
			return(null);
		
		
		String ringHash = "r" + aftff.genHexHash(this.getFullText());
		
		OpenHelper openHelper = new OpenHelper(context);
		SQLiteDatabase db = openHelper.getWritableDatabase();
		
		Cursor cursor = db.query(openHelper.TABLE, new String[] { "date", "message" }, "id = ? and ringHash = ?", new String[] { id, ringHash }, null, null, "id desc" );
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
			   msg = new Message(this,Integer.parseInt(id),date,msgData,signatures);
			} else {
			   msg = new Message(this,Integer.parseInt(id),date,msgData);

			}
			
			
			return(msg);
		}
		cursor.close();
		
		
		
		return(msg);
	}
	
	
	// FIXME: should refactor
	public void saveMsgToDb(Integer id, String date, String msg) {
		if (context == null) 
			return;	
		
		String ringHash = "r" + aftff.genHexHash(getFullText());
		OpenHelper openHelper = new OpenHelper(context);
		SQLiteDatabase db = openHelper.getWritableDatabase();
		//db.execSQL("CREATE TABLE if not exists r" + table + " (id INTEGER PRIMARY KEY, date TEXT, message TEXT)");
		SQLiteStatement insrt = db.compileStatement("INSERT INTO " + openHelper.TABLE + " (ringHash,id,date,message) VALUES (?,?,?,?)");
		insrt.bindString(1, ringHash);
		insrt.bindLong(2, id);
		insrt.bindString(3, date);
		insrt.bindString(4, msg);
		insrt.executeInsert();
		
	}
	
	//FIXME: should refactor
	public void saveMsgToDb(Integer id, String date, String msg, String[] signatures) {
		if (context == null) 
			return;	
		
		String ringHash = "r" + aftff.genHexHash(getFullText());
		OpenHelper openHelper = new OpenHelper(context);
		SQLiteDatabase db = openHelper.getWritableDatabase();
		//db.execSQL("CREATE TABLE if not exists r" + table + " (id INTEGER PRIMARY KEY, date TEXT, message TEXT)");
		SQLiteStatement insrt = db.compileStatement("INSERT INTO " + openHelper.TABLE + " (ringHash,id,date,message) VALUES (?,?,?,?)");
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
		
	}
	
	
	
	public Message getMsg(String id) throws ClientProtocolException, IOException {
		Message msg = null;
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
	
	
	public Message getMsgFromTor(String id) throws ClientProtocolException, IOException {
		HttpGet httpGet = new HttpGet("http://" + server + "/" + keyHash + "/" + id);
    	HttpResponse resp = httpClient.execute(httpGet);
    	
    	String jsonString = EntityUtils.toString(resp.getEntity());
    	Header lastModified = resp.getFirstHeader("Last-Modified");
    	Message msg = null;
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
			msg = new Message(this,jsonString,date);
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
	
	
	public String[] getMsgOld(String id) throws IllegalBlockSizeException, BadPaddingException, ClientProtocolException, IOException {
		
		String[] result = new String[2];
		if (httpClient == null) {
			initHttp();
		}
		HttpGet httpGet = new HttpGet("http://" + server + "/" + keyHash + "/" + id);
    	HttpResponse resp = httpClient.execute(httpGet);
    	
    	String encMsg = EntityUtils.toString(resp.getEntity());

    	Crypto cryptoDec;
		try {
			cryptoDec = new Crypto(key.getBytes(),encMsg.getBytes("ISO-8859-1"));
			Header[] lastModified = resp.getHeaders("Last-Modified");
			result[0] = lastModified[0].getValue();
			String dateString = lastModified[0].getValue();
			SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
			try {
				Date d = format.parse(dateString);
				result[0] = d.getMonth()+1 + "/" + d.getDate() + " " + d.getHours() + ":" + d.getMinutes();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
			
			byte[] msgData = cryptoDec.decrypt();
			result[1] = new String(msgData);
	        return(result);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return(null);
    	        
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
