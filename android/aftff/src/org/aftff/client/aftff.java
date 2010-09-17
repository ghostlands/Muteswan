package org.aftff.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.aftff.client.data.Identity;
import org.aftff.client.data.IdentityStore;
import org.aftff.client.data.Ring;
import org.aftff.client.data.RingStore;
import org.aftff.client.ui.CreateRing;
import org.aftff.client.ui.GenerateIdentity;
import org.aftff.client.ui.IdentityList;
import org.aftff.client.ui.LatestMessages;
import org.aftff.client.ui.MsgList;
import org.aftff.client.ui.Preferences;
import org.aftff.client.ui.RingList;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.util.EntityUtils;
import org.torproject.android.service.ITorService;

import uk.ac.cam.cl.dtg.android.tor.TorProxyLib.SocksProxy;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;




public class aftff extends Activity implements Runnable {
	//Store store = null;
	//public static Ring activeRing = null;

	public final static int TOR_STATUS_OFF = -1;
	public final static int TOR_STATUS_READY = 0;
	public final static int TOR_STATUS_ON = 1;
	public final static int TOR_STATUS_CONNECTING = 2;

	
	public final static String PREFS = "AftffPrefs"; 
	private ProgressDialog dialog;
	
	
	ITorService torService;

    IMessageService newMsgService;

	private boolean justCreated;
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (torService == null) {
			try {
				Thread.currentThread().sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.v("AFTFF", "Tor service still null.");
		}
		Log.v("AFTFF", "Tor is not null.");
		
		
		dialogWaitOnNewMsgService.sendEmptyMessage(0);
		while (newMsgService == null) {
			try {
				Thread.currentThread().sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	  try {
		Log.v("AFFF", "Getting tor status");
		if (torService.getStatus() != TOR_STATUS_ON) {
			   Log.v("AFTFF", "Tor is not on.");
		       torService.setProfile(TOR_STATUS_ON);
		       dialogWaitOnTor.sendEmptyMessage(0);
		       while (torService.getStatus() != TOR_STATUS_ON) {
		    	   Log.v("AFTFF", "Still waiting on Tor...");
		    	   try {
					Thread.currentThread().sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		       }
		  }
		
		  dialogDismiss.sendEmptyMessage(0);
			   
		  if (justCreated) {
			   justCreated = false;
		  }
			
		  
	} catch (RemoteException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	}
	
	
	
	
	

	private Handler dialogDismiss = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	              	dialog.dismiss();
	        }
	 };
	 
	 private Handler dialogWaitOnTor = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	              	dialog.setMessage("Waiting for Tor to come online.");
	        }
	 };
	 
