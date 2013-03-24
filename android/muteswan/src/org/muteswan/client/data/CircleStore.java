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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.muteswan.client.Main;
import org.muteswan.client.MuteLog;
import org.muteswan.client.MuteswanHttp;

import android.content.Context;
import android.content.SharedPreferences;



import android.preference.PreferenceManager;
import android.util.Log;

@SuppressWarnings("serial")
final public class CircleStore extends LinkedList<Circle> {
	public static boolean libsLoaded = false;
    public Context context;
	private MuteswanHttp muteswanHttp;
	private String cipherSecret;
	private SharedPreferences prefs;
	

	
	
	public CircleStore(String secret, Context applicationContext, boolean readDb, boolean initCache, MuteswanHttp muteswanHttp) {
		MuteLog.Log("CircleStore", "Circle store called!");
		context = applicationContext;
	    
	    this.muteswanHttp = muteswanHttp;
	    this.cipherSecret = secret;
	    prefs = context.getSharedPreferences("circles",0);
	    

	    MuteLog.Log("CircleStore", "Circle store is " + cipherSecret);
	    
		if (readDb && initCache) {
          initStore(muteswanHttp,true);
		} else if (readDb) {
			initStore(muteswanHttp,false);
		}
	}
	
	public CircleStore(String secret, Context applicationContext, boolean readDb, boolean initCache) {
		MuteLog.Log("CircleStore", "Circle store called!");
		context = applicationContext;
	    
	    this.cipherSecret = secret;
	    prefs = context.getSharedPreferences("circles",0);
	    

		if (readDb && initCache) {
          initStore(true);
		} else if (readDb) {
			initStore(false);
		}
	}
	
	public CircleStore(String cipherSecret,Context applicationContext) {
		context = applicationContext;
	    
	    this.cipherSecret = cipherSecret;
	    prefs = context.getSharedPreferences("circles",0);
	    
	}

	
	
	
	  final public void deleteCircle(Circle circle) {
		  if (cipherSecret == null) { MuteLog.Log("Circle", "Error: refusing use database with null cipherSecret"); return; }

		  
		  prefs.edit().remove(Main.genHexHash(circle.getFullText())).commit();
		  
		 
		  circle.deleteAllMessages(true);
	  }
	

	  
	final public String getAsString() {
		String returnString = "";
		
		for (Circle r : this) {
			returnString = returnString + r.getFullText() + "---";
		}
		return(returnString);
	}
	  
	  
	  
	  private void initStore(MuteswanHttp muteswanHttp, boolean initCache) {
		  if (cipherSecret == null) { MuteLog.Log("Circle", "Error: refusing use database with null cipherSecret"); return; }
		  MuteLog.Log("CIPHER", "Initialize circle store with " + cipherSecret);
			
		  Map<String, ?> allCircles = prefs.getAll();
		  for (String cir : allCircles.keySet()) {
			  
			  //Circle r = new Circle(cipherSecret,context,cir);
			  //add(r);
			  String circleText = (String) allCircles.get(cir);
			  try {
				JSONObject jsonObject = new JSONObject(circleText);
				Circle r = new Circle(cipherSecret,context,jsonObject,muteswanHttp);
				add(r);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			  //FIXME decrypt
			  
			  
		  }
		  
		  
		
	  }
	  
	  private void initStore(boolean initCache) {
		  if (cipherSecret == null) { MuteLog.Log("Circle", "Error: refusing use database with null cipherSecret"); return; }

		  MuteLog.Log("CIPHER", "Initialize circle store with " + cipherSecret);
		  
			
		  Map<String, ?> allCircles = prefs.getAll();
		  for (String cir : allCircles.keySet()) {
			  
			  String circleText = (String) allCircles.get(cir);
			  try {
				JSONObject jsonObject = new JSONObject(circleText);
				Circle r = new Circle(cipherSecret,context,jsonObject);
				add(r);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			  
			
		  }
	  }
	  
	  
	 
	  
	  public void updateStore(String contents) {
		  Circle circle = new Circle(cipherSecret,context,contents);
		  for (Circle r : this) {
			  if (r.getFullText().equals(contents)) {
				  return;
			  }
		  }
		  addCircleToDb(circle);
	  }
	  
	  public void updateStore(String key, String uuid, String shortname, String server) {
		  Circle circle = new Circle(cipherSecret,context,key,uuid,shortname,server);
		  for (Circle r : this) {
			  if (r.getKey().equals(key) && r.getShortname().equals(shortname) && r.getServer().equals(server)) {
				  return;
			  }
		  }
		  addCircleToDb(circle);
	  }
	  
	  private void addCircleToDb(Circle circle) {
		  if (cipherSecret == null) { MuteLog.Log("Circle", "Error: refusing use database with null cipherSecret"); return; }

		  
		  JSONObject jsonObject = circle.getCryptJSON(cipherSecret);
		  prefs.edit().putString(Main.genHexHash(circle.getFullText()), jsonObject.toString()).commit();
		
		  
	
		  circle.createLastMessage(0);
	  
		  add(circle);
	  }
	  

	  
	  public HashMap<String,Circle> asHashMap() {
		HashMap<String,Circle> map = new HashMap<String,Circle>();
		for (Circle r : this) {
			map.put(Main.genHexHash(r.getFullText()), r);
		}
		
		return map;
		  
	  }

	public boolean containsShortname(String shortname) {
		for (Circle r : this) {
			if (r.getShortname().contains(shortname)) {
				return true;
			}
		}
		return false;
		
	}

	
	  
	public Set<String> getUniqServers() {
		HashMap<String,String> map = new HashMap<String,String>();
	
		
		
		CIRCLE: for (Circle r : this) {
			for (String s : map.keySet()) {
				if (s.equals(r.getServer())) {
					continue CIRCLE;
				}
			}
			
			map.put(r.getServer(), "");
		}
		
		return map.keySet();
	}
	

	
}
