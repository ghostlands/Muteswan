package org.aftff.client;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.http.client.ClientProtocolException;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
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
	
	private String[] mStrings = { "1", "2", "3", "4", "5", "6","7","8","9","10","11","12","13" };
	private ArrayAdapter listAdapter;
	private EditText msgPostField;
	
	private String[] msgList = new String[MAX_MESSAGES];
	
	private List<Integer> seenMsgs = new LinkedList();

	@Override
    public void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);

       setContentView(R.layout.msglist);
       
       EditText msgPostField = (EditText) findViewById(R.id.android_newMsgTextInline);
       
       TextView msglistPrompt = (TextView) findViewById(R.id.android_msglistprompt);
       if (aftff.activeRing == null) {
    	 msglistPrompt.setText("active ring is null...");
    	 return;
       }
       
      mStrings = aftff.activeRing.getMsgIndex();
      int newIndex = 0;
      for (int i = 0; i<mStrings.length;i++) {
    	  if (mStrings[i] == null || mStrings[i].getBytes().length == 0) {
    	  	  continue;
    	  }
    	  if (newIndex == MAX_MESSAGES) {
    		  break;
    	  }
    	  msgList[newIndex] = mStrings[i];
    	  newIndex++;
      }
//      List<String> list = Arrays.asList(mStrings);
//      Collections.reverse(list);
//      mStrings = (String[]) list.toArray();
      
       if (mStrings == null || mStrings.length == 0) {
           msglistPrompt.setText("No messages for " + aftff.activeRing.getShortname());
   	       return;
       } else if (mStrings[0].equals("error")) {
    	   msglistPrompt.setText("Error: " + mStrings[1]);
   	       return;
       } else {
         msglistPrompt.setText(aftff.activeRing.getShortname());
       }
       
       // Use an existing ListAdapter that will map an array
       // of strings to TextViews
       listAdapter = new ArrayAdapter<String>(this,
               android.R.layout.simple_list_item_1, msgList);
       setListAdapter(listAdapter);
    }
	
	//@Override
	//public void onContentChanged() {
	//  
	//}
	
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.msglistmenu, menu);
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        //setContentView(R.layout.writemsg);
				
		EditText newMsgText = (EditText) findViewById(R.id.android_newMsgTextInline);
		Editable txt = newMsgText.getText();
    	String txtData = txt.toString();
    	if (txtData.getBytes().length != 0) {	
    	  try {
			aftff.activeRing.postMsg(txtData);
			Toast.makeText(this, 
					"Posted message to " + aftff.activeRing.getShortname(), 
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
		  }
    	} else {
		   startActivity(new Intent( this, WriteMsg.class));
		   return true;
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
	      Toast.makeText(this,
			"Fetching msg " + msgList[position] + " for ring " + aftff.activeRing.getShortname(), 
			  Toast.LENGTH_LONG).show();
	      Ring ring = aftff.activeRing;
	      try {
	    	String msgId = msgList[position].replace("\n", "");
			String[] msg = ring.getMsg(msgId);
			msgList[position] = msgId + " - " + msg[0] + "\n" + msg[1];
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
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			TextView txt = new TextView(this);
			txt.setText("Illegal blocksize exception");
			setContentView(txt);
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			TextView txt = new TextView(this);
			txt.setText("Bad Padding Exception");
			setContentView(txt);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			TextView txt = new TextView(this);
			txt.setText("client protocol exception");
			setContentView(txt);
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//TextView txt = new TextView(this);
			//txt.setText("IO exception");
			//setContentView(txt);
			Toast.makeText(this,
					"IO error: " + e.toString() + " for " + ring.getShortname() + "\n", 
					  Toast.LENGTH_SHORT).show();
			//e.printStackTrace();
		}
	 }  

}