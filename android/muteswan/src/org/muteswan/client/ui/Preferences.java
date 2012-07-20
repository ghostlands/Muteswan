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
package org.muteswan.client.ui;

import org.muteswan.client.AlertDialogs;
import org.muteswan.client.NewMessageReceiver;
import org.muteswan.client.R;

import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import org.muteswan.client.MuteLog;
import org.muteswan.client.data.Circle;
import org.muteswan.client.data.CircleStore;


public class Preferences extends PreferenceActivity {

	private Bundle extra;
	private String cipherSecret;
	private boolean offerToInstallOISafe = false;


	public void onResume() {
		super.onResume();
		if (offerToInstallOISafe) {
			AlertDialogs alertDialogs = new AlertDialogs(Preferences.this);
	 		alertDialogs.offerToInstallOISafe();
		}
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(org.muteswan.client.R.xml.preferences);
			
		extra = getIntent().getExtras();
        cipherSecret = extra.getString("secret");
		
		 Preference backgroundMsgCheckP = (Preference) this.findPreference("backgroundMessageCheck");
         backgroundMsgCheckP.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                                 public boolean onPreferenceChange(Preference preference, Object pref) {
                                	 	 final SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());	
                                	     boolean backgroundMessageCheck = defPrefs.getBoolean("backgroundMessageCheck", false);
                                	     Integer checkMsgInterval = Integer.parseInt(defPrefs.getString("checkMsgInterval", "5"));
                                	     
                                	     // old values, so inversion necessary
                                	     if (backgroundMessageCheck == false) {
                                	    	 scheduleServiceAlarm(defPrefs,checkMsgInterval);
                                	    	 Toast.makeText(getBaseContext(),
                                                         R.string.n_check_service_enabled,
                                                         Toast.LENGTH_LONG).show();
                                	     } else {
                                	    	 //Boolean keepSecret = defPrefs.getBoolean("keepsecret", true);
                                	    	 //if (!keepSecret)
                                	    	 //		 defPrefs.edit().remove("cipherSecret").commit();
                                	    	 Toast.makeText(getBaseContext(),
                                                     R.string.n_check_service_disabled,
                                                     Toast.LENGTH_LONG).show();
                                	     }
                              	    	 return true;
                                 }


                         });
         
         
         Preference useOISafe = (Preference) this.findPreference("useoisafe");
         useOISafe.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
             

			public boolean onPreferenceChange(Preference preference, Object pref) {
            	boolean useoisafe = (Boolean) pref;
            	
        	 	final SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        	 	
        	 	if (useoisafe == true) {
        	 		
        	 		defPrefs.edit().putBoolean("keepsecret", false).commit();
        	 		
        	 		Intent intent = new Intent("org.openintents.action.SET_PASSWORD");
        			intent.putExtra("org.openintents.extra.UNIQUE_NAME", "muteswan");
        			intent.putExtra("org.openintents.extra.PASSWORD", cipherSecret);

        			MuteLog.Log("Preferences", "Calling setsafe secret "); 
        			
        			try {
        			  startActivityForResult(intent,0);
        			} catch (ActivityNotFoundException e) {
        				MuteLog.Log("Main", "Activity not found.");
        				offerToInstallOISafe = true;
        				
        			} catch (java.lang.SecurityException e) {
        				MuteLog.Log("Main", "Security exception " + e); 
        			}
        	 		
        	 		
        	 	} else {
        	 		
        	 		defPrefs.edit().putBoolean("keepsecret", true).commit();
        	 		//alertDialogs.
        	 	}

				return true;
            	 
             }
         });
         
         Preference cleanData = (Preference) this.findPreference("cleanData");
         
         /* uncomment to get clear data
          * cleanData.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference arg0) {
				MuteLog.Log("Preferences", "Clean data pressed!");
				
				AlertDialog.Builder wipeDataDialog = new AlertDialog.Builder(Preferences.this);
			    wipeDataDialog.setTitle("Wipe Message Data?");
			    wipeDataDialog.setMessage("This will delete all message data. Your circles will still be saved, but you need to download any new messages.");
			    wipeDataDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			    	public void onClick(DialogInterface dialogInterface, int i) {
			    		 CircleStore store = new CircleStore(cipherSecret,getApplicationContext(),true,false);
			    		for (Circle c : store) {
			    			c.deleteAllMessages(true);
			    		}
				      }
			      
			    });
			    wipeDataDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			      public void onClick(DialogInterface dialogInterface, int i) {}
			    });
			    wipeDataDialog.create();
			    wipeDataDialog.show();
				
				return true;
			}
        	 
         });
         */
         
         Preference checkMsgIntervalP = (Preference) this.findPreference("checkMsgInterval");
         checkMsgIntervalP.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                                 public boolean onPreferenceChange(Preference preference, Object pref) {
                                	     MuteLog.Log("Preferences","pref is " + pref.toString());
                                	 	 final SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());	
                                	     scheduleServiceAlarm(defPrefs,Integer.parseInt((String) pref));
                              	    	 return true;
                                 }


                         });
		
	}
	
	

	 private void scheduleServiceAlarm(SharedPreferences defPrefs, Integer minutes) { 
		
		  boolean backgroundMessageCheck = defPrefs.getBoolean("backgroundMessageCheck", false);			
		  if (backgroundMessageCheck == true) {
		   MuteLog.Log("Preferences","Alarm set for " + minutes + " minutes.");
		
		   int checkMsgIntervalMs = minutes * 60 * 1000;
		   
		   
		   Intent intent = new Intent(getApplicationContext(),NewMessageReceiver.class);
		   PendingIntent receiverIntent = NewMessageReceiver.getPendingIntent(getApplicationContext());
		   
		   
		   AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		   alarm.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime()+minutes*60,checkMsgIntervalMs,receiverIntent);
		  }
	
	 }
}
