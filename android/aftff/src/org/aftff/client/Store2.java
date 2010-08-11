package org.aftff.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.LinkedList;



public class Store2 extends LinkedList<Ring> {
	//extends LinkedList<Ring> implements Serializable {
//}

	Store2 alt = null;
	/**
	 * 
	 */
	//private static final long serialVersionUID = 6935618427061639442L;
	//LinkedList<Ring> dataStore = new LinkedList<Ring>();

	public Store2 getAlt() {
		return this.alt;
	}
	
	public void freeze() {
		  File file = new File("/sdcard/aftff.dat");
      	
	      FileOutputStream output;
	      try {
				output = new FileOutputStream(file);
				ObjectOutputStream serialize = new ObjectOutputStream(output);
				serialize.writeObject(alt);
		   } catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		   } catch (IOException e) {
					// TODO Auto-generated catch block
				e.printStackTrace();
		   }
	        	
	}
	
	private void init() {
		File rFile = new File("/sdcard/aftff.dat");
	       
		try {
		   FileInputStream input = new FileInputStream(rFile);
		   ObjectInputStream inSerialize = new ObjectInputStream(input);
	       alt = (Store2) inSerialize.readObject();
		
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			//store = new Store();
				        
	        return;
	        //e1.printStackTrace();

		} catch (StreamCorruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
				        
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	
	//public Store() {
		//init();
	//}

	public Ring addAsString(String contents) {
		String[] nameAndAddr = contents.split("+");
		String name = nameAndAddr[0];
		String addr = nameAndAddr[1];
		
		String[] keyAndSrv = addr.split("@");
		String key = keyAndSrv[0];
		String srv = keyAndSrv[1];
		
		if (key == null || name == null || srv == null) {
			return(null);
		}
		
		Ring newRing = new Ring(key,name,srv);
		
		//this.add(newRing);
		
		return(newRing);
	}
	

	
    //public LinkedList<Ring> getStore() {
	// return dataStore;
    //}

    //public void setDataStore(LinkedList<Ring> dataStore) {
	// this.dataStore = dataStore;
    //}
 
    //public void addRing(Ring ring) {
	// dataStore.add(ring);
    //}
 
    //public void deleteRing(Ring ring) {
	// dataStore.remove(ring);
    //}
 
}
