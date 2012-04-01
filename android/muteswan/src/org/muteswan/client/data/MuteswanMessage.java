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

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.muteswan.client.Base64;
import org.muteswan.client.Crypto;
import org.muteswan.client.MuteLog;

import android.util.Log;

public class MuteswanMessage {

	private String msgData;
	private String date;
	private Circle circle;
	private String rawMsg;
	private Integer id;
	Crypto cryptoDec;
	
	// FIXME: define max signatures per message
	public String[] signatures = new String[50];
	
	private LinkedList<Identity> validSigs;
	private Date dateObj;
	private String base64Msg;
	private String rawJSON;
	private String base64IVData;


	public MuteswanMessage(Circle circle, Integer id, String date, String msg, String rawMsg) {
		this.date = date;
		this.circle = circle;
		this.msgData = msg;
		this.id = id;
		this.rawMsg = rawMsg;
	}
	
	public MuteswanMessage(Circle circle, Integer id, String date, String msg, String[] signatures, String rawMsg) {
		this.date = date;
		this.circle = circle;
		this.msgData = msg;
		this.id = id;
		this.signatures = signatures;
		this.rawMsg = rawMsg;
	}
	
	public MuteswanMessage(Integer id, Circle circle, JSONObject jsonObj, String date) throws JSONException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {
		this.date = date;
		this.circle = circle;
		
		base64Msg = jsonObj.getString("message");
		rawJSON = jsonObj.toString();
	
		
		String base64IVData = null;
		try  {
		   base64IVData = jsonObj.getString("iv");
		} catch (JSONException e) {
			byte[] ivData = new byte[] { '0','1','2','3','4','5','6','7','0','1','2','3','4','5','6','7' };
			base64IVData = Base64.encodeBytes(ivData);
		}
		
		
	
		
		byte[] rawMsgBytes = null;
		try {
			rawMsgBytes = Base64.decode(base64Msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	
		byte[] ivData = null;
		try {
			ivData = Base64.decode(base64IVData);
		} catch (IOException e) {
			e.printStackTrace();
		}

		MuteLog.Log("MuteswanMessage","base64IVData: " + base64IVData);
		MuteLog.Log("MuteswanMessage", "Key length when decoding: " + Base64.decode(circle.getKey()).length);
		if (circle.getKey().endsWith("=")) {
			cryptoDec = new Crypto(Base64.decode(circle.getKey()),rawMsgBytes,ivData);
		} else {
			//cryptoDec = new Crypto(Base64.decode(circle.getKey()),rawMsgBytes,ivData);
			cryptoDec = new Crypto(circle.getKey().getBytes(),rawMsgBytes,ivData);
		}
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
	
	public void setBase64Msg(String base64Msg) {
		this.base64Msg = base64Msg;
		
	}
	
	public String getBase64Msg() {
		return(base64Msg);
	}
	
	public void setRawJSON(String rawJSON) {
		this.rawJSON = rawJSON;
		
	}
	
	public String getRawJSON() {
		return(rawJSON);
	}
	
	
	public Date getDateObj() {
		if (dateObj == null) {
		  SimpleDateFormat df = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		  try {
			this.dateObj = df.parse(date);
		  } catch (ParseException e) {
			e.printStackTrace();
		  }
		}
		return(dateObj);
	}

	public void addValidSig(Identity identity) {
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
		//MuteLog.Log("Message", "In verifySignatures");
		
		Signature sig;
		try {
			sig = Signature.getInstance("MD5WithRSA");
		} catch (NoSuchAlgorithmException e1) {
			return(null);
		}
		
		
		for (int i=0; i<signatures.length; i++) {
			if (signatures[i] == null)
				break;
			
			String[] signComponents = signatures[i].split(":");
			for (Identity identity : idStore) {
				if (identity.pubKeyHash.equals(signComponents[0])) {
					//MuteLog.Log("Message", "checking identity " + identity.getName() + " hash " + identity.pubKeyHash);
					
					
					try {
						//MuteLog.Log("Message", "signComponents[0]:" + signComponents[0]);
						//MuteLog.Log("Message", "signComponents[1]:" + signComponents[1]);

						
						byte[] sigBytes = Base64.decode(signComponents[1]);
						sig.initVerify(identity.getPublicKey());
					    sig.update(getMsg().getBytes("UTF8"));
					    if (sig.verify(sigBytes)) {
							addValidSig(identity);
							MuteLog.Log("Message", "Verified identity " + identity.getName());
					      } else {
					    	MuteLog.Log("Message", "Failed to verify signature.");
					      }
					} catch (IOException e) {
						e.printStackTrace();
					} catch (SignatureException e) {
						e.printStackTrace();
					} catch (InvalidKeyException e) {
						e.printStackTrace();
					} catch (NoSuchAlgorithmException e) {
						e.printStackTrace();
					} catch (InvalidKeySpecException e) {
						e.printStackTrace();
					}
				  
						
					
				}
			}
			
			
		}
		
	
		return(validSigs);
	}

	public Circle getCircle() {
		return circle;
	}
	
	public LinkedList<Identity> getValidSigs() {
		return(validSigs);
	}

	public String getId() {
		return id.toString();
	}
	
}
