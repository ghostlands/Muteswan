/*
Copyright 2011-2012 James Unger, Rob Wolffe, Chris Churnick.
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

package org.muteswan.client;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Crypto {

	
	// FIXME: better solution for ivData than this? obviously protocol implications
	// use value from Last-Modified header? how would we know that? use iso date?
	final byte[] ivData = new byte[] { '0','1','2','3','4','5','6','7','0','1','2','3','4','5','6','7' };
	
	Cipher cipher;
	Cipher cipherd;
	
	IvParameterSpec iv;
	SecretKey secretKey;
	byte[] key;
	byte[] data;	
	
	public Crypto(byte[] key, byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException {
		
		this.secretKey = new SecretKeySpec(key,"AES");	
		this.iv = new IvParameterSpec(ivData);
		this.key = key;
		this.data = data;    
		
	   cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
	   cipherd = Cipher.getInstance("AES/CBC/PKCS5Padding");
	   
	   
	try {
		cipher.init(Cipher.ENCRYPT_MODE, secretKey,iv);
		cipherd.init(Cipher.DECRYPT_MODE, secretKey,iv);
	} catch (InvalidKeyException e) {
		e.printStackTrace();
	} catch (InvalidAlgorithmParameterException e) {
		e.printStackTrace();
	}
		
	   
	}
	
	public byte[] decrypt() throws IllegalBlockSizeException, BadPaddingException {
	       return(cipherd.doFinal(data));
	}
	
	public byte[] encrypt() throws IllegalBlockSizeException, BadPaddingException {
	       return(cipher.doFinal(data));
	}
	
}
