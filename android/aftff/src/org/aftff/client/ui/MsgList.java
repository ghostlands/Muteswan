package org.aftff.client.ui;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.aftff.client.R;
import org.aftff.client.aftff;
import org.aftff.client.R.id;
import org.aftff.client.R.layout;
import org.aftff.client.R.menu;
import org.aftff.client.data.Identity;
import org.aftff.client.data.IdentityStore;
import org.aftff.client.data.AftffMessage;
import org.aftff.client.data.Ring;
import org.aftff.client.data.RingStore;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import com.google.zxing.client.a.r;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MsgList extends ListActivity implements Runnable {

	private static final int MAX_MESSAGES = 50;
	
	//private String[] mStrings = { "1", "2", "3", "4", "5", "6","7","8","9","10","11","12","13" };
	private ArrayAdapter listAdapter;
	private EditText msgPostField;
	
	private String[] msgList;
	
	private List<Integer> seenMsgs = new LinkedList();
	Ring ring;
	
	IdentityStore idStore;
	ProgressDialog loadIndexDialog;
	ProgressDialog loadMsgDialog;
	Integer lastMessage;
	TextView msglistPrompt;

	private AftffMessage lastMsg;

	
	public void run() {
		if (loadIndexDialog != null && loadIndexDialog.isShowing()) {  
		   lastMessage = ring.getMsgIndex();
		   dialogIndexHandler.sendEmptyMessage(0);
		} else if (loadMsgDialog != null && loadMsgDialog.isShowing()) {
			// FIXME: fix msgId garbage
			String msgId = msgList[lastPosition].replace("\n", "");
			try {
				lastMsg = ring.getMsg(msgId);
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    dialogMsgHandler.sendEmptyMessage(0);
		}
	}
	
	private Handler dialogIndexHandler = new Handler() {
		
        @Override
        public void handleMessage(Message msg) {
              	loadIndexDialog.dismiss();
              	renderList();
        }
    };
    
    private Handler dialogMsgHandler = new Handler() {
		
        @Override
        public void handleMessage(Message msg) {
              	loadMsgDialog.dismiss();
              	renderMsg();
        }
    };

	private int lastPosition;
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       
       Bundle extras = getIntent().getExtras();
       ring = new Ring(getApplicationContext(),extras.getString("ring"));
       
       loadIndexDialog = ProgressDialog.show(this, "", "Downloading message list...", true);
       Thread thread = new Thread(this);
       thread.start();

       setContentView(R.layout.msglist);
       
       idStore = new IdentityStore(getApplicationContext());
       EditText msgPostField = (EditText) findViewById(R.id.android_newMsgTextInline);
       
       msglistPrompt = (TextView) findViewById(R.id.android_msglistprompt);
       msglistPrompt.setText(ring.getShortname());

       if (ring == null) {
    	 msglistPrompt.setText("active ring is null...");
    	 return;
       }
       
       
      //lastMessage = ring.getMsgIndex();
	}
     
	private void renderList() {
      if (lastMessage == 0 || lastMessage == null) {
          msglistPrompt.setText("No messages for " + ring.getShortname());
  	       return;
      }
      
//      } else {
//        msglistPrompt.setText(ring.getShortname());
//      }
//      
      
      if (lastMessage < MAX_MESSAGES) {
    	  msgList = new String[lastMessage];
      } else {
    	  msgList = new String[MAX_MESSAGES];
      }
      
      int newIndex = 0;
      for (Integer i = lastMessage; i>0; i--) {
    	  
    	  if (newIndex == MAX_MESSAGES) {
    		  break;
    	  }
    	  
    	  AftffMessage msg = ring.getMsgFromDb(i.toString());
    	  
    	  
    	  
    	  if (msg != null) {
    		  String msgStr;
    		  
    		  LinkedList<Identity> validSigs = msg.verifySignatures(idStore);
  			  if (validSigs != null && validSigs.size() != 0) {
  				msgStr = i + " - " + msg.getDate() + "\n" + msg.getMsg();
  				for (Identity identity : validSigs) {
  					msgStr = msgStr + "\n" + "s: " + identity.getName();
  				}
  				msgList[newIndex] = msgStr;
  			   } else {
  				msgList[newIndex] = i.toString() + " - " + msg.getDate() + "\n" + msg.getMsg();
  	           }
    		  
    		  
  			  seenMsgs.add(newIndex);
    	  } else {
    	      msgList[newIndex] = i.toString();
    	  }
    	  newIndex++;
      }

      
       
       // Use an existing ListAdapter that will map an array
       // of strings to TextViews
       listAdapter = new ArrayAdapter<String>(this,
               android.R.layout.simple_list_item_1, msgList);
       setListAdapter(listAdapter);
    }
	
	
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.msglistmenu, menu);
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        //setContentView(R.layout.writemsg);
		
		
		//export ring to jpg using zxing
		if (item.getTitle().toString().equals("Export Ring")) {
			Intent showQrcode = new Intent("com.google.zxing.client.android.ENCODE");
			showQrcode.putExtra("ENCODE_DATA",ring.getFullText());
			showQrcode.putExtra("ENCODE_TYPE", "TEXT_TYPE");
			startActivity(showQrcode);
			return true;
		} else if (item.getTitle().toString().equals("Delete Ring")) {
			SharedPreferences prefs = getSharedPreferences(aftff.PREFS,0);
			RingStore store = new RingStore(prefs);
			store.deleteRing(ring, prefs);
			Toast.makeText(this,
					"Deleted ring " + ring.getShortname() + " from saved keys.", 
						  Toast.LENGTH_LONG).show();
			return true;
		} else {
			
		 // new message / FIXME: should be clearer
		 EditText newMsgText = (EditText) findViewById(R.id.android_newMsgTextInline);
		 Editable txt = newMsgText.getText();
    	 String txtData = txt.toString();
    	 if (txtData.getBytes().length != 0) {	
    	  try {
			ring.postMsg(txtData);
			Toast.makeText(this, 
				"Posted message to " + ring.getShortname(), 
					  Toast.LENGTH_LONG).show();
			newMsgText.setText("");
			return true;
			
		  } catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return true;
		  } catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return true;
		  } catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return true;
		  } catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return true;
		  } catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return true;
		}
    	 } else {
    	   Intent intent = new Intent(this,WriteMsg.class);
    	   Bundle extr = intent.getExtras();
    	   extr.putString("ring",ring.getFullText());
		   startActivity(intent);
		   return true;
	     }
		}
	}
	
	 public void onListItemClick(
		ListView parent, View v,
	    int position, long id) 
	 	{
		  
		 for (int i : seenMsgs) {
			 if (i == position) {
				 return;
			 }
		 }
		 
		  lastPosition = position;
	      loadMsgDialog = ProgressDialog.show(this, "", "Downloading message " + msgList[position].replace("\n", ""), true);
	      Thread thread = new Thread(this);
	      thread.start();
		 
	 	}
	 
	 private void renderMsg() {
	      String msgId = msgList[lastPosition].replace("\n", "");

		LinkedList<Identity> validSigs = lastMsg.verifySignatures(idStore);
		if (validSigs != null && validSigs.size() != 0) {
			String msgStr = msgId + " - " + lastMsg.getDate() + "\n" + lastMsg.getMsg();
			for (Identity identity : validSigs) {
				msgStr = msgStr + "\n" + "s: " + identity.getName();
			}
			msgList[lastPosition] = msgStr;
		} else {
			msgList[lastPosition] = msgId + " - " + lastMsg.getDate() + "\n" + lastMsg.getMsg();
		}
		seenMsgs.add(lastPosition);
		
		
		listAdapter.notifyDataSetChanged();
	 }  

}