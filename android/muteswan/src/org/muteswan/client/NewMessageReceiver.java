package org.muteswan.client;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

public class NewMessageReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctx, Intent intent) {
		Intent svc = new Intent(ctx,NewMessageService.class);
		
        IMessageService msgService = (IMessageService) peekService(ctx,svc);
     

   	    SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);

   	    
		boolean backgroundMessageCheck = defPrefs.getBoolean("backgroundMessageCheck", false);				
		if (backgroundMessageCheck == false)
			return;
   	
   	
   	    Log.v("MuteswanReceiver", "Received alarm, trying to connect to service.");

   	    int count = 0;
   	    while (msgService == null) {
   		  try {
				Thread.currentThread().sleep(200);
				count++;
				if (count > 3)
					break;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
   	    }	
	
       if (msgService == null) {
   	     Log.v("MuteswanReceiver", "Service not running, starting.");
	     ctx.startService(svc);
   	   } else {
   	     Log.v("MuteswanReceiver", "Service already running.");
   	     try {
		   msgService.longPoll();
	    } catch (RemoteException e) {
		// TODO Auto-generated catch block
		  e.printStackTrace();
	   }
   	  }
   	
   	

        
	}
	
	public static PendingIntent getPendingIntent(Context ctx) {
         return PendingIntent.getBroadcast(ctx,
                         PendingIntent.FLAG_CANCEL_CURRENT, new Intent(ctx,
                                         NewMessageReceiver.class), 0);
    }

	
	

}
