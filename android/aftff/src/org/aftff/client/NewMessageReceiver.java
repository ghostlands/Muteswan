package org.aftff.client;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NewMessageReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctx, Intent intent) {
		// TODO Auto-generated method stub
		
        Intent svc = new Intent(ctx, NewMessageService.class);
        //ctx.stopService(svc);
        
        //String nullExc = null;
        //nullExc.length();
       ctx.startService(svc);
        //ctx.bindService(service, conn, flags)
        
	}
	
	public static PendingIntent getPendingIntent(Context ctx) {
         return PendingIntent.getBroadcast(ctx,
                         PendingIntent.FLAG_CANCEL_CURRENT, new Intent(ctx,
                                         NewMessageReceiver.class), 0);
    }

	
	

}
