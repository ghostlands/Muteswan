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

import android.os.Bundle;
import android.os.SystemClock;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class Preferences extends PreferenceActivity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(org.muteswan.client.R.xml.preferences);
			
		 final SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());	
		
		 Preference backgroundMsgCheckP = (Preference) this.findPreference("backgroundMessageCheck");
         backgroundMsgCheckP.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                                 public boolean onPreferenceChange(Preference preference, Object pref) {
                                	     boolean backgroundMessageCheck = defPrefs.getBoolean("backgroundMessageCheck", false);
                                	     
                                	     if (!backgroundMessageCheck) {
                                	    	 scheduleServiceAlarm(defPrefs);
                                	    	 Toast.makeText(getBaseContext(),
                                                         "Background check is enabled.",
                                                         Toast.LENGTH_LONG).show();
                                	     } else {
                                	    	 Toast.makeText(getBaseContext(),
                                                     "Background disabled",
                                                     Toast.LENGTH_LONG).show();
                                	     }
                              	    	 return true;
                                 }


                         });
		
		
	}
	
	

	 private void scheduleServiceAlarm(SharedPreferences defPrefs) { 
		
		  boolean backgroundMessageCheck = defPrefs.getBoolean("backgroundMessageCheck", false);			
		  if (!backgroundMessageCheck) {
		   Integer checkMsgInterval = Integer.parseInt(defPrefs.getString("checkMsgInterval", "5"));
		
		   int checkMsgIntervalMs = checkMsgInterval * 60 * 1000;
		
		   AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		   alarm.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime()+checkMsgInterval*60,checkMsgIntervalMs,NewMessageReceiver.getPendingIntent(getApplicationContext()));
		  }
	
	 }
}
