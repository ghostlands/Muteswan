/*
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
import java.util.LinkedList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.muteswan.client.Base64;
import org.muteswan.client.Crypto;

import android.util.Log;

public class MuteswanMessage {

	private String msgData;
	private String date;
	private Circle circle;
	private Integer id;
	Crypto cryptoDec;
	
	// FIXME: define max signatures per message
	public String[] signatures = new String[50];
	
	private LinkedList<Identity> validSigs;


	public MuteswanMessage(Circle circle, Integer id, String date, String msg) {
		this.date = date;
		this.circle = circle;
		this.msgData = msg;
		this.id = id;
	}
	
	public MuteswanMessage(Circle circle, Integer id, String date, String msg, String[] signatures) {
		this.date = date;
		this.circle = circle;
		this.msgData = msg;
		this.id = id;
		this.signatures = signatures;
	}
	
	public MuteswanMessage(Integer id, Circle circle, JSONObject jsonObj, String date) throws JSONException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException {
		this.date = date;
		this.circle = circle;
		
		String base64Msg = jsonObj.getString("message");
		
		// signatures disabled
		//JSONArray sigs = null;
		/*try {
		  sigs = jsonObj.getJSONArray("signatures");
		  for (int i=0; i<sigs.length(); i++) {
			    String sig = sigs.getString(i);

			    // FIXME: better way to check for non encrypted signatures
			    // not encrypted signature
			    if (sig.indexOf(":") != -1) {
				  this.signatures[i] = sig;
				// encrypted signature
			    } else {
				  byte[] sigKeyBytes = Base64.decode(sig);
			      Crypto cryptoSig = new Crypto(circle.getKey().getBytes(),sigKeyBytes);
			      byte[] realSignature = cryptoSig.decrypt();
				  this.signatures[i] = new String(realSignature);
			    }
		  }
		} catch (JSONException e) {
			
		}
		*/
		
		
		
		byte[] rawMsgBytes = null;
		try {
			rawMsgBytes = Base64.decode(base64Msg);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
			
		cryptoDec = new Crypto(circle.getKey().getBytes(),rawMsgBytes);
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
							Log.v("Message", "Verified identity " + identity.getName());
					      } else {
					    	Log.v("Message", "Failed to verify signature.");
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
