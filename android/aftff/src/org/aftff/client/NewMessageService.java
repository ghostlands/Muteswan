package org.aftff.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import org.aftff.client.data.AftffMessage;
import org.aftff.client.data.Ring;
import org.aftff.client.data.RingStore;
import org.aftff.client.ui.LatestMessages;
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
		Log.v("AftffService", "onStart called.");
		start();
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		Log.v("AftffService", "onStartCommand called.");
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
		alarm.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime()+checkMsgInterval*60,checkMsgIntervalMs,NewMessageReceiver.getPendingIntent(this));
		
		
		
		notificationIntent = new Intent(this, aftff.class);
	    contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notifyIds = new HashMap<String,Integer>();
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
	
		//if (justLaunched == true) {
		//	justLaunched = false;
		//	return;
		//}
		
		if (started  == false) {
		
		   pollList.clear();
		   Log.v("AftffService", "Starting up, we are: " + Thread.currentThread().getId());
		   RingStore rs = new RingStore(getApplicationContext(),true);
		   for (Ring r : rs) {
			  Log.v("AftffService", "Ring " + r.getShortname() + " registered.");
			  registerLongpoll(r);
		  }
		  runLongpoll();
		  started = true;
		} else {
			Log.v("AftffService", "Start flag is true, running runLongpoll.");
			runLongpoll();
		}
	}

	public void pollForNewMessages() {
		
		new Thread() {
			
			@Override
			public void run() {
				
				
				if (!backgroundMessageCheck) {
					Log.v("Service", "backgroundMessageCheck is false, not polling.");
					return;
				}
				
				
				
				RingStore store = new RingStore(getApplicationContext(),true);
				for (Ring r : store) {
					
					Integer lastIndex = r.getLastMsgId();
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
		
			
			
		}.start(); 
		
	}
	
	private void showNotification(Ring r, CharSequence title, CharSequence content) {
		// TODO Auto-generated method stub
		long when = System.currentTimeMillis();
		int icon = R.drawable.icon;
		Notification notify = new Notification(icon,title,when);
		
		
		
		
		
		if (content == null)
			return;
		
		
		Intent msgIntent = new Intent(getApplicationContext(), LatestMessages.class);
		msgIntent.putExtra("ring", r.getFullText());
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
		
		
		 Log.v("AftffService","pollList size " + pollList.size());
		 for (final Ring ring : pollList.keySet()) {
			    Log.v("AftffService", "Starting poll of " + ring.getShortname());
				notifyIds.put(ring.getFullText(), notifyIdLast++);
				
			 if (pollList.get(ring) == null) {
				
				final Integer startLastId = ring.getLastMsgId();
				
				
				
			    Thread nThread = new Thread() {
			    	
			    // Some explanation needed here unfortunately. This is rather complex
			    // and annoying so here is a general outline of this algorithm:
			    // 1. Get the last message id from tor and start polling on
			    //    on the next id (id++)
			    // 2. Continue to longpoll and update last id as messages come in
			    // 3. After 4 successful polls, get a message index. If we are
			    //    not keeping up the messages (lastId != new message index)
			    //    then call downloadMessages and grab the latest as needed.
			    // 4. Continue polling
				 public void run() {
				    	Log.v("AftffService","THREAD RUNNING: " + ring.getShortname());

				    	
				    	Integer lastId;
				    	if (startLastId == null || startLastId == 0) {
							lastId = ring.getMsgIndex() - numMsgDownload;
							ring.updateLastMessage(lastId);
						    ring.saveLastMessage();
						} else {
							lastId = startLastId;
						}
				    	
					 Log.v("AftffService", "Polling for " + ring.getShortname() + " at thread " + Thread.currentThread().getId());
			        //Integer lastId = ring.getMsgIndex();
			        //ring.updateLastMessage(lastId);
					
			        int count = 0;
			        if (lastId == null) {
			        	Log.v("AftffService", "lastId is null");
			        } else {
			        	Log.v("AftffService", ring.getShortname() + " has lastId " + lastId);
			        }
			        while (longpollForNewMessage(ring,++lastId)) {
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
			        		Log.v("AftffService", "Loop count of 4 reached for " + ring.getShortname());
			        		count = 0;
			        		int nLastId = ring.getMsgIndex();
			        		if (lastId != nLastId) {
			        			Log.v("AftffService", "Running downloadMessages() for ring " + ring.getShortname());
			        			ring.updateLastMessage(nLastId);
			        			ring.saveLastMessage();
			        			downloadMessages(ring);
			        			lastId = nLastId;
			        		}
			        	}
			        	
			        }
			        //ring.updateLastMessage(lastId);
			        //longpollForNewMessage(ring,++lastId);
				 }
				};
				
				pollList.put(ring, nThread);
				nThread.start();
			} else if (!(pollList.get(ring).isAlive())) {
				 Log.v("AftffService","Hey, looks like not alive.");
				 pollList.get(ring).run();
			} else {
				 Log.v("AftffService", "Ring " + ring.getShortname() + " skipped because already polling.");
			}
		  }
		
	
	}
	
	
	private boolean longpollForNewMessage(final Ring ring, Integer id) {
		if (ring == null) {
			Log.v("AtffService", "WTF, ring is null.");
		}
		Log.v("AftffService","Longpoll for " + ring.getShortname());
		AftffMessage msg = ring.getMsgLongpoll(id);
		if (msg != null) {
		  Log.v("AftffService", "Received msg from long poll: " + msg.getMsg());
		  return true;
		} else {
			return false;
		}
	}
	
	private void getLastMessageAll() {
		final RingStore rs = new RingStore(getApplicationContext(), true);

		new Thread() {
			public void run() {
				for (final Ring r : rs) {
			//new Thread() {
				//public void run() {
					Integer lastMessage = r.getMsgIndex();
					r.updateLastMessage(lastMessage);
				//}
			//}.start();
			
				Log.v("AftffService", "Downloaded messages index for " + r.getShortname());
				}
				isWorking = false;
		 }
		}.start();
	}
	
	private void downloadMessages(Ring ring) {
		Integer lastIndex = ring.getLastMsgId();
		if (lastIndex == null || lastIndex == 0) {
			Log.v("AftffService", "lastIndex is null or 0");
			return;
		}
		
		Log.v("AftffService", "lastIndex is " + lastIndex);
		MSG: for (Integer i=lastIndex; i>lastIndex - numMsgDownload; i--) {
			if (i == 0)
				break MSG;
			
			try {
				ring.getMsg(i.toString());
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
		
		public void poll() {
			isWorking = true;
			pollForNewMessages();
		}
		
		public boolean isWorking() {
			return isWorking;
		}

		public void longPoll() {
			Log.v("AftffService", "Longpoll() called.");
			
			//if (isWorking)
			//	return;
			
			
			//isWorking = true;
			//RingStore rs = new RingStore(getApplicationContext(),true);
			//for (Ring r : rs) {
			//	Log.v("AftffService", "Ring " + r.getShortname() + " registered.");
			//	registerLongpoll(r);
			//}
			
		
			//new Thread() {
			//	 public void run() {
					  runLongpoll();
			//	 }
			//}.start();
			isWorking = false;
		}
		
		
	};
	
	public IBinder onBind(Intent intent) {
		Log.v("NewMessageService","onBind called.");
		return binder;
	}

		
	
	private void stopservice() {

	}

	
	
}