	 private Handler dialogWaitOnNewMsgService = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	              	dialog.setMessage("Connecting to new message service.");

	        }
	 };
	 
	 
	 private Handler stopTitleProgressBar = new Handler() {
		 @Override
		 public void handleMessage(Message msg) {
           	    //setTitle("aftff");
		        //setProgressBarIndeterminateVisibility(false);
		 }
	 };
	 
	 
	public void onResume() {
		 super.onResume();
		 
		 
		 dialog = ProgressDialog.show(this, "", "Connecting to Tor service...", true);
	     Thread thread = new Thread(this); 
	     thread.start();
	        	        
	       
	        
		 
		 
	 }
	
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

        
        // Check tor status
        Intent torServiceIntent = new Intent();
        torServiceIntent.setAction("org.torproject.android.service.ITorService");
        boolean isBoundTor = bindService(torServiceIntent,mTorConn,Context.BIND_AUTO_CREATE);
        
        
        Intent serviceIntent = new Intent(this,NewMessageService.class);
        boolean isBound = bindService(serviceIntent,mNewMsgConn,Context.BIND_AUTO_CREATE);
        startService(serviceIntent);
        
        // indicate we were just created
        justCreated = true;
        


        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.customtitlebar);

  	   
        final Button mLatestMessagesButton = (Button) findViewById(R.id.mLatestMessages);
        mLatestMessagesButton.setOnClickListener(mLatestMessages);
        
        TextView postButton = (TextView) findViewById(R.id.latestmessagesTitlePostButton);
		postButton.setOnClickListener(postClicked);
		
		Button panicButton = (Button) findViewById(R.id.panicButton);
		panicButton.setOnClickListener(panicButtonClicked);
		
		Button shareOrbotButton = (Button) findViewById(R.id.shareOrbotButton);
		shareOrbotButton.setOnClickListener(shareOrbotButtonClicked);
		Button shareAftffButton = (Button) findViewById(R.id.shareAftffButton);
		shareAftffButton.setOnClickListener(shareAftffButtonClicked);
        
       
        final Button mManageRingsButton = (Button) findViewById(R.id.mManageRings);
        mManageRingsButton.setOnClickListener(mManageRings); 
      
        
      
        if (!isBoundTor) {
        	Log.v("AFTFF", "failed to bind, service conn definitely busted.\n");
        } else if (torService == null) {
        	Log.v("AFTFF", "Service is bound but torService is null.");
        } else {
        	Log.v("AFTFF", "Hey, we are bound to the tor service\n");
        }   
       
        if (!isBound) {
        	Log.v("Aftff", "IMessageService is not bound.");
        } else {
        	Log.v("Aftff", "IMessageService is bound.");
        }
       
    }
    
    public View.OnClickListener postClicked = new View.OnClickListener() {
    	public void onClick(View v) {
    		Intent intent = new Intent(getApplicationContext(),RingList.class);
    		intent.putExtra("action",RingList.WRITE);
    		startActivity(intent);
    	}
    };
    
    public View.OnClickListener panicButtonClicked = new View.OnClickListener() {
    	public void onClick(View v) {
    		Intent intent = new Intent(Intent.ACTION_DELETE);
    		String packageName = "org.aftff.client";
    		Uri data = Uri.fromParts("package", packageName, null);
    		intent.setData(data);
    		startActivity(intent);
    	}
    };
    
    public View.OnClickListener shareOrbotButtonClicked = new View.OnClickListener() {
    	public void onClick(View v) {
    		Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
			intent.putExtra("ENCODE_DATA","http://www.torproject.org/dist/android/0.2.2.13-alpha-orbot-0.0.8.apk");
			intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
			startActivity(intent);
    	}
    };
    
    public View.OnClickListener shareAftffButtonClicked = new View.OnClickListener() {
    	public void onClick(View v) {
    		Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
			intent.putExtra("ENCODE_DATA","http://unionoftheother.org/android/aftff.apk");
			intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
			startActivity(intent);
    	}
    };
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.clear();
    
    	
    	
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        
     
        
        //menu.add("Create Identity");
        //menu.add("List Identities");
        //menu.add("Create Ring");
        
        //menu.add("Options");
       
        
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	
		//if (item.toString().equals("Create Ring")) {
		//	startActivity(new Intent(this, CreateRing.class));
		//	return true;
		if (item.toString().equals("Identities")) {
			startActivity(new Intent(this,IdentityList.class));
			return true;
		//} else if (item.toString().equals("Create Identity")) {
		//	startActivity(new Intent(this,GenerateIdentity.class));
		//	return true;
		} else if (item.toString().equals("Options")) {
			startActivity(new Intent(this,Preferences.class));
			return true;
		}


		return true;

	}
    
    
    
    
    private void selectMsg(Ring r) {
    	Intent intent = new Intent(this,LatestMessages.class);
    	intent.putExtra("ring",r.getFullText());
    	return;
    }
    
    private void showRings(Integer action) {
    	Intent intent = new Intent(this,RingList.class);
    	intent.putExtra("action", action);
    	
    	
    	startActivity(intent);
    	return;
    }
    
    private void createRing() {
    	startActivity(new Intent(this,CreateRing.class));
    	return;
    
    }
    
    private void showLatestMessages() {
    	startActivity(new Intent(this,LatestMessages.class));
    	return;
    }
    
    
 
    
  
    
    private void createIdentity() {
    	startActivity(new Intent(this,GenerateIdentity.class));
    	return;
    }
    
    private void showIdentities() {
    	startActivity(new Intent(this,IdentityList.class));
    	return;
    }
    
   
    
    public Button.OnClickListener mManageRings = new Button.OnClickListener() {
	    public void onClick(View v) {
	        showRings(RingList.ANY);
	 }
    };
    
    public Button.OnClickListener mLatestMessages = new Button.OnClickListener() {
	    public void onClick(View v) {
	    	showLatestMessages();
	    }
    };
    
    
    
    
    
    
    
    
    
    
    
    private ServiceConnection mTorConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder service) {
        	torService = ITorService.Stub.asInterface(service);
        	if (torService == null) {
        		Log.e("AFTFF", "torService is null in mTorConn Service Connection callback.");
        	}

        }

        public void onServiceDisconnected(ComponentName className) {
           torService = null;
        }
    };
    
    private ServiceConnection mNewMsgConn = new ServiceConnection() {

		public void onServiceConnected(ComponentName className,
                IBinder service) {
        	newMsgService = IMessageService.Stub.asInterface(service);
        	Log.v("Aftff", "onServiceConnected called.");
        	if (newMsgService == null) {
        		Log.e("AFTFF", "newMsgService is null ");
        	}

        }

        public void onServiceDisconnected(ComponentName className) {
           newMsgService = null;
        }
    };
    

    
    
    
    
    
    
    
    
    
	public static String genHexHash(String data) {
		MessageDigest sha = null;
		try {
			sha = MessageDigest.getInstance("SHA");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sha.reset();
		
		
		sha.update(data.getBytes());
		byte messageDigest[] = sha.digest();
		
	            
		StringBuffer hexString = new StringBuffer();
		for (int i=0;i<messageDigest.length;i++) {
			String hex = Integer.toHexString(0xFF & messageDigest[i]); 
			if(hex.length()==1)
			  hexString.append('0');
			hexString.append(hex);
		}
	    return(new String(hexString));
		
	}
	
    
}
