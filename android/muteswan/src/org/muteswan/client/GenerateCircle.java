package org.muteswan.client;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;

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

	public GenerateCircle(String secret, Context ctx, String name) {
 
		String server;
		
		this.ctx = ctx;
		this.name = name;
		this.cipherSecret = secret;
		
		SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
	    customServer = defPrefs.getString("customCircleServer", "");
	    usePublicServer = defPrefs.getBoolean("usePublicServer", false);
	    
	  
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
    	
    	
    	
    	circleFullText = name + "+" + UUID.randomUUID().toString() + "$" + generateKey() + "@" + server;
		
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
		String genKeyStr;
	       
		SecureRandom sr = null;
		try {
			sr = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		
		SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		Boolean use256bit = defPrefs.getBoolean("use256bit", false);
		
		if (!use256bit) {
			/*** 128 (48 bit really) keys ***/
			sr.generateSeed(24);
			genKeyStr = new BigInteger(130,sr).toString(32).substring(0,16);
			return genKeyStr;
			
		} else {
		
			/**** 256 bit keys ***/
			sr.generateSeed(256);		
			genKeyStr = Base64.encodeBytes(new BigInteger(256,sr).toByteArray());
			MuteLog.Log("GenerateCircle", "Key length: " + genKeyStr);
			MuteLog.Log("GenerateCircle", "Key length: " + genKeyStr.getBytes().length);
			// it seems like this isn't always encoding right??
			while (!genKeyStr.substring(genKeyStr.length()-1, genKeyStr.length()).equals("=")) {
				 MuteLog.Log("GenerateCircle", "Key length: wtf did not have = " + genKeyStr);
				 sr.generateSeed(256);
				 genKeyStr = Base64.encodeBytes(new BigInteger(256,sr).toByteArray());
			}

			return genKeyStr;
		}
	}


}
