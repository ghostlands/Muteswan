package org.muteswan.client.ui;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.muteswan.client.R;
import org.muteswan.client.muteswan;
import org.muteswan.client.data.MuteswanMessage;
import org.muteswan.client.data.Identity;
import org.muteswan.client.data.IdentityStore;
import org.muteswan.client.data.Ring;
import org.muteswan.client.data.RingStore;
import org.muteswan.client.data.RingStore.OpenHelper;
import org.apache.http.client.ClientProtocolException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.net.ParseException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

public class LatestMessages extends ListActivity implements Runnable {

	private Bundle extra;
	private String ringExtra;
	private RingStore store;
	final ArrayList<MuteswanMessage> messageList = new ArrayList<MuteswanMessage>();
	HashMap<String,Ring> ringMap;
	IdentityStore idStore;
	private LatestMessagesListAdapter listAdapter;
	private int messageViewCount;
	HashMap<View, AlertDialog> moreButtons;
	private ProgressDialog gettingMsgsDialog;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
       

        store = new RingStore(this,true);
		ringMap = store.asHashMap();
		idStore = new IdentityStore(this);
        
        extra = getIntent().getExtras();
        if (extra != null) 
         ringExtra = extra.getString("ring");
         
        setContentView(R.layout.latestmessages);

        
        if (ringExtra != null) {
		 TextView txtTitle = (TextView) findViewById(R.id.android_latestmessagesprompt);
		 txtTitle.setText("Messages for " + ringMap.get(ringExtra).getShortname());
		 //txtTitle.setText("Messages for " + ringExtra);
		 //txtTitle.setText("ugh");
        }
        

        
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.customtitlebar);
		TextView postButton = (TextView) findViewById(R.id.latestmessagesTitlePostButton);
		postButton.setOnClickListener(postClicked);
        
        
		
		messageViewCount = 0;
		moreButtons = new HashMap<View,AlertDialog>();
        //messageList = loadRecentMessages(messageList,0,5);
		//this.run();
		
        listAdapter = new LatestMessagesListAdapter(this);
        setListAdapter(listAdapter);
        
        Thread thread = new Thread(this);
        thread.start();
        showDialog();

        
    }
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.latestmessagesmenu, menu);
        return true;
    }
	
	public boolean onOptionsItemSelected(MenuItem item) {

		
		Integer start = messageViewCount;
		messageViewCount = messageViewCount + 5;
		//messageList.clear();
		Log.v("LatestMessages", "Start is " + start);
		//loadRecentMessages(messageList,start,5);
		Thread thread = new Thread(this);
		thread.start();
		showDialog();		
		return true;
		
	}

	private void showDialog() {
		gettingMsgsDialog = ProgressDialog.show(this, "", "Getting messages...", true);
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
	
    public View.OnClickListener showRing = new View.OnClickListener() {
    	public void onClick(View v) {
    		showRing((TextView) v);
    	}
    };
    
    
   
	
    
    
    
	public HashMap<View, Integer> replyButtons = new HashMap<View,Integer>();
	public HashMap<View, Integer> repostButtons = new HashMap<View,Integer>();
    
    
    private void listItemClicked(View v) {
    	Log.v("LatestMessages","List item clicked.");
    }
    
    private void showRing(TextView v) {
    	for (Ring r : store) {
    		if (r.getShortname().equals(v.getText().toString())) {
    			Intent intent = new Intent(this,LatestMessages.class);
    			intent.putExtra("ring", muteswan.genHexHash(r.getFullText()));
    			startActivity(intent);
    		}
    	}
    }
    
    public View.OnClickListener postClicked = new View.OnClickListener() {
    	public void onClick(View v) {
    		if (ringExtra != null) {
    		  Intent intent = new Intent(getApplicationContext(),WriteMsg.class);
    		  intent.putExtra("ring",ringMap.get(ringExtra).getFullText());
    		  startActivity(intent);
    		} else {
    		   Intent intent = new Intent(getApplicationContext(),RingList.class);
      		  intent.putExtra("action",RingList.WRITE);
      		  startActivity(intent);
    		}
    	}
    };
    
    
	public class LatestMessagesListAdapter extends BaseAdapter {

		
		private Context context;
	
		
		 public View.OnClickListener replyClicked = new View.OnClickListener() {
		    	public void onClick(View v) {
		    		if (replyButtons.get(v) == null) {
		    		  Log.v("LatestMessages", "map is null");
		    		  return;
		    		}
		    		MuteswanMessage msg = messageList.get(replyButtons.get(v));
		    		Intent intent = new Intent(getApplicationContext(),WriteMsg.class);
		    		intent.putExtra("ring",msg.getRing().getFullText());
		    		intent.putExtra("initialText","@" + msg.getId() + "\n");
		    		startActivity(intent);
		    	}
		 };
		    
		    public View.OnClickListener repostClicked = new View.OnClickListener() {
		    	public void onClick(View v) {
		    		MuteswanMessage msg = messageList.get(repostButtons.get(v));
		    		Intent intent = new Intent(getApplicationContext(),RingList.class);
		    		intent.putExtra("ring",msg.getRing().getFullText());
		    		intent.putExtra("initialText",msg.getMsg());
		    		intent.putExtra("action",RingList.WRITE);
		    		startActivity(intent);
		    	}
		    };
		
		    
		
		public LatestMessagesListAdapter(Context context) {
			this.context = context;
			//this.messages = messages;
		}
		
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
      		  
      		  TextView txtRing = (TextView) layout.findViewById(R.id.android_latestmessagesRing);
      		  TextView txtDate = (TextView) layout.findViewById(R.id.android_latestmessagesDate);
      		  TextView txtMessage = (TextView) layout.findViewById(R.id.android_latestmessagesMessage);
      		  TextView txtSigs = (TextView) layout.findViewById(R.id.android_latestmessagesSignatures);
      		  
      		  
      		  TextView txtReply = (TextView) layout.findViewById(R.id.android_latestmessagesReplyButton);
      		  TextView txtRepost = (TextView) layout.findViewById(R.id.android_latestmessagesRepostButton);


      		  txtReply.setOnClickListener(replyClicked);
      		  txtRepost.setOnClickListener(repostClicked);
              replyButtons.put(txtReply,position);
              repostButtons.put(txtRepost,position);

      		  
      		  // not used and painful
      		  //int totalWidth = layout.getLayoutParams().width;
      		  //int ringWidth = txtRing.getLayoutParams().width;
      		  //int mainWidth = mainLayout.getLayoutParams().width;	  
      		  
      		  txtRing.setText(msg.getRing().getShortname() + "/" + msg.getId());
      		  txtRing.setClickable(true);
      		  txtRing.setOnClickListener(showRing);
      		  txtDate.setText(msg.getDate());
      		  String white = "";
      		  // HAH WTF! fix this, please FIXME
      		  //for (int i=0; i<msg.getRing().getShortname().length()-1; i++) {
      			//  white = white + "   ";
      		  //}
      		  
      		  //txtMessage.setText(white + "/" + msg.getId() + ": " + msg.getMsg());
      		  txtMessage.setText(msg.getMsg());

      		  
      		  String sigDataStr = "-- \n";
      		  LinkedList<Identity> list = msg.getValidSigs();
			  if (list == null) {
				  sigDataStr = "";
			  } else if (list.size() == 0) {
      			  sigDataStr = "No valid signatures.";
      		  } else {
      			  
      		   for (Identity id : msg.getValidSigs()) {
      			    sigDataStr = sigDataStr + id.getName() + "\n";
      		   }
      		   
      		  txtSigs.setText(sigDataStr);
      		  }
      		  
			
			 layout.setClickable(true);
			 layout.setOnClickListener(listItemClicked);
			 
      		 // images are not showed right now, uncomment to show them
			 //if (msg.getRing().getImage() != null) {
      		//	 ImageView imageView = (ImageView) layout.findViewById(R.id.latestmessagesImage);
      		//	 imageView.setImageBitmap(BitmapFactory.decodeByteArray(msg.getRing().getImage(), 0, msg.getRing().getImage().length));
      		//	 //layout.addView(imageView);
      		// } else {
      			 ImageView imageView = (ImageView) layout.findViewById(R.id.latestmessagesImage);
      			 layout.removeView(imageView);
      		// }
			 
      		       		 
      		 return layout;	
		}
		
	}
	
	
	
	private ArrayList<MuteswanMessage> loadRecentMessages(
			ArrayList<MuteswanMessage> msgs, Integer first, Integer last) {

		if (ringExtra != null) {
			return (getLatestMessages(msgs, ringExtra, first, last));
		} else {
			return (getLatestMessages(msgs, first, last));
		}

		
	}


	final Handler dismissDialog = new Handler() {
		
        @Override
        public void handleMessage(Message msg) {
        	    gettingMsgsDialog.dismiss();
        }
    };
	
    final Handler dataSetChanged = new Handler() {
		
        @Override
        public void handleMessage(Message msg) {
        	    listAdapter.notifyDataSetChanged();
        }
    };
	
    final Handler updateDialogText = new Handler() {
    	
    	@Override
    	public void handleMessage(Message msg) {
    		gettingMsgsDialog.setMessage(msg.getData().getString("txt"));
    	}
    };
    
	
	private void updateLatestMessages(ArrayList<MuteswanMessage> msgs, Ring r,
			Integer start, Integer last) {
		IdentityStore idStore = new IdentityStore(this);
		Integer lastId = r.getLastMessageId();

		if (lastId == null || lastId == 0)
			return;

		if (start != 0)
			lastId = lastId - start;

		if (lastId <= 0)
			return;

		Log.v("RingStore", "Update messages, lastId is " + lastId
				+ " and last is " + last + " (" + (lastId - last) + ")");
		RING: for (Integer i = lastId; i > lastId - last; i--) {
			if (i <= 0)
				break;
			MuteswanMessage msg = null;
			Message m = new Message();
			Bundle b = new Bundle();
			b.putString("txt", "Loading....." + i);
			m.setData(b);
			updateDialogText.sendMessage(m);
			msg = r.getMsgFromDb(i.toString());

			if (msg == null && ringExtra != null) {
				try {
					m = new Message();
					b = new Bundle();
					b.putString("txt", "Downloading..." + i);
					m.setData(b);
					updateDialogText.sendMessage(m);
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
			}

			if (msg != null) {
				Log.v("RingStore", msg.getId() + " loaded.");
				msg.verifySignatures(idStore);

				if (msgs.size() == 0) {
					msgs.add(msg);
					continue RING;
				}

				// figure out where to insert the msg such that msgs is ordered
				// by date
				Integer insertIndex = msgs.size() - 1;
				for (int j = msgs.size() - 1; j >= 0; j--) {
					SimpleDateFormat df = new SimpleDateFormat(
							"yyyy-MM-dd HH:mm:ss");
					MuteswanMessage omsg = msgs.get(j);

					try {
						Date mDate = df.parse(msg.getDate());
						Date oDate = df.parse(omsg.getDate());
						if (mDate.after(oDate)) {
							insertIndex = j;

							// break;
						} else {

							break;
						}

					} catch (java.text.ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				// Log.v("RingStore", "insertIndex is " + insertIndex);
				if (msgs.size() - 1 == insertIndex) {
					msgs.add(msg);
				} else {
					msgs.add(insertIndex, msg);
				}

			}
		}

	}

	public ArrayList<MuteswanMessage> getLatestMessages(String ringHash,
			Integer first, Integer last) {
		ArrayList<MuteswanMessage> msgs = new ArrayList<MuteswanMessage>();
		updateLatestMessages(msgs, store.asHashMap().get(ringHash), first, last);
		return (msgs);
	}

	public ArrayList<MuteswanMessage> getLatestMessages(
			ArrayList<MuteswanMessage> msgs, String ringHash, Integer first,
			Integer last) {
		updateLatestMessages(msgs, store.asHashMap().get(ringHash), first, last);
		return (msgs);
	}

	public ArrayList<MuteswanMessage> getLatestMessages(Integer first, Integer last) {
		ArrayList<MuteswanMessage> msgs = new ArrayList<MuteswanMessage>();

		for (Ring r : store) {
			updateLatestMessages(msgs, r, first, last);
		}

		return (msgs);
	}

	public ArrayList<MuteswanMessage> getLatestMessages(
			ArrayList<MuteswanMessage> msgs, Integer first, Integer amount) {

		for (Ring r : store) {
			updateLatestMessages(msgs, r, first, amount);
		}

		return (msgs);
	}


	@Override
	public void run() {
		Log.v("LatestMessages","Running!");
		
		Ring ring = ringMap.get(ringExtra);
		if (ring != null) {
			ring.updateLastMessage(ring.getMsgIndex());
		}
		
		final int start = messageViewCount;
		loadRecentMessages(messageList,start,5);
		dismissDialog.sendEmptyMessage(0);
		dataSetChanged.sendEmptyMessage(0);
		Log.v("LatestMessages","Not running!");
	}
	
    
    
       
    
}
