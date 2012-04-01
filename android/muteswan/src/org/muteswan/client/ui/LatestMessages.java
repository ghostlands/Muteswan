/*
Copyright 2011-2012 James Unger,  Chris Churnick.
Copyright 2011-2012 James Unger,  Chris Churnick.
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
package org.muteswan.client.ui;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.muteswan.client.AlertDialogs;
import org.muteswan.client.IMessageService;
import org.muteswan.client.MuteswanHttp;
import org.muteswan.client.NewMessageService;
import org.muteswan.client.R;
import org.muteswan.client.Main;
import org.muteswan.client.TorStatus;
import org.muteswan.client.data.Circle;
import org.muteswan.client.data.CircleStore;
import org.muteswan.client.data.IdentityStore;
import org.muteswan.client.data.MuteswanMessage;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import org.muteswan.client.MuteLog;


public class LatestMessages extends ListActivity implements Runnable {

	protected static final int MSGDOWNLOADAMOUNT = 5;
	public static final String CHECKING_MESSAGES = "currentlycheckingmessages";
	private Bundle extra;
	private String circleExtra;
	private CircleStore store;
	final ArrayList<MuteswanMessage> messageList = new ArrayList<MuteswanMessage>();
	private ArrayList<MuteswanMessage> msgBucket = new ArrayList<MuteswanMessage>();
	HashMap<String,Circle> circleMap;
	IdentityStore idStore;
	private LatestMessagesListAdapter listAdapter;
	private int messageViewCount;
	HashMap<View, AlertDialog> moreButtons;
	private ProgressDialog gettingMsgsDialog;
	
	private ConcurrentHashMap<String, Integer> newMsgCheckState = new ConcurrentHashMap<String,Integer>();
	private ConcurrentHashMap<String, String> newMsgCheckResults = new ConcurrentHashMap<String,String>();
	
	protected IMessageService msgService;
	private ImageView spinneyIcon;
	private boolean moreMessages = false;
	private RotateAnimation ranimnation;
	
	private final CompareDates comparatorDates = new CompareDates();
	
	
	public void onResume() {
		super.onResume();
		//init();
		
		 if (torNotAvailableReceiver == null)
			 torNotAvailableReceiver = new TorNotAvailableReceiver();
		 IntentFilter intentFilter = new IntentFilter(Main.TOR_NOT_AVAILABLE);
		 registerReceiver(torNotAvailableReceiver, intentFilter);
		
		//cleanup();
		 messageList.clear();
		
		extra = getIntent().getExtras();
		
        if (extra != null) {
         MuteLog.Log("LatestMessages", "onResume extra is " + extra.getString("circle"));
         circleExtra = extra.getString("circle");
        } else {
        	MuteLog.Log("LatestMessages", "onResume extra is null.");
        	circleExtra = null;
        }
		
        
        
		refresh();
		
	}
	
	public void onPause() {
		super.onPause();
		if (torNotAvailableReceiver != null)
			unregisterReceiver(torNotAvailableReceiver);
		cleanup(false);
	}

	
	final LinkedBlockingQueue<Thread> oldThreads = new LinkedBlockingQueue<Thread>();
	private void cleanOldThreads(boolean join) {
		for (Thread t : oldThreads) {
			MuteLog.Log("LatestMessages", "Cleaning thread " + t.toString());
			t.interrupt();
			 while (join == true && t != null) {
			        try {
			            t.join();
			            t = null;
			        } catch (InterruptedException e) {
			        	MuteLog.Log("LatestMessages", "interrupted while trying to join");
			        }
			    }
		}
	}

	public void onDestroy() {
		super.onDestroy();
		gettingMsgsDialog = null;
		
		cleanup(false);
		
		if (msgService != null) {
			unbindService(msgServiceConn);
		}
	}
	
	public void cleanup(boolean join) {
		messageViewCount = 0;
		//messageList.clear();
  	    newMsgCheckResults.clear();
		
		cleanOldThreads(join);
		//for (String c : store.asHashMap().keySet()) {
		//	store.asHashMap().get(c).closedb();
		//}
	}
	
	
	OnScrollListener scrollListener = new OnScrollListener() {
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {}
		
		
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			
			int lastInScreen = firstVisibleItem + visibleItemCount;
			
			if (lastInScreen == 0)
				return;
			
			if (firstVisibleItem == 0)
				return;
			
			
			//MuteLog.Log("LatestMessages", "lastInScreen " + lastInScreen + " and firstVisibleItem" + firstVisibleItem );
			if (lastInScreen == totalItemCount) {
				MuteLog.Log("LatestMessages", "End of list: " + moreMessages);
				if (moreMessages == false) {
					moreMessages = true;
					messageViewCount = messageViewCount + getMsgDownloadAmount();
					loadmore();
				}
				//messageViewCount = messageViewCount + LatestMessages.MSGDOWNLOADAMOUNT;
				//refresh();
			}
		}


		
	};
	private boolean refreshing;
	private boolean verbose;
	private long previousRefreshTime = 0;
	private long previousLoadMoreTime;
	private View footerView;
	private AlertDialogs alertDialogs;
	private String cipherSecret;
	
	
	private void init() {
        
		
	
        
        extra = getIntent().getExtras();
        cipherSecret = extra.getString("secret");

        if (cipherSecret == null) {
        	MuteLog.Log("LatestMessages","Can't do nothing without a secret...");
        	return;
        }
        
        if (extra.containsKey("circle")) { 
		 store = new CircleStore(cipherSecret,this,true,false);
		
		 circleMap = store.asHashMap();
         circleExtra = extra.getString("circle");
         //circleMap.get(circleExtra).initCache();
        } else {
		 
		 store = new CircleStore(cipherSecret,this,true,true);
		 circleMap = store.asHashMap();
        }
        
	}
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       
        
        init();
        
         
        setContentView(R.layout.latestmessages);

        
        if (circleExtra != null) {
         MuteLog.Log("LatestMessages", "circleExtra is " + circleMap.get(circleExtra).getShortname());
		 TextView txtTitle = (TextView) findViewById(R.id.android_latestmessagesprompt);
		 txtTitle.setText("Messages for " + circleMap.get(circleExtra).getShortname());
        }
        

        SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		verbose = defPrefs.getBoolean("verbose", false);
		

		// initialize alert dialogs
	    alertDialogs = new AlertDialogs(this);
		
		
	
        final Button postButton = (Button) findViewById(R.id.latestmessagesPost);
        postButton.setOnClickListener(postClicked);
        
        final ImageView titleBarImage = (ImageView) findViewById(R.id.latestmessagesTitle);
        titleBarImage.setOnClickListener(titleBarClicked);
        
        
		messageViewCount = 0;
		moreButtons = new HashMap<View,AlertDialog>();
       
		
        listAdapter = new LatestMessagesListAdapter(this,R.id.android_latestmessagesprompt);
        
        
        footerView = ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.latestmessagesfooter, null, false);
        getListView().addFooterView(footerView);
        getListView().setOnScrollListener(scrollListener);
        setListAdapter(listAdapter);
        
    }
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.latestmessagesmenu, menu);
        return true;
    }
	
	public boolean onOptionsItemSelected(MenuItem item) {

	
		showInitialLoad();
		messageViewCount = 0;
		messageList.clear();
		refresh();
		
		
		
		
		return true;
		
	}

	
	// loadmore loads more messages as we scroll down
	private void loadmore() {
		
		
		
		if (previousLoadMoreTime != 0 && ((System.currentTimeMillis()/1000) - previousLoadMoreTime) < 1) {
			MuteLog.Log("LatestMessages", "Loadmore called within 1 second, not running.");
			moreMessages = false;
			return;
		}
		
		
		MuteLog.Log("LatestMessages", "moreMessages at start of refresh(): " + moreMessages);
		previousLoadMoreTime = System.currentTimeMillis()/1000;
		Thread thread = new Thread(this);
		oldThreads.add(thread);
		thread.start();
		

	}
	
	
	// refresh() checks for latest
	private void refresh() {
		
		if (previousRefreshTime != 0 && ((System.currentTimeMillis()/500) - previousRefreshTime) < 1) {
			MuteLog.Log("LatestMessages", "Refresh called within 0.5 second, not running.");
			return;
		}
	
		
		if (store.isEmpty()) {
			setFooterText(getString(R.string.n_no_circles_to_check));
			return;
		}
		sendBroadcast(new Intent(LatestMessages.CHECKING_MESSAGES));
		
		
		newMsgCheckState.clear();
		
		MuteLog.Log("LatestMessages", "Refreshing at start of refresh(): " + refreshing);
		refreshing = true;
		previousRefreshTime = System.currentTimeMillis()/1000;
		Thread thread = new Thread(this);
		oldThreads.add(thread);
		thread.start();
		spinIcon();
		spinneyIcon.setImageResource(R.drawable.refresh);
		if (verbose == true)
			showDialog();
	}

	private void showInitialLoad() {
	
	}

	private void showDialog() {
		gettingMsgsDialog = ProgressDialog.show(this, "", "Checking for new messages...", true);
		gettingMsgsDialog.setCancelable(true);
	}
	
	private void spinIcon() {
		spinneyIcon = (ImageView) findViewById(R.id.checkingMessagesIcon);
		ranimnation = new RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		ranimnation.setDuration((long) 2*1000);
		ranimnation.setRepeatCount(RotateAnimation.INFINITE);
		final ImageView refreshButton = (ImageView) findViewById(R.id.checkingMessagesIcon);
		refreshButton.setOnClickListener(null);
		spinneyIcon.startAnimation(ranimnation);
	}
	
	public View.OnClickListener listItemClicked = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			listItemClicked(v);
		}
	    
    };
    
    public Button.OnClickListener buttonClicked = new Button.OnClickListener() {

		@Override
		public void onClick(View v) {
			//listItemClicked(v);
			AlertDialog alert = moreButtons.get(v);
			alert.show();			
		}
	    
    };
	
    public View.OnClickListener showCircle = new View.OnClickListener() {
    	public void onClick(View v) {
    		showCircle((TextView) v);
    	}
    };
    
    
   
	
    
    
    
	//public HashMap<View, Integer> repostButtons = new HashMap<View,Integer>();
    
    
    private void listItemClicked(View v) {
    	MuteLog.Log("LatestMessages","List item clicked.");
    }
    
    private void showCircle(TextView v) {
    	for (Circle r : store) {
    		if (r.getShortname().equals(v.getText().toString())) {
    			Intent intent = new Intent(this,LatestMessages.class);
    			intent.putExtra("circle", Main.genHexHash(r.getFullText()));
    			startActivity(intent);
    		}
    	}
    }
    
    public View.OnClickListener postClicked = new View.OnClickListener() {
    	public void onClick(View v) {
    		if (circleExtra != null) {
    		  Intent intent = new Intent(getApplicationContext(),WriteMsg.class);
    		  intent.putExtra("circle",circleMap.get(circleExtra).getFullText());
    		  intent.putExtra("secret",cipherSecret);
    		  startActivity(intent);
    		} else {
    		   Intent intent = new Intent(getApplicationContext(),WriteMsg.class);
    		   intent.putExtra("secret",cipherSecret);
      		   startActivity(intent);
    		}
    	}
    };
    
    public View.OnClickListener titleBarClicked = new View.OnClickListener() {
    	public void onClick(View v) {
    		  Intent intent = new Intent(getApplicationContext(),Main.class);
      		  startActivity(intent);
    		}
    };
    
    
    public View.OnClickListener refreshClicked = new View.OnClickListener() {
    	public void onClick(View v) {
    		showInitialLoad();
    		messageViewCount = 0;
    		messageList.clear();
    		refresh();
    	}
    };
    
    public View.OnClickListener showAlertDialog = new View.OnClickListener() {
    	public void onClick(View v) {
    		alertDialog.create();
    		alertDialog.show();
    	}
    };
	public TextView txtCircle;
	public TextView txtDate;
	public TextView txtMessage;
	public TextView txtRepost;
	public MuteswanMessage msg;
	public RelativeLayout layout;
	private String longPressedCircle;
	private String longPressedMsg;

    private int getMsgDownloadAmount() {
    	if (circleExtra == null && store.size() > 2) {
    	  return(LatestMessages.MSGDOWNLOADAMOUNT);
    	} else {
    	  return(LatestMessages.MSGDOWNLOADAMOUNT+10);
    	}
	}
    
    
    public static class RelativeDateFormat {

    	 private static final long ONE_MINUTE = 60000L;
    	 private static final long ONE_HOUR = 3600000L;
    	 private static final long ONE_DAY = 86400000L;

    	 public static String format(Date date) {

    	  long delta = new Date().getTime() - date.getTime();
    	  if (delta < 1L * ONE_MINUTE) {
    	   return toSeconds(delta) == 1 ? "one second" : toSeconds(delta)
    	     + " seconds ago";
    	  }
    	  if (delta < 2L * ONE_MINUTE) {
    	   return "one minute";
    	  }
    	  if (delta < 45L * ONE_MINUTE) {
    	   return toMinutes(delta) + " minutes";
    	  }
    	  if (delta < 90L * ONE_MINUTE) {
    	   return "one hour";
    	  }
    	  if (delta < 24L * ONE_HOUR) {
    	   return toHours(delta) + " hours";
    	  }
    	  if (delta < 48L * ONE_HOUR) {
    	   return "yesterday";
    	  }
    	  if (delta < 5L * ONE_DAY) {
    	   return toDays(delta) + " days";
    	  }
    	  /*
    	  if (delta < 12L * 4L * ONE_WEEK) {
    	   long months = toMonths(delta);
    	   return months <= 1 ? "one month ago" : months + " months ago";
    	  } else {
    	   long years = toYears(delta);
    	   return years <= 1 ? "one year ago" : years + " years ago";
    	  }*/
    	  return null;
    	 }
    	 

    	 private static long toSeconds(long date) {
    	  return date / 1000L;
    	 }

    	 private static long toMinutes(long date) {
    	  return toSeconds(date) / 60L;
    	 }

    	 private static long toHours(long date) {
    	  return toMinutes(date) / 60L;
    	 }

    	 private static long toDays(long date) {
    	  return toHours(date) / 24L;
    	 }

    	 //private static long toMonths(long date) {
    	 // return toDays(date) / 30L;
    	 //}

    	 //private static long toYears(long date) {
    	 // return toMonths(date) / 365L;
    	 //}

    	}


	public class LatestMessagesListAdapter extends ArrayAdapter<MuteswanMessage> {
		
		 public LatestMessagesListAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
			
		}

		    
		    //public View.OnClickListener repostClicked = new View.OnClickListener() {
		    //	public void onClick(View v) {
		    //		//MuteswanMessage msg = messageList.get(repostButtons.get(v));
		    //		Intent intent = new Intent(getApplicationContext(),WriteMsg.class);
		    //		intent.putExtra("initialText",msg.getMsg());
		    //		startActivity(intent);
		    //	}
		    //};
		
		
		@Override
		public int getCount() {
			return(messageList.size());
		}

		@Override
		public MuteswanMessage getItem(int position) {
			return(messageList.get(position));
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		
		
		
		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			
			
			  layout = (RelativeLayout) convertView;
			  if (layout == null)
      		     layout = (RelativeLayout) getLayoutInflater().inflate(R.layout.latestmessagesentry,
      				  parent, false);
			  
	
	

			  if (messageList == null || messageList.size() == 0 || (messageList.size() == 1 && position > 1))
				  return(layout);
			  
      		  msg = messageList.get(position);
      		  
      		  if (msg == null) {
      			  Log.e("LatestMessages", "Msg is null in getView!");
      			  return(layout);
      		  }
      		  
      		  txtCircle = (TextView) layout.findViewById(R.id.android_latestmessagesCircle);
      		  txtDate = (TextView) layout.findViewById(R.id.android_latestmessagesDate);
      		  txtMessage = (TextView) layout.findViewById(R.id.android_latestmessagesMessage);
      		  
      		  registerForContextMenu(txtMessage);
      		  txtMessage.setTag(R.id.messageReply, msg.getCircle().getFullText());
      		  txtMessage.setTag(R.id.messageRepost, msg.getMsg());
      		  
      		  
      		  //txtRepost = (TextView) layout.findViewById(R.id.android_latestmessagesRepostButton);


      		  //txtRepost.setOnClickListener(repostClicked);
              //repostButtons.put(txtRepost,position);
  
      		
      		  
              if (circleExtra == null) {
            	txtCircle.setText(msg.getCircle().getShortname());
      		  	txtCircle.setClickable(true);
      		  	txtCircle.setOnClickListener(showCircle);
			  } else {
				  txtCircle.setVisibility(View.INVISIBLE);
			  }
              
              //We assume local time zone since we wrote the timestamp converted from GMT
              SimpleDateFormat df = new SimpleDateFormat( "MMM d hh:mm a" );
             
			 String dateString = RelativeDateFormat.format(msg.getDateObj());
			 if (dateString == null)
				 dateString = df.format(msg.getDateObj());
			 txtDate.setText(dateString);
			 
			 
      		  txtMessage.setText(msg.getMsg());
      		  
			
			 layout.setClickable(true);
			 layout.setOnClickListener(listItemClicked);
			 
      		 return layout;	
		}
		
	}
	
	
    public void onCreateContextMenu(ContextMenu menu, View v,
                                 ContextMenuInfo menuInfo) {
                        super.onCreateContextMenu(menu, v, menuInfo);
                        MenuInflater inflater = getMenuInflater();
                        //currentViewSelected = (TextView) v.getParent()
                        //TextView newView = (TextView) v;
                        
                        //String txtCircle2 = (TextView) layout.findViewById(R.id.android_latestmessagesCircle);
                        //longPressedCircle = v.getLayout().findViewById(R.id.android_latestmessagesCircle);
                        //BOOK
                        //TextView txtCircleView = v.requestLayout().findViewById(R.id.android_latestmessagesCircle);
                        //longPressedCircle = v.getLayout().findViewById(R.id.android_latestmessagesCircle);
                        longPressedCircle = (String) v.getTag(R.id.messageReply);
                        longPressedMsg = (String) v.getTag(R.id.messageRepost);
                        inflater.inflate(R.menu.messagecontextmenu, menu);
    }

    public boolean onContextItemSelected(MenuItem item) {

    	if (item.getTitle().equals(getString(R.string.menu_repost))) {
		  Intent intent = new Intent(getApplicationContext(),WriteMsg.class);
		  intent.putExtra("circle",longPressedCircle);
		  intent.putExtra("initialText",longPressedMsg);
		  startActivity(intent);
		  return true;
    	} else if (item.getTitle().equals(getString(R.string.menu_reply))) {
		  Intent intent = new Intent(getApplicationContext(),WriteMsg.class);
		  intent.putExtra("circle",longPressedCircle);
		  startActivity(intent);
		  return true;
    	}
    	
    	return false;
		
    }
	
	final Handler newMsgCheckEventHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle b = msg.getData();
			
			
			
			MuteLog.Log("LatestMessages", "Setting circle to " + b.getString("circle") + " and state to " + b.getString("state"));
			if (b.getString("circle") != null && b.getString("state") != null) {
				Integer msgDelta = b.getInt("msgDelta");
				//if (!b.getString("state").equals("done"))
				 newMsgCheckState.put(b.getString("circle"),-1);
				
				newMsgCheckResults.put(b.getString("circle"), b.getString("state"));
				
				if ((b.getString("state").equals("failed") || b.getString("state").equals("done")) 
						&& msgDelta != null && msgDelta >= 0)
				 newMsgCheckState.put(b.getString("circle"),msgDelta);
				 
				 // if anything has failed we don't want to change the spinney state
				 boolean someFailed = false;					
				 for (String status : newMsgCheckResults.keySet()) {
					if (newMsgCheckResults.get(status).equals("failed")) {
						someFailed = true;
						break;
					}
				}
				 
				
				
				if (b.getString("state").equals("starting") && !someFailed) {
					spinneyIcon.setImageResource(R.drawable.refresh_checking);
				} else if (b.getString("state").equals("done") && !someFailed) {
					
					spinneyIcon.setImageResource(R.drawable.refresh_downloading);
				} 
				
				if(someFailed) {
					//newMsgCheckState.put(b.getString("circle"),0);
					spinneyIcon.setImageResource(R.drawable.refresh_yellow);
				}
				
				if (gettingMsgsDialog != null) {
					gettingMsgsDialog.setMessage(circleMap.get(b.getString("circle")).getShortname() + getString(R.string.n_checking_for_new_messages));
				}
			}
			
		}
	};
	protected Builder alertDialog;
	

	final Handler dismissDialog = new Handler() {
		
		
		
        @Override
        public void handleMessage(Message msg) {
        	  if (gettingMsgsDialog != null)
        	    gettingMsgsDialog.dismiss();
        	  if (spinneyIcon != null) {
        		  
        		  boolean allFailed = true;
        		  boolean someFailed = false;
        		  MuteLog.Log("LatestMessages", "New message check is empty? " + newMsgCheckResults.isEmpty());
        		  for (String status : newMsgCheckResults.keySet()) {
        			  MuteLog.Log("LatestMessages", "checking state in dismiss dialog (" + status + ": " + newMsgCheckResults.get(status));
        			  if (newMsgCheckResults.get(status).equals("done")) {
        				  allFailed = false;
        				  continue;
        			  }
        			  
        			  if (newMsgCheckResults.get(status).equals("failed")) {
        				  someFailed = true;
        				  continue;
        			  }
        			  
        		  }
        		  
        		 
        		  final ImageView refreshButton = (ImageView) findViewById(R.id.checkingMessagesIcon);
        		  if (allFailed == true) {
        			  alertDialog = getAllFailedAlertDialog();
        			  spinneyIcon.setImageResource(R.drawable.refresh_red);
        			  refreshButton.setOnClickListener(showAlertDialog);
        		  } else if (someFailed == true) {
        			  spinneyIcon.setImageResource(R.drawable.refresh_yellow);
        			  alertDialog = getSomeFailedAlertDialog();
        			  refreshButton.setOnClickListener(showAlertDialog);
        		  } else {
        			  spinneyIcon.setImageResource(R.drawable.refresh_done);
        			  refreshButton.setOnClickListener(refreshClicked);
        		  }
        		
        		  
        		  if (!getFooterText().equals("")) {
           				setFooterText(getString(R.string.n_no_message_in_circle));
           	   	  }
        		  
        		  //refreshButton.setOnClickListener(refreshClicked);
        		  newMsgCheckResults.clear();
        		  spinneyIcon.setAnimation(null);
        		  showCheckTimeDelta();
        	  }
        }

    };
    
	private void showCheckTimeDelta() {
		long delta = (System.currentTimeMillis()/1000) - previousRefreshTime;
		TextView deltaField = (TextView) findViewById(R.id.android_checktimedelta);
		if (verbose)
		  deltaField.setVisibility(View.VISIBLE);
		deltaField.setText(Integer.toString((int)delta) + "s");
	}
    
    private AlertDialog.Builder getSomeFailedAlertDialog() {
    	AlertDialog.Builder someFailedAlert = new AlertDialog.Builder(LatestMessages.this);
    	
    	
    	String alertMessage = getString(R.string.n_some_message_checks_failed);
    	for (String status : newMsgCheckResults.keySet()) {
    		if (newMsgCheckResults.get(status).equals("failed")) {
    			// FIXME string painting
    			alertMessage = alertMessage + " " + circleMap.get(status).getShortname() + "\n";
    		}
    	}
    	
	    someFailedAlert.setTitle(R.string.t_some_messages_failed_check);
	    someFailedAlert.setMessage(alertMessage);
	    someFailedAlert.setPositiveButton(R.string.refresh_messages_confirm_yes, new DialogInterface.OnClickListener() {
	      public void onClick(DialogInterface dialogInterface, int i) {
	    	messageViewCount = 0;
	  		messageList.clear();
	        refresh();
	      }
	    });
	    someFailedAlert.setNegativeButton(R.string.refresh_messages_confirm_no, new DialogInterface.OnClickListener() {
	      public void onClick(DialogInterface dialogInterface, int i) {}
	    });
	    
	    return(someFailedAlert);
    }
    
    private AlertDialog.Builder getAllFailedAlertDialog() {
    	AlertDialog.Builder allFailedAlert = new AlertDialog.Builder(LatestMessages.this);
    	
    	
    	String alertMessage = getString(R.string.n_no_messages_found_or_downloaded);
    	
    	
	    allFailedAlert.setTitle(R.string.n_error_checking_for_latest_messages);
	    allFailedAlert.setMessage(alertMessage);
	    allFailedAlert.setPositiveButton(R.string.refresh_messages_confirm_yes, new DialogInterface.OnClickListener() {
	      public void onClick(DialogInterface dialogInterface, int i) {
	    	messageViewCount = 0;
	  		messageList.clear();
	        refresh();
	      }
	    });
	    allFailedAlert.setNegativeButton(R.string.refresh_messages_confirm_no, new DialogInterface.OnClickListener() {
	      public void onClick(DialogInterface dialogInterface, int i) {}
	    });
	    
	    return(allFailedAlert);
    }
	
    
    
    final Handler dataSetChanged = new Handler() {
		
        @Override
        public void handleMessage(Message msg) {
        		//listAdapter.sortli(new CompareDates());
        		
        	    listAdapter.notifyDataSetChanged();
        }
    };
    
 final Handler updateMessageList = new Handler() {
		
        @Override
        public void handleMessage(Message msg) {
        		for (MuteswanMessage msMsg : msgBucket) {
        			messageList.add(msMsg);
        		}
        	    listAdapter.notifyDataSetChanged();
        	    msgBucket.clear();
        }
        
    };
	
	
    boolean sorting = false;
    
    final Handler sortMessageList = new Handler() {
		
        @Override
        public void handleMessage(Message msg) {
        		
        	   MuteLog.Log("LatestMessages", "Sorting!");
        	   if (messageList == null || messageList.size() == 0) {
        		MuteLog.Log("LatestMessages", "Refusing to sort empty or null list.");
				return;
        	   }
        	
        	 
   			   setFooterText("");
       	
        		if (sorting == false) {
        			sorting = true;
        			
        			Thread.currentThread();
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

        		    Collections.sort(messageList, getComparatorDates());
        		    listAdapter.notifyDataSetChanged();
        		    
        		    sorting = false;
        		    
        		} else {
        			MuteLog.Log("LatestMessages", "Concurrent sort request!");
        		}
        		
        }
    };
	
    final Handler updateDialogText = new Handler() {
    	
    	@Override
    	public void handleMessage(Message msg) {
    		if (gettingMsgsDialog != null)
    		  gettingMsgsDialog.setMessage(msg.getData().getString("txt"));
    	}
    };
    
    final Handler setSpinneyDownloading = new Handler() {
    	
    	@Override
    	public void handleMessage(Message msg) {
    		spinneyIcon.setImageResource(R.drawable.refresh_downloading);
    	}
    };
    
    final Handler setSpinneyAllFailed = new Handler() {
    	
    	@Override
    	public void handleMessage(Message msg) {
    		spinneyIcon.setImageResource(R.drawable.refresh_red);
    		spinneyIcon.setAnimation(null);
    	}
    };
    
    final Handler startSpinningHandler = new Handler() {
    	
    	@Override
    	public void handleMessage(Message msg) {
    		spinIcon();
    		
    	}
    };
    
