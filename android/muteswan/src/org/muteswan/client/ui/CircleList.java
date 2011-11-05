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

import org.muteswan.client.NewMessageService;
import org.muteswan.client.R;
import org.muteswan.client.muteswan;
import org.muteswan.client.data.MuteswanMessage;
import org.muteswan.client.data.Identity;
import org.muteswan.client.data.IdentityStore;
import org.muteswan.client.data.Circle;
import org.muteswan.client.data.CircleStore;
import org.apache.http.client.ClientProtocolException;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ViewGroup;
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
import android.widget.AdapterView.AdapterContextMenuInfo;

public class CircleList extends ListActivity {


	public static int SHARE = 0;
	public static int READ = 1;
	public static int WRITE = 2;
	public static int ANY = 3;
	public static int SCAN = 4;
	public static String[] actionPrompts = new String[] { "Select a circle to share.", 
														  "Select a circle to read messages.", 
														  "Select a circle to write a message.",
														  "",
														  "New circle added"};
	public Integer action;
	Bundle extra;
	public Circle[] circleList;
	private String initialText;

	
	private CircleStore store;
	private CircleListAdapter listAdapter;
	
	
	
	@Override
	public void onResume() {
		super.onResume();
    	store = new CircleStore(this,true);
        circleList = getArray();
        //listAdapter = new ArrayAdapter<Circle>(this,
        //        android.R.layout.simple_list_item_1, circleList);
        listAdapter = new CircleListAdapter(this);
          setListAdapter(listAdapter);
	}
	
	public View.OnClickListener postClicked = new View.OnClickListener() {
    	public void onClick(View v) {
    		Intent intent = new Intent(getApplicationContext(),CircleList.class);
    		intent.putExtra("action",CircleList.WRITE);
    		startActivity(intent);
    		
    	}
    };
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        
       

        extra = getIntent().getExtras();
        action = extra.getInt("action");
        initialText = extra.getString("initialText");
        
    	store = new CircleStore(this,true);

        setContentView(R.layout.circlelist);
        
        // don't use title bar post button for now
        //getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.customtitlebar);
        //TextView postButton = (TextView) findViewById(R.id.latestmessagesTitlePostButton);
		//postButton.setOnClickListener(postClicked);
        
        
        TextView txt = (TextView) findViewById(R.id.android_circlelistprompt);
        LinearLayout circlelist = (LinearLayout) findViewById(R.id.circlelistButtons);
        txt.setText(actionPrompts[action]);
        
	   if (action == SCAN) {
		Intent intent = new Intent("com.google.zxing.client.android.SCAN");
        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
        startActivityForResult(intent, 0);
	   }
        
        
        if (action == null || action == ANY || action == SCAN) {
          Button addCircle = (Button) findViewById(R.id.android_circlelistAddCircle);
          Button createCircle = (Button) findViewById(R.id.android_circlelistCreateCircle);
          addCircle.setOnClickListener(addCircleListener);
          createCircle.setOnClickListener(createCircleListener);
          circlelist.setVisibility(View.VISIBLE);
        } else {
          circlelist.setVisibility(View.GONE);
        }

        
        
