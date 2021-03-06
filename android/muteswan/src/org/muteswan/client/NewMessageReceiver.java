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
package org.muteswan.client;

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
		
		
   	    MuteLog.Log("MuteswanReceiver", "Received alarm, trying to connect to service.");
   	    

   	    int count = 0;
   	    while (msgService == null) {
   		  try {
				Thread.currentThread();
				Thread.sleep(200);
				count++;
				if (count > 3)
					break;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
   	    }
	
       if (msgService == null) {
   	     MuteLog.Log("MuteswanReceiver", "Service not running, starting.");
	     ctx.startService(svc);
   	   } else {
   		   
   		 try {
			if (msgService.isSkipNextCheck()) {
				 MuteLog.Log("MuteswanReceiver", "skipNextCheck is true, bailing out of check.");
				 msgService.setSkipNextCheck(false);
				 return;
			 }
		} catch (RemoteException e1) {
			e1.printStackTrace();
		}
   	     MuteLog.Log("MuteswanReceiver", "Service already running.");
   	     try {
		   msgService.refreshLatest();
	    } catch (RemoteException e) {
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