final Handler stopSpinningHandler = new Handler() {
    	
    	@Override
    	public void handleMessage(Message msg) {
    		spinneyIcon.setAnimation(null);
    		spinneyIcon.setImageResource(R.drawable.refresh_done);
    	}
    };

	

    
    class CompareDates implements Comparator<MuteswanMessage>
    {
        public int compare(MuteswanMessage obj1, MuteswanMessage obj2)
        {
        	MuteswanMessage msg1 =  obj1;
        	MuteswanMessage msg2 =  obj2;
        	
        	
        	Date mDate = msg1.getDateObj();
        	Date oDate = msg2.getDateObj();
			
			if (mDate == null || oDate == null) {
				return -1;
			}
			
			
			if (mDate.after(oDate)) {
				return -1;
			} else if (mDate.before(oDate)) {
				return 1;
			} else {
				return 0;
			}
			
        }
    }

    
	
	private void updateLatestMessages(ArrayList<MuteswanMessage> msgs, Circle r,
			Integer start, Integer last) {
		
		
		MuteLog.Log("LatestMessages","updateLatestMessages circle " + r.getShortname());
		
		if (Thread.currentThread().isInterrupted()) {
			MuteLog.Log("LatestMessages", "Interrupted 0.5 " + Thread.currentThread().toString());
			return;
		}		
		
		Integer lastId = r.getLastCurMsgId(false);
		
		
		Message m = new Message();
		Bundle b = new Bundle();
		b.putString("txt", "Loading..." + r.getShortname());
		m.setData(b);
		updateDialogText.sendMessage(m);
		

		if (lastId == null || lastId == 0)
			return;

		if (start != 0)
			lastId = lastId - start;

		if (lastId <= 0)
			return;

		MuteLog.Log("LatestMessages", "Update messages, lastId is " + lastId
				+ " and last is " + last + " (" + (lastId - last) + ")");
		for (Integer i = lastId; i > lastId - last; i--) {
			if (i <= 0)
				break;
			MuteswanMessage msg = null;

			
			
			//MuteLog.Log("LatestMessages", "Reading message " + i + " moreMessages " + moreMessages + " refreshing " + refreshing);
			if (Thread.currentThread().isInterrupted()) {
    			MuteLog.Log("LatestMessages", "Interrupted 1 " + Thread.currentThread().toString());
    			return;
    		}
			msg = r.getMsgFromDb(i.toString(),true);
			if (Thread.currentThread().isInterrupted()) {
    			MuteLog.Log("LatestMessages", "Interrupted 2 " + Thread.currentThread().toString());
    			return;
    		}
			

			if (msg == null) {
				//downloaded = true;
					m = new Message();
					b = new Bundle();
					b.putString("txt", "[" + r.getShortname() + "] downloading..." + i);
					m.setData(b);
					updateDialogText.sendMessage(m);
					setSpinneyDownloading.sendEmptyMessage(0);
					
				    if (!refreshing)
					   startSpinningHandler.sendEmptyMessage(0);

					try {
						MuteLog.Log("LatestMessages", "I am " + Thread.currentThread());
						msgService.downloadMsgRangeFromTor(Main.genHexHash(r.getFullText()), i, lastId - last);
					} catch (RemoteException e) {
						Log.e("LatestMessages", "Error downloading message " + i + " from msgService!");
					}
					
					if (Thread.currentThread().isInterrupted()) {
		    			MuteLog.Log("LatestMessages", "Interrupted after downloading " + Thread.currentThread().toString());
		    			return;
		    		}
					
					msg = r.getMsgFromDb(i.toString(),true);
					
					if (msg == null) {
						stopSpinningHandler.sendEmptyMessage(0);
						Log.e("LatestMessages", "Message " + i + " is still null after downloading it from service!");
						return;
					}
			
					
					// if we are interrupted, don't update msgs because we don't know what is going on
					if (Thread.currentThread().isInterrupted()) {
		    			return;
		    		}
				
				    //msg.verifySignatures(idStore);
				    msgs.add(msg);
				
				    // stop spinning if we are not refreshing to show we are done
				    if (!refreshing)
					   stopSpinningHandler.sendEmptyMessage(0);
				
			} else {
				// if we are interrupted, don't update msgs because we don't know what is going on
				if (Thread.currentThread().isInterrupted()) {
	    			return;
	    		}
				msgs.add(msg);
			}


		}

	}

	

	protected void setFooterText(String footerString) {
      	TextView footerText = (TextView) footerView.findViewById(R.id.latestmessagesFooterText);
      	footerText.setText(footerString);
		
	}
	
	protected CharSequence getFooterText() {
      	TextView footerText = (TextView) footerView.findViewById(R.id.latestmessagesFooterText);
      	return(footerText.getText());
		
	}

	public ArrayList<MuteswanMessage> getLatestMessages(
			ArrayList<MuteswanMessage> msgs, String circleHash, Integer first,
			Integer last) {
		updateLatestMessages(msgs, store.asHashMap().get(circleHash), first, last);
		return (msgs);
	}



	public ArrayList<MuteswanMessage> getLatestMessages(
			ArrayList<MuteswanMessage> msgs, Integer first, Integer amount) {

		
		MuteLog.Log("LatestMessages", "Refreshing: " + refreshing);
		
		// populate list from db initially
		MuteLog.Log("LatestMessages", "circleExtra is " + circleExtra);
		for (Circle r : store) {
			if (circleExtra != null && !circleMap.get(circleExtra).getFullText().equals(r.getFullText()))
				continue;
			updateLatestMessages(msgs, r, first, amount);
			updateMessageList.sendEmptyMessage(0);
		}
		
	    sortMessageList.sendEmptyMessage(0);
		
		// we are just loading more messages here, no need to check for new messages and potentially download. 
		if (moreMessages == true) {
			MuteLog.Log("LatestMessages", "messageViewCount is " + messageViewCount);
			moreMessages = false;
			return(msgs);
		}
		
		if (!TorStatus.haveNetworkConnection(this)) {
			setSpinneyAllFailed.sendEmptyMessage(0);
			return(null);
		}
		
		// get the latest message counts
		if (circleExtra == null) {
			for (Circle r : store) {
				oldThreads.add(getLastestMessageCountFromTor(r));
			
			}
		} else if (circleExtra != null) {
			Circle circle = circleMap.get(circleExtra);
			 oldThreads.add(getLastestMessageCountFromTor(circle));
		}
		
		while (newMsgCheckState.isEmpty()) {
		  try {
			    MuteLog.Log("LatestMessages", "Waiting for first population of check messages.");
				Thread.currentThread();
				Thread.sleep(250);
			  } catch (InterruptedException e) {
				  MuteLog.Log("LatestMessages", "Error: thread interrupted " + e.getMessage());
				  return(null);
			  }
		}
		
		boolean stillWorking = true;
		while (stillWorking) {
		
			
			if (Thread.currentThread().isInterrupted()) {
    			MuteLog.Log("LatestMessages", "Interrupted 2 " + Thread.currentThread().toString());
    			return(null);
    		}
			
			if (newMsgCheckState.isEmpty()) {
				stillWorking = false;
				continue;
			}
			
			String[] circleNewMsgs = popFirstDoneMsgCheck();
			
			if (Thread.currentThread().isInterrupted())
				return(null);
			
			
			if (circleNewMsgs != null) {
				
				Integer msgDelta = Integer.parseInt(circleNewMsgs[1]);
				if (msgDelta >= getMsgDownloadAmount())
					msgDelta = getMsgDownloadAmount();
				MuteLog.Log("LatestMessages", "CurCircle is " + circleNewMsgs[0] + " and delta is " + msgDelta);
				//dataSetChanged.sendEmptyMessage(0);
				if (msgDelta != 0) {
				  MuteLog.Log("LatestMessages", "Update latest messages because delta is " + msgDelta + " first is " + first);
				  try {
					msgService.downloadLatestMsgRangeFromTor(Main.genHexHash(store.asHashMap().get(circleNewMsgs[0]).getFullText()), msgDelta);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				  updateLatestMessages(msgs, store.asHashMap().get(circleNewMsgs[0]), 0, msgDelta);
				  updateMessageList.sendEmptyMessage(0);
				  //dataSetChanged.sendEmptyMessage(0);
				}
				
				
			} else {
			
			  try {
				Thread.currentThread();
				Thread.sleep(500);
			  } catch (InterruptedException e) {
				  MuteLog.Log("LatestMessages", "Error: thread interrupted " + e.getMessage());
				  return(null);
			  }
			}
			
			
			
		}
	
		
		
		dismissDialog.sendEmptyMessage(0);
		newMsgCheckState.clear();
		
		sortMessageList.sendEmptyMessage(0);
		//dataSetChanged.sendEmptyMessage(0);
		refreshing = false;

		return (msgs);
	}

	

	private String[] popFirstDoneMsgCheck() {
		
		String[] retVal = new String[2];
		
		for (String key : newMsgCheckState.keySet()) {
			String circle = key;
			String delta = newMsgCheckState.get(key).toString();
			//MuteLog.Log("LatestMessages", "popFirstDoneMsgCheck circle is " + key + " and delta is " + delta);
			if (delta != null &&  newMsgCheckState.get(key) != -1) {
				newMsgCheckState.remove(key);
				
				retVal[0] = circle;
				retVal[1] = delta;
				return retVal;
				//return(circle);
			}
		}
		
		return(null);
	}

	@Override
	public void run() {
		MuteLog.Log("LatestMessages","Running!");
		
		
        
	
		Intent serviceIntent = new Intent(this,NewMessageService.class);
		bindService(serviceIntent,msgServiceConn,Context.BIND_AUTO_CREATE);

		

		final int start = messageViewCount;
		
		
		// wait for initialization to settle
		Thread.currentThread();
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			return;
		}
		
		
		
		getLatestMessages(msgBucket, start,getMsgDownloadAmount());
		//updateMessageList.sendEmptyMessage(0);
	}

	private Thread getLastestMessageCountFromTor(final Circle circle) {

		
		
		//MuteLog.Log("LatestMessages","getLatestMessages got circle passed down " + circle.getShortname());
		

		
        Thread nThread = new Thread() {
        	
			public void run() {
        		if (Thread.currentThread().isInterrupted()) {
        			MuteLog.Log("LatestMessages", "Thread interrupted!");
        			return;
        		}
		
        		Message m = Message.obtain();
        		Bundle b = new Bundle();
        		
        		b.putString("circle", Main.genHexHash(circle.getFullText()));
        		b.putString("state", "starting");
        		m.setData(b);
        		MuteLog.Log("LatestMessages","Sending Message with value " + b.getString("circle") + " Current thread " + Thread.currentThread().toString());
        		newMsgCheckEventHandler.sendMessage(m);
        	
        		if (Thread.currentThread().isInterrupted()) {
        			MuteLog.Log("LatestMessages", "Interrupted 0.5 " + Thread.currentThread().toString());
        			return;
        		}
		
        		Integer prevLastMsgId = circle.getLastMsgId(true);
        		if (Thread.currentThread().isInterrupted()) {
        			MuteLog.Log("LatestMessages", "Interrupted 1 " + Thread.currentThread().toString());
        			return;
        		}
        	
        		Integer lastMsg = null;
				try {
					
					
					// on slow devices this may not be initialized yet
					while (msgService == null) {
							 try {
								Thread.currentThread().sleep(50);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
					}
       
					
					long before = System.currentTimeMillis();
					MuteLog.Log("NewMessageService", "IKF before getlasttormsgid");
					lastMsg = msgService.getLastTorMsgId(Main.genHexHash(circle.getFullText()));
					long after = System.currentTimeMillis();
					if (lastMsg != null && lastMsg == -1) {
					  MuteLog.Log("NewMessageService", "IKF " + circle.getShortname() + " "  + (after - before) + " after getlasttormsgid is -1");
					  lastMsg = msgService.getLastTorMsgId(Main.genHexHash(circle.getFullText()));
					}
				} catch (RemoteException e) {
					Log.e("LatestMessages", "Error getting latest message from service!");
					e.printStackTrace();
				}
        		
        		if (Thread.currentThread().isInterrupted()) {
        			MuteLog.Log("LatestMessages", "Interrupted 2 " + Thread.currentThread().toString());
        			return;
        		}
        			
        		
        		MuteLog.Log("LatestMessages", "Current thread " + Thread.currentThread().toString());
        		if (lastMsg != null && lastMsg >= 0) {
        			//circle.updateLastMessage(lastMsg,true);
        			try {
        				circle.setCurLastMsgId(lastMsg);
						msgService.updateLastMessage(Main.genHexHash(circle.getFullText()),lastMsg);
					} catch (RemoteException e) {
						MuteLog.Log("LatestMessages", "Error updating latest message using msgService!");
						return;
					}
        		
        			
        			if (prevLastMsgId == null)
        				prevLastMsgId = 0;
        			//BOOK
        			
        			Integer delta = lastMsg - prevLastMsgId;
        			Message m2 = Message.obtain();
        			Bundle b2 = new Bundle();
        			MuteLog.Log("LatestMessages","Circle " + Main.genHexHash(circle.getFullText()) + " has last message of: " + lastMsg + " and delta of " + delta);
        			b2.putString("circle", Main.genHexHash(circle.getFullText()));
        			b2.putString("state", "done");
        			b2.putInt("msgDelta", delta);
        			m2.setData(b2);
        			newMsgCheckEventHandler.sendMessage(m2);
        		} else {
        			Message m2 = Message.obtain();
        			Bundle b2 = new Bundle();
        			MuteLog.Log("LatestMessages","Circle failed to get last message: " + circle.getShortname());
        			b2.putString("circle", Main.genHexHash(circle.getFullText()));
        			b2.putString("state", "failed");
        			m2.setData(b2);
        			newMsgCheckEventHandler.sendMessage(m2);
        			
        		}
        		
        		
        	}
        };
	
          
          nThread.start();
          return(nThread);
        
	}
	
	public CompareDates getComparatorDates() {
		return comparatorDates;
	}


	private class TorNotAvailableReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	alertDialogs.dialogTorNotAvailable.sendEmptyMessage(0);
	    }
	}

	private TorNotAvailableReceiver torNotAvailableReceiver;
	
	
 
 
 private ServiceConnection msgServiceConn = new ServiceConnection() {
		public void onServiceConnected(ComponentName className,
             IBinder service) {
     	msgService = IMessageService.Stub.asInterface(service);
     	try {
				msgService.setSkipNextCheck(true);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
     	MuteLog.Log("LatestMessages", "onServiceConnected called.");
     	if (msgService == null) {
     		Log.e("LatestMessages", "msgService is null ");
     	}

     }

     public void onServiceDisconnected(ComponentName className) {
        msgService = null;
     }
 };
	

}
