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

import org.muteswan.client.NewMessageReceiver;
import org.muteswan.client.R;

import android.os.Bundle;
import android.os.SystemClock;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import org.muteswan.client.MuteLog;


public class Preferences extends PreferenceActivity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(org.muteswan.client.R.xml.preferences);
			
		
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
                                	    	 Toast.makeText(getBaseContext(),
                                                     R.string.n_check_service_disabled,
                                                     Toast.LENGTH_LONG).show();
                                	     }
                              	    	 return true;
                                 }


                         });
         
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
		
		   AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		   alarm.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime()+minutes*60,checkMsgIntervalMs,NewMessageReceiver.getPendingIntent(getApplicationContext()));
		  }
	
	 }
}
