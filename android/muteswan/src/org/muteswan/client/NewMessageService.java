package org.muteswan.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import org.muteswan.client.data.MuteswanMessage;
import org.muteswan.client.data.Circle;
import org.muteswan.client.data.CircleStore;
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
	private HashMap<Circle,Thread> pollList = new HashMap<Circle,Thread>();
	private boolean started = false;
	protected boolean torActive = false;
    
	
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
	
		// tor service is now permissioned
		//TorStatus torStatus = new TorStatus(muteswan.torService);
		//if (torStatus.checkStatus() == false)
		//	return;
		
		// Register the available circles and then poll
		if (started  == false) {
		
		   pollList.clear();
		   Log.v("MuteswanService", "Starting up, we are: " + Thread.currentThread().getId());
		   CircleStore rs = new CircleStore(getApplicationContext(),true);
		   for (Circle r : rs) {
			  Log.v("MuteswanService", "Circle " + r.getShortname() + " registered.");
			  registerLongpoll(r);
		  }
		  runLongpoll();
		  started = true;
		  
		// Run again
		} else {
			Log.v("MuteswanService", "Start flag is true, running runLongpoll.");
			
			// FIXME UGLY. make sure the circle list is up to date 
			CircleStore rs = new CircleStore(getApplicationContext(),true);
			for (Circle r : rs) {
			
			 boolean has = false;
			 for (Circle pollr : pollList.keySet()) {
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

	
	private void showNotification(Circle r, CharSequence title, CharSequence content) {
		long when = System.currentTimeMillis();
		int icon = R.drawable.icon;
		Notification notify = new Notification(icon,title,when);
		
		
		
		
		
		if (content == null)
			return;
		
		
		Intent msgIntent = new Intent(getApplicationContext(), LatestMessages.class);
		msgIntent.putExtra("circle", muteswan.genHexHash(r.getFullText()));
		PendingIntent pendingMsgIntent = PendingIntent.getActivity(getApplicationContext(), 0, msgIntent, 0);
		
		notify.setLatestEventInfo(getApplicationContext(), title, content, pendingMsgIntent);
		mNM.notify((Integer) notifyIds.get(r.getFullText()), notify);
	}
	
	
	private void registerLongpoll(Circle circle) {
		if (pollList.containsKey(circle))
			return;
		pollList.put(circle,null);
	}

	
	private void runLongpoll() {
		
		 isWorking = true;
		
		 Log.v("MuteswanService","pollList size " + pollList.size());
		 for (final Circle circle : pollList.keySet()) {
			 
			 
			    //FIXME: UGLY
			 	CircleStore rs = new CircleStore(getApplicationContext(),true);
			 	boolean hasCircle = false;
			 	for (Circle r : rs) {
			 		if (circle.getFullText().equals(r.getFullText())) {
			 			hasCircle = true;
			 		}
			 	}
			 	
			 	if (hasCircle == false) {
			 		Log.v("NewMessageService", "We don't have " + circle.getShortname() + " anymore, stopping thread.");
			 		stopList.add(circle);
			 		pollList.get(circle).interrupt();
			 	}
			 	
			 
			    Log.v("MuteswanService", "Starting poll of " + circle.getShortname());
				notifyIds.put(circle.getFullText(), notifyIdLast++);
				
			 if (pollList.get(circle) == null) {
				
				final Integer startLastId = circle.getLastMsgId();
				
				
				
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
				    	Log.v("MuteswanService","THREAD RUNNING: " + circle.getShortname());

				    		boolean poll = true;
				    	
				    		Integer lastId = circle.getLastTorMessageId();
				    		if (lastId == null || lastId == 0) {
				    			Log.v("MuteswanService", "Got null or 0 from Tor, bailing out.");
				    			poll = false;
				    			torActive = false;
				    			//return;
				    		}
							circle.updateLastMessage(lastId);
						  
				    	
					 Log.v("MuteswanService", "Polling for " + circle.getShortname() + " at thread " + Thread.currentThread().getId());
			       
					
			        int count = 0;
			        Log.v("MuteswanService", circle.getShortname() + " has lastId " + lastId);
			        

			        while (poll) {
			        	torActive = true;
			        	
			        	
			        	MuteswanMessage msg = null;
						try {
							msg = longpollForNewMessage(circle,++lastId);
							
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							//e1.printStackTrace();
							Log.v("MuteswanService", "IO exception connecting to tor.");
							poll = false;
						}
						
						if (msg == null) {
							Log.v("MuteswanService", "Null msg, continuing.");
							--lastId;
						    continue;
						}
						
					
			        	for (Circle r : stopList) {
			        		if (r.getFullText().equals(circle.getFullText())) {
			        			stopList.remove(r);
			        			Log.v("MuteswanService", "We are on the stop list, bailing out.");
			        			return;
			        		}
			        	}
			        	
			        	circle.updateLastMessage(lastId);
			        	circle.saveLastMessage();
			        	CharSequence notifTitle = "New message in " + circle.getShortname();
			        	CharSequence notifText = "";
						try {
							notifText = circle.getMsg(lastId.toString()).getMsg();
						} catch (ClientProtocolException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
			        	showNotification(circle,notifTitle,notifText);
			        	count++;
			        	
			        	if (count == 4) {
			        		Log.v("MuteswanService", "Loop count of 4 reached for " + circle.getShortname());
			        		count = 0;
			        		Integer nLastId = circle.getLastTorMessageId();
			        		
			        		
			        		if (lastId < nLastId) {
			        			Log.v("MuteswanService", "Running downloadMessages() for circle " + circle.getShortname());
			        			circle.updateLastMessage(nLastId);
			        			circle.saveLastMessage();
			        			downloadMessages(circle);
			        			lastId = nLastId;
			        		}
			        	}
			        	msg = null;
			        }
			        
				 }
				};
				
				pollList.put(circle, nThread);
				nThread.start();
			} else if (pollList.get(circle).isInterrupted()) {
				Log.v("MuteswanService","Service is interrupted.");
				//pollList.remove(circle);
			} else if (!(pollList.get(circle).isAlive())) {
				 Log.v("MuteswanService","Hey, looks like not alive, starting.");
				 pollList.get(circle).run();
			} else {
				 Log.v("MuteswanService", "Circle " + circle.getShortname() + " skipped because already polling.");
			}
		  }
		
	
	}
	
	
	private MuteswanMessage longpollForNewMessage(final Circle circle, Integer id) throws IOException {
		if (circle == null) {
			Log.v("AtffService", "WTF, circle is null.");
		}
		Log.v("MuteswanService","Longpoll for " + circle.getShortname());
		MuteswanMessage msg = circle.getMsgLongpoll(id);
		return(msg);
	}

	
	private void getLastMessageAll() {
		final CircleStore rs = new CircleStore(getApplicationContext(), true);

		new Thread() {
			public void run() {
				for (final Circle r : rs) {
			
					Integer lastMessage = r.getLastTorMessageId();
					r.updateLastMessage(lastMessage);
			
				Log.v("MuteswanService", "Downloaded messages index for " + r.getShortname());
				}
				isWorking = false;
		 }
		}.start();
	}
	
	private void downloadMessages(Circle circle) {
		Integer lastIndex = circle.getLastMsgId();
		if (lastIndex == null || lastIndex == 0) {
			Log.v("MuteswanService", "lastIndex is null or 0");
			return;
		}
		
		Log.v("MuteswanService", "lastIndex is " + lastIndex);
		MSG: for (Integer i=lastIndex; i>lastIndex - numMsgDownload; i--) {
			if (i == 0)
				break MSG;
			
			try {
				circle.getMsg(i.toString());
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
		final CircleStore store = new CircleStore(getApplicationContext(),true);
		
			
		Thread nthread = new Thread() {
			public void run() {
				for (final Circle r : store) {
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
		
		public boolean torOnline() {
			return torActive ;
		}
		
	};
	final private LinkedList<Circle> stopList = new LinkedList<Circle>();
	
	public IBinder onBind(Intent intent) {
		Log.v("NewMessageService","onBind called.");
		return binder;
	}

		
	
	private void stopservice() {
		for (Circle r : pollList.keySet()) {
			stopList.add(r);
			pollList.get(r).interrupt();
		}
	}

	
	
}