        circleList = getArray();
        registerForContextMenu(getListView());

      
          //listAdapter = new ArrayAdapter<Circle>(this,
          //      android.R.layout.simple_list_item_1, circleList);
          listAdapter = new CircleListAdapter(this);
          setListAdapter(listAdapter);
       
        
    }
    
    //FIXME: should be part of store
    private Circle[] getArray() {
    	
    	Circle[] circleList = new Circle[store.size()];
    	int i = 0;
    	for (Circle r : store) {
    		circleList[i] = r;
    		i++;
    	}
    	
    	return(circleList);
    }
    
    
    
    
    
		
		
	
    
    public class CircleListAdapter extends BaseAdapter {

    	private Context context;
    	
    	
    	public CircleListAdapter(Context context) {
			this.context = context;
		}
    	
		@Override
		public int getCount() {
			
			return(circleList.length);
			
		}

		@Override
		public Object getItem(int position) {
			
			return(circleList[position]);
		}

		@Override
		public long getItemId(int position) {
			return(position);
		}

		
		   public View.OnClickListener circleClicked = new View.OnClickListener() {
		    	public void onClick(View v) {
		    		Integer position = (Integer) v.getTag(R.id.android_circleListName);
		    		dispatchActivity(position);
		    	}
		    };
		    
		    public View.OnClickListener circleShareClicked = new View.OnClickListener() {
		    	public void onClick(View v) {
		    		Integer position = (Integer) v.getTag(R.id.circleListShare);
		    		shareCircle(position);
		    	}
		    };
		    public View.OnClickListener circleDeleteClicked = new View.OnClickListener() {
		    	public void onClick(View v) {
		    		final Integer position = (Integer) v.getTag(R.id.circleListDelete);
		    		//Message m = Message.obtain();
	        		//Bundle b = new Bundle();
		    		//b.putInt("position", position);
		    		//m.setData(b);
		    		//showCircleDeleteDialog.sendMessage(m);
		    		
		    		
		    		AlertDialog.Builder builder = new AlertDialog.Builder(CircleList.this);
		    		builder.setMessage("Are you sure you want to delete this circle?")
		    		       .setCancelable(false)
		    		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		    		           public void onClick(DialogInterface dialog, int id) {
		    		        	   deleteCircle(position);
		    		           }
		    		       })
		    		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		    		           public void onClick(DialogInterface dialog, int id) {
		    		                dialog.cancel();
		    		           }
		    		       });
		    		AlertDialog alert = builder.create();
		    		alert.show();
		    		
		    	
		    		
		    		
		    	}

				
		    };
		    
		   
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			 RelativeLayout layout = (RelativeLayout) getLayoutInflater().inflate(R.layout.circlelistentry,
     				  parent, false);
			 
			 layout.setTag(R.id.android_circleListName, position);
			 
			 TextView txtCircle = (TextView) layout.findViewById(R.id.android_circleListName);
			 txtCircle.setClickable(true);
			 txtCircle.setTag(R.id.android_circleListName, position);
			 txtCircle.setOnClickListener(circleClicked);
			 txtCircle.setText(circleList[position].getShortname());
			 
			 layout.setClickable(true);
			 layout.setOnClickListener(circleClicked);
			 
			
			 Button shareCircleButton = (Button) layout.findViewById(R.id.circleListShare);
			 ImageView deleteCircleButton = (ImageView) layout.findViewById(R.id.circleListDelete);
			 if (action == ANY) {
			 
			   shareCircleButton.setClickable(true);
			   shareCircleButton.setTag(R.id.circleListShare, position);
			   shareCircleButton.setOnClickListener(circleShareClicked);
			 
			   
			   deleteCircleButton.setClickable(true);
			   deleteCircleButton.setTag(R.id.circleListDelete, position);
			   deleteCircleButton.setOnClickListener(circleDeleteClicked);

			  
			 } else {
				 shareCircleButton.setVisibility(View.GONE);
				 deleteCircleButton.setVisibility(View.GONE);
				 
			 }
			 
			 
			 
			return layout;
		}
    	
    }
    
    public View.OnClickListener listItemClicked = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			
		}
	    
    };
    
    protected void onListItemClick(ListView parent, View v, int position, long id)
    	 	{
    	
    			dispatchActivity(position);

    			
    	 	}

	private void dispatchActivity(int position) {
		Intent intent = null;
		if (action == WRITE) {
			intent = new Intent(getApplicationContext(),WriteMsg.class);
			intent.putExtra("circle", circleList[position].getFullText());
		} else if (action == READ || action == ANY) {
			intent = new Intent(getApplicationContext(),LatestMessages.class);
			intent.putExtra("circle", muteswan.genHexHash(circleList[position].getFullText()));
	    } else if (action == SHARE) {
			intent = new Intent("com.google.zxing.client.android.ENCODE");
			intent.putExtra("ENCODE_DATA",circleList[position].getFullText());;
			intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");;
	    } else if (action == SCAN) {
	    	return;
	    }
		
	
		
		
		intent.putExtra("initialText", initialText);
		//Log.v("CircleList", "Would launch " + action.toString());
		startActivity(intent);

	}
	
	public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
			super.onCreateContextMenu(menu, v, menuInfo);
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.circlelistcontextmenu, menu);
	}

	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
		
		
		
		switch (item.getItemId()) {
		  case R.id.circleListDelete:
			  deleteCircle(info.position);
			  break;
		  case R.id.circleListRead:
			  showMsgList(info.position);
			  break;
		  case R.id.circleListShare:
			  shareCircle(info.position);
			  break;
	     // MANIFEST features not used right now
		 // case R.id.circleListView:
		 //	  viewCircle(info.position);
		 //	  break;
		 // case R.id.circleListEdit:
		 //  editCircle(info.position);
		 //	  break;
		  case R.id.circleListWriteMsg:
			  writeMsg(info.position);
			  break;
		}
		return true;
	
	}

	private Button.OnClickListener addCircleListener  = new Button.OnClickListener() {
        public void onClick( View v ) {
        	Intent intent = new Intent("com.google.zxing.client.android.SCAN");
	        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
	        startActivityForResult(intent, 0);
         }
     };
	
     
     private Button.OnClickListener createCircleListener = new Button.OnClickListener() {
    	 public void onClick(View v) {
    		 startActivity(new Intent(getApplicationContext(),CreateCircle.class));
    	     return;
    	 }
     };
     
	
	private void deleteCircle(int position) {
		CircleStore store = new CircleStore(getApplicationContext());
		store.deleteCircle(circleList[position]);
		Toast.makeText(this,
				"Deleted circle " + circleList[position].getShortname() + " from saved keys.", 
					  Toast.LENGTH_LONG).show();
		onResume();
	}

	private void writeMsg(Integer position) {
		Intent intent = new Intent(getApplicationContext(),WriteMsg.class);
		intent.putExtra("circle", circleList[position].getFullText());
		startActivity(intent);
	}

	
	private void viewCircle(Integer position) {
		Intent intent = new Intent(getApplicationContext(),ViewCircle.class);
		intent.putExtra("circle",circleList[position].getFullText());
		startActivity(intent);
	}
	
	private void editCircle(Integer position) {
		Intent intent = new Intent(getApplicationContext(),EditCircle.class);
		intent.putExtra("circle",circleList[position].getFullText());
		startActivity(intent);
	}
	
	private void shareCircle(Integer position) {
		Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
		intent.putExtra("ENCODE_DATA",circleList[position].getFullText());
		intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
		startActivity(intent);
	}

	private void showMsgList(Integer position) {
		Intent intent = new Intent(getApplicationContext(),LatestMessages.class);
		intent.putExtra("circle", muteswan.genHexHash(circleList[position].getFullText()));
		startActivity(intent);
	}

	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	  if (requestCode == 0) {
    	    if (resultCode == RESULT_OK) {
    	    	    //Handle successful scan
    	            String contents = intent.getStringExtra("SCAN_RESULT");
    	            String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
    	            
    	            int atIndex = contents.indexOf("@");
    	            
    	            // RING
    	            if (atIndex != -1) {
    	            
      	              CircleStore store = new CircleStore(getApplicationContext(),true);
    	              Circle circle = new Circle(getApplicationContext(),contents);
    	              store.updateStore(contents);
    	               
    	              
    	              //this.activeCircle = circle;
    	    //          selectMsg(circle);
    	            
    	            // IDENTITY
    	            } else {
    	            	String[] parts = contents.split(":");
    	            	Identity identity = new Identity(parts[0],parts[1],parts[2]);
    	            	IdentityStore idStore = new IdentityStore(getApplicationContext());
    	            	idStore.addToDb(identity);
    	            }
    	            
    	            
    	            
            } else if (resultCode == RESULT_CANCELED) {
            	
            	final String testSite = "testsite+dba4fe6ef22b494d@tckwndlytrphlpyo.onion";

	            CircleStore store = new CircleStore(getApplicationContext(),true);
            	Circle circle = new Circle(getApplicationContext(),testSite);
	            store.updateStore(testSite);
	            
            
            }
    	  }

	}
}
