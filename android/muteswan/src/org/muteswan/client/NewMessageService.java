package org.muteswan.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import org.muteswan.client.data.MuteswanMessage;
import org.muteswan.client.data.Ring;
import org.muteswan.client.data.RingStore;
import org.muteswan.client.ui.LatestMessages;
import org.muteswan.client.ui.MsgList;
import org.apache.http.client.ClientProtocolException;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
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
	HashMap<String,Integer> notifyIds;
	int notifyIdLast;
	final int PERSISTANT_NOTIFICATION = 220;
	private boolean backgroundMessageCheck;
	private int checkMsgInterval;
	private int numMsgDownload;
	private SharedPreferences defPrefs;
	private boolean justLaunched = false;
	protected boolean isWorking;
	private HashMap<Ring,Thread> pollList = new HashMap<Ring,Thread>();
	private boolean started = false;
    
	
	@Override
	public void onStart(Intent intent, int startId) {
		Log.v("MuteswanService", "onStart called.");
		start();
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		Log.v("MuteswanService", "onStartCommand called.");
        //return START_STICKY;START_STICKY_COMPATIBILITY
		start();
		return 1;
    }

	
	
	public void onCreate() {
		super.onCreate();
		
		defPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		
		checkMsgInterval = Integer.parseInt(defPrefs.getString("checkMsgInterval", "5"));
		
		int checkMsgIntervalMs = checkMsgInterval * 60 * 1000;
		
		AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarm.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime()+checkMsgInterval*60,checkMsgIntervalMs,NewMessageReceiver.getPendingIntent(this));
		
		
		
		notificationIntent = new Intent(this, muteswan.class);
	    contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notifyIds = new HashMap<String,Integer>();
		notifyIdLast = 0;

		
		backgroundMessageCheck = defPrefs.getBoolean("backgroundMessageCheck", false);				
		numMsgDownload = Integer.parseInt(defPrefs.getString("numMsgDownload","5"));
	
		
		

		
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
		
		backgroundMessageCheck = defPrefs.getBoolean("backgroundMessageCheck", false);				
		numMsgDownload = Integer.parseInt(defPrefs.getString("numMsgDownload","5"));
	
		
		TorStatus torStatus = new TorStatus(muteswan.torService);
		if (torStatus.checkStatus() == false)
			return;
		
		// Register the available rings and then poll
		if (started  == false) {
		
		   pollList.clear();
		   Log.v("MuteswanService", "Starting up, we are: " + Thread.currentThread().getId());
		   RingStore rs = new RingStore(getApplicationContext(),true);
		   for (Ring r : rs) {
			  Log.v("MuteswanService", "Ring " + r.getShortname() + " registered.");
			  registerLongpoll(r);
		  }
		  runLongpoll();
		  started = true;
		  
		// Run again
		} else {
			Log.v("MuteswanService", "Start flag is true, running runLongpoll.");
			
			// FIXME UGLY. make sure the ring list is up to date 
			RingStore rs = new RingStore(getApplicationContext(),true);
			for (Ring r : rs) {
			
			 boolean has = false;
			 for (Ring pollr : pollList.keySet()) {
			   if (pollr.getFullText().equals(r.getFullText())) {
				   has = true;
			   }
			   
			   
			 }
			 if (!has)
		      registerLongpoll(r);
			 
			 
			}
			runLongpoll();
		}
	}

	
	private void showNotification(Ring r, CharSequence title, CharSequence content) {
		long when = System.currentTimeMillis();
		int icon = R.drawable.icon;
		Notification notify = new Notification(icon,title,when);
		
		
		
		
		
		if (content == null)
			return;
		
		
		Intent msgIntent = new Intent(getApplicationContext(), LatestMessages.class);
		msgIntent.putExtra("ring", muteswan.genHexHash(r.getFullText()));
		PendingIntent pendingMsgIntent = PendingIntent.getActivity(getApplicationContext(), 0, msgIntent, 0);
		
		notify.setLatestEventInfo(getApplicationContext(), title, content, pendingMsgIntent);
		mNM.notify((Integer) notifyIds.get(r.getFullText()), notify);
	}
	
	
	private void registerLongpoll(Ring ring) {
		if (pollList.containsKey(ring))
			return;
		pollList.put(ring,null);
	}

	
	private void runLongpoll() {
		
		
		 Log.v("MuteswanService","pollList size " + pollList.size());
		 for (final Ring ring : pollList.keySet()) {
			 
			 
			    //FIXME: UGLY
			 	RingStore rs = new RingStore(getApplicationContext(),true);
			 	boolean hasRing = false;
			 	for (Ring r : rs) {
			 		if (ring.getFullText().equals(r.getFullText())) {
			 			hasRing = true;
			 		}
			 	}
			 	
			 	if (hasRing == false) {
			 		Log.v("NewMessageService", "We don't have " + ring.getShortname() + " anymore, stopping thread.");
			 		stopList.add(ring);
			 		pollList.get(ring).interrupt();
			 	}
			 	
			 
			    Log.v("MuteswanService", "Starting poll of " + ring.getShortname());
				notifyIds.put(ring.getFullText(), notifyIdLast++);
				
			 if (pollList.get(ring) == null) {
				
				final Integer startLastId = ring.getLastMsgId();
				
				
				
			    Thread nThread = new Thread() {
			    	
			    // Some explanation needed here unfortunately. This is rather complex
			    // and annoying so here is a general outline of this algorithm:
			    // 1. Get the last message id and start polling on
			    //    on the next id (id++)
			    // 2. Continue to longpoll and update last id as messages come in
			    // 3. After 4 successful polls, get a message index. If we are
			    //    not keeping up the messages (lastId != new message index)
			    //    then call downloadMessages and grab the latest as needed.
			    // 4. Continue polling
				 public void run() {
				    	Log.v("MuteswanService","THREAD RUNNING: " + ring.getShortname());

				    	
				    	Integer lastId;
				    	//if (startLastId == null || startLastId == 0) {
							//lastId = ring.getMsgIndex() - numMsgDownload;
				    		lastId = ring.getLastTorMessageId();
						//	if (lastId <= 0)
						//		lastId = 1;
							ring.updateLastMessage(lastId);
						    //ring.saveLastMessage();
						//} else {
						//	lastId = startLastId;
						//}
				    	
					 Log.v("MuteswanService", "Polling for " + ring.getShortname() + " at thread " + Thread.currentThread().getId());
			       
					
			        int count = 0;
			        if (lastId == null) {
			        	Log.v("MuteswanService", "lastId is null");
			        } else {
			        	Log.v("MuteswanService", ring.getShortname() + " has lastId " + lastId);
			        }

			        while (true) {
			        	
			        	MuteswanMessage msg = longpollForNewMessage(ring,++lastId);
						if (msg == null) {
							Log.v("MuteswanService", "Null msg, continuing.");
							--lastId;
						    continue;
						}
						
			        	for (Ring r : stopList) {
			        		if (r.getFullText().equals(ring.getFullText())) {
			        			stopList.remove(r);
			        			Log.v("MuteswanService", "We are on the stop list, bailing out.");
			        			return;
			        		}
			        	}
			        	
			        	ring.updateLastMessage(lastId);
			        	ring.saveLastMessage();
			        	CharSequence notifTitle = "New message in " + ring.getShortname();
			        	CharSequence notifText = "";
						try {
							notifText = ring.getMsg(lastId.toString()).getMsg();
						} catch (ClientProtocolException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
			        	showNotification(ring,notifTitle,notifText);
			        	count++;
			        	
			        	if (count == 4) {
			        		Log.v("MuteswanService", "Loop count of 4 reached for " + ring.getShortname());
			        		count = 0;
			        		int nLastId = ring.getLastTorMessageId();
			        		if (lastId != nLastId) {
			        			Log.v("MuteswanService", "Running downloadMessages() for ring " + ring.getShortname());
			        			ring.updateLastMessage(nLastId);
			        			ring.saveLastMessage();
			        			downloadMessages(ring);
			        			lastId = nLastId;
			        		}
			        	}
			        	msg = null;
			        }
			        
				 }
				};
				
				pollList.put(ring, nThread);
				nThread.start();
			} else if (pollList.get(ring).isInterrupted()) {
				Log.v("MuteswanService","Service is interrupted.");
				//pollList.remove(ring);
			} else if (!(pollList.get(ring).isAlive())) {
				 Log.v("MuteswanService","Hey, looks like not alive, starting.");
				 pollList.get(ring).run();
			} else {
				 Log.v("MuteswanService", "Ring " + ring.getShortname() + " skipped because already polling.");
			}
		  }
		
	
	}
	
	
	private MuteswanMessage longpollForNewMessage(final Ring ring, Integer id) {
		if (ring == null) {
			Log.v("AtffService", "WTF, ring is null.");
		}
		Log.v("MuteswanService","Longpoll for " + ring.getShortname());
		MuteswanMessage msg = ring.getMsgLongpoll(id);
		return(msg);
	}

	
	private void getLastMessageAll() {
		final RingStore rs = new RingStore(getApplicationContext(), true);

		new Thread() {
			public void run() {
				for (final Ring r : rs) {
			
					Integer lastMessage = r.getLastTorMessageId();
					r.updateLastMessage(lastMessage);
			
				Log.v("MuteswanService", "Downloaded messages index for " + r.getShortname());
				}
				isWorking = false;
		 }
		}.start();
	}
	
	private void downloadMessages(Ring ring) {
		Integer lastIndex = ring.getLastMsgId();
		if (lastIndex == null || lastIndex == 0) {
			Log.v("MuteswanService", "lastIndex is null or 0");
			return;
		}
		
		Log.v("MuteswanService", "lastIndex is " + lastIndex);
		MSG: for (Integer i=lastIndex; i>lastIndex - numMsgDownload; i--) {
			if (i == 0)
				break MSG;
			
			try {
				ring.getMsg(i.toString());
				Log.v("MuteswanService", "(downloadMessages) Downloaded msg " + i.toString());
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	private void downloadMessagesAll() {
		final RingStore store = new RingStore(getApplicationContext(),true);
		
			
		Thread nthread = new Thread() {
			public void run() {
				for (final Ring r : store) {
					downloadMessages(r);
				}
			}
		};
		
		nthread.start();
	}
		
	private final IMessageService.Stub binder = new IMessageService.Stub() {
		public void updateLastMessage() {
			if (isWorking)
				return;
			
			isWorking = true;
			getLastMessageAll();
		}
		
		public void downloadMessages() {
			if (isWorking)
				return;
			isWorking = true;
			downloadMessagesAll();
		}
		
		
		public boolean isWorking() {
			return isWorking;
		}

		public void longPoll() {
			Log.v("MuteswanService", "Longpoll() called.");
			
			
			runLongpoll();
			
			isWorking = false;
		}
		
		
	};
	final private LinkedList<Ring> stopList = new LinkedList<Ring>();
	
	public IBinder onBind(Intent intent) {
		Log.v("NewMessageService","onBind called.");
		return binder;
	}

		
	
	private void stopservice() {
		for (Ring r : pollList.keySet()) {
			stopList.add(r);
			pollList.get(r).interrupt();
		}
	}

	
	
}
