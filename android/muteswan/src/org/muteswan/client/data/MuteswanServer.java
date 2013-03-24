package org.muteswan.client.data;

import org.json.JSONException;
import org.json.JSONObject;

public class MuteswanServer {

	
	private String hostname;
	private ServerInfo serverInfo;
	
	
	public class ServerInfo {
		public String Name = "";
		
		public void setName(String arg) {
			this.Name = arg;
		}
		
		public String getName() {
			return Name;
		}
	}
	
	public ServerInfo getServerInfo() {
		return serverInfo;
	}
	
	public String getHostname() {
		return hostname;
	}
	
	public String toString() {
		return serverInfo.getName() + " (" + getHostname() + ")";
	}
	
	public void init(String server, JSONObject jsonObj) {
		this.hostname = server;
		serverInfo = new ServerInfo();
		try {
			serverInfo.setName(jsonObj.getString("Name"));
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}
		
	}
	
	
}
