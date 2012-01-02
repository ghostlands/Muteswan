/*
Copyright 2011-2012 James Unger, Rob Wolffe, Chris Churnick.
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

import java.util.Arrays;
import java.util.Comparator;

import org.muteswan.client.AlertDialogs;
import org.muteswan.client.R;
import org.muteswan.client.Main;
import org.muteswan.client.data.Circle;
import org.muteswan.client.data.CircleStore;
import org.muteswan.client.data.Identity;
import org.muteswan.client.data.IdentityStore;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CircleList extends ListActivity {


	public static final String DELETED_CIRCLE_BROADCAST = "DELETEDCIRCLE";
	public static final String JOINED_CIRCLE_BROADCAST = "JOINEDCIRCLE";
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
	private String newCircle;

	
	private CircleStore store;
	private CircleListAdapter listAdapter;
	
	
	
	@Override
	public void onResume() {
		super.onResume();
		
		sendBroadcast(new Intent(LatestMessages.CHECKING_MESSAGES));
		
    	store = new CircleStore(this,true,false);
        circleList = getArray();
        //listAdapter = new ArrayAdapter<Circle>(this,
        //        android.R.layout.simple_list_item_1, circleList);
        listAdapter = new CircleListAdapter();
          setListAdapter(listAdapter);
          
          Arrays.sort(circleList, comparatorCircles);
		  listAdapter.notifyDataSetChanged();
	}
	
	public View.OnClickListener postClicked = new View.OnClickListener() {
    	public void onClick(View v) {
    		Intent intent = new Intent(getApplicationContext(),CircleList.class);
    		intent.putExtra("action",CircleList.WRITE);
    		startActivity(intent);
    		
    	}
    };
    
    public View.OnClickListener titleBarClicked = new View.OnClickListener() {
    	public void onClick(View v) {
    		  Intent intent = new Intent(getApplicationContext(),Main.class);
      		  startActivity(intent);
    		}
    };
	private AlertDialogs alertDialogs;
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
       

        extra = getIntent().getExtras();
        action = extra.getInt("action");
        newCircle = extra.getString("newCircle");
        initialText = extra.getString("initialText");
     
        Log.v("CircleList", "Before CircleStore constructor.");
    	store = new CircleStore(this,true,false);
    	Log.v("CircleList", "After CircleStore constructor.");

        setContentView(R.layout.circlelist);
        
        SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		Boolean shareManually = defPrefs.getBoolean("allowManualJoining", false);
        
        TextView txt = (TextView) findViewById(R.id.android_circlelistprompt);
        LinearLayout circlelist = (LinearLayout) findViewById(R.id.circlelistButtons);
        txt.setText(actionPrompts[action]);
        
        final ImageView titleBarImage = (ImageView) findViewById(R.id.titlebarImage);
        titleBarImage.setOnClickListener(titleBarClicked);
        
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
          if (shareManually) {
            Button addCircleManually = (Button) findViewById(R.id.android_circlelistAddCircleManually);
            addCircleManually.setOnClickListener(addCircleManuallyListener);
            addCircleManually.setVisibility(View.VISIBLE);
          }
        } else {
          circlelist.setVisibility(View.GONE);
        }
        
        
        circleList = getArray();
        registerForContextMenu(getListView());
    
        alertDialogs = new AlertDialogs(this);
    }
	
	private ComparatorCircles comparatorCircles = new ComparatorCircles();
	class ComparatorCircles implements Comparator<Circle> {
		 public int compare(Circle obj1, Circle obj2)
	        {
			 	Circle circle1 = obj1;
			 	Circle circle2 = obj2;
			 	
			 	if (newCircle != null && circle1.getShortname().equals(newCircle)) {
			 		return(-1);
			 	}
			 	
			 	if (newCircle != null && circle2.getShortname().equals(newCircle)) {
			 		return(1);
			 	}
			 	
			 	return(circle1.getShortname().compareTo(circle2.getShortname()));
	        }
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
		    		RelativeLayout innerLayout = (RelativeLayout) v.findViewById(R.id.circleListInnerEntryLayout);
		    		innerLayout.setBackgroundColor(R.drawable.darkerborder);
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
			
			
			 RelativeLayout layout = (RelativeLayout) convertView;
			  if (layout == null)
				  layout = (RelativeLayout) getLayoutInflater().inflate(R.layout.circlelistentry,
	     				  parent, false);
			  
			 
			 layout.setTag(R.id.android_circleListName, position);
			 
			 TextView txtCircle = (TextView) layout.findViewById(R.id.android_circleListName);
			 txtCircle.setClickable(true);
			 txtCircle.setTag(R.id.android_circleListName, position);
			 txtCircle.setOnClickListener(circleClicked);
			 
			 
			 if (newCircle != null && circleList[position].getShortname().equals(newCircle)) {
				 txtCircle.setText(circleList[position].getShortname() + " (new!)");
			 } else {
				 txtCircle.setText(circleList[position].getShortname());
			 }
			 
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
			intent.putExtra("circle", Main.genHexHash(circleList[position].getFullText()));
	    } else if (action == SHARE) {
			intent = new Intent("com.google.zxing.client.android.ENCODE");
			intent.putExtra("ENCODE_DATA",circleList[position].getFullText());;
			intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
			try {
			  startActivity(intent);
			} catch (ActivityNotFoundException e) {
		    	  alertDialogs.offerToInstallBarcodeScanner();
		          
		    }
			return;
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
	        try {
	           startActivityForResult(intent, 0);
	        } catch (ActivityNotFoundException e) {
	        	alertDialogs.offerToInstallBarcodeScanner();
	        }
         }
     };
     
     private Button.OnClickListener addCircleManuallyListener  = new Button.OnClickListener() {
         public void onClick( View v ) {
        	 AlertDialog.Builder builder = new AlertDialog.Builder(CircleList.this);
        	 LayoutInflater layoutInflater = (LayoutInflater)getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        	 View view = layoutInflater.inflate(R.layout.addcirclemanually, null );

        	 builder.setView(view);
        	
        	 builder.setMessage("Join Circle Manually");
        	 final EditText editTxt = (EditText) view.findViewById(R.id.circleListManualJoinCircle);
        	
        	 builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int id) {
                	 
                	 // FIXME refactor
                	String circleTxt = editTxt.getText().toString();

     	            CircleStore store = new CircleStore(getApplicationContext(),true,false);
                 	Circle circle = new Circle(getApplicationContext(),circleTxt);
                 	if (circle.getShortname() == null)
                 		Log.v("CircleList","Circle is null after initializing manually.");
     	            store.updateStore(circleTxt);
     	            
         	        Intent joinCircleIntent = new Intent(CircleList.JOINED_CIRCLE_BROADCAST);
         	      	joinCircleIntent.putExtra("circle", Main.genHexHash(circle.getFullText()));
         	      	sendBroadcast(joinCircleIntent);
         	      	
         	        newCircle = circle.getShortname();
         	        onResume();
               	 
                 }}
        	 );
        	 builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int id) {
                 }}
        	 );
        	 builder.show();
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
		
		Intent deleteCircleIntent = new Intent(CircleList.DELETED_CIRCLE_BROADCAST);
		deleteCircleIntent.putExtra("circle", Main.genHexHash(circleList[position].getFullText()));
		sendBroadcast(deleteCircleIntent);
		
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

	
	@SuppressWarnings("unused")
	private void viewCircle(Integer position) {
		Intent intent = new Intent(getApplicationContext(),ViewCircle.class);
		intent.putExtra("circle",circleList[position].getFullText());
		startActivity(intent);
	}
	
	@SuppressWarnings("unused")
	private void editCircle(Integer position) {
		Intent intent = new Intent(getApplicationContext(),EditCircle.class);
		intent.putExtra("circle",circleList[position].getFullText());
		startActivity(intent);
	}
	
	private void shareCircle(Integer position) {
		Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
		intent.putExtra("ENCODE_DATA",circleList[position].getFullText());
		intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
		intent.putExtra("ENCODE_SHOW_CONTENTS", true);
        try {
	        startActivityForResult(intent, 0);
	    } catch (ActivityNotFoundException e) {
	       	alertDialogs.offerToInstallBarcodeScanner();
	    }
	}

	private void showMsgList(Integer position) {
		Intent intent = new Intent(getApplicationContext(),LatestMessages.class);
		intent.putExtra("circle", Main.genHexHash(circleList[position].getFullText()));
		startActivity(intent);
	}

	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	  if (requestCode == 0) {
    	    if (resultCode == RESULT_OK) {
    	    	    //Handle successful scan
    	            String contents = intent.getStringExtra("SCAN_RESULT");
    	            
    	            int atIndex = contents.indexOf("@");
    	            
    	            // RING
    	            if (atIndex != -1) {
    	            
      	              CircleStore store = new CircleStore(getApplicationContext(),true,false);
    	              Circle circle = new Circle(getApplicationContext(),contents);
    	              store.updateStore(contents);
    	              
    	              
    	              Intent joinCircleIntent = new Intent(CircleList.JOINED_CIRCLE_BROADCAST);
    	      		  joinCircleIntent.putExtra("circle", Main.genHexHash(circle.getFullText()));
    	      		  sendBroadcast(joinCircleIntent);
    	      		  
    	      		  newCircle = circle.getShortname();
    	              
    	              
    	            // IDENTITY
    	            } else {
    	            	String[] parts = contents.split(":");
    	            	Identity identity = new Identity(parts[0],parts[1],parts[2]);
    	            	IdentityStore idStore = new IdentityStore(getApplicationContext());
    	            	idStore.addToDb(identity);
    	            }
    	            
            } else if (resultCode == RESULT_CANCELED) {
            	
            	final String testSite = "testsite+dba4fe6ef22b494d@tckwndlytrphlpyo.onion";

	            CircleStore store = new CircleStore(getApplicationContext(),true,false);
            	Circle circle = new Circle(getApplicationContext(),testSite);
	            store.updateStore(testSite);
	            
    	        Intent joinCircleIntent = new Intent(CircleList.JOINED_CIRCLE_BROADCAST);
    	      	joinCircleIntent.putExtra("circle", Main.genHexHash(circle.getFullText()));
    	      	sendBroadcast(joinCircleIntent);
    	      	
    	        newCircle = circle.getShortname();
	            
            
            }
    	  }

	}
	

	
}
