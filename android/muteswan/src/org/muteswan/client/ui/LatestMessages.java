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
package org.muteswan.client.ui;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;

import org.apache.http.client.ClientProtocolException;
import org.muteswan.client.IMessageService;
import org.muteswan.client.R;
import org.muteswan.client.muteswan;
import org.muteswan.client.data.Circle;
import org.muteswan.client.data.CircleStore;
import org.muteswan.client.data.IdentityStore;
import org.muteswan.client.data.MuteswanMessage;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
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
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class LatestMessages extends ListActivity implements Runnable {

	protected static final int MSGDOWNLOADAMOUNT = 5;
	private Bundle extra;
	private String circleExtra;
	private CircleStore store;
	final ArrayList<MuteswanMessage> messageList = new ArrayList<MuteswanMessage>();
	HashMap<String,Circle> circleMap;
	IdentityStore idStore;
	private LatestMessagesListAdapter listAdapter;
	private int messageViewCount;
	HashMap<View, AlertDialog> moreButtons;
	private ProgressDialog gettingMsgsDialog;
	
	private HashMap<String, Integer> newMsgCheckState = new HashMap<String,Integer>();
	
	protected IMessageService newMsgService;
	private ImageView spinneyIcon;
	private boolean moreMessages = false;
	private RotateAnimation ranimnation;
	
	private final CompareDates comparatorDates = new CompareDates();
	
	
	public void onResume() {
		super.onResume();
		messageViewCount = 0;
		messageList.clear();
		refresh();
		
	}
	
	public void onDestroy() {
		super.onDestroy();
		gettingMsgsDialog = null;
		//if (newMsgService != null) {
		//	unbindService(mNewMsgConn);
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
			
			Log.v("LatestMessages", "lastInScreen " + lastInScreen + " and firstVisibleItem" + firstVisibleItem );
			
			if (lastInScreen == totalItemCount && newMsgCheckState.isEmpty()) {
				Log.v("LatestMessages", "End of list.");
				if (refreshing == false) {
					messageViewCount = messageViewCount + LatestMessages.MSGDOWNLOADAMOUNT;
					moreMessages = true;
					refresh();
				}
				//messageViewCount = messageViewCount + LatestMessages.MSGDOWNLOADAMOUNT;
				//refresh();
			}
			
		}
	};
	private boolean refreshing;
	private boolean verbose;
	private long previousRefreshTime = 0;
	
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
       

        store = new CircleStore(this,true);
		circleMap = store.asHashMap();
		idStore = new IdentityStore(this);
        
        extra = getIntent().getExtras();
        if (extra != null) 
         circleExtra = extra.getString("circle");
         
        setContentView(R.layout.latestmessages);

        
        if (circleExtra != null) {
		 TextView txtTitle = (TextView) findViewById(R.id.android_latestmessagesprompt);
		 txtTitle.setText("Messages for " + circleMap.get(circleExtra).getShortname());
		
        }
        

        SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		verbose = defPrefs.getBoolean("verbose", false);
		
        
		
		
	
        final Button postButton = (Button) findViewById(R.id.latestmessagesPost);
        postButton.setOnClickListener(postClicked);
        
        
        final ImageView refreshButton = (ImageView) findViewById(R.id.checkingMessagesIcon);
        refreshButton.setOnClickListener(refreshClicked);
        
		messageViewCount = 0;
		moreButtons = new HashMap<View,AlertDialog>();
       
		
        listAdapter = new LatestMessagesListAdapter(this,R.id.android_latestmessagesprompt);
        setListAdapter(listAdapter);
        
        
        View footerView = ((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.latestmessagesfooter, null, false);
        getListView().addFooterView(footerView);
        getListView().setOnScrollListener(scrollListener);
        
       
        
        

        
    }
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.latestmessagesmenu, menu);
        return true;
    }
	
	public boolean onOptionsItemSelected(MenuItem item) {

	
		
		messageViewCount = 0;
		messageList.clear();
		refresh();
		
		
		
		
		return true;
		
	}
	
	private void refresh() {
		
		if (previousRefreshTime != 0 && ((System.currentTimeMillis()/1000) - previousRefreshTime) < 1) {
			Log.v("LatestMessages", "Refresh called within 1 second, not running.");
			return;
		}
		
		Log.v("LatestMessages", "Refreshing at start of refresh(): " + refreshing);
		Log.v("LatestMessages", "moreMessages at start of refresh(): " + moreMessages);
		refreshing = true;
		previousRefreshTime = System.currentTimeMillis()/1000;
		Thread thread = new Thread(this);
		thread.start();
		spinIcon();
		spinneyIcon.setImageResource(R.drawable.refresh);
		if (verbose == true)
			showDialog();
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
    
    
   
	
    
    
    
	public HashMap<View, Integer> replyButtons = new HashMap<View,Integer>();
	public HashMap<View, Integer> repostButtons = new HashMap<View,Integer>();
    
    
    private void listItemClicked(View v) {
    	Log.v("LatestMessages","List item clicked.");
    }
    
    private void showCircle(TextView v) {
    	for (Circle r : store) {
    		if (r.getShortname().equals(v.getText().toString())) {
    			Intent intent = new Intent(this,LatestMessages.class);
    			intent.putExtra("circle", muteswan.genHexHash(r.getFullText()));
    			startActivity(intent);
    		}
    	}
    }
    
    public View.OnClickListener postClicked = new View.OnClickListener() {
    	public void onClick(View v) {
    		if (circleExtra != null) {
    		  Intent intent = new Intent(getApplicationContext(),WriteMsg.class);
    		  intent.putExtra("circle",circleMap.get(circleExtra).getFullText());
    		  startActivity(intent);
    		} else {
    		   Intent intent = new Intent(getApplicationContext(),WriteMsg.class);
      		  startActivity(intent);
    		}
    	}
    };
    
    public View.OnClickListener refreshClicked = new View.OnClickListener() {
    	public void onClick(View v) {
    		messageViewCount = 0;
    		messageList.clear();
    		refresh();
    	}
    };
    
	public class LatestMessagesListAdapter extends ArrayAdapter {

		
		//private Context context;
	
		
		 public LatestMessagesListAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
			
		}

		public View.OnClickListener replyClicked = new View.OnClickListener() {
		    	public void onClick(View v) {
		    		if (replyButtons.get(v) == null) {
		    		  Log.v("LatestMessages", "map is null");
		    		  return;
		    		}
		    		MuteswanMessage msg = messageList.get(replyButtons.get(v));
		    		Intent intent = new Intent(getApplicationContext(),WriteMsg.class);
		    		intent.putExtra("circle",msg.getCircle().getFullText());
		    		intent.putExtra("initialText","@" + msg.getId() + "\n");
		    		startActivity(intent);
		    	}
		 };
		    
		    public View.OnClickListener repostClicked = new View.OnClickListener() {
		    	public void onClick(View v) {
		    		MuteswanMessage msg = messageList.get(repostButtons.get(v));
		    		Intent intent = new Intent(getApplicationContext(),WriteMsg.class);
		    		intent.putExtra("circle",msg.getCircle().getFullText());
		    		startActivity(intent);
		    	}
		    };
		
		
		//public LatestMessagesListAdapter(Context context) {
		//	this.context = context;
			//this.messages = messages;
		//}
		
		@Override
		public int getCount() {
			return(messageList.size());
		}

		@Override
		public Object getItem(int position) {
			return(messageList.get(position));
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
      		  RelativeLayout layout = (RelativeLayout) getLayoutInflater().inflate(R.layout.latestmessagesentry,
      				  parent, false);
      		  
      		  final MuteswanMessage msg = messageList.get(position);
      		  
      		  TextView txtCircle = (TextView) layout.findViewById(R.id.android_latestmessagesCircle);
      		  TextView txtDate = (TextView) layout.findViewById(R.id.android_latestmessagesDate);
      		  TextView txtMessage = (TextView) layout.findViewById(R.id.android_latestmessagesMessage);
      		  //TextView txtSigs = (TextView) layout.findViewById(R.id.android_latestmessagesSignatures);
      		  
      		  
      		  //TextView txtReply = (TextView) layout.findViewById(R.id.android_latestmessagesReplyButton);
      		  TextView txtRepost = (TextView) layout.findViewById(R.id.android_latestmessagesRepostButton);


      		  //txtReply.setOnClickListener(replyClicked);
      		  txtRepost.setOnClickListener(repostClicked);
              //replyButtons.put(txtReply,position);
              repostButtons.put(txtRepost,position);
  
      		
      		  
      		  //txtCircle.setText(msg.getCircle().getShortname() + "/" + msg.getId());
              if (circleExtra == null) {
            	txtCircle.setText(msg.getCircle().getShortname());
      		  	txtCircle.setClickable(true);
      		  	txtCircle.setOnClickListener(showCircle);
			  } else {
				  layout.removeView(txtCircle);
			  }
      		//txtDate.setText("#"+msg.getId() + " " + msg.getDate());
      		txtDate.setText(msg.getDate());
      		
      		  txtMessage.setText(msg.getMsg());

      		  
      		  
      		  // not verifying signatures right now
      		  //String sigDataStr = "-- \n";
      		  //LinkedList<Identity> list = msg.getValidSigs();
			  //if (list == null) {
			  //	  sigDataStr = "";
			  //} else if (list.size() == 0) {
      		  //	  sigDataStr = "No valid signatures.";
      		  //} else {
      			  
      		  // for (Identity id : msg.getValidSigs()) {
      		  //	    sigDataStr = sigDataStr + id.getName() + "\n";
      		  // }
      		   
      		  //txtSigs.setText(sigDataStr);
      		  //}
      		  
			
			 layout.setClickable(true);
			 layout.setOnClickListener(listItemClicked);
			 
      		 // images are not shown right now, uncomment to show them
			 //if (msg.getCircle().getImage() != null) {
      		//	 ImageView imageView = (ImageView) layout.findViewById(R.id.latestmessagesImage);
      		//	 imageView.setImageBitmap(BitmapFactory.decodeByteArray(msg.getCircle().getImage(), 0, msg.getCircle().getImage().length));
      		//	 //layout.addView(imageView);
      		// } else {
      		//	 ImageView imageView = (ImageView) layout.findViewById(R.id.latestmessagesImage);
      		//	 layout.removeView(imageView);
      		// }
			 
      		       		 
      		 return layout;	
		}
		
	}
	
	
	

	
	final Handler newMsgCheckEventHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle b = msg.getData();
			
			Log.v("LatestMessages", "Setting circle to " + b.getString("circle") + " and state to " + b.getString("state"));
			if (b.getString("circle") != null && b.getString("state") != null) {
				Integer msgDelta = b.getInt("msgDelta");
				if (!b.getString("state").equals("done"))
				 newMsgCheckState.put(b.getString("circle"),-1);
				
				
				
				if (b.getString("state").equals("starting")) {
					spinneyIcon.setImageResource(R.drawable.refresh_checking);
				} else if (b.getString("state").equals("done")) {
					newMsgCheckState.put(b.getString("circle"),msgDelta);;
					spinneyIcon.setImageResource(R.drawable.refresh_downloading);
				}
				if (gettingMsgsDialog != null) {
					gettingMsgsDialog.setMessage(circleMap.get(b.getString("circle")).getShortname() + ": checking for new messages.");
				}
			}
			
			
			
		}
	};

	final Handler dismissDialog = new Handler() {
		
		
		
        @Override
        public void handleMessage(Message msg) {
        	  if (gettingMsgsDialog != null)
        	    gettingMsgsDialog.dismiss();
        	  if (spinneyIcon != null) {
        		  spinneyIcon.setImageResource(R.drawable.refresh_done);
        		  //ranimnation.cancel();
        		  spinneyIcon.setAnimation(null);
        	  }
        }
    };
	
    
    
    final Handler dataSetChanged = new Handler() {
		
        @Override
        public void handleMessage(Message msg) {
        		//listAdapter.sort(new CompareDates());
        		
        	    listAdapter.notifyDataSetChanged();
        }
    };
	
    
    final Handler sortMessageList = new Handler() {
		
        @Override
        public void handleMessage(Message msg) {
        		//listAdapter.sort(new CompareDates());
        		
        		Collections.sort(messageList, comparatorDates);
        		
        		
        		Log.v("LatestMessages", "sortMessageList handler refreshing: " + refreshing);
        		
        		listAdapter.notifyDataSetChanged();
        		refreshing = false;
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
	
	

    
    class CompareDates implements Comparator
    {
        public int compare(Object obj1, Object obj2)
        {
        	MuteswanMessage msg1 = (MuteswanMessage) obj1;
        	MuteswanMessage msg2 = (MuteswanMessage) obj2;
        	
        	Log.v("LatestMessages", "Comparing " + msg1.getId() + " with " + msg2.getId());
        	
        	SimpleDateFormat df = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss");
        	Date mDate = null;
        	Date oDate = null;
			try {
				mDate = df.parse(msg1.getDate());
				oDate = df.parse(msg2.getDate());
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if (mDate == null || oDate == null) {
				return 0;
			}
			
			
			if (mDate.after(oDate)) {
				return -1;
			}
			else {
				return 1;
			
			}        	
			
        }
    }

    
	
	private void updateLatestMessages(ArrayList<MuteswanMessage> msgs, Circle r,
			Integer start, Integer last) {
		IdentityStore idStore = new IdentityStore(this);
		
		
		Log.v("LatestMessages","circle " + r.getShortname());
		
		
		Integer lastId = r.getLastMsgId();
		
		
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

		Log.v("LatestMessages", "Update messages, lastId is " + lastId
				+ " and last is " + last + " (" + (lastId - last) + ")");
		for (Integer i = lastId; i > lastId - last; i--) {
			if (i <= 0)
				break;
			MuteswanMessage msg = null;

			msg = r.getMsgFromDb(i.toString());

			if (msg == null) {
				//downloaded = true;
				try {
					m = new Message();
					b = new Bundle();
					b.putString("txt", "[" + r.getShortname() + "] downloading..." + i);
					m.setData(b);
					updateDialogText.sendMessage(m);
					setSpinneyDownloading.sendEmptyMessage(0);
					msg = r.getMsgFromTor(i.toString());
					if (msg != null && msg.signatures[0] != null) {
						r.saveMsgToDb(i, msg.getDate(), msg.getMsg(),
								msg.signatures);
					} else if (msg != null) {
						r.saveMsgToDb(i, msg.getDate(), msg.getMsg());
					}
					// r.saveMsgToDb(i, msg.getDate(), msg)
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				msg.verifySignatures(idStore);
				msgs.add(0,msg);
				
			} else {
				msg.verifySignatures(idStore);
				Log.v("LatestMessages", "Adding " + msg.getId());
				msgs.add(0,msg);
			}


		}

	}

	

	public ArrayList<MuteswanMessage> getLatestMessages(
			ArrayList<MuteswanMessage> msgs, String circleHash, Integer first,
			Integer last) {
		updateLatestMessages(msgs, store.asHashMap().get(circleHash), first, last);
		return (msgs);
	}



	public ArrayList<MuteswanMessage> getLatestMessages(
			ArrayList<MuteswanMessage> msgs, Integer first, Integer amount) {

		
		Log.v("LatestMessages", "Refreshing: " + refreshing);
		
		// populate list from db initially
		Log.v("LatestMessages", "circleExtra is " + circleExtra);
		for (Circle r : store) {
			if (circleExtra != null && !circleMap.get(circleExtra).getFullText().equals(r.getFullText()))
				continue;
			updateLatestMessages(msgs, r, first, amount);
		}
		
		
		// we are just loading more messages here, no need to check again. the user can use the refresh button
		// for that
		if (moreMessages == true) {
			Log.v("LatestMessages", "messageViewCount is " + messageViewCount);
			dismissDialog.sendEmptyMessage(0);
			sortMessageList.sendEmptyMessage(0);
			//dataSetChanged.sendEmptyMessage(0);
			//refreshing = false;
			moreMessages = false;
			return(msgs);
		} else {
			sortMessageList.sendEmptyMessage(0);
			// AGHH FIXME please
			refreshing = true;
			//dataSetChanged.sendEmptyMessage(0);
		}
		
		
		
		// get the latest message counts
		if (circleExtra == null) {
			for (Circle r : store) {
				getLastestMessageCountFromTor(r);
			
			}
		} else if (circleExtra != null) {
			Circle circle = circleMap.get(circleExtra);
			 getLastestMessageCountFromTor(circle);
		}
		
		while (newMsgCheckState.isEmpty()) {
		  try {
			    Log.v("LatestMessages", "Waiting for first population of check messages.");
				Thread.currentThread().sleep(250);
			  } catch (InterruptedException e) {
				  Log.e("LatestMessages", "Error: thread interrupted " + e.getMessage());
			  }
		}
		
		boolean stillWorking = true;
		while (stillWorking) {
			
			if (newMsgCheckState.isEmpty()) {
				stillWorking = false;
				continue;
			}
			
			String[] circleNewMsgs = popFirstDoneMsgCheck();
			
			if (circleNewMsgs != null) {
				Log.v("LatestMessages", "CurCircle is " + circleNewMsgs[0] + " and delta is " + circleNewMsgs[1]);
				//dataSetChanged.sendEmptyMessage(0);
				if (Integer.parseInt(circleNewMsgs[1]) != 0) {
				  Log.v("LatestMessages", "Update latest messages because delta is " + circleNewMsgs[1] + " first is " + first);
				  updateLatestMessages(msgs, store.asHashMap().get(circleNewMsgs[0]), 0, Integer.parseInt(circleNewMsgs[1]));
				  dataSetChanged.sendEmptyMessage(0);
				}
				
				
			} else {
			
			  try {
				Thread.currentThread().sleep(500);
			  } catch (InterruptedException e) {
				  Log.e("LatestMessages", "Error: thread interrupted " + e.getMessage());
			  }
			}
			
			
			
		}
	
		
		newMsgCheckState.clear();
		
		dismissDialog.sendEmptyMessage(0);
		sortMessageList.sendEmptyMessage(0);
		//dataSetChanged.sendEmptyMessage(0);
		//refreshing = false;

		return (msgs);
	}

	

	private String[] popFirstDoneMsgCheck() {
		
		String[] retVal = new String[2];
		
		for (String key : newMsgCheckState.keySet()) {
			String circle = key;
			String delta = newMsgCheckState.get(key).toString();
			//Log.v("LatestMessages", "popFirstDoneMsgCheck circle is " + key + " and delta is " + delta);
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
		Log.v("LatestMessages","Running!");
		
		
	
		
		

		final int start = messageViewCount;
		getLatestMessages(messageList, start,LatestMessages.MSGDOWNLOADAMOUNT);
	
		
	
		
	}

	private void getLastestMessageCountFromTor(final Circle circle) {

		
		
		Log.v("LatestMessages","getLatestMessages got circle passed down " + circle.getShortname());
		

		
        Thread nThread = new Thread() {
        	@SuppressWarnings("static-access")
			public void run() {
		
        		Log.v("LatestMessages",Thread.currentThread().getName() + " is launched!");
        		
        		try {
					Thread.currentThread().sleep(2000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        		
        		Message m = Message.obtain();
        		Bundle b = new Bundle();
        		
        		b.putString("circle", muteswan.genHexHash(circle.getFullText()));
        		b.putString("state", "starting");
        		m.setData(b);
        		Log.v("LatestMessages","Sending Message with value " + b.getString("circle"));
        		newMsgCheckEventHandler.sendMessage(m);
        		
        		
        	
		
        		Integer prevLastMsgId = circle.getLastMsgId();
        		Integer lastMsg = circle.getLastTorMessageId();
        		if (lastMsg != null ) {
        			circle.updateLastMessage(lastMsg);
        		}
        		
        		Integer delta = lastMsg - prevLastMsgId;
        		Message m2 = Message.obtain();
        		Bundle b2 = new Bundle();
        		Log.v("LatestMessages","Circle has last message of: " + circle.getLastMsgId() + " and delta of " + delta);
        		b2.putString("circle", muteswan.genHexHash(circle.getFullText()));
        		b2.putString("state", "done");
        		b2.putInt("msgDelta", delta);
        		m2.setData(b2);
        		newMsgCheckEventHandler.sendMessage(m2);
        		
        	}
        };
	
        
          nThread.start();
        
	}
    
}
