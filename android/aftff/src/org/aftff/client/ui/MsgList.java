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
import org.aftff.client.data.Message;
import org.aftff.client.data.Ring;
import org.aftff.client.data.RingStore;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import com.google.zxing.client.a.r;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
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

public class MsgList extends ListActivity {

	private static final int MAX_MESSAGES = 50;
	
	//private String[] mStrings = { "1", "2", "3", "4", "5", "6","7","8","9","10","11","12","13" };
	private ArrayAdapter listAdapter;
	private EditText msgPostField;
	
	private String[] msgList;
	
	private List<Integer> seenMsgs = new LinkedList();
	Ring ring;

	@Override
    public void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       
       Bundle extras = getIntent().getExtras();
       ring = new Ring(getApplicationContext(),extras.getString("ring"));
       

       setContentView(R.layout.msglist);
       
       EditText msgPostField = (EditText) findViewById(R.id.android_newMsgTextInline);
       
       TextView msglistPrompt = (TextView) findViewById(R.id.android_msglistprompt);
       if (ring == null) {
    	 msglistPrompt.setText("active ring is null...");
    	 return;
       }
       
       
      
      Integer lastMessage = ring.getMsgIndex();

      if (lastMessage == 0 || lastMessage == null) {
          msglistPrompt.setText("No messages for " + ring.getShortname());
  	       return;
      }
      
//      } else {
//        msglistPrompt.setText(ring.getShortname());
//      }
//      
      
      msglistPrompt.setText(ring.getShortname());
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
    	  
    	  Message msg = ring.getMsgFromDb(i.toString());
    	  if (msg != null) {
    		  msgList[newIndex] = i.toString() + " - " + msg.getDate() + "\n" + msg.getMsg();
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
	      // Toast.makeText(this,
		  //	"Fetching msg " + msgList[position] + " for ring " + aftff.activeRing.getShortname(), 
		  //	  Toast.LENGTH_LONG).show();
	      //Ring ring = aftff.activeRing;
	      try {
	    	String msgId = msgList[position].replace("\n", "");
			Message msg = ring.getMsg(msgId);
			
			Identity validSig = msg.getFirstValidSig();
			if (validSig != null) {
				msgList[position] = msgId + " - " + msg.getDate() + " from " + validSig.getName() + "\n" + msg.getMsg();
			} else {
				msgList[position] = msgId + " - " + msg.getDate() + "\n" + msg.getMsg();
	        }
			seenMsgs.add(position);
			//v.requestLayout();
			//v.forceLayout();
			//v.refreshDrawableState();
			
			listAdapter.notifyDataSetChanged();
			
			
			
			//parent.requestLayout();
			
			//setListAdapter(new ArrayAdapter<String>(this,
		      //      android.R.layout.simple_list_item_1, mStrings));
		       
			//TextView txt = new TextView(this);
			//txt.setText(msg);
			
			
			//Intent showMsgIntent = new Intent(this,ShowMsg.class);
			//showMsgIntent.putExtra("msg",msg);
			//startActivity(showMsgIntent);
			
			//startActivity(new Intent( this, ShowMsg.class));
			//return;
			
			//startActivity(new Intent( this, ShowMsg.class));
		    
			//Toast.makeText(this,msg,120).show();
			//setContentView(txt);
			//aftff.showMsg(msg);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			TextView txt = new TextView(this);
			txt.setText("client protocol exception");
			setContentView(txt);
			e.printStackTrace();
		} catch (IOException e) {
			
			Toast.makeText(this,
					"IO error: " + e.toString() + " for " + ring.getShortname() + "\n", 
					  Toast.LENGTH_SHORT).show();
			//e.printStackTrace();
		}
	 }  

}