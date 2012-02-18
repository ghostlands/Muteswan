package org.muteswan.client;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

public class OnBootReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

                if (intent.getAction() != null
                                && intent.getAction().equals("android.intent.action.BOOT_COMPLETED"))
                {
                  Log.v("MuteswanOnBoot", "Registering service if configured.");
          		  SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(context);
             		
           		  boolean backgroundMessageCheck = defPrefs.getBoolean("backgroundMessageCheck", false);			
           		  if (backgroundMessageCheck == true) {
           		   Integer checkMsgInterval = Integer.parseInt(defPrefs.getString("checkMsgInterval", "5"));
           		
           		   int checkMsgIntervalMs = checkMsgInterval * 60 * 1000;
           		
           		   AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
           		   alarm.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime()+checkMsgInterval*60,checkMsgIntervalMs,NewMessageReceiver.getPendingIntent(context));
           		  }
                }


        }


}

