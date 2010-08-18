package org.aftff.client;

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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

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
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Entity;

import uk.ac.cam.cl.dtg.android.tor.TorProxyLib.SocksProxy;

import android.os.Build;
import android.text.format.DateFormat;
import android.widget.TextView;

public class Ring {
     
	
	
	
	public Ring(String key, String shortname, String server) {
		super();
		this.key = key;
		this.shortname = shortname;
		this.server = server;
		
		genHex();
		initHttp();
		
	}
	
	
	private String key;
	private String shortname;
	private String server;
	private String notes;
	private String keyHash;

	
	private DefaultHttpClient httpClient;
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getShortname() {
		return shortname;
	}
	public void setShortname(String shortname) {
		this.shortname = shortname;
	}
	public String getServer() {
		return server;
	}
	public void setServer(String server) {
		this.server = server;
	}
	public String getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes = notes;
	}
	
	public Ring(String contents) {
		Integer plusIndx = contents.indexOf("+");
		Integer atIndx = contents.indexOf("@");
		
		if (plusIndx == -1 || atIndx == -1)
			return;
		
		
		String name = contents.substring(0,plusIndx);
		String key = contents.substring(plusIndx+1,atIndx);
		String srv = contents.substring(atIndx+1,contents.length());
		
		if (name == null || key == null || srv == null)
			return;
		
		//String[] keyAndSrv = addr.split("/@/");
		//String key = keyAndSrv[0];
		//String srv = keyAndSrv[1];
		
		this.key = key;
		this.shortname = name;
		this.server = srv;
		genHex();
		initHttp();
		
		
		//this.add(newRing);
	}
	
	public String getFullText() {
		return(getShortname()+"+"+getKey()+"@"+getServer());
	}
	
	private void genHex() {
		MessageDigest sha = null;
		try {
			sha = MessageDigest.getInstance("SHA");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	sha.reset();
    	
    	
    	sha.update(key.getBytes());
    	byte messageDigest[] = sha.digest();
    	
                
    	StringBuffer hexString = new StringBuffer();
    	for (int i=0;i<messageDigest.length;i++) {
    		String hex = Integer.toHexString(0xFF & messageDigest[i]); 
    		if(hex.length()==1)
    		  hexString.append('0');
    		hexString.append(hex);
    	}
    	this.keyHash = new String(hexString);
    	
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
	
	
	public void postMsg(String msg) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, JSONException {
		Crypto crypto = new Crypto(getKey().getBytes(), msg.getBytes());
		byte[] encData = crypto.encrypt();
		
		String base64EncData = Base64.encodeBytes(encData);
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("message", base64EncData);
		
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
	
	public void postMsgOld(String msg) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		//initHttp();
		

		Crypto crypto = new Crypto(getKey().getBytes(), msg.getBytes());
		byte[] encData = crypto.encrypt();
		
		
		HttpPost httpPost = new HttpPost("http://" + server + "/" + keyHash);
		ByteArrayEntity entity = new ByteArrayEntity(encData);
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
	
	public String getMsgIndexRaw() {
		 HttpGet httpGet = new HttpGet("http://" + server + "/" + keyHash);
		   try {
		    HttpResponse resp = httpClient.execute(httpGet);
			
			String yamlRaw = EntityUtils.toString(resp.getEntity());
			return(yamlRaw);
		   } catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return(null);
			
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
//
//	    for (int i = Integer.parseInt(lastMessage[0].getValue()); i>0; i--) {
//	    	//msgs[i] = new String();
//	    	
//	    }
//	
//	    
//	    return(msgs);
//		
//		String yamlRaw = EntityUtils.toString(resp.getEntity());
//		
//		//BufferedReader list = new BufferedReader(new StringReader(yamlRaw));
//		msgs = yamlRaw.split("--- ");
//		//String line;
//		//int i=0;
//		//while ((line = list.readLine()) != null) {
//	       //String[] chars = line.split(" ");
//	       //msgs[i] = chars[1];
//		   //msgs[i] = line;
//		   //i++;
//		//}
//		return(msgs);
	} catch (ClientProtocolException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
//		//e.printStackTrace();
//		//TextView txt = new TextView(this);
//		//txt.setText("Error getting message index: " + e.toString());
//		//msgs[0] = "error";
//		//msgs[1] = e.toString();
//		String[] error = { "error", e.toString() };
//		return(error);
	}
	
	return(0);
//	return(msgs);
	
	}
	
	
	public Message getMsg(String id) throws ClientProtocolException, IOException {
		HttpGet httpGet = new HttpGet("http://" + server + "/" + keyHash + "/" + id);
    	HttpResponse resp = httpClient.execute(httpGet);
    	
    	String jsonString = EntityUtils.toString(resp.getEntity());
    	Header lastModified = resp.getFirstHeader("Last-Modified");
    	Message msg = null;
    	try {
			msg = new Message(this,jsonString,lastModified.getValue());
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

}
