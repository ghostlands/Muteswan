/*
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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.json.JSONException;
import org.muteswan.client.data.MuteswanMessage;
import org.muteswan.client.data.Circle;
import org.muteswan.client.data.CircleStore;
import org.muteswan.client.ui.CircleList;
import org.muteswan.client.ui.CreateCircle;
import org.muteswan.client.ui.LatestMessages;

import org.apache.http.client.ClientProtocolException;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
	int notifyId;
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
	
	protected LinkedBlockingQueue linkedQueue = new LinkedBlockingQueue<Circle>();
	
	// long poll is experimental and currently destroys batteries. We should investigate this at another time
	protected boolean useLongPoll = false;
	private CircleStore circleStore;
    
	
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
		
		//int checkMsgIntervalMs = checkMsgInterval * 60 * 1000;
		
		//AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		//alarm.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime()+checkMsgInterval*60,checkMsgIntervalMs,NewMessageReceiver.getPendingIntent(this));
		
		
		notificationIntent = new Intent(this, muteswan.class);
	    contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notifyIds = new HashMap<String,Integer>();
		notifyId = 0;

		
		backgroundMessageCheck = defPrefs.getBoolean("backgroundMessageCheck", false);				
		numMsgDownload = Integer.parseInt(defPrefs.getString("numMsgDownload","5"));
	
		justLaunched = true;
		
		if (isUserCheckingMessagesReceiver == null) isUserCheckingMessagesReceiver = new IsUserCheckingMessagesReceiver();
		registerReceiver(isUserCheckingMessagesReceiver, new IntentFilter(LatestMessages.CHECKING_MESSAGES));
		registerReceiver(deletedCircleReceiver, new IntentFilter(CircleList.DELETED_CIRCLE_BROADCAST));
		registerReceiver(joinedCircleReceiver, new IntentFilter(CircleList.JOINED_CIRCLE_BROADCAST));
		registerReceiver(createdCircleReceiver, new IntentFilter(CreateCircle.CREATED_CIRCLE_BROADCAST));
		
		init();
	}
	
	@Override
	public void onDestroy() {
		stopservice();
		//mNM.cancel(PERSISTANT_NOTIFICATION);
		//AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		//alarm.cancel(NewMessageReceiver.getPendingIntent(this));
		unregisterReceiver(isUserCheckingMessagesReceiver);
		unregisterReceiver(deletedCircleReceiver);
		unregisterReceiver(joinedCircleReceiver);
		unregisterReceiver(createdCircleReceiver);
	}
	
	private void init() {
		 pollList.clear();
		   
		 Log.v("MuteswanService", "Service initialized, we are: " + Thread.currentThread().getId());
		 circleStore = new CircleStore(getApplicationContext(),true,false);
		 for (Circle r : circleStore) {
				  Log.v("MuteswanService", "Circle " + r.getShortname() + " registered.");
				  registerPoll(r);
		 }
		 
	}
	
	private void start() {
		
		backgroundMessageCheck = defPrefs.getBoolean("backgroundMessageCheck", false);				
		numMsgDownload = Integer.parseInt(defPrefs.getString("numMsgDownload","5"));
	
		// tor service is now permissioned
		//TorStatus torStatus = new TorStatus(muteswan.torService);
		//if (torStatus.checkStatus() == false)
		//	return;
		
		
		 // get a list of running processes and iterate through them
	  /*   ActivityManager am = (ActivityManager) this
			                .getSystemService(ACTIVITY_SERVICE);
			 
		// get the info from the currently running task
		List<RunningTaskInfo> taskInfo = am.getRunningTasks(1);	 
		Log.d("current task :", "CURRENT Activity ::"
			                + taskInfo.get(0).topActivity.getClassName());
		if (taskInfo.get(0).topActivity.getClassName().contains("org.muteswan"))
			return; */
			
			
			
		
		
		
		// Startup
		if (started  == false) {
		
		   Log.v("MuteswanService", "Start flag is false, exiting.");
		  
		  
		  //runLongpoll();
		  started = true;
		  runPoll();
		  
		// Run again
		} else {
			
			
			
			// FIXME UGLY. make sure the circle list is up to date 
			/*CircleStore rs = new CircleStore(getApplicationContext(),true);
			for (Circle r : rs) {
			
			 boolean has = false;
			 for (Circle pollr : pollList.keySet()) {
			   if (pollr.getFullText().equals(r.getFullText())) {
				   has = true;
			   }
			   
			   
			 }
			 if (!has)
		      registerPoll(r);
			 
			 
			}*/
			runPoll();
		}
	}

	
	private void showNotification(Circle c, CharSequence title, CharSequence content) {
		long when = System.currentTimeMillis();
		int icon = R.drawable.icon;
		Notification notify = new Notification(icon,title,when);
		
		
		notify.flags |= Notification.FLAG_AUTO_CANCEL;
		notify.flags |= Notification.FLAG_SHOW_LIGHTS;
		notify.defaults |= Notification.DEFAULT_SOUND;
	
		if (content == null)
			return;
		
		Intent msgIntent = new Intent(getApplicationContext(), LatestMessages.class);
		msgIntent.putExtra("circle", muteswan.genHexHash(c.getFullText()));
		PendingIntent pendingMsgIntent = PendingIntent.getActivity(getApplicationContext(), 0, msgIntent, PendingIntent.FLAG_UPDATE_CURRENT);
	
		//PendingIntent.get
	
		Log.v("NewMessageService", "Set pending intent to launch " + c.getShortname() + "(" + muteswan.genHexHash(c.getFullText()) + ")");
		notify.setLatestEventInfo(getApplicationContext(), title, content, pendingMsgIntent);
		mNM.notify((Integer) notifyIds.get(c.getFullText()), notify);
	}
	
	
	private void registerPoll(Circle circle) {
		if (pollList.containsKey(circle))
			return;
		pollList.put(circle,null);
	}

	
	private void runPoll() {
		
		 isWorking = true;
		 notifyIds = new HashMap<String,Integer>();
		 //notifyId = 0;
		
		 Log.v("MuteswanService","pollList size " + pollList.size());
		 for (final Circle circle : pollList.keySet()) {
			 
			 Thread oldThread = pollList.get(circle);
			 while (oldThread != null) {
			        try {
			            oldThread.join();
			            oldThread = null;
			            pollList.put(circle, null);
			        } catch (InterruptedException e) {
			        }
			    }

			
			    //FIXME: UGLY
			 	/*CircleStore rs = new CircleStore(getApplicationContext(),true);
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
			 	*/
			 
		     Log.v("MuteswanService", "Starting poll of " + circle.getShortname());
			
			
			 if (useLongPoll == false) {
				 //if (pollList.get(circle) == null) {
				 Thread nThread = new Thread() {
				    	
					   
					 public void run() {
					    	Log.v("MuteswanService","THREAD RUNNING: " + circle.getShortname());

					    		boolean poll = true;
					    		final Integer startLastId = circle.getLastMsgId(false);
								Integer lastId = circle.getLastTorMessageId();
					    		if (lastId == null || lastId < 0) {
					    			Log.v("MuteswanService", "Got null or negative from tor, bailing out.");
					    			poll = false;
					    			torActive = false;
					    			//return;
					    		}
					    		
					    		if (lastId > startLastId)
								  circle.updateLastMessage(lastId,false);
							  
					    	
						 Log.v("MuteswanService", "Polling for " + circle.getShortname() + " at thread " + Thread.currentThread().getId());
				       
						
				        Log.v("MuteswanService", circle.getShortname() + " has lastId " + lastId);
				        

				        
				        // FIXME: REFACTOR
				    	  
				    	 Log.v("NewMessageService", "Got last id of " + startLastId);
				    	 if (startLastId < lastId) {
				      
				    	   Log.v("NewMessageService", "Not using long poll, starting check for " + circle.getShortname());
				    	   
				    	   for (Integer i = lastId; i > startLastId; i--) {
				    		 Log.v("NewMessageService", "Downloading " + i +  " for " + circle.getShortname());
				    		 try {
								MuteswanMessage msg = circle.getMsgFromTor(i);
								if (msg != null && msg.signatures[0] != null) {
									circle.saveMsgToDb(i, msg.getDate(), msg.getMsg(),
											msg.signatures);
								} else if (msg != null) {
									circle.saveMsgToDb(i, msg.getDate(), msg.getMsg());
								}
								
								if (!notifyIds.containsKey(circle.getFullText())) {
									notifyIds.put(circle.getFullText(), notifyId);
									notifyId++;
								}
					        	CharSequence notifTitle = "New message in " + circle.getShortname();
					        	CharSequence notifText = msg.getMsg();
					        	showNotification(circle,notifTitle,notifText);
								
							  } catch (ClientProtocolException e) {
								e.printStackTrace();
							  } catch (IOException e) {
								e.printStackTrace();
							  }
				    	  }
				    	}
				    	circle.closedb();
				    	
				      }
					};
					pollList.put(circle, nThread);
					nThread.start();
					
				 //} else {
				 //	pollList.get(circle).start();
				 //}
			 } else  {
				 if (pollList.get(circle) == null) {
					 
					 Thread nThread = new Thread() {
					    	
						   
						 public void run() {
						    	Log.v("MuteswanService","THREAD RUNNING: " + circle.getShortname());

						    		boolean poll = true;
						    		final Integer startLastId = circle.getLastMsgId(true);
									Integer lastId = circle.getLastTorMessageId();
						    		if (lastId == null || lastId < 0) {
						    			Log.v("MuteswanService", "Got null or negative from tor, bailing out.");
						    			poll = false;
						    			torActive = false;
						    			//return;
						    		}
									circle.updateLastMessage(lastId,false);
								  
						    	
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
					        	
					        	circle.updateLastMessage(lastId,true);
					        	// updateLastMessage also saves the message
					        	//circle.saveLastMessage();
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
					        	
					        	msg = null;
					        }
						 }
					 }; 
				 } else if (pollList.get(circle).isInterrupted()) {
						Log.v("MuteswanService","Service is interrupted.");
						//pollList.remove(circle);
				} else if (!(pollList.get(circle).isAlive())) {
						 Log.v("MuteswanService","Hey, looks like not alive, starting.");
						 try {
							pollList.get(circle).join();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						 pollList.get(circle).run();
				} else {
						 Log.v("MuteswanService", "Circle " + circle.getShortname() + " skipped because already polling.");
				}
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

	
		
	private final IMessageService.Stub binder = new IMessageService.Stub() {

		public void refreshLatest() {
			Log.v("MuteswanService", "runPoll() called.");
			
			runPoll();
			
			isWorking = false;
		}
		
		
		public void checkTorStatus(ITorVerifyResult verifyResult) {
			TorStatus checkTorStatus = new TorStatus();
			if (checkTorStatus.checkStatus()) {
				
				sendBroadcast(new Intent(muteswan.TOR_AVAILABLE));
			} else {
				sendBroadcast(new Intent(muteswan.TOR_NOT_AVAILABLE));
			}
		}

		@Override
		public boolean isUserCheckingMessages() throws RemoteException {
			Log.v("NewMessageService", "isUserCheckingMessages " + isUserCheckingMessages);
	    	if (isUserCheckingMessages == true) {
	    		isUserCheckingMessages = false;
	    		return(true);
	    	} else {
	    		return(false);
	    	}
		}
		
		public void setUserChecking(boolean checkValue) {
			Log.v("NewMessageService", "setUserChecking " + checkValue);
			isUserCheckingMessages = checkValue;
		}


		@Override
		public int getLastTorMsgId(String circleHash) throws RemoteException {
			Integer lastId = circleStore.asHashMap().get(circleHash).getLastTorMessageId();
			return(lastId);
		}
		

		@Override
		public int downloadMsgFromTor(String circleHash, int id) throws RemoteException {
			Circle circle = circleStore.asHashMap().get(circleHash);
			MuteswanMessage msg;
		
			if (!linkedQueue.contains(circle)) {
			  linkedQueue.add(circle);
			} else {
				Log.v("NewMessageSservice", "Two downloads at once to " + circle.getShortname());
				while (linkedQueue.contains(circle)) {
					try {
						Thread.currentThread().sleep(250);
					} catch (InterruptedException e) {
						return(-4);
					}
				}
				linkedQueue.add(circle);
			}
			
			
			try {
				msg = circle.getMsgFromTor(id);
				Log.v("NewMessageService", "I am " + Thread.currentThread());
				if (msg != null && msg.signatures[0] != null) {
					circle.saveMsgToDb(id, msg.getDate(), msg.getMsg(),
							msg.signatures);
					linkedQueue.remove(circle);
					return(0);
				} else if (msg != null) {
					circle.saveMsgToDb(id, msg.getDate(), msg.getMsg());
					linkedQueue.remove(circle);
					return(0);
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				linkedQueue.remove(circle);
				return -1;
			} catch (IOException e) {
				e.printStackTrace();
				linkedQueue.remove(circle);
				return -2;
			}
			
			linkedQueue.remove(circle);
			return -3;
		}


		@Override
		public void updateLastMessage(String circleHash, int lastMsg) throws RemoteException {
			circleStore.asHashMap().get(circleHash).updateLastMessage(lastMsg, true);
		}


		@Override
		public int postMsg(String circleHash, String msgContent) throws RemoteException {
			try {
				return(circleStore.asHashMap().get(circleHash).postMsg(msgContent));
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return(-1);
		}
		
		
		
		
		
	};
	final private LinkedList<Circle> stopList = new LinkedList<Circle>();
	private boolean isUserCheckingMessages = true;
	
	

	private IsUserCheckingMessagesReceiver isUserCheckingMessagesReceiver = new IsUserCheckingMessagesReceiver();
	private DeletedCircleReceiver deletedCircleReceiver = new DeletedCircleReceiver();
	private JoinedCircleReceiver joinedCircleReceiver = new JoinedCircleReceiver();
	private CreatedCircleReceiver createdCircleReceiver = new CreatedCircleReceiver();
	
	public IBinder onBind(Intent intent) {
		Log.v("NewMessageService","onBind called.");
		return binder;
	}

		
	
	private void stopservice() {
		for (Circle r : pollList.keySet()) {
			stopList.add(r);
			if (pollList.get(r) != null)
			  pollList.get(r).interrupt();
		}
	}

	
	private class IsUserCheckingMessagesReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	Log.v("NewMessageService", "Got alarm to not check!");
	    	isUserCheckingMessages = true;
	    }
	}
	
	private class DeletedCircleReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	Log.v("NewMessageService", "Got deleted circle receiver!");
	    	init();
	    }
	}
	
	private class JoinedCircleReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	Log.v("NewMessageService", "Got joined circle receiver!");
	    	init();
	    }
	}
	
	private class CreatedCircleReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	Log.v("NewMessageService", "Got created circle receiver!");
	    	init();
	    }
	}
	

	

	

}
