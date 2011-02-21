package org.muteswan.client.data;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.muteswan.client.Base64;
import org.muteswan.client.muteswan;

public class Identity {
	
	String name;
	public String publicKeyEnc;
	public String privateKeyEnc;
	String pubKeyHash;
	String privKeyHash;
	
	public String formatPub;
	public String formatPriv;
	
	
	 public void genKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
	        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
	        keyGen.initialize(512);
	        
	        KeyPair keyPair = keyGen.generateKeyPair();
	        
	        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
	        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
			
	        
	        formatPub = publicKey.getFormat();
	        formatPriv = privateKey.getFormat();
	        //publicKey.getEncoded();

	        publicKeyEnc = Base64.encodeBytes(publicKey.getEncoded());
	        privateKeyEnc = Base64.encodeBytes(privateKey.getEncoded());
      
	 }
	 
	 
	 public RSAPublicKey getPublicKey() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		EncodedKeySpec encKeySpec = new X509EncodedKeySpec(Base64.decode(publicKeyEnc));
		
		return (RSAPublicKey) (keyFactory.generatePublic(encKeySpec));
		

	 }
	
	 public RSAPrivateKey getPrivateKey() throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
		 
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			EncodedKeySpec encKeySpec = new PKCS8EncodedKeySpec(Base64.decode(privateKeyEnc));
			return (RSAPrivateKey) (keyFactory.generatePrivate(encKeySpec));
			
	 }
	 
	 public String toString() {
		 if (privateKeyEnc.length() > 1) {
			 return name + " (pub/priv)";
		 } else {
			 return name + " (pub)";
		 }
	 }
	 
	 public Identity() {
		 
	 }
	
	 public void setName(String name) {
		 this.name = name;
	 }
	 
	 public String getName() {
		 return(name);
	 }
	 
	 
	 
	public Identity(String name, String publicKeyEnc, String privateKeyEnc, String pubKeyHash, String privKeyHash) {
		this.name = name;
		this.publicKeyEnc = publicKeyEnc;
		this.privateKeyEnc = privateKeyEnc;
		this.pubKeyHash = pubKeyHash;
		this.privKeyHash = privKeyHash;
	}
	
	// constructor when import public key
	public Identity(String name, String publicKeyEnc, String privKeyHash) {
		this.name = name;
		this.publicKeyEnc = publicKeyEnc;
		this.privateKeyEnc = "";
		this.pubKeyHash = getPubKeyHash();
		this.privKeyHash = privKeyHash;
	}
	
	
	public String getPrivKeyHex() {
			
	        byte[] privateKeyBytes = privateKeyEnc.getBytes();
	        StringBuffer privKeyHexBuf = new StringBuffer();
	      
	        for (int i = 0; i < privateKeyBytes.length; ++i) {
	            privKeyHexBuf.append(Integer.toHexString(0x0100 + (privateKeyBytes[i] & 0x00FF)).substring(1));
	        }
	        return(privKeyHexBuf.toString());
	}
	
	public String getPubKeyHex() {
		  byte[] privateKeyBytes = privateKeyEnc.getBytes();
	      StringBuffer privKeyHexBuf = new StringBuffer();
	      
	      for (int i = 0; i < privateKeyBytes.length; ++i) {
	         privKeyHexBuf.append(Integer.toHexString(0x0100 + (privateKeyBytes[i] & 0x00FF)).substring(1));
	      }
	      return(privKeyHexBuf.toString());
    }
	
	public String getPubKeyHash() {
		return(muteswan.genHexHash(publicKeyEnc.toString()));
	}
	
	public String getPrivKeyHash() {
		return(muteswan.genHexHash(privateKeyEnc.toString()));
	}


	public String getShareableString() {
		return(name + ":" + publicKeyEnc + ":" + privKeyHash );

		
	}
	
	
	
	
}
