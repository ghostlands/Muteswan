package org.muteswan.client;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.muteswan.client.data.CircleStore;
import org.muteswan.client.ui.CircleList;
import org.muteswan.client.ui.CreateCircle;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.EditText;
import android.widget.TextView;

public class GenerateCircle {

	private static final int MAX_CIRCNAME_LENGTH = 50;
	private String customServer;
	private boolean usePublicServer;
	private String circleFullText;
	private Context ctx;
	private String name;
	private String cipherSecret;
	private boolean noUuid;

	public GenerateCircle(String secret, Context ctx, String name) {
 
		String server;
		
		this.ctx = ctx;
		this.name = name;
		this.cipherSecret = secret;
		
		SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
	    customServer = defPrefs.getString("customCircleServer", "");
	    usePublicServer = defPrefs.getBoolean("usePublicServer", false);
	    noUuid = defPrefs.getBoolean("noUuid", false);
	    
	  
	    // figure out which server to use
	    if (usePublicServer) {
	    	server = ctx.getString(R.string.defaultcircleserver);
		} else {
			server = ctx.getString(R.string.defaulthiddencircleserver);
		}
	    
	    
	    // if they have custom, stomp on it
	    if (customServer.length() != 0) {
	    	server = customServer;
	    }
	    
    	if (name.length() == 0 || server.length() == 0 || name.length() >= MAX_CIRCNAME_LENGTH)
    		return;
    	
    	
    	if (noUuid) {
    		circleFullText = name + "+" + generateKey() + "@" + server;
    	} else {
    		circleFullText = name + "+" + UUID.randomUUID().toString() + "$" + generateKey() + "@" + server;
    	}
		
	}
	
	public void saveCircle() {
		CircleStore newStore = new CircleStore(cipherSecret,ctx,true,false);
    	newStore.updateStore(circleFullText);
    	
	}
	
	public void broadcastCreate() {
        Intent createdCircleIntent = new Intent(CreateCircle.CREATED_CIRCLE_BROADCAST);
        createdCircleIntent.putExtra("circle", Main.genHexHash(circleFullText));
        ctx.sendBroadcast(createdCircleIntent);
        
        //Intent circleListIntent = new Intent(ctx,CircleList.class);
        //circleListIntent.putExtra("newCircle", name);
        //circleListIntent.putExtra("action", CircleList.ANY);
        //ctx.startActivity(circleListIntent);
	}

	private String generateKey() {
		String genKeyStr = "";
	       
		SecureRandom sr = null;
		try {
			sr = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		
		SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		Boolean use256bit = defPrefs.getBoolean("use256bit", false);
		Boolean useLowEnc = defPrefs.getBoolean("useLowEnc",false);
		
		
		
		// if configured to use 256 bit, we do
		if (use256bit) {
		
			/**** 256 bit keys ***/
			KeyGenerator keyGenerator;
			try {
				keyGenerator = KeyGenerator.getInstance("AES");
				keyGenerator.init(256);
				SecretKey key = keyGenerator.generateKey();
				genKeyStr = Base64.encodeBytes(key.getEncoded());
				MuteLog.Log("GenerateCircle","Using 256bit enryption!");
				MuteLog.Log("GenerateCircle", "Key length: " + genKeyStr);
				MuteLog.Log("GenerateCircle", "Key length: " + genKeyStr.getBytes().length);
				return genKeyStr;
				
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return genKeyStr;
		}
		
		// if configured to use low encryption we do
		if (useLowEnc) {
			/*** 128 (but less key space) keys ***/
			sr.generateSeed(24);
			genKeyStr = new BigInteger(130,sr).toString(32).substring(0,16);
			MuteLog.Log("GenerateCircle","Low Encryption!");
			MuteLog.Log("GenerateCircle", "Key length: " + genKeyStr);
			MuteLog.Log("GenerateCircle", "Key length: " + genKeyStr.getBytes().length);
			return genKeyStr;
		}
	
		// fall through to default
		/**** 128 bit keys ***/
		KeyGenerator keyGenerator;
		try {
			keyGenerator = KeyGenerator.getInstance("AES");
			keyGenerator.init(128);
			SecretKey key = keyGenerator.generateKey();
			genKeyStr = Base64.encodeBytes(key.getEncoded());
			MuteLog.Log("GenerateCircle","Default 128bit enryption!");
			MuteLog.Log("GenerateCircle", "Key length: " + genKeyStr);
			MuteLog.Log("GenerateCircle", "Key length: " + genKeyStr.getBytes().length);
			
			
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		

		return genKeyStr;
		
	}


}
