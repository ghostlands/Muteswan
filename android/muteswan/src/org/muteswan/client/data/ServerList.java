package org.muteswan.client.data;

import java.io.File;
import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;
import org.muteswan.client.Main;
import org.muteswan.client.MuteLog;

import android.content.Context;

public class ServerList extends LinkedList<MuteswanServer> {
	
	private Context context;
	
	public void init(Context context) {
		this.context = context;
		File fileDir = getStorePath();
		fileDir.mkdir();
		
		File[] files = fileDir.listFiles();
		for (File f  : files) {
			String jsonString = Circle.getFileContents(f);
			try {
				JSONObject jsonObj = new JSONObject(jsonString);
				MuteswanServer mtsnServer = new MuteswanServer();
				mtsnServer.init(f.getName(), jsonObj);
				this.add(mtsnServer);
			} catch (JSONException e) {
				JSONObject jsonObj = new JSONObject();
				MuteswanServer mtsnServer = new MuteswanServer();
				mtsnServer.init(f.getName(),jsonObj);
				this.add(mtsnServer);
			}
		}
		
		
	}
	
	public MuteswanServer[] getArray() {
		MuteswanServer[] array = new MuteswanServer[this.size()];
		
		
		int indx = 0;
		for (MuteswanServer s : this) {
			array[indx] = s;
			indx++;
		}
		
		return(array);
	}
	
	public boolean addServer(MuteswanServer server) {
		
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("Name", server.getServerInfo().getName());
			File fileDir = getStorePath();
			File f = new File(fileDir.toString() + "/" + server.getHostname());
			Circle.writeFileContent(f,jsonObj.toString());
		} catch (JSONException e) {
			File fileDir = getStorePath();
			File f = new File(fileDir.toString() + "/" + server.getHostname());
			Circle.writeFileContent(f, "");
		}
		
		
		this.add(server);
		return true;
	}
	
	public File getStorePath() {
		
		return(new File(context.getFilesDir() + "/servers"));
	}
}


