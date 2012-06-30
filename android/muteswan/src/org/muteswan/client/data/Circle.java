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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.muteswan.client.Base64;
import org.muteswan.client.Crypto;
import org.muteswan.client.MuteLog;
import org.muteswan.client.MuteswanHttp;
import org.muteswan.client.Main;

import android.content.Context;
import android.content.SharedPreferences;


//import info.guardianproject.database.sqlcipher.SQLiteDatabase;
//import info.guardianproject.database.sqlcipher.SQLiteStatement;
import android.util.Log;
import android.preference.PreferenceManager;

public class Circle {
     
	

	public static boolean libsLoaded = false;

	final private String key;
	
	
	
		
	final private String shortname;
	final private String server;
	final private String notes = null;
	final private String keyHash;
	private MuteswanHttp muteswanHttp;

	
	public Context context;

	private Integer curLastMsgId = 0;

	private String postPolicy;
	private String authKey;
	private byte[] image;
	private String longDescription;
	private String description;
	private String[] keylist;
	
	
	private HashMap<Integer,MuteswanMessage> msgCache = new HashMap<Integer,MuteswanMessage>();

	
	private String[] parseCircle(String fullText) {
		Integer plusIndx = fullText.indexOf("+");
		Integer atIndx = fullText.indexOf("@");
		
		String[] parsedCircle = new String[3];
		
		if (plusIndx == -1 || atIndx == -1) {
			
			return(null);
		}
		
		parsedCircle[0] = fullText.substring(0,plusIndx);
		parsedCircle[1] = fullText.substring(plusIndx+1,atIndx);
		parsedCircle[2] = fullText.substring(atIndx+1,fullText.length());
		
		return(parsedCircle);
	}

  

