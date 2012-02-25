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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

public class NewMessageService extends Service {

    Intent notificationIntent;
	PendingIntent contentIntent;
	NotificationManager mNM;
	HashMap<String,Integer> notifyIds;
	int notifyId;
	final int PERSISTANT_NOTIFICATION = 220;
	protected boolean isWorking;
	private HashMap<Circle,Thread> pollList = new HashMap<Circle,Thread>();
	private boolean started = false;
	protected boolean torActive = false;
	
	protected LinkedBlockingQueue<Circle> linkedQueue = new LinkedBlockingQueue<Circle>();
	
	// long poll is experimental and currently destroys batteries. We should investigate this at another time
	protected boolean useLongPoll = false;
	private CircleStore circleStore;
	private int numMsgDownload = 5;
	private MuteswanHttp muteswanHttp;
    
	
	@Override
	public void onStart(Intent intent, int startId) {
		Log.v("MuteswanService", "onStart called.");
		this.stopForeground(false);
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
		unregisterReceiver(isUserCheckingMessagesReceiver);
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
    
	private void init() {
		 pollList.clear();
		 
		notificationIntent = new Intent(this, Main.class);
		contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notifyIds = new HashMap<String,Integer>();
		notifyId = 8392;
		   
		 Log.v("MuteswanService", "Service initialized, we are: " + Thread.currentThread().getId());
		 if (muteswanHttp == null)
		   muteswanHttp = new MuteswanHttp();
		 circleStore = new CircleStore(getApplicationContext(),true,false,muteswanHttp);
		 for (Circle r : circleStore) {
				  Log.v("MuteswanService", "Circle " + r.getShortname() + " registered.");
				  registerPoll(r);
		 }
		 
	}
	
	private void start() {
		
	
		
		
		// Startup
		if (started  == false) {
		   Log.v("MuteswanService", "Start flag is false, exiting.");
		  
		  
		  started = true;
		  runPoll();
		  
		} else {
			runPoll();
		}
	}

	
	private void showNotification(Circle c, CharSequence title, CharSequence content) {
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
		//PendingIntent pendingMsgIntent = PendingIntent.getActivity(getApplicationContext(), 0, msgIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent pendingMsgIntent = PendingIntent.getActivity(getApplicationContext(), (int) System.currentTimeMillis(), msgIntent,0);
	
	
		Log.v("NewMessageService", "Set pending intent to launch " + c.getShortname() + "(" + Main.genHexHash(c.getFullText()) + ")");
		Log.v("NewMessageService", "Setting notify id of " + notifyId);
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
	
			 Log.v("NewMessageService", "Circlestore: " + circleStore.hashCode());
		 Log.v("MuteswanService","pollList size " + pollList.size());
		 for (final Circle circle : pollList.keySet()) {
			 
			 Thread oldThread = pollList.get(circle);
 	         try {
				  Log.v("MuteswanService","Interrupting old thread " + oldThread.toString() + ": " + circle.getShortname());
			      oldThread.interrupt();
			      oldThread.join(5);
			      oldThread = null;
			      pollList.put(circle, null);
			  } catch (InterruptedException e) {
			  }

			 Log.v("NewMessageService", "Circle: " + circle.hashCode());
			
		     Log.v("MuteswanService", "Starting poll of " + circle.getShortname());
			
			
				 Thread nThread = new Thread() {
				    	
					   
					 public void run() {
					    	Log.v("MuteswanService","THREAD RUNNING: " + circle.getShortname());

					    		final Integer startLastId = circle.getLastMsgId(false);
								Integer lastId = circle.getLastTorMessageId();
						 Log.v("MuteswanService", "Polling for " + circle.getShortname() + " at thread " + Thread.currentThread().getId());
					    		if (lastId == null || lastId < 0) {
					    			Log.v("MuteswanService", "Got null or negative from tor for " + circle.getShortname() + ", bailing out.");
					    			//pollList.remove(circle);
					    			return;
					    			//handleStopSelf.sendEmptyMessage(0);
					    		}
					    		
					    		if (lastId > startLastId)
								  circle.updateLastMessage(lastId,false);
							  
					    	
				       
						
				        Log.v("MuteswanService", circle.getShortname() + " has lastId " + lastId);
				        

				        
				        // FIXME: REFACTOR
				    	  
				    	 Log.v("NewMessageService", "Got last id of " + startLastId);
				    	 if (startLastId < lastId) {
				      
				    	   Log.v("NewMessageService", "Not using long poll, starting check for " + circle.getShortname());
				    	   int downloadCount = 0;
				    	   
				    	   for (Integer i = lastId; i > startLastId; i--) {
				    		 if (downloadCount >= numMsgDownload)
				    		 	 break;
				    		 Log.v("NewMessageService", "Downloading " + i +  " for " + circle.getShortname());
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
					        	showNotification(circle,notifTitle,notifText);
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
		
	
	}
	
	
	@SuppressWarnings("unused")
	/*
	private MuteswanMessage longpollForNewMessage(final Circle circle, Integer id) throws IOException {
		if (circle == null) {
			Log.v("AtffService", "WTF, circle is null.");
		}
		Log.v("MuteswanService","Longpoll for " + circle.getShortname());
		MuteswanMessage msg = circle.getMsgLongpoll(id);
		return(msg);
	}*/

	
		
	private final IMessageService.Stub binder = new IMessageService.Stub() {

		public void refreshLatest() {
			Log.v("MuteswanService", "runPoll() called.");
			
			runPoll();
			
		}
	
		public boolean isPolling() {
			return(isWorking);
		}
		
		public void checkTorStatus(ITorVerifyResult verifyResult) {
			TorStatus checkTorStatus = new TorStatus(muteswanHttp);
			if (checkTorStatus.checkStatus()) {
				
				sendBroadcast(new Intent(Main.TOR_AVAILABLE));
			} else {
				sendBroadcast(new Intent(Main.TOR_NOT_AVAILABLE));
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
					Log.v("NewMessageSservice", "Two downloads at once to " + circle.getShortname());
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
					Log.v("NewMessageService","msgs is null or was interrupted");
					linkedQueue.remove(circle);
					circle.closedb();
					return(-4);
				}
				Log.v("NewMessageService", "We got " + msgs.size() + " downloaded.");
				
				for (MuteswanMessage msg : msgs) {
					Log.v("NewMessageService", "I am " + Thread.currentThread() + " downloading " + msg.getId());
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
				Log.v("NewMessageSservice", "Two downloads at once to " + circle.getShortname());
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
