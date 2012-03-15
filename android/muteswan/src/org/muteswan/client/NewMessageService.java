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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.muteswan.client.data.Circle;
import org.muteswan.client.data.CircleStore;
import org.muteswan.client.data.MuteswanMessage;
import org.muteswan.client.ui.CircleList;
import org.muteswan.client.ui.CreateCircle;
import org.muteswan.client.ui.LatestMessages;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

public class NewMessageService extends Service {

    private static final int STATIC_NOTIFYID = 5;
    private static final int MAX_IO_FAIL = 5;
	Intent notificationIntent;
	PendingIntent contentIntent;
	NotificationManager mNM;
	HashMap<String,Integer> notifyIds;
	int notifyId;
	protected boolean isWorking;
	private HashMap<Circle,Thread> pollList = new HashMap<Circle,Thread>();
	private boolean started = false;
	protected boolean torActive = false;
	
	protected LinkedBlockingQueue<Circle> linkedQueue = new LinkedBlockingQueue<Circle>();
	
	// long poll is experimental and currently destroys batteries. We should investigate this at another time (maybe)
	protected boolean useLongPoll = false;
	private CircleStore circleStore;
	private int numMsgDownload = 5;
	private MuteswanHttp muteswanHttp;
    
	
	@Override
	public void onStart(Intent intent, int startId) {
		MuteLog.Log("MuteswanService", "onStart called.");
		this.stopForeground(false);
		start();
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		MuteLog.Log("MuteswanService", "onStartCommand called.");
        //return START_STICKY;START_STICKY_COMPATIBILITY
		start();
		return 1;
    }

	
	