	public Circle(String secret, Context context, String contents) {
		
		
		String[] parsedCircle = parseCircle(contents);
		String name = parsedCircle[0];
		String key = parsedCircle[1];
		String srv = parsedCircle[2];
		
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
		this.keyHash = Main.genHexHash(key);
		this.context = context;
		
		
		
		initializeDirStore(context.getFilesDir());
		
		SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        curLastMsgId = defPrefs.getInt(Main.genHexHash(getFullText()), 0);
	    
		
	}
	
	
public Circle(String secret, Context context, JSONObject jsonObject, MuteswanHttp muteswanHttp) {
		
		
		byte[] ivData;
		String[] parsedCircle = null;
		try {
			ivData = Base64.decode(jsonObject.getString("iv"));
			byte[] cirData = Base64.decode(jsonObject.getString("circle"));
			
			Crypto crypto = new Crypto(secret.getBytes(),cirData,ivData);
			String circleText = new String(crypto.decrypt());
			parsedCircle = parseCircle(circleText);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		
			
	
		String name = parsedCircle[0];
		String key = parsedCircle[1];
		String srv = parsedCircle[2];
		
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
		this.keyHash = Main.genHexHash(key);
		this.context = context;
		this.muteswanHttp = muteswanHttp;
		
		
		
		initializeDirStore(context.getFilesDir());
		
		SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        curLastMsgId = defPrefs.getInt(Main.genHexHash(getFullText()), 0);
	    
		
	}

public Circle(String secret, Context context, JSONObject jsonObject) {
	
	
	byte[] ivData;
	String[] parsedCircle = null;
	try {
		ivData = Base64.decode(jsonObject.getString("iv"));
		byte[] cirData = Base64.decode(jsonObject.getString("circle"));
		
		Crypto crypto = new Crypto(secret.getBytes(),cirData,ivData);
		String circleText = new String(crypto.decrypt());
		parsedCircle = parseCircle(circleText);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
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
	
		

	String name = parsedCircle[0];
	String key = parsedCircle[1];
	String srv = parsedCircle[2];
	
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
	this.keyHash = Main.genHexHash(key);
	this.context = context;
	this.muteswanHttp = muteswanHttp;
	
	
	
	initializeDirStore(context.getFilesDir());
	
	SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    curLastMsgId = defPrefs.getInt(Main.genHexHash(getFullText()), 0);
    
	
}

	
	public Circle(String secret, Context context, String contents, MuteswanHttp muteswanHttp) {
		
		
		String[] parsedCircle = parseCircle(contents);
		String name = parsedCircle[0];
		String key = parsedCircle[1];
		String srv = parsedCircle[2];
		
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
		this.keyHash = Main.genHexHash(key);
		this.context = context;
		this.muteswanHttp = muteswanHttp;
		
		
		
		initializeDirStore(context.getFilesDir());
		
		SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        curLastMsgId = defPrefs.getInt(Main.genHexHash(getFullText()), 0);
	    
		
	}
	
	private void initializeDirStore(File filesDir) {
		if (!filesDir.exists())
			filesDir.mkdir();
		
		
		File storePath = new File(filesDir.getAbsolutePath() + "/" + Main.genHexHash(getFullText()));
		if (!storePath.exists())
		    storePath.mkdir();
		
	}
	
	private File getStorePath() {
		return(new File(context.getFilesDir() + "/" + Main.genHexHash(getFullText())));
	}

	public Circle(String secret, Context context, String key, String shortname, String server, MuteswanHttp muteswanHttp) {
		this.key = key;
		this.shortname = shortname;
		this.server = server;
		this.context = context;
		
		

		this.keyHash = Main.genHexHash(key);
		
		
		

		
	  	this.muteswanHttp = muteswanHttp;
		
	  	SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        curLastMsgId = defPrefs.getInt(Main.genHexHash(getFullText()), 0);
	  	initializeDirStore(context.getFilesDir());
	}
	
	public Circle(String secret, Context context, String key, String shortname, String server) {
		this.key = key;
		this.shortname = shortname;
		this.server = server;
		this.context = context;
		this.keyHash = Main.genHexHash(key);
		


		
		
		SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        curLastMsgId = defPrefs.getInt(Main.genHexHash(getFullText()), 0);
	    
	    initializeDirStore(context.getFilesDir());
	}
	
	
	
	public void setCurLastMsgId(int lastMsg) {
		curLastMsgId = lastMsg;
	}
	
	
	
	public String getPostPolicy() {
		return(postPolicy);
	}

	public String getAuthKey() {
		return(authKey);
	}

	public byte[] getImage() {
		return(image);
	}
	
	public String[] getKeylist() {
		return(keylist);
	}

	public String getLongDescription() {
		return(longDescription);
	}

	public String getDescription() {
		return(description);
	}

	final public String getFullText() {
		return(getShortname()+"+"+getKey()+"@"+getServer());
	}
	
	private void setKeylist(String[] keylist) {
		this.keylist = keylist;
	}
	
	private void setPostPolicy(String policy) {
		this.postPolicy = policy;
	}

	private void setAuthKey(String key) {
		this.authKey = key;
	}

	private void setImage(byte[] image) {
		this.image = image;
	}

	private void setLongDescription(String description) {
		this.longDescription = description;
	}

	private void setDescription(String description) {
		this.description = description;
	}

	
	
	
	
	public String getKey() {
		return key;
	}

	
	
	
	
	
	/**
	 * Gets the last known message from the database. Does not check tor.
	 * @return integer
	 */
	public Integer getLastMsgId(boolean closedb) {
		
		String circleHash = Main.genHexHash(getFullText());
		
		int lastMessageId = 0;
		
		SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        lastMessageId = defPrefs.getInt(circleHash, 0);
        
        //if (lastMessageId == 0)
        //	return null;
				

		curLastMsgId = lastMessageId;
		return(lastMessageId);
	}
	
	public Integer getLastCurMsgId(boolean closedb) {
		if (curLastMsgId == null || curLastMsgId == 0)
			curLastMsgId = getLastMsgId(closedb);
		return(curLastMsgId);
	}
	
	
	
	
	
	
	
	
	
	private String getFileContents(File file) {
		StringBuilder text = new StringBuilder();

		if (!file.exists())
			return null;
		
		try {
		    BufferedReader br = new BufferedReader(new FileReader(file));
		    String line;

		    while ((line = br.readLine()) != null) {
		        text.append(line);
		    }
		}
		catch (IOException e) {
		    MuteLog.Log("Circle", "Failed to read file data in " + file.getAbsolutePath());
		}
		
		return(text.toString());
	}
	
	private boolean writeFileContent(File file, String data) {
		

		try {
		    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		    bw.append(data);
		    bw.close();
		    return true;
		   
		}
		catch (IOException e) {
		    MuteLog.Log("Circle", "Failed to write file data in " + file.getAbsolutePath());
		}
		
		return false;
		
	}
		

	
	public MuteswanMessage getMsgFromTor(int id) throws ClientProtocolException, IOException, InterruptedIOException {
		HttpGet httpGet = new HttpGet("http://" + server + "/" + keyHash + "/" + id);
		MuteLog.Log("Circle", "Fetching message " + id);
    	HttpResponse resp = muteswanHttp.execute(httpGet);
    	MuteLog.Log("Circle", "Fetched message " + id);
    	
    	return(parseMsgFromTor(id,resp));
    	
	}
	
	public ArrayList<MuteswanMessage> getMsgRangeFromTor(int max, int min) throws ClientProtocolException, IOException, InterruptedIOException {

		ArrayList<MuteswanMessage> msgs = new ArrayList<MuteswanMessage>();
		
		HttpGet httpGet = new HttpGet("http://" + server + "/" + keyHash + "/" + max + "-" + min);
		MuteLog.Log("Circle", "Fetching messages " + max + " to " + min);
    	HttpResponse resp = muteswanHttp.execute(httpGet);
		MuteLog.Log("Circle", "Fetched messages " + max + " to " + min);

		
		String jsonString = EntityUtils.toString(resp.getEntity());
		if (jsonString == null) {
			MuteLog.Log("Circle", "getMsgRangeFromTor(): jsonString is null");
			return null;
		}
		
		try {
			JSONArray jsonArray = new JSONArray(jsonString);
			for (int i = 0; i<jsonArray.length();i++) {
				JSONObject jsonObj = jsonArray.getJSONObject(i);
				JSONObject contentObj = jsonObj.getJSONObject("content");
				String date = parseHttpDate(jsonObj.getString("timestamp"));
				MuteLog.Log("Circle", "Raw json: " + jsonString);
				MuteLog.Log("Circle", "Got date: " + jsonObj.getString("timestamp"));
				
				MuteswanMessage msg = new MuteswanMessage(max,this,contentObj,date);
				msgs.add(msg);
				max--;
			}
		} catch (JSONException e) {
			MuteLog.Log("Circle", "getMsgRangeFromTor(): jsonString is not parseable: " + jsonString);
			return null;
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

		return(msgs);
    	
	}

	private String parseHttpDate(String dateIn) {
		String date = null;
		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
		try {
			Date d = format.parse(dateIn);
			//date = d.getMonth()+1 + "/" + d.getDate() + " " + d.getHours() + ":" + d.getMinutes();
			SimpleDateFormat df = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss zzz" );
			
			Calendar cal = Calendar.getInstance();
			TimeZone tz = cal.getTimeZone();
	        df.setTimeZone( tz );
	        date = df.format(d);
		} catch (ParseException e) {
			MuteLog.Log("Circle", "Parse error parsing " + dateIn + " with " + e.toString());
			return(null);
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
		return(date);
	}
	
	public JSONObject getCryptJSON(String cipherSecret) {
		Crypto crypto = null;
		
		try {
			crypto = new Crypto(cipherSecret.getBytes(),getFullText().getBytes());
			String encData = Base64.encodeBytes(crypto.encrypt());
			String ivData = Base64.encodeBytes(crypto.getIVData());
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("circle", encData);
			jsonObject.put("iv", ivData);
			return(jsonObject);
			
			
		} catch (NoSuchAlgorithmException e) {
			
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return(null);
	}
	
	
	
	private MuteswanMessage parseMsgFromTor(Integer id, HttpResponse resp) throws org.apache.http.ParseException, IOException {

		
		String jsonString = EntityUtils.toString(resp.getEntity());
		if (jsonString == null) {
			MuteLog.Log("Circle", "WTF, jsonString is null");
			return null;
		}
		JSONObject jsonObj = null;
		try {
			jsonObj = new JSONObject(jsonString);
		} catch (JSONException e1) {
			MuteLog.Log("Circle", "WTF, jsonString is not parseable");
			return null;
		}
		
    	Header lastModified = resp.getFirstHeader("Last-Modified");
    	MuteswanMessage msg = null;
    	String date = null;
    	
    	if (lastModified == null) {
			MuteLog.Log("Circle", "WTF, lastModified is null");
			return null;
    	}
    
    	date = parseHttpDate(lastModified.getValue());
    	
    	try {
			msg = new MuteswanMessage(id,this,jsonObj,date);
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

	
	// returns 0 or greater on success, -1 and -2 on failure
	public Integer getLastTorMessageId() {
	  
		
	   HttpGet httpGet = new HttpGet("http://" + server + "/" + keyHash);
	   try {
	    HttpResponse resp = muteswanHttp.execute(httpGet);
	   
	    String lastMessage = null;
	    String jsonString = EntityUtils.toString(resp.getEntity());
		if (jsonString == null) {
			Log.e("Circle", "getLastTorMessage(): jsonString is null");
			return null;
		}
		
		try {
			JSONObject jsonObj = new JSONObject(jsonString);
			lastMessage = jsonObj.getString("lastMessage");
		} catch (JSONException e) {
			Log.e("Circle", "unable to parse json message");
			return null;
		}

	    
	    if (lastMessage == null || lastMessage.equals("null")) {
	    	MuteLog.Log("LatestMessages","lastMessage header is null, indicates no messages posted yet.");
	    	return 0;
	    }
	    
	    MuteLog.Log("LatestMessages","getLastTorMessage(): lastmessage is " + lastMessage);
	    Integer result = Integer.parseInt(lastMessage);
	    if (result == null)
	    	return -1;
	    	
		return(result);

	} catch (ClientProtocolException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		MuteLog.Log("Circle", "IKF IO exception");
		return (-2);
	} 
	
	return(-1);
	
	}
	
	/*
	public MuteswanMessage getMsgLongpoll(Integer id) throws IOException {
		HttpGet httpGet = new HttpGet("http://" + server + "/" + keyHash + "/longpoll/" + id);
    	HttpResponse resp;
    	
    	MuteLog.Log("Circle", "getMsgLongpoll called for " + getShortname());
    	
		try {
			resp = muteswanHttp.httpClient.execute(httpGet);
			MuteswanMessage msg = parseMsgFromTor(id,resp);
			if (msg.signatures[0] != null) {
				   saveMsgToDb(id,msg.getDate(),msg.getMsg(),msg.signatures);
			} else {
				   saveMsgToDb(id,msg.getDate(),msg.getMsg());
			}
			return(msg);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return(null);
    	
	}*/
	
	

	public String getNotes() {
		return notes;
	}
	
	public String getServer() {
		return server;
	}
	
	public String getShortname() {
		return shortname;
	}
	
		
	
	
	
	
	
	// return the HTTP code, if IO error returns -1, protocol error -2, -3 key error 
	public Integer postMsg(JSONObject jsonObj) {
		
		HttpPost httpPost = new HttpPost("http://" + server + "/" + keyHash);
		ByteArrayEntity entity = new ByteArrayEntity(jsonObj.toString().getBytes());
		
		if (getPostPolicy() != null && !getPostPolicy().equals("ANY")) {
			if (getPostPolicy().equals("AUTHKEY")) {
				MuteLog.Log("Circle", "AUTHKEY Adding Signature header for " + getShortname());
				Signature sig = null;
				try {
					sig = Signature.getInstance("MD5WithRSA");
				} catch (NoSuchAlgorithmException e1) {
					// TODO Auto-generated catch block
					//e1.printStackTrace();
					return(-3);
				}
				IdentityStore idStore = new IdentityStore(context);
				RSAPrivateKey rsaPrivKey = null;
				for (Identity id : idStore) {
					if (id.publicKeyEnc.equals(getAuthKey())) {
						try {
							rsaPrivKey = id.getPrivateKey();
						} catch (NoSuchAlgorithmException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InvalidKeySpecException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					}
				}
				
				if (rsaPrivKey == null) {
					Log.e("Circle", "Could not find appropriate identity for " + getShortname() + " in idstore.");
					return(-3);
				}
				
				   
				try {
					sig.initSign(rsaPrivKey);
					sig.update(jsonObj.toString().getBytes());
					//sig.update("some random sign data".getBytes("UTF8"));
					byte[] sigBytes = sig.sign();
					httpPost.setHeader("Signature",Base64.encodeBytes(sigBytes));
				} catch (InvalidKeyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SignatureException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				//circle.updateManifest(jsonObj,Base64.encodeBytes(sigBytes));
			} else if (getPostPolicy().equals("KEYLIST")) {
				MuteLog.Log("Circle", "KEYLIST Adding Signature header for " + getShortname());
				Signature sig = null;
				try {
					sig = Signature.getInstance("MD5WithRSA");
				} catch (NoSuchAlgorithmException e1) {
					// TODO Auto-generated catch block
					//e1.printStackTrace();
					return(-3);
				}
				
				IdentityStore idStore = new IdentityStore(context);
				RSAPrivateKey rsaPrivKey = null;
				KEYS: for (String key : keylist) {
					for (Identity id : idStore) {
					  if (key.equals(id.publicKeyEnc) && id.privateKeyEnc != null) {
						  MuteLog.Log("Circle", "Found identity " + id.getName());
						  try {

							rsaPrivKey = id.getPrivateKey();

						} catch (NoSuchAlgorithmException e) {
							e.printStackTrace();
							Log.e("Circle", "NoSuchAlgorithmException getting private key.");
						} catch (InvalidKeySpecException e) {
							// TODO Auto-generated catch block
							Log.e("Circle", "InvalidKeySpecException getting private key.");

						} catch (IOException e) {
							// TODO Auto-generated catch block
							Log.e("Circle", "IOException getting private key.");

						}
						  break KEYS;
					  }
				 }
				}
				
				if (rsaPrivKey == null) {
					Log.e("Circle", "Could not find appropriate identity for " + getShortname() + " in idstore.");
					return(-3);
				}
				

				try {
					sig.initSign(rsaPrivKey);
					sig.update(jsonObj.toString().getBytes());
					//sig.update("some random sign data".getBytes("UTF8"));
					
					byte[] sigBytes = sig.sign();

					httpPost.setHeader("Signature",Base64.encodeBytes(sigBytes));
				} catch (InvalidKeyException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					MuteLog.Log("Circle", "Invalid key posting.");
				} catch (SignatureException e) {
					// TODO Auto-generated catch block
					MuteLog.Log("Circle", "Signature exception posting.");
				}
				
			
		  }
		}
		
		
		httpPost.setEntity(entity);
		

		try {
			// POST MESSAGE
			HttpResponse response = muteswanHttp.execute(httpPost);
			return(response.getStatusLine().getStatusCode());

		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return(-2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return(-1);
		}
		
	}
	
	
	public Integer postMsg(String msg) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, JSONException, IOException {
		MuteLog.Log("Circle", "Key length on post: " + getKey().getBytes().length);
		Crypto crypto;
		if (getKey().endsWith("=")) {
			crypto = new Crypto(Base64.decode(getKey()), msg.getBytes());
		} else {
			crypto = new Crypto(getKey().getBytes(), msg.getBytes());
		}
		byte[] encData = crypto.encrypt();
		byte[] ivData = crypto.getIVData();
		MuteLog.Log("Circle", "iv data: " + ivData.toString());
		
		String base64EncData = Base64.encodeBytes(encData);
		String base64IVData = Base64.encodeBytes(ivData);
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("message", base64EncData);
		jsonObj.put("iv", base64IVData);
		
		return(postMsg(jsonObj));
	}
	
	public Integer postMsg(String msg, Identity[] identities) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, JSONException, InvalidKeyException, SignatureException, InvalidKeySpecException, IOException {
		Crypto crypto = new Crypto(getKey().getBytes(), msg.getBytes());
		byte[] encData = crypto.encrypt();
		
		String base64EncData = Base64.encodeBytes(encData);
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("message", base64EncData);
		
		JSONArray jsonArray = new JSONArray();
		
		
		
		for (int i=0; i<identities.length; i++) {
		    Signature sig = Signature.getInstance("MD5WithRSA");
		    
		    if (identities[i] == null) {
		    	MuteLog.Log("Circle", "Wtf, identities is null\n");
		    	break;
		    }
		    
		    MuteLog.Log("Circle", "Signing with " + identities[i].getName() + "\n");
		
		    RSAPrivateKey rsaPrivKey = identities[i].getPrivateKey();
		   
			sig.initSign(rsaPrivKey);
			sig.update(msg.getBytes("UTF8"));
			byte[] sigBytes = sig.sign();
			
			String sigLine = identities[i].pubKeyHash + ":" + Base64.encodeBytes(sigBytes);
			Crypto cryptoEnc = new Crypto(key.getBytes(),sigLine.getBytes("UTF8"));
			byte[] sigData = cryptoEnc.encrypt();
			jsonArray.put(Base64.encodeBytes(sigData));
		}
		
		jsonObj.put("signatures", jsonArray);
		
		
		return(postMsg(jsonObj));
		
	}
	
	public boolean msgExists(Integer id) {
		
		File msgPath = new File(getStorePath() + "/" + id);
		if (msgPath.exists()) {
			return true;
		} else {
			return false;
		}
		
	}
	
	public MuteswanMessage getMsgFromDb(String id, Boolean closedb) {
		MuteswanMessage msg = null;
		
        if (msgCache.containsKey(Integer.parseInt(id))) {
                MuteLog.Log("Circle", "Fetched from msgCache: " + id);
                return (MuteswanMessage) (msgCache.get(Integer.parseInt(id)));
        }

		
		if (context == null)
			return(null);
		
		String circleHash = Main.genHexHash(this.getFullText());
		
		
		msg = getMsgFromDb(circleHash,id);
		
		return(msg);
		
	}
	
	public MuteswanMessage getMsgFromDb(String circleHash, String id) {
		MuteswanMessage msg = null;
		
		if (Thread.currentThread().isInterrupted())
			return(null);

		
		
		File msgPath = new File(getStorePath() + "/" + id);
		String jsonData = getFileContents(msgPath);
		
		//String msgPath = keyHash + "-" + id;
		//String jsonData = circleMsgCache.getString(msgPath, null);
		
		
		
		
		if (jsonData == null)
			return null;
		
		MuteLog.Log("Circle", "Got json data: " + jsonData);
		
		
		
		
		JSONObject jsonObj = null;
		try {
			jsonObj = new JSONObject(jsonData);
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
			
		try {
			msg = new MuteswanMessage(Integer.parseInt(id), this, jsonObj, jsonObj.get("msgdate").toString());
			msgCache.put(Integer.parseInt(id), msg);
		} catch (NumberFormatException e) {
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
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		
		return(msg);
		
	}
	
	
	public void saveMsgToDb(Integer id, String date, String msgContent) {
		
		if (context == null) 
			return;	
		
		if (id == null) {
			MuteLog.Log("Circle","id is null!");
			return;
		}
		if (date == null) {
			MuteLog.Log("Circle","date is null!");
			return;
		}
		
		if (msgContent == null) {
			MuteLog.Log("Circle","msgcontent is null!");
			return;
		}
		
		JSONObject jsonObj = null;
		try {
			jsonObj = new JSONObject(msgContent);
			jsonObj.put("msgdate", date);
			
			
			File msgPath = new File(getStorePath() + "/" + id);
			MuteLog.Log("Circle","Saving message " + id + " with content " + msgContent + " and path " + msgPath);
			writeFileContent(msgPath,jsonObj.toString());
			
			//String msgPath = keyHash + "-" + id;
			//circleMsgCache.edit().putString(msgPath, jsonObj.toString()).commit();
			
			
			//MuteLog.Log("Circle", "Wrote message to file " + msgPath);
		} catch (JSONException e) {
			
		}
		
		
		MuteLog.Log("Circle","Saved message " + id);
	}
	
	
	
	
	
	

	@Override
	public String toString() {
		return getShortname();
	}

public void deleteAllMessages(boolean closedb) {
		
		File msgPath = getStorePath();
		
		File[] fileList = msgPath.listFiles();
		for (int i=0; i<fileList.length;i++) {
			fileList[i].delete();
		}
		msgPath.delete();
		
	}
	
	public void createLastMessage(Integer curIndex) {
		
		
		String circleHash = Main.genHexHash(getFullText());
		SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        defPrefs.edit().putInt(circleHash, curIndex).commit();
        
        curLastMsgId = curIndex;
        
	}
	
	public void updateLastMessage(Integer curIndex) {
		createLastMessage(curIndex);
			
	}
	
	public void closedb() {
		// FIXME noop
		
	}
	
	public void saveLastMessage() {
		
		createLastMessage(curLastMsgId);
	}

	public void updateManifest(JSONObject jsonObj) {
		HttpPut httpPut = new HttpPut("http://" + server + "/" + keyHash + "/manifest");
		ByteArrayEntity entity = new ByteArrayEntity(jsonObj.toString().getBytes());
		httpPut.setEntity(entity);
		
		try {
			muteswanHttp.execute(httpPut);
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
	
	public void updateManifest(JSONObject jsonObj, String signature) {
		HttpPut httpPut = new HttpPut("http://" + server + "/" + keyHash + "/manifest");
		ByteArrayEntity entity = new ByteArrayEntity(jsonObj.toString().getBytes());
		httpPut.setHeader("Signature",signature);
		httpPut.setEntity(entity);
		
		try {
			muteswanHttp.execute(httpPut);
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

	public void downloadManifest() {
			HttpGet httpGet = new HttpGet("http://" + server + "/" + keyHash + "/manifest");
			MuteLog.Log("Circle", "Downloading manifest for " + getShortname());
	    	try {
				HttpResponse resp = muteswanHttp.execute(httpGet);
				JSONObject jsonObj = parseManifest(resp);
				if (jsonObj == null || !jsonObj.has("manifest"))
					return;
				JSONObject jsonManifest = jsonObj.getJSONObject("manifest");
				
				if (jsonManifest.has("description"))
				  setDescription(new String(Base64.decode(jsonManifest.getString("description"))));
				if (jsonManifest.has("longdescription"))
				  setLongDescription(new String(Base64.decode(jsonManifest.getString("longdescription"))));
				if (jsonManifest.has("authkey"))
				  setAuthKey(jsonManifest.getString("authkey"));
				if (jsonManifest.has("postpolicy"))
				  setPostPolicy(jsonManifest.getString("postpolicy"));
				if (jsonManifest.has("image"))
				  setImage(Base64.decode(jsonManifest.getString("image")));
				if (jsonManifest.has("keylist")) {
					JSONArray keylist = jsonManifest.getJSONArray("keylist");
					String[] keylistArr = new String[keylist.length()];
					for (int i=0; i<keylist.length();i++) {
						keylistArr[i] = keylist.getString(i);
					}
					setKeylist(keylistArr);
				}
				
				
				//saveManifestToDb();

		    	MuteLog.Log("Circle", "Downloaded manifest for " + getShortname());
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
   }
	
   
	
private JSONObject parseManifest(HttpResponse resp) {
	   JSONObject jsonObj = null;
	   try {
		String jsonString = EntityUtils.toString(resp.getEntity());
		jsonObj = new JSONObject(jsonString);
	} catch (org.apache.http.ParseException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (JSONException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

	   return jsonObj;
   }




		
	


}
