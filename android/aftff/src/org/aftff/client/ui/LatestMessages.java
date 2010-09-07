package org.aftff.client.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.aftff.client.R;
import org.aftff.client.data.AftffMessage;
import org.aftff.client.data.Identity;
import org.aftff.client.data.IdentityStore;
import org.aftff.client.data.Ring;
import org.aftff.client.data.RingStore;
import org.aftff.client.data.RingStore.OpenHelper;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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

public class LatestMessages extends ListActivity {

	private Bundle extra;
	private int action;
	private RingStore store;
	ArrayList<AftffMessage> messageList;
	HashMap<String,Ring> ringMap;
	IdentityStore idStore;
	private LatestMessagesListAdapter listAdapter;
	private int messageViewCount;
	HashMap<View, AlertDialog> moreButtons;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
       

        //extra = getIntent().getExtras();
        //action = extra.getInt("action");
        
    	

        setContentView(R.layout.latestmessages);
        
       
        store = new RingStore(this,true);
		ringMap = store.asHashMap();
		idStore = new IdentityStore(this);
		
		messageViewCount = 20;
		moreButtons = new HashMap<View,AlertDialog>();
        messageList = loadRecentMessages();
        listAdapter = new LatestMessagesListAdapter(this);

        setListAdapter(listAdapter);
          
       
        
        
    }

	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.latestmessagesmenu, menu);
        return true;
    }
	
	public boolean onOptionsItemSelected(MenuItem item) {

		messageViewCount = messageViewCount + 20;
		//messageList.clear();
		messageList = loadRecentMessages(messageList, messageViewCount);
		listAdapter.notifyDataSetChanged();
		
		return true;
		
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
	
    private void listItemClicked(View v) {
    	Log.v("LatestMessages","List item clicked.");
    }
    
    private void showRing(TextView v) {
    	for (Ring r : store) {
    		if (r.getShortname().equals(v.getText().toString())) {
    			Intent intent = new Intent(this,MsgList.class);
    			intent.putExtra("ring", r.getFullText());
    			startActivity(intent);
    		}
    	}
    }
    
    
	public class LatestMessagesListAdapter extends BaseAdapter {

		
		private Context context;
		
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
      		  
      		  final AftffMessage msg = messageList.get(position);
      		  
      		  TextView txtRing = (TextView) layout.findViewById(R.id.android_latestmessagesRing);
      		  TextView txtDate = (TextView) layout.findViewById(R.id.android_latestmessagesDate);
      		  TextView txtMessage = (TextView) layout.findViewById(R.id.android_latestmessagesMessage);
      		  TextView txtSigs = (TextView) layout.findViewById(R.id.android_latestmessagesSignatures);
      		  ImageButton plusButton = (ImageButton) layout.findViewById(R.id.latestmessagesInfoButton);
      		  LinearLayout mainLayout = (LinearLayout) layout.findViewById(R.id.latestmessagesMainLayout);
      		
      		  
      		  
      		  // not used and painful
      		  //int totalWidth = layout.getLayoutParams().width;
      		  //int ringWidth = txtRing.getLayoutParams().width;
      		  //int mainWidth = mainLayout.getLayoutParams().width;
      	
      		final CharSequence[] items = {"Post", "Reply", "Reply with Quote", "Share"};

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Choose an action");
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                	if (items[item].equals("Post")) {
                		//Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_LONG).show();
                		Intent intent = new Intent(context,WriteMsg.class);
                		intent.putExtra("ring",msg.getRing().getFullText());
                		startActivity(intent);
                	} else if (items[item].equals("Reply")) {
                		Intent intent = new Intent(context,WriteMsg.class);
                		intent.putExtra("ring",msg.getRing().getFullText());
                		intent.putExtra("initialText", "@" + msg.getId() + ":\n");
                		startActivity(intent);
                	} else if (items[item].equals("Reply with Quote")) {
                		Intent intent = new Intent(context,WriteMsg.class);
                		intent.putExtra("ring",msg.getRing().getFullText());
                		String[] msgLines = msg.getMsg().split("\n");
                		StringBuilder initialText = new StringBuilder();
                		initialText.append("From @" + msg.getId() + "\n");
                		for (int i=0; i<msgLines.length; i++) {
                			initialText.append("> " + msgLines[i] + "\n");
                		}
                		initialText.append("\n");
                		intent.putExtra("initialText", initialText.toString());
                		startActivity(intent);
                	} else if (items[item].equals("Share")) {
                		Intent showQrcode = new Intent("com.google.zxing.client.android.ENCODE");
            			showQrcode.putExtra("ENCODE_DATA",msg.getRing().getFullText());
            			showQrcode.putExtra("ENCODE_TYPE", "TEXT_TYPE");
            			startActivity(showQrcode);
                	}
                    //Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
                }
            });
            AlertDialog alert = builder.create();
    		moreButtons.put(plusButton,alert);

            //alert.show();
      		  
      		  
      		  
      		  txtRing.setText(msg.getRing().getShortname());
      		  txtRing.setClickable(true);
      		  txtRing.setOnClickListener(showRing);
      		  txtDate.setText(msg.getDate());
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
      		  
			 plusButton.setImageResource(R.drawable.more);
			 plusButton.setClickable(true);
			 layout.setClickable(true);
			 layout.setOnClickListener(listItemClicked);
      		 plusButton.setOnClickListener(buttonClicked);
			 
      		 
      		 //layout.setMinimumHeight(320);
      		 
      		 return layout;	
		}
		
	}
	
	
	private ArrayList<AftffMessage> loadRecentMessages() {
		ArrayList<AftffMessage> msgs = new ArrayList<AftffMessage>();
		loadRecentMessages(msgs,20);
		return(msgs);
	}
	
	private ArrayList<AftffMessage> loadRecentMessages(ArrayList<AftffMessage> msgs) {
		loadRecentMessages(msgs,20);
		return(msgs);
	}

    private ArrayList<AftffMessage> loadRecentMessages(ArrayList<AftffMessage> msgs, Integer last) {
		
    	
    	
    	
		SQLiteDatabase db = store.getOpenHelper().getReadableDatabase();
		Log.v("LatestMessages", "Fetching messages from db...");
		//Cursor cursor = db.query(OpenHelper.MESSAGESTABLE, new String[] { "msgId", "ringHash", "date", "message" }, null, null, null, null, "date desc", "20" );
		Cursor cursor = db.query(OpenHelper.MESSAGESTABLE, new String[] { "msgId", "ringHash" }, null, null, null, null, "date desc", last.toString());
		
		if (msgs.size() != 0) {
			cursor.moveToPosition(msgs.size());
		}
		
		int count=0;
		while (cursor.moveToNext()) {
			
			//if (cursor.getPosition() > ) {
			//	newMsgs.add(msgs.get(count));
			//	continue;
			//}
			
			String msgId = cursor.getString(0);
			String ringHash = cursor.getString(1);
			//String date = cursor.getString(2);
			//String message = cursor.getString(3);
			Ring ring = ringMap.get(ringHash);
			if (ring == null) {
				Log.v("LatestMessages", "Ring for hash " + ringHash + " is null.");
				return(null);
			}
			//AftffMessage msg = new AftffMessage(ring,msgId,)
			//AftffMessage msg = ring.getMsgFromDb(msgId);
			//msgs[count] = new AftffMessage(ring,Integer.parseInt(msgId),date,message);
    		
			AftffMessage msg = ring.getMsgFromDb(msgId);
			msg.verifySignatures(idStore);
			msgs.add(msg);
			
			count++;
			Log.v("LatestMessages", "Got message " + msgId + " ring " + ring.getShortname());
		}
		cursor.close();
		db.close();
		
		
		
		return msgs;
	}
	
	
}
