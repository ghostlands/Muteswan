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

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
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
														  "Long-press a circle for actions.",
														  "New circle added"};
	public Integer action;
	Bundle extra;
	public Circle[] circleList;
	private String initialText;

	
	private CircleStore store;
	private ArrayAdapter<Circle> listAdapter;
	
	@Override
	public void onResume() {
		super.onResume();
    	store = new CircleStore(this,true);
        circleList = getArray();
        listAdapter = new ArrayAdapter<Circle>(this,
                android.R.layout.simple_list_item_1, circleList);
          setListAdapter(listAdapter);
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
       

        extra = getIntent().getExtras();
        action = extra.getInt("action");
        initialText = extra.getString("initialText");
        
    	store = new CircleStore(this,true);

        setContentView(R.layout.circlelist);
        
        
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

      
          listAdapter = new ArrayAdapter<Circle>(this,
                android.R.layout.simple_list_item_1, circleList);
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
    
    
    protected void onListItemClick(ListView parent, View v, int position, long id)
    	 	{
    	
    			dispatchActivity(position);

    			
    	 	}

	private void dispatchActivity(int position) {
		Intent intent = null;
		if (action == WRITE) {
			intent = new Intent(getApplicationContext(),WriteMsg.class);
			intent.putExtra("circle", circleList[position].getFullText());
		} else if (action == READ) {
			intent = new Intent(getApplicationContext(),LatestMessages.class);
			intent.putExtra("circle", muteswan.genHexHash(circleList[position].getFullText()));
	    } else if (action == SHARE) {
			intent = new Intent("com.google.zxing.client.android.ENCODE");
			intent.putExtra("ENCODE_DATA",circleList[position].getFullText());;
			intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");;
	    } else if (action == ANY || action == SCAN) {
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
