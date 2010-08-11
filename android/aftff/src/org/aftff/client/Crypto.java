package org.aftff.client;

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
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (InvalidAlgorithmParameterException e) {
		// TODO Auto-generated catch block
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
