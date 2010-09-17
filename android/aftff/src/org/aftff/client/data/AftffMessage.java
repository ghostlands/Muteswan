package org.aftff.client.data;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.LinkedList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.aftff.client.Base64;
import org.aftff.client.Crypto;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class AftffMessage {

	private JSONObject jsonObj;
	private String msgData;
	private String date;
	private Ring ring;
	private Integer id;
	Crypto cryptoDec;
	
	// FIXME: define max signatures per message
	public String[] signatures = new String[50];
	
	private LinkedList<Identity> validSigs;


	public AftffMessage(Ring ring, Integer id, String date, String msg) {
		this.date = date;
		this.ring = ring;
		this.msgData = msg;
		this.id = id;
	}
	
	public AftffMessage(Ring ring, Integer id, String date, String msg, String[] signatures) {
		this.date = date;
		this.ring = ring;
		this.msgData = msg;
		this.id = id;
		this.signatures = signatures;
	}
	
	public AftffMessage(Integer id, Ring ring, String jsonString, String date) throws JSONException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {
		jsonObj = new JSONObject(jsonString);
		this.date = date;
		this.ring = ring;
		
		String base64Msg = jsonObj.getString("message");
		JSONArray sigs = null;
		try {
		  sigs = jsonObj.getJSONArray("signatures");
		  for (int i=0; i<sigs.length(); i++) {
			    String sig = sigs.getString(i);
			    byte[] sigKeyBytes = Base64.decode(sig);
			    Crypto cryptoSig = new Crypto(ring.getKey().getBytes(),sigKeyBytes);
			    byte[] realSignature = cryptoSig.decrypt();
				this.signatures[i] = new String(realSignature);
		  }
		} catch (JSONException e) {
			
		}
		
		
		
		byte[] rawMsgBytes = null;
		try {
			rawMsgBytes = Base64.decode(base64Msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
			
		cryptoDec = new Crypto(ring.getKey().getBytes(),rawMsgBytes);
		byte[] msg = cryptoDec.decrypt();
		this.msgData = new String(msg);
				
		this.id = id;
	}
	
	public String getFirstSignature() {
		return(signatures[0]);
	}
	
	public String getMsg() {
		return msgData;
	}
	
	public String getDate() {
		return date;
	}

	public void addValidSig(Identity identity) {
		// TODO Auto-generated method stub
		if (validSigs == null) {
			validSigs = new LinkedList<Identity>();
		}
		validSigs.add(identity);
	}
	
	public Identity getFirstValidSig() {
		if (validSigs != null) {
		  return(validSigs.getFirst());
		} else {
			return(null);
		}
	}
	
	
	public LinkedList<Identity> verifySignatures(IdentityStore idStore) {
		//Log.v("Message", "In verifySignatures");
		
		Signature sig;
		try {
			sig = Signature.getInstance("MD5WithRSA");
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			//e1.printStackTrace();
			return(null);
		}
		
		
		for (int i=0; i<signatures.length; i++) {
			if (signatures[i] == null)
				break;
			
			String[] signComponents = signatures[i].split(":");
			for (Identity identity : idStore) {
				if (identity.pubKeyHash.equals(signComponents[0])) {
					//Log.v("Message", "checking identity " + identity.getName() + " hash " + identity.pubKeyHash);
					
					
					try {
						//Log.v("Message", "signComponents[0]:" + signComponents[0]);
						//Log.v("Message", "signComponents[1]:" + signComponents[1]);

						
						byte[] sigBytes = Base64.decode(signComponents[1]);
						sig.initVerify(identity.getPublicKey());
					    sig.update(getMsg().getBytes("UTF8"));
					    if (sig.verify(sigBytes)) {
							addValidSig(identity);
							//Log.v("Message", "Verified identity " + identity.getName());
					      } else {
					    	  //Log.v("Message", "Failed to verify signature.");
					      }
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (SignatureException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvalidKeyException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvalidKeySpecException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				  
						
					
				}
			}
			
			
		}
		
	
		return(validSigs);
	}

	public Ring getRing() {
		return ring;
	}
	
	public LinkedList<Identity> getValidSigs() {
		return(validSigs);
	}

	public String getId() {
		return id.toString();
	}
	
}
