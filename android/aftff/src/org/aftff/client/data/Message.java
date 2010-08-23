package org.aftff.client.data;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.util.LinkedList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.aftff.client.Base64;
import org.aftff.client.Crypto;
import org.json.JSONException;
import org.json.JSONObject;

public class Message {

	private JSONObject jsonObj;
	private String msgData;
	private String date;
	private Ring ring;
	private Integer id;
	Crypto cryptoDec;
	
	// FIXME: define max signatures per message
	private String[] signatures = new String[50];
	
	LinkedList<Identity> validSigs;


	public Message(Ring ring, Integer id, String date, String msg) {
		this.date = date;
		this.ring = ring;
		this.msgData = msg;
		this.id = id;
	}
	
	public Message(Ring ring, String jsonString, String date) throws JSONException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException {
		jsonObj = new JSONObject(jsonString);
		this.date = date;
		this.ring = ring;
		
		String base64Msg = jsonObj.getString("message");
		byte[] rawMsgBytes = null;
		try {
			rawMsgBytes = Base64.decode(base64Msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
			
		//cryptoDec = new Crypto(ring.getKey().getBytes(),jsonObj.getString("message").getBytes("ISO-8859-1"));
		cryptoDec = new Crypto(ring.getKey().getBytes(),rawMsgBytes);
		byte[] msg = cryptoDec.decrypt();
		this.msgData = new String(msg);
		
		//FIXME: only one signature
		String sigStr = jsonObj.getString("signature");
		this.signatures[0] = sigStr;
		
		
		
		
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
			validSigs = new LinkedList();
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
	
	
	
}