	public void onCreate() {
		super.onCreate();
		
	
		if (isUserCheckingMessagesReceiver == null) isUserCheckingMessagesReceiver = new IsUserCheckingMessagesReceiver();
		registerReceiver(isUserCheckingMessagesReceiver, new IntentFilter(LatestMessages.CHECKING_MESSAGES));
		registerReceiver(deletedCircleReceiver, new IntentFilter(CircleList.DELETED_CIRCLE_BROADCAST));
		registerReceiver(joinedCircleReceiver, new IntentFilter(CircleList.JOINED_CIRCLE_BROADCAST));
		registerReceiver(createdCircleReceiver, new IntentFilter(CreateCircle.CREATED_CIRCLE_BROADCAST));
		registerReceiver(networkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		init(false);
		
	}
	
	@Override
	public void onDestroy() {
		stopservice();
		unregisterReceiver(isUserCheckingMessagesReceiver);
		unregisterReceiver(networkChangeReceiver);
		unregisterReceiver(deletedCircleReceiver);
		unregisterReceiver(joinedCircleReceiver);
		unregisterReceiver(createdCircleReceiver);
	}
	
	
    final Handler handleStopSelf = new Handler() {
    	@Override
    	public void handleMessage(Message msg) {
    		stopservice();
    		stopSelf();
    	}
    };
	private boolean torOK;
    
	private void init(boolean reinit) {
		 pollList.clear();
		 
		notificationIntent = new Intent(this, Main.class);
		contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notifyIds = new HashMap<String,Integer>();
		notifyId = 8392;
		   
		 MuteLog.Log("MuteswanService", "Service initialized, we are: " + Thread.currentThread().getId());
		 if (muteswanHttp == null)
		   muteswanHttp = new MuteswanHttp();

		 if (circleStore == null || reinit) {
		 	circleStore = new CircleStore(getApplicationContext(),true,false,muteswanHttp);
		 	for (Circle r : circleStore) {
				  MuteLog.Log("MuteswanService", "Circle " + r.getShortname() + " registered.");
				  registerPoll(r);
		 	}
		 	reinit = false;
		 }
		 
	}
	
	private void start() {
		
	
		
		
		// Startup
		if (started  == false) {
		  
		  
		  started = true;
		  runPoll();
		  
		} else {
			runPoll();
		}
	}

	
	private void showCircleNotification(Circle c, CharSequence title, CharSequence content) {
		long when = System.currentTimeMillis();
		int icon = R.drawable.icon;
		Notification notify = new Notification(icon,title,when);
	
		if (!notifyIds.containsKey(c.getFullText())) {
			notifyIds.put(c.getFullText(), notifyId);
			notifyId++;
		}
		
		notify.flags |= Notification.FLAG_AUTO_CANCEL;
		notify.flags |= Notification.FLAG_SHOW_LIGHTS;
		notify.defaults |= Notification.DEFAULT_SOUND;
	
		if (content == null)
			return;
		
		Intent msgIntent = new Intent(getApplicationContext(), LatestMessages.class);
		msgIntent.putExtra("circle", Main.genHexHash(c.getFullText()));
		PendingIntent pendingMsgIntent = PendingIntent.getActivity(getApplicationContext(), (int) System.currentTimeMillis(), msgIntent,0);
	
	
		MuteLog.Log("NewMessageService", "Set pending intent to launch " + c.getShortname() + "(" + Main.genHexHash(c.getFullText()) + ")");
		MuteLog.Log("NewMessageService", "Setting notify id of " + notifyId);
		notify.setLatestEventInfo(getApplicationContext(), title, content, pendingMsgIntent);
		mNM.notify((Integer) notifyIds.get(c.getFullText()), notify);
	}
	
	private void showNotification(CharSequence title, CharSequence content) {
		long when = System.currentTimeMillis();
		int icon = R.drawable.sync_error;
		Notification notify = new Notification(icon,title,when);
	
		
		notify.flags |= Notification.FLAG_AUTO_CANCEL;
		notify.flags |= Notification.FLAG_SHOW_LIGHTS;
		//notify.defaults |= Notification.DEFAULT_SOUND;
	
		if (content == null)
			return;
		
		Intent msgIntent = new Intent(getApplicationContext(), Main.class);
		PendingIntent pendingMsgIntent = PendingIntent.getActivity(getApplicationContext(), (int) System.currentTimeMillis(), msgIntent,0);
	
	
		MuteLog.Log("NewMessageService", "Setting notify id of " + STATIC_NOTIFYID);
		notify.setLatestEventInfo(getApplicationContext(), title, content,pendingMsgIntent);
		mNM.notify(STATIC_NOTIFYID, notify);
	}
	
	private Integer getLastTorMsgIdPatiently(Circle circle) {
		int ioFailCount = 0;
		Integer lastId = circle.getLastTorMessageId();
		while (lastId != null && lastId == -2 && ioFailCount <= 5) {
			MuteLog.Log("NewMessageService", "IKF Trying again..." + ioFailCount);
			lastId = circle.getLastTorMessageId();
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ioFailCount++;
		}
		return(lastId);
	}
	
	private void registerPoll(Circle circle) {
		if (pollList.containsKey(circle))
			return;
		pollList.put(circle,null);
	}

	
	private void runPoll() {
		
		 isWorking = true;
		 notifyIds = new HashMap<String,Integer>();
	
		 MuteLog.Log("NewMessageService", "Circlestore: " + circleStore.hashCode());
		 MuteLog.Log("MuteswanService","pollList size " + pollList.size());
		 
		 if (skipNextCheck) {
			 MuteLog.Log("NewMessageService", "skipNextCheck is set, bailing out.");
			 skipNextCheck = false;
			 return;
		 }
		 
		 		 // run a tor check
				 final Thread torCheckThread = new Thread() {
				 	 public void run() {
				 		 torOK = false;
				 		 TorStatus torCheck = new TorStatus(muteswanHttp,getApplicationContext());
				 		 if (torCheck.checkStatus()) {
				 			 torOK = true;
				 		 }
				 	 }
				 };
				 torCheckThread.start();
		 
		 for (final Circle circle : pollList.keySet()) {
			 
		
			 
			Thread oldThread = pollList.get(circle);
			if (oldThread != null) {
 	         try {
				  MuteLog.Log("MuteswanService","Interrupting old thread " + oldThread.toString() + ": " + circle.getShortname());
			      oldThread.interrupt();
			      oldThread.join(5);
			      oldThread = null;
			      pollList.put(circle, null);
			  } catch (InterruptedException e) {
			  }
			}

			 MuteLog.Log("NewMessageService", "Circle: " + circle.hashCode());
			
		     MuteLog.Log("MuteswanService", "Starting poll of " + circle.getShortname());
			
			
				 Thread nThread = new Thread() {
				    	
					   
					 public void run() {
						 // bail out if tor check fails
						 int waitCount = 0;
						 while (!torOK) {
						 	 if (waitCount >= 29) {
						 		 MuteLog.Log("LatestMessages", "Tor seems to down, bailing out.");
						 		 return;
						 	 }
						 	 Thread.currentThread();
						 	try {
						 		Thread.sleep(1000);
						 		waitCount++;
						 	} catch (InterruptedException e) {
						 		e.printStackTrace();
						 	}
						 } 
						 
						
						 		//if (skipNextCheck) {
						 		//	MuteLog.Log("NewMessageService", "skipNextCheck is now true!");
						 		//	return;
						 		//}
						 
					    	    MuteLog.Log("MuteswanService","THREAD RUNNING: " + circle.getShortname());

					    		final Integer startLastId = circle.getLastMsgId(false);
					    		//Integer lastId = circle.getLastTorMessageId();
					    		Integer lastId = getLastTorMsgIdPatiently(circle);
					    		
					    		
								MuteLog.Log("MuteswanService", "Polling for " + circle.getShortname() + " at thread " + Thread.currentThread().getId());
								if (Thread.interrupted())
									return;
								
					    		if (lastId == null || lastId < 0) {
					    			MuteLog.Log("MuteswanService", "Got null or negative from tor for " + circle.getShortname() + ", bailing out.");
					    			//pollList.remove(circle);
					    			return;
					    			//handleStopSelf.sendEmptyMessage(0);
					    		}
					    		
					    		if (lastId > startLastId)
								  circle.updateLastMessage(lastId,false);
							  
					    	
				       
						
					    		MuteLog.Log("MuteswanService", circle.getShortname() + " has lastId " + lastId);
				        

				        
				        // FIXME: REFACTOR
				    	  
						if (Thread.interrupted())
								return;
						
				    	 MuteLog.Log("NewMessageService", "Got last id of " + startLastId);
				    	 if (startLastId < lastId) {
				      
				    	   MuteLog.Log("NewMessageService", "Not using long poll, starting check for " + circle.getShortname());
				    	   int downloadCount = 0;
				    	   
				    	   for (Integer i = lastId; i > startLastId; i--) {
				    		 if (downloadCount >= numMsgDownload)
				    		 	 break;
				    		 MuteLog.Log("NewMessageService", "Downloading " + i +  " for " + circle.getShortname());
				    		 try {
								MuteswanMessage msg = circle.getMsgFromTor(i);
								if (msg != null && msg.signatures[0] != null) {
									circle.saveMsgToDb(i, msg.getDate(), msg.getMsg(),
											msg.signatures);
								} else if (msg != null) {
									circle.saveMsgToDb(i, msg.getDate(), msg.getMsg());
								}
								
								
					        	CharSequence notifTitle = circle.getShortname();
					        	CharSequence notifText = msg.getMsg();
					        	showCircleNotification(circle,notifTitle,notifText);
					        	downloadCount++;
								
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
					
		  }
		 
		 Thread monThread = new Thread() {
			 public void run() {
				 boolean someRunning = true;
				 int count = 0;
				 while (someRunning) {
					 someRunning = false;
					 count++;
					 
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					 
					 
					 
					 for (final Circle c : pollList.keySet()) {
						if (!pollList.get(c).getState().toString().equals("TERMINATED")) {
							MuteLog.Log("LatestMessages","thread state for " + c.getShortname() + " is " + pollList.get(c).getState());
							someRunning = true;
							if (count >= 40)
								pollList.get(c).interrupt();
						}
					 }
					 
				 }
			
			 	 if (!torOK) {
				   CharSequence notifTitle = getString(R.string.error_muteswan_check_failed);
				   CharSequence notifText = getString(R.string.error_muteswan_failed_check_content);
				   showNotification(notifTitle,notifText);
			 	 } else {
			 		 removeNotify();
			 	 }
				 torCheckThread.interrupt();
				 handleStopSelf.sendEmptyMessage(0);
				 
				 MuteLog.Log("NewMessageService", "Done polling thread state.");
			 }
			 
		 };
		 monThread.start();
		
	
	}
	
	
	protected void removeNotify() {
		mNM.cancel(STATIC_NOTIFYID);
	}


	@SuppressWarnings("unused")
	/*
	private MuteswanMessage longpollForNewMessage(final Circle circle, Integer id) throws IOException {
		if (circle == null) {
			MuteLog.Log("AtffService", "WTF, circle is null.");
		}
		MuteLog.Log("MuteswanService","Longpoll for " + circle.getShortname());
		MuteswanMessage msg = circle.getMsgLongpoll(id);
		return(msg);
	}*/

	
		
	private final IMessageService.Stub binder = new IMessageService.Stub() {

		public void refreshLatest() {
			MuteLog.Log("MuteswanService", "runPoll() called.");
			
			runPoll();
			
		}
	
		public boolean isPolling() {
			return(isWorking);
		}
		
		public void checkTorStatus(ITorVerifyResult verifyResult) {
			TorStatus checkTorStatus = new TorStatus(muteswanHttp,getApplicationContext());
			if (checkTorStatus.checkStatus()) {
				
				sendBroadcast(new Intent(Main.TOR_AVAILABLE));
			} else {
				sendBroadcast(new Intent(Main.TOR_NOT_AVAILABLE));
			}
		}

		@Override
		public boolean isSkipNextCheck() throws RemoteException {
			MuteLog.Log("NewMessageService", "skipNextCheck is " + skipNextCheck);
	    	if (skipNextCheck) {
	    		return(true);
	    	} else {
	    		return(false);
	    	}
		}
		
		public void setSkipNextCheck(boolean checkValue) {
			MuteLog.Log("NewMessageService", "setSkipNextCheck " + checkValue);
			skipNextCheck = checkValue;
		}


		@Override
		public int getLastTorMsgId(String circleHash) throws RemoteException {
			return(getLastTorMsgIdPatiently(circleStore.asHashMap().get(circleHash)));
		}
		
		
		@Override
		public int downloadLatestMsgRangeFromTor(String circleHash, int delta) throws RemoteException {
			Circle circle = circleStore.asHashMap().get(circleHash);
			Integer lastMessage = circle.getLastCurMsgId(false);
			return(downloadMsgRangeFromTor(circle,lastMessage,lastMessage-delta));
		}
		
		@Override
		public int downloadMsgRangeFromTor(String circleHash, int start, int last) throws RemoteException {
			Circle circle = circleStore.asHashMap().get(circleHash);
			return(downloadMsgRangeFromTor(circle,start,last));
		}
	
		private int downloadMsgRangeFromTor(Circle circle, int start, int last) throws RemoteException {
			
			ArrayList<MuteswanMessage> msgs;
			
			if (last <= 0)
				last = 1;
			
			// FIXME: refactor to use common method
			if (!linkedQueue.contains(circle)) {
				  linkedQueue.add(circle);
				} else {
					MuteLog.Log("NewMessageSservice", "Two downloads at once to " + circle.getShortname());
					while (linkedQueue.contains(circle)) {
						try {
							Thread.currentThread();
							Thread.sleep(250);
						} catch (InterruptedException e) {
							circle.closedb();
							return(-4);
						}
					}
					linkedQueue.add(circle);
				}	
			
			try {
				msgs = circle.getMsgRangeFromTor(start,last);
		
				
				if (Thread.currentThread().isInterrupted() || msgs == null) {
					MuteLog.Log("NewMessageService","msgs is null or was interrupted");
					linkedQueue.remove(circle);
					circle.closedb();
					return(-4);
				}
				MuteLog.Log("NewMessageService", "We got " + msgs.size() + " downloaded.");
				
				for (MuteswanMessage msg : msgs) {
					MuteLog.Log("NewMessageService", "I am " + Thread.currentThread() + " downloading " + msg.getId());
					if (msg != null && msg.signatures[0] != null) {
						circle.saveMsgToDb(Integer.parseInt(msg.getId()), msg.getDate(), msg.getMsg(),
							msg.signatures);
					} else if (msg != null) {
						circle.saveMsgToDb(Integer.parseInt(msg.getId()), msg.getDate(), msg.getMsg());
					}
				}
				linkedQueue.remove(circle);
				return 0;	
				
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				linkedQueue.remove(circle);
				return -1;
			} catch (IOException e) {
				e.printStackTrace();
				linkedQueue.remove(circle);
				return -2;
			}
			
			
		}

		@Override
		public int downloadMsgFromTor(String circleHash, int id) throws RemoteException {
			Circle circle = circleStore.asHashMap().get(circleHash);
			MuteswanMessage msg;

			// FIXME: refactor to use common method
			if (!linkedQueue.contains(circle)) {
			  linkedQueue.add(circle);
			} else {
				MuteLog.Log("NewMessageSservice", "Two downloads at once to " + circle.getShortname());
				while (linkedQueue.contains(circle)) {
					try {
						Thread.currentThread();
						Thread.sleep(250);
					} catch (InterruptedException e) {
						circle.closedb();
						return(-4);
					}
				}
				linkedQueue.add(circle);
			}
			
			
			try {
				msg = circle.getMsgFromTor(id);
				
				if (Thread.currentThread().isInterrupted()) {
					linkedQueue.remove(circle);
					circle.closedb();
					return(-4);
				}
				
				MuteLog.Log("NewMessageService", "I am " + Thread.currentThread());
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
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				e.printStackTrace();
			} catch (BadPaddingException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return(-1);
		}
		
		
		
		
		
	};
	final private LinkedList<Circle> stopList = new LinkedList<Circle>();
	private boolean skipNextCheck = false;
	
	

	private IsUserCheckingMessagesReceiver isUserCheckingMessagesReceiver = new IsUserCheckingMessagesReceiver();
	private NetworkChangeReceiver networkChangeReceiver = new NetworkChangeReceiver();
	private DeletedCircleReceiver deletedCircleReceiver = new DeletedCircleReceiver();
	private JoinedCircleReceiver joinedCircleReceiver = new JoinedCircleReceiver();
	private CreatedCircleReceiver createdCircleReceiver = new CreatedCircleReceiver();
	
	public IBinder onBind(Intent intent) {
		MuteLog.Log("NewMessageService","onBind called.");
		return binder;
	}

		
	
	private void stopservice() {
		muteswanHttp.cleanup();
		for (Circle r : pollList.keySet()) {
			stopList.add(r);
			if (pollList.get(r) != null)
			  pollList.get(r).interrupt();
		}
	}

	
	private class IsUserCheckingMessagesReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	MuteLog.Log("NewMessageService", "Got alarm to not check!");
	    	skipNextCheck = true;
	    }
	}
	
	private class DeletedCircleReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	MuteLog.Log("NewMessageService", "Got deleted circle receiver!");
	    	init(true);
	    }
	}
	
	private class JoinedCircleReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	MuteLog.Log("NewMessageService", "Got joined circle receiver!");
	    	init(true);
	    }
	}
	
	private class CreatedCircleReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	MuteLog.Log("NewMessageService", "Got created circle receiver!");
	    	init(true);
	    }
	}
	
	private class NetworkChangeReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	MuteLog.Log("NewMessageService", "Network change event!");
	    	MuteLog.Log("NewMessageServicE", "Intent: " + intent.toString());
	    	MuteLog.Log("NewMessageService", "extras " + intent.getExtras().describeContents());
	    	skipNextCheck = true;
	    }
	}
	
	

	

	

}
