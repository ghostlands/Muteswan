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
package org.muteswan.client.ui;

import java.util.ArrayList;
import java.util.HashMap;

import org.muteswan.client.IMessageService;
import org.muteswan.client.MuteswanHttp;
import org.muteswan.client.NewMessageService;
import org.muteswan.client.R;
import org.muteswan.client.Main;
import org.muteswan.client.data.Circle;
import org.muteswan.client.data.CircleStore;
import org.muteswan.client.data.Identity;
import org.muteswan.client.data.IdentityStore;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class WriteMsg extends ListActivity {

	protected static final String SENT = "sent";
	protected static final int MAX_MSG_LENGTH = 2500;
	Circle circle;
	boolean[] signSelections;
	CharSequence[] signIdentities;
	Identity[] identities;
	String initialText;
	
	
	protected IMessageService msgService;
	protected AlertDialog verifyPostAlert;
	protected ArrayList<Thread> threads = new ArrayList<Thread>();

	
	
	
	final ArrayList<Circle> circles = new ArrayList<Circle>();

	Boolean[] checkedCircles;
	ListView listView;
	private Button postButton;
	
	private View.OnClickListener newMsgTextClicked  = new View.OnClickListener() {
        public void onClick( View v ) {
        	enablePostButton();
        }
	 };
	private MuteswanHttp muteswanHttp;
	
	
	public void onResume() {
		super.onResume();
		sendingMsgDialog = null;
	}
	
	public void onDestroy() {
		super.onDestroy();
		sendingMsgDialog = null;
		if (msgService != null) {
			unbindService(msgServiceConn);
		}
		
		for (Thread t : threads)
			t.interrupt();
		
	}
	
	public void onCreate(Bundle savedInstanceState) {
	       super.onCreate(savedInstanceState);

	       Bundle extras = getIntent().getExtras();
	       muteswanHttp = new MuteswanHttp();
	       CircleStore cs = new CircleStore(getApplicationContext(),true,false);
	
	       
	       if (extras != null && extras.containsKey("circle")) {
	        circle = new Circle(this,extras.getString("circle"));
	       }
	       
	       if (extras != null && extras.containsKey("initialText")) {
	        initialText = extras.getString("initialText");
	       }
	       
	       
	       
	       //initialize circles list
	       Log.v("CircleSize", "Circle store size: " + cs.size());
	       
	       checkedCircles = new Boolean[cs.size()];
	       for (Circle c : cs) {
	    	   circles.add(c);
	    	   if (circle != null && c.getShortname().equals(circle.getShortname())) {
	    		   checkedCircles[cs.indexOf(c)] = true;
	    	   } else {
	    	       checkedCircles[cs.indexOf(c)] = false;
	    	   }
	       }
	       
	       
	       setContentView(R.layout.writemsg);
	       
	       
	       setListAdapter(new WriteMsgListAdapter());
	       listView = getListView();
	       listView.setItemsCanFocus(false);
	       listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	       listView.setClickable(false);
	       
	       TextView prompt = (TextView) findViewById(R.id.android_writemsgPrompt);
	       if (circle != null && prompt != null)
	         prompt.setText("Post to " + circle.getShortname());
	       
	       
	       IdentityStore idStore = new IdentityStore(getApplicationContext());
	       identities = idStore.asArray(true);
	       signIdentities = new CharSequence[identities.length];
	       for (int i=0; i<identities.length;i++) {
	    	   signIdentities[i] = identities[i].getName();
	       }
	       signSelections = new boolean[signIdentities.length];
	       for(int i=0; i<signSelections.length; i++) {
	    	   signSelections[i] = false;
	       }
	       
	      
	       postButton = (Button) findViewById(R.id.submitMsg);
	       postButton.setOnClickListener(submitMsg);
	       
	       EditText newMsgText = (EditText) findViewById(R.id.newMsgText);
	       newMsgText.setOnClickListener(newMsgTextClicked);
	       
	       if (initialText != null) {
	    	newMsgText.setText(initialText);
	       }
	         
	}
	
	
	
	@Override
    protected void onListItemClick(ListView lv, View v, int pos, long id)
{
            super.onListItemClick(lv, v, pos, id);
            
            CheckedTextView tv = (CheckedTextView) v.findViewById(R.id.writeMsgCircleTextView);
           
            if (tv.isChecked()) {
            	checkedCircles[pos] = false;
            	tv.setChecked(false);
            } else {
            	checkedCircles[pos] = true;
            	tv.setChecked(true);
            }
            
    }



	public class WriteMsgListAdapter extends BaseAdapter {

		
		
		@Override
		public int getCount() {
			
			return circles.size();
		}

		@Override
		public Object getItem(int position) {
			return circles.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.writemsgcirclelist,
    				  parent, false);
			
			
			
			CheckedTextView circleTextView = (CheckedTextView) layout.findViewById(R.id.writeMsgCircleTextView);
			
			circleTextView.setText(circles.get(position).getShortname());
			
			if (checkedCircles[position] == true)
				circleTextView.setChecked(true);
			
			return layout;
		}


	}
	
	
	 @Override
     protected Dialog onCreateDialog( int id )
     {
             return
             new AlertDialog.Builder( this )
             .setTitle( "Sign message with identity" )
             .setMultiChoiceItems(signIdentities, signSelections, new DialogSelectionClickHandler() )
             .setPositiveButton( "OK", new DialogButtonClickHandler() )
             .create();
     }

	 public class DialogSelectionClickHandler implements DialogInterface.OnMultiChoiceClickListener
     {
             public void onClick( DialogInterface dialog, int clicked, boolean selected )
             {
            	 signSelections[clicked] = selected;
            	 Log.v("WriteMsg", "Set " + clicked + " to " + selected);
             }
     }
	 
	 public class DialogButtonClickHandler implements DialogInterface.OnClickListener
     {
             public void onClick( DialogInterface dialog, int clicked )
             {
                     switch( clicked )
                     {
                             case DialogInterface.BUTTON_POSITIVE:
                                     break;
                     }
             }
     }


   
	 public Button.OnClickListener selectSigButtonHandler  = new View.OnClickListener() {
        public void onClick( View v ) {
        		Log.v("WriteMsg", "select sig button clicked.\n");
                 showDialog( 0 );
         }
     };
	protected ProgressDialog sendingMsgDialog;

	
	private HashMap<String,String> sendingDialogData = new HashMap<String,String>();
	
	 final Handler updateSendDialog = new Handler() {
	
	        @Override
	        public void handleMessage(Message msg) {
	              	Bundle b = msg.getData();
	              	
	              	
	              	if (b.getString("error") != null) {
	              		
	              		if (sendingMsgDialog != null)
	              		  sendingMsgDialog.dismiss();
						
						AlertDialog.Builder builder = new AlertDialog.Builder(WriteMsg.this);
			    		builder.setMessage("A problem occurred: " + b.getString("error"))
			    		       .setCancelable(true).setPositiveButton("OK", new DialogInterface.OnClickListener() {
			    		           public void onClick(DialogInterface dialog, int id) {
			    		               
			    		           }}
			    		    		   );
			    		       
			    		AlertDialog alert = builder.create();
			    		alert.show();
			    		enablePostButton();
	              		return;
	              	}
	              	
	              	
	        		//sendingMsgDialog.dismiss();
					//sendingMsgDialog.setCancelable(true);
	              	if (sendingMsgDialog != null)
					  sendingMsgDialog.setMessage("Message posted: " + b.get("circles"));
					
					if (b.get("circle") != null) {
						sendingDialogData.put((String) b.get("circle"), (String)b.get("status"));
						
						if (sendingMsgDialog != null)
						 sendingMsgDialog.setMessage(renderDialog(false));
					}
					
					
					//FIXME: not workable
					Boolean finishedSending = true;
					for (String key : sendingDialogData.keySet()) {
						if (checkedCircles[b.getInt("position")] == true && sendingDialogData.get(key).equals(WriteMsg.SENT)) {
							checkedCircles[b.getInt("position")] = false;
							//circleTextView.setChecked(true);
						}
						
						
						if (!sendingDialogData.get(key).equals(WriteMsg.SENT)) {
							finishedSending = false;
							continue;
						}
					}
					if (finishedSending == true) {
						if (sendingMsgDialog != null)
						  sendingMsgDialog.dismiss();
						
						AlertDialog.Builder builder = new AlertDialog.Builder(WriteMsg.this);
			    		builder.setMessage("All messages sent successfully.")
			    		       .setCancelable(true).setPositiveButton("OK", new DialogInterface.OnClickListener() {
			    		           public void onClick(DialogInterface dialog, int id) {
			    		               finish();
			    		           }}
			    		    		   );
			    		       
			    		AlertDialog alert = builder.create();
			    		alert.show();
			    		
					}
					
				
	        }
	 };
	

	 private String renderDialog(Boolean finished) {
		 String dialogText = "Sending messages:";
		 
		 for (String key : sendingDialogData.keySet()) {
			 dialogText = dialogText + "\n" + key + ": " + sendingDialogData.get(key);
		 }
		 
		 return dialogText;
	 }

	
	
	public Button.OnClickListener submitMsg = new Button.OnClickListener() {
	    public void onClick(final View v) {
	    	showVerifySendDialog(v);
	    }
	    
	    
	    private void postMessage(View v) {
	    	
	    	if (verifyPostAlert != null)
	    		verifyPostAlert.dismiss();
	    	
	    	EditText newMsgText = (EditText) findViewById(R.id.newMsgText);
	    	Editable txt = newMsgText.getText();
	    	final String txtData = txt.toString();
	    	
	    
    	    sendingMsgDialog = ProgressDialog.show(v.getContext(), "", "Sending message...", true);
   		    sendingMsgDialog.setCancelable(true);

   		    disablePostButton();
   		    
   		    
   		    
	    	
    		Intent serviceIntent = new Intent(getApplicationContext(),NewMessageService.class);
            bindService(serviceIntent,msgServiceConn,Context.BIND_NOT_FOREGROUND);
	    
          
	    	
	    	
			 for (final Circle cir: circles) {
				 
				 
				 if (checkedCircles[circles.indexOf(cir)] == false)
					 continue;
				 
				 
				
				
				if (txtData == null || txtData.equals("")) {
				 	Bundle b = new Bundle();
					Message msg = Message.obtain();
					b.putString("error", "Empty message.");
					msg.setData(b);
					updateSendDialog.sendMessage(msg);
					return;
				} else if (txtData.length() > WriteMsg.MAX_MSG_LENGTH) {
				 	Bundle b = new Bundle();
					Message msg = Message.obtain();
					b.putString("error", "Message exceeds 2500 character maximum.");
					msg.setData(b);
					updateSendDialog.sendMessage(msg);
					return;
				}
				
				
	    			
				  Thread nThread =  new Thread() {
					 
					  
					  
					  public void run() {
						  Bundle b = new Bundle();
						  Message msg = Message.obtain();
					      msg.setData(b);
					      b.putString("circle", cir.getShortname());
					      b.putString("status", "sending...");
					      updateSendDialog.sendMessage(msg);
						  
						try {
							  while (msgService == null) {
					            	Thread.currentThread();
									try {
										Thread.sleep(10);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
					            }	
							
							
							Integer httpCode = msgService.postMsg(Main.genHexHash(cir.getFullText()), txtData);
							Bundle b2 = new Bundle();
							Message msg2 = Message.obtain();
							b2.putString("circle", cir.getShortname());
							b2.putInt("position",circles.indexOf(cir));
							msg2.setData(b2);

							
							if (httpCode == 200) {
							 b2.putString("status", WriteMsg.SENT);
						
							} else if (httpCode >= 500) {
							 b2.putString("status", "server error");
							} else if (httpCode < 0) {
							 b2.putString("status", "timeout");
							}
							
						
							if (Thread.currentThread().isInterrupted())
								return;
							
							updateSendDialog.sendMessage(msg2);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					  }
				  };
				  nThread.start();
				  threads.add(nThread);
				  

	    	}
	    	
			
	    }

		private void showVerifySendDialog(final View v) {
		
			
			String alertMsg = "Post messages to: \n";
			boolean noCheckedCircles = true;
			for (final Circle cir: circles) {
		      if (checkedCircles[circles.indexOf(cir)] == true) {
		    	  alertMsg = alertMsg + "  " + cir.getShortname() + "\n";
		    	  noCheckedCircles = false;
		      }
		      
			}
		    	  
			
			AlertDialog.Builder builder = new AlertDialog.Builder(WriteMsg.this);
			if (noCheckedCircles) {
				builder.setMessage("No circles chosen, please select at least one circle.");
				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialogInterface, int i) {}
	   		     });
			} else {
    		  builder.setMessage(alertMsg);
    		
    		
    		 builder.setPositiveButton("Post", new DialogInterface.OnClickListener() {
   		      public void onClick(DialogInterface dialogInterface, int i) {
   		    	  postMessage(v);
   		      }
   		     });
    		 
   		     builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
   		      public void onClick(DialogInterface dialogInterface, int i) {}
   		     });
			}
    		       
    		verifyPostAlert = builder.create();
    		verifyPostAlert.show();
		}
	    	
	};
	
	private ServiceConnection msgServiceConn = new ServiceConnection() {
		public void onServiceConnected(ComponentName className,
             IBinder service) {
     	msgService = IMessageService.Stub.asInterface(service);
     	try {
				msgService.setUserChecking(true);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
     	Log.v("LatestMessages", "onServiceConnected called.");
     	if (msgService == null) {
     		Log.e("LatestMessages", "msgService is null ");
     	}

     }

     public void onServiceDisconnected(ComponentName className) {
        msgService = null;
     }
 };


	protected void disablePostButton() {
		postButton.setEnabled(false);
	}
	
	protected void enablePostButton() {
		postButton.setEnabled(true);
	}
	
}

