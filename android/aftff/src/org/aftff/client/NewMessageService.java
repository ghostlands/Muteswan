package org.aftff.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.aftff.client.data.AftffMessage;
import org.aftff.client.data.Ring;
import org.aftff.client.data.RingStore;
import org.aftff.client.ui.MsgList;
import org.apache.http.client.ClientProtocolException;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;

public class NewMessageService extends Service {

    Intent notificationIntent;
	PendingIntent contentIntent;
	NotificationManager mNM;
	HashMap notifyIds;
	int notifyIdLast;
	final int PERSISTANT_NOTIFICATION = 220;
	private final IBinder mBinder = new MyBinder();

    
	@Override
	public IBinder onBind(Intent intent) {		
		return mBinder;
	}

	public class MyBinder extends Binder {
		NewMessageService getService() {
				return NewMessageService.this;
		}
    }

	
	@Override
	public void onStart(Intent intent, int startId) {
		start();
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		
        //return START_STICKY;START_STICKY_COMPATIBILITY
		start();
		return 1;
    }

	
	//private Timer timer = new Timer();
	//private Timer timer;
	public void onCreate() {
		super.onCreate();
		AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarm.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime(),300000,NewMessageReceiver.getPendingIntent(this));
		
		
		notificationIntent = new Intent(this, aftff.class);
	    contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notifyIds = new HashMap();
		notifyIdLast = 0;
		
		
		CharSequence txt = "aftff checking for messages";
		long when = System.currentTimeMillis();
		int icon = R.drawable.icon;
		Notification notify = new Notification(icon,txt,when);
		//notify.flags |= Notification.FLAG_NO_CLEAR;

		
		Context context = getApplicationContext();
		CharSequence contentTitle = "aftff message check";
		CharSequence contentText = "aftff polling at 5 minute intervals";
		
		
		notify.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		mNM.notify(PERSISTANT_NOTIFICATION, notify);

	}
	
	@Override
	public void onDestroy() {
		stopservice();
		mNM.cancel(PERSISTANT_NOTIFICATION);
		AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarm.cancel(NewMessageReceiver.getPendingIntent(this));
	}
	
	private void start() {
		
		//timer.cancel();
				
		poll();
	}

	public void poll() {
		
		new Thread() {
			
			@Override
			public void run() {
				SharedPreferences prefs = getSharedPreferences(aftff.PREFS,0);
				RingStore store = new RingStore(getApplicationContext(),prefs);
				
				
				for (Ring r : store) {
					
					Integer lastIndex = prefs.getInt("lastMessage" + r.getFullText(), 0);
					Integer curIndex = r.getMsgIndex();
					if (lastIndex != 0 && curIndex != 0 && lastIndex != curIndex) {
						SharedPreferences.Editor ed = prefs.edit();
						ed.putInt("lastMessage" + r.getFullText(), curIndex);
						ed.commit();
						notifyIds.put(r.getFullText(), notifyIdLast++);
						showNotification(r,lastIndex,curIndex);
					} else if (lastIndex == 0) {
						SharedPreferences.Editor ed = prefs.edit();
						ed.putInt("lastMessage" + r.getFullText(), curIndex);
						ed.commit();
					}
				}
				
				
				
			}
		
			private void showNotification(Ring r, Integer lastIndex, Integer curIndex) {
				// TODO Auto-generated method stub
				CharSequence txt = "New message(s) in " + r.getShortname();
				long when = System.currentTimeMillis();
				int icon = R.drawable.icon;
				Notification notify = new Notification(icon,txt,when);
				notify.flags |= Notification.DEFAULT_LIGHTS;
				
				Integer diff = curIndex - lastIndex;
				
				Context context = getApplicationContext();
				CharSequence contentTitle = null;
				//"New aftff Message(s)";
				CharSequence contentText = null;
				//= r.getShortname() + " has new message(s).";
				
				// download and show latest message, don't set read
				if (diff == 1) {
					AftffMessage msg = null;
					try {
						msg = r.getMsg(curIndex.toString());
					} catch (ClientProtocolException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						return;
					}
					contentTitle = "New aftff Message";
					contentText = "New message in " + r.getShortname() + ":\n" + msg.getMsg();
				} else {
					contentTitle = "New aftff Messages";
					contentText = "New messages in " + r.getShortname() + "\n";
				}
				
				//else if (diff > 1 && diff <= 5) {
				//	for (int i = curIndex; i>(curIndex-diff); i--) {
				//		
				//	}
				//}
				
				if (contentText == null)
					return;
				
				
				Intent msgIntent = new Intent(context, MsgList.class);
				msgIntent.putExtra("ring", r.getFullText());
				PendingIntent pendingMsgIntent = PendingIntent.getActivity(context, 0, msgIntent, 0);
				
				notify.setLatestEventInfo(context, contentTitle, contentText, pendingMsgIntent);
				mNM.notify((Integer) notifyIds.get(r.getFullText()), notify);
			}
			
		
		}.start(); 
		
	}
	
		
		
		//}, 0, 300000);
		
		
	
		
//		timer = new Timer();
//		timer.scheduleAtFixedRate( new TimerTask() {
//		public void run() {
//			//Do whatever you want to do every “INTERVAL”
//			
//			
//			SharedPreferences prefs = getSharedPreferences(aftff.PREFS,0);
//			Store store = aftff.getStore(prefs);
//			
//			for (Ring r : store) {
//				String lastIndex = prefs.getString("lastIndex" + r.getFullText(), null);
//				String curIndex = r.getMsgIndexRaw();
//				if (lastIndex != null && curIndex != null && !lastIndex.equals(curIndex)) {
//					SharedPreferences.Editor ed = prefs.edit();
//					ed.putString("lastIndex" + r.getFullText(), curIndex);
//					ed.commit();
//					notifyIds.put(r.getFullText(), notifyIdLast++);
//					showNotification(r);
//				} else if (lastIndex == null) {
//					SharedPreferences.Editor ed = prefs.edit();
//					ed.putString("lastIndex" + r.getFullText(), curIndex);
//					ed.commit();
//				}
//			}
				
			
			
		//}

		
	
	private void stopservice() {
//		if (timer != null){
//			timer.cancel();
//		}
	}

	
	
}
