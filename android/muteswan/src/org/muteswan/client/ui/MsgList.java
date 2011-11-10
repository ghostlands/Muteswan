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
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.muteswan.client.R;
import org.muteswan.client.muteswan;
import org.muteswan.client.R.id;
import org.muteswan.client.R.layout;
import org.muteswan.client.R.menu;
import org.muteswan.client.data.Identity;
import org.muteswan.client.data.IdentityStore;
import org.muteswan.client.data.MuteswanMessage;
import org.muteswan.client.data.Circle;
import org.muteswan.client.data.CircleStore;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;


import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.util.Log;
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
	Circle circle;
	
	IdentityStore idStore;
	ProgressDialog loadIndexDialog;
	ProgressDialog loadMsgDialog;
	Integer lastMessageIndex;
	TextView msglistPrompt;

	private MuteswanMessage lastMsg;

	
	public void run() {
		if (loadIndexDialog != null && loadIndexDialog.isShowing()) {
		   lastMessageIndex = circle.getLastTorMessageId();
		   circle.updateLastMessage(lastMessageIndex);
		   //circle.saveLastMessage();
		   dialogIndexHandler.sendEmptyMessage(0);
		} else if (loadMsgDialog != null && loadMsgDialog.isShowing()) {
			// FIXME: fix msgId garbage
			String msgId = msgList[lastPosition].replace("\n", "");
			try {
				lastMsg = circle.getMsg(msgId);
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
       CircleStore rs = new CircleStore(getApplicationContext());
       circle = new Circle(getApplicationContext(),extras.getString("circle"));
       
       SharedPreferences defPref = PreferenceManager.getDefaultSharedPreferences(this);
       boolean alwaysUseLastMessage = defPref.getBoolean("alwaysUseLastMessage", false);
       
       setContentView(R.layout.msglist);

       idStore = new IdentityStore(getApplicationContext());
       EditText msgPostField = (EditText) findViewById(R.id.android_newMsgTextInline);
       
       msglistPrompt = (TextView) findViewById(R.id.android_msglistprompt);
       msglistPrompt.setText(circle.getShortname());

       if (circle == null) {
    	 msglistPrompt.setText("active circle is null...");
    	 return;
       }
       
       
       lastMessageIndex = circle.getLastMsgId();
       if (!alwaysUseLastMessage || lastMessageIndex == null) {
         loadIndexDialog = ProgressDialog.show(this, "", "Downloading message list...", true);
         Thread thread = new Thread(this);
         thread.start();
       } else {
    	   renderList();
       }

       
       
       
       
      //lastMessage = circle.getMsgIndex();
	}
     
	private void renderList() {
		if (lastMessageIndex == null) {
			Log.v("MsgList", "lastMessageIndex is null");
		}
		
		Log.v("MsgList", "lastMessageIndex: " + lastMessageIndex);
      if (lastMessageIndex == 0 || lastMessageIndex == null) {
          msglistPrompt.setText("No messages for " + circle.getShortname());
  	       return;
      }
      
//      } else {
//        msglistPrompt.setText(circle.getShortname());
//      }
//      
      
      if (lastMessageIndex < MAX_MESSAGES) {
    	  msgList = new String[lastMessageIndex];
      } else {
    	  msgList = new String[MAX_MESSAGES];
      }
      
      int newIndex = 0;
      for (Integer i = lastMessageIndex; i>0; i--) {
    	  
    	  if (newIndex == MAX_MESSAGES) {
    		  break;
    	  }
    	  
    	  MuteswanMessage msg = circle.getMsgFromDb(i.toString());
    	  
    	  
    	  
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
		
		
		//export circle to jpg using zxing
		if (item.getTitle().toString().equals("Export Circle")) {
			Intent showQrcode = new Intent("com.google.zxing.client.android.ENCODE");
			showQrcode.putExtra("ENCODE_DATA",circle.getFullText());
			showQrcode.putExtra("ENCODE_TYPE", "TEXT_TYPE");
			startActivity(showQrcode);
			return true;
		} else if (item.getTitle().toString().equals("Refresh")) {
			loadIndexDialog = ProgressDialog.show(this, "", "Downloading message list...", true);
		    Thread thread = new Thread(this);
		    thread.start();
			return true;
		} else {
			
		 // new message / FIXME: should be clearer
		 EditText newMsgText = (EditText) findViewById(R.id.android_newMsgTextInline);
		 Editable txt = newMsgText.getText();
    	 String txtData = txt.toString();
    	 if (txtData.getBytes().length != 0) {	
    	  try {
			circle.postMsg(txtData);
			Toast.makeText(this, 
				"Posted message to " + circle.getShortname(), 
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
    	   extr.putString("circle",circle.getFullText());
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

	      if (lastMsg == null)
	    	  return;
	      
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
