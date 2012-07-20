package org.muteswan.client;

public class MuteLog {

	private final static boolean logEnabled = true;
	
	public static void Log(String prefix, String message) {
		if (logEnabled) {
		  android.util.Log.v(prefix,message);
		}
	}
	
}
