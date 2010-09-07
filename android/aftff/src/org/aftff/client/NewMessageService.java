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
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

public class NewMessageService extends Service {

    Intent notificationIntent;
	PendingIntent contentIntent;
	NotificationManager mNM;
	HashMap notifyIds;
	int notifyIdLast;
	final int PERSISTANT_NOTIFICATION = 220;
	private boolean backgroundMessageCheck;
	private int checkMsgInterval;
	private int numMsgDownload;
	private SharedPreferences defPrefs;
	private boolean justLaunched = false;
	protected boolean isWorking;
    
	

	

	
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
		
		defPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
		checkMsgInterval = Integer.parseInt(defPrefs.getString("checkMsgInterval", "5"));
		
		int checkMsgIntervalMs = checkMsgInterval * 60 * 1000;
		
		AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarm.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime(),checkMsgIntervalMs,NewMessageReceiver.getPendingIntent(this));
		
		
		
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
		CharSequence contentTitle = "aftff background service";
		CharSequence contentText = "aftff polling at " + checkMsgInterval + " minute intervals";
		
		
		backgroundMessageCheck = defPrefs.getBoolean("backgroundMessageCheck", false);				
		numMsgDownload = Integer.parseInt(defPrefs.getString("numMsgDownload","1"));
	
		
		notify.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		if (backgroundMessageCheck) 
		  mNM.notify(PERSISTANT_NOTIFICATION, notify);

		
		justLaunched = true;
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
		backgroundMessageCheck = defPrefs.getBoolean("backgroundMessageCheck", false);				
		numMsgDownload = Integer.parseInt(defPrefs.getString("numMsgDownload","5"));
	
		if (justLaunched == true) {
			justLaunched = false;
			return;
		}
		
		//poll();
	}

	public void poll() {
		
		new Thread() {
			
			@Override
			public void run() {
				
				
				if (!backgroundMessageCheck) {
					Log.v("Service", "backgroundMessageCheck is false, not polling.");
					return;
				}
				
				
				
				RingStore store = new RingStore(getApplicationContext(),true);
				
				for (Ring r : store) {
					
					Integer lastIndex = r.getLastMessage();
					Integer curIndex = r.getMsgIndex();
					Integer diff = curIndex - lastIndex;
					
					CharSequence notifTitle = null;
					CharSequence notifText = null;
					
					if (lastIndex != 0 && curIndex != 0 && lastIndex != curIndex) {
						r.updateLastMessage(curIndex);
						Log.v("NewMessageService", r.getShortname() + ": just updated curIndex to " + curIndex);
						notifyIds.put(r.getFullText(), notifyIdLast++);
						
						if (numMsgDownload == 0) {
							continue;
						} else if (diff == 1) {
							AftffMessage msg = null;
							try {
								msg = r.getMsg(curIndex.toString());
							} catch (ClientProtocolException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							notifTitle = "New message in " + r.getShortname();
							notifText = msg.getMsg();
							
						} else {
							
							int dCount = 0;
						    Log.v("Service","numMsgDownload is " + numMsgDownload + " and diff is " + diff);
							int start = curIndex;
							int end = curIndex - diff;
							
							if (diff > numMsgDownload) {
								end = curIndex - numMsgDownload;
							}
						    
							Log.v("Service", "start is " + start + " and end is " + end);
						    for (Integer i = start; i>end; i--) {
						    	try {
									AftffMessage msg = r.getMsg(i.toString());
									dCount++;
								} catch (ClientProtocolException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
						    }
						    notifTitle = "New messages in " + r.getShortname();
							notifText = dCount + " messages downloaded.";
							

						}
						
						showNotification(r,notifTitle,notifText);
					} else if (lastIndex == 0) {
						r.updateLastMessage(curIndex);
					}
				}
				
				
				
			}
		
			private void showNotification(Ring r, CharSequence title, CharSequence content) {
				// TODO Auto-generated method stub
				long when = System.currentTimeMillis();
				int icon = R.drawable.icon;
				Notification notify = new Notification(icon,title,when);
				
				
				
				
				
				if (content == null)
					return;
				
				
				Intent msgIntent = new Intent(getApplicationContext(), MsgList.class);
				msgIntent.putExtra("ring", r.getFullText());
				PendingIntent pendingMsgIntent = PendingIntent.getActivity(getApplicationContext(), 0, msgIntent, 0);
				
				notify.setLatestEventInfo(getApplicationContext(), title, content, pendingMsgIntent);
				mNM.notify((Integer) notifyIds.get(r.getFullText()), notify);
			}
			
		}.start(); 
		
	}
	
	
	private void getLastMessageAll() {
		RingStore rs = new RingStore(getApplicationContext(), true);
		for (final Ring r : rs) {
			new Thread() {
				public void run() {
					Integer lastMessage = r.getMsgIndex();
					r.updateLastMessage(lastMessage);
				}
			}.start();
			
			Log.v("AftffService", "Downloaded messages index for " + r.getShortname());
		}
	}
	
	private void downloadMessagesAll() {
		RingStore store = new RingStore(getApplicationContext(),true);
		
		for (Ring r : store) {
			
			Integer lastIndex = r.getLastMessage();
			if (lastIndex == null || lastIndex == 0) {
				Log.v("AftffService", "lastIndex is null or 0");
				continue;
			}
			
			Log.v("AftffService", "lastIndex is " + lastIndex);
			MSG: for (Integer i=lastIndex; i>lastIndex - numMsgDownload; i--) {
				if (i == 0) 
					break MSG;
				
				try {
					r.getMsg(i.toString());
					Log.v("AftffService", "(downloadMessages) Downloaded msg " + i.toString());
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
		
	private final IMessageService.Stub binder = new IMessageService.Stub() {
		public void updateLastMessage() {
			if (isWorking)
				return;
			
			isWorking = true;
			getLastMessageAll();
			isWorking = false;
		}
		
		public void downloadMessages() {
			if (isWorking)
				return;
			isWorking = true;
			downloadMessagesAll();
			isWorking = false;
		}
		
		public boolean isWorking() {
			return isWorking;
		}

		
		
	};
	
	public IBinder onBind(Intent intent) {
		Log.v("NewMessageService","onBind called.");
		return binder;
	}

		
	
	private void stopservice() {

	}

	
	
}
