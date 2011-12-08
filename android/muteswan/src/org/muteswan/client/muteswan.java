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
package org.muteswan.client;

import java.io.IOException;
import java.io.InputStream;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.muteswan.client.data.Identity;
import org.muteswan.client.data.IdentityStore;
import org.muteswan.client.data.Circle;
import org.muteswan.client.data.CircleStore;
import org.muteswan.client.ui.CreateCircle;
import org.muteswan.client.ui.GenerateIdentity;
import org.muteswan.client.ui.IdentityList;
import org.muteswan.client.ui.LatestMessages;
import org.muteswan.client.ui.WriteMsg;

import org.muteswan.client.ui.Preferences;
import org.muteswan.client.ui.CircleList;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.util.EntityUtils;
import org.torproject.android.service.ITorService;

import uk.ac.cam.cl.dtg.android.tor.TorProxyLib.SocksProxy;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;




public class muteswan extends Activity implements Runnable {
	//Store store = null;
	//public static Circle activeCircle = null;

	public final static int TOR_STATUS_OFF = -1;
	public final static int TOR_STATUS_READY = 0;
	public final static int TOR_STATUS_ON = 1;
	public final static int TOR_STATUS_CONNECTING = 2;
	
	public final static String TOR_NOT_AVAILABLE = "tornotavailable";
	public final static String TOR_AVAILABLE = "toravailable";

	
	public final static String PREFS = "MuteswanPrefs";
	private ProgressDialog dialog;
	
	
	public static ITorService torService;

    IMessageService newMsgService;

	private boolean justCreated;
	
	
	private ProgressDialog checkTorDialog;
	
	
	@Override
	public void run() {
		
		//dialog.show();
		//dialogWaitOnNewMsgService.sendEmptyMessage(0);
        //Intent serviceIntent = new Intent(this,NewMessageService.class);
      
		
//		while (newMsgService == null) {
//			try {
//				Thread.currentThread().sleep(500);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//   
//		
//		  try {
//				if (!newMsgService.isWorking()) {
//					startService(serviceIntent);
//				}
//			} catch (RemoteException e1) {
//				// TODO Auto-generated catch block
//				e1.printStackTrace();
//			}
//		
//		  if (justCreated) {
//			   justCreated = false;
//		  }
			
		
		
		//ORIGINAL TOR CHECK 
		//if (alreadyCheckedTor == false) {
		//  TorStatus checkTorStatus = new TorStatus();
		//  if (checkTorStatus.checkStatus()) {
		//	checkTorDialogDismiss.sendEmptyMessage(0);
		//  } else {
		//	dialogTorNotAvailable.sendEmptyMessage(0);
		 // }
		 // alreadyCheckedTor = true;
		//}
		  
		try {
			while (newMsgService == null) {
				Thread.currentThread();
				Thread.sleep(15);
			}
			newMsgService.checkTorStatus(torResultCallback);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}
	

	private Handler checkTorDialogDismiss = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	        	  if (checkTorDialog != null) {
	              	
	        		
	        		  
	              	
	        		checkTorDialog.dismiss();
	        	  }
	        }

			
	 };
	 
	 private void scheduleServiceAlarm() { 
		  SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
 		
 		  boolean backgroundMessageCheck = defPrefs.getBoolean("backgroundMessageCheck", false);			
 		  //if (backgroundMessageCheck == true) {
 		  //	Intent serviceIntent = new Intent(this,NewMessageService.class);
 		  //	startService(serviceIntent);
 		  //}
 		  if (backgroundMessageCheck == true) {
 		   Integer checkMsgInterval = Integer.parseInt(defPrefs.getString("checkMsgInterval", "5"));
 		
 		   int checkMsgIntervalMs = checkMsgInterval * 60 * 1000;
 		
 		   AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
 		   alarm.setRepeating(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime()+checkMsgInterval*60,checkMsgIntervalMs,NewMessageReceiver.getPendingIntent(getApplicationContext()));
 		  }
			
	}
	 
	 private Handler dialogTorAvailable = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	          	   AlertDialog.Builder dialog = new AlertDialog.Builder(muteswan.this);
	       		    dialog.setTitle("Tor available");
	       		    dialog.create();
	       		    dialog.show();     		

	        		
	       }
	 };
	 
	 private Handler dialogTorNotAvailable = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	        		  offerToStartTor();
	        }

			private void offerToStartTor() {
				AlertDialog.Builder noTorDialog = new AlertDialog.Builder(muteswan.this);
    		    noTorDialog.setTitle("Tor Unavailable");
    		    noTorDialog.setMessage("Tor is not available at this time. Please start Tor or ensure it is running properly. Only cached data will be available otherwise.");
    		    noTorDialog.setPositiveButton("Start Tor?", new DialogInterface.OnClickListener() {
    		      public void onClick(DialogInterface dialogInterface, int i) {
    		      
    		    	Intent intent = null;
    		    	try {
    		    	  intent = new Intent("org.torproject.android.START_TOR");
    		    	  startActivity(intent);
    		    	} catch (ActivityNotFoundException e) {
    		    	  offerToInstallTor();
    		          
    		    	}
    		      }
    		    });
    		    noTorDialog.setNegativeButton("No, thanks", new DialogInterface.OnClickListener() {
    		      public void onClick(DialogInterface dialogInterface, int i) {}
    		    });
    		    noTorDialog.create();
    		    noTorDialog.show();
			}
			
			private void offerToInstallTor() {
				AlertDialog.Builder noTorDialog = new AlertDialog.Builder(muteswan.this);
    		    noTorDialog.setTitle("Install Tor?");
    		    noTorDialog.setMessage("Tor is not currently installed. Do you want to install it from the market?");
    		    noTorDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
    		      public void onClick(DialogInterface dialogInterface, int i) {
    		        Uri uri = Uri.parse("market://search?q=pname:org.torproject.android");
    		    	Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    		        startActivity(intent);
    		      }
    		    });
    		    noTorDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
    		      public void onClick(DialogInterface dialogInterface, int i) {}
    		    });
    		    noTorDialog.create();
    		    noTorDialog.show();
			}
			
			
	 };
	 
	
	 
	 private Handler dialogWaitOnNewMsgService = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	              	dialog.setMessage("Connecting to new message service.");
	        }
	 };
	 
	 
	private boolean alreadyCheckedTor;
	private Builder torStatusDialog;
	
	 
	
	public void onPause() {
		super.onPause();
		if (torNotAvailableReceiver != null)
			unregisterReceiver(torNotAvailableReceiver);

	}
	public void onResume() {
		 super.onResume();

		 // TorStatus torStatus = new TorStatus(torService);
	     // torStatus.checkView(postButton);

		 
		 //if (alreadyCheckedTor == false) {
		   //showCheckTorDialog();
		   scheduleServiceAlarm();		   
		 //  alreadyCheckedTor = true;
		 //}
			
		 if (torNotAvailableReceiver == null)
			 torNotAvailableReceiver = new TorNotAvailableReceiver();
		 IntentFilter intentFilter = new IntentFilter(muteswan.TOR_NOT_AVAILABLE);
		 registerReceiver(torNotAvailableReceiver, intentFilter);
  
		   
		 
	 }
	
	public void onDestroy() {
		
		super.onDestroy();
		
		checkTorDialog = null;
		if (newMsgService != null) {
			unbindService(mNewMsgConn);
		}
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
       
        
        // Check tor status
        //Intent torServiceIntent = new Intent();
        
        
        //torServiceIntent.setAction("org.torproject.android.service.ITorService");
        //boolean isBoundTor = bindService(torServiceIntent,mTorConn,Context.BIND_AUTO_CREATE);
        
        //TorStatus torStatus = new TorStatus(torService);
        
        
        // SERVICE BIND
        Intent serviceIntent = new Intent(this,NewMessageService.class);
        boolean isBound = bindService(serviceIntent,mNewMsgConn,Context.BIND_AUTO_CREATE);
        
        
        
        // indicate we were just created
       
        justCreated = true;
        

        Thread thread = new Thread(this); 
	    thread.start();
     

        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        

        
        final ImageView mLatestMessagesButton = (ImageView) findViewById(R.id.mLatestMessages);
        mLatestMessagesButton.setOnClickListener(mLatestMessages);
        
       
        
		
		
		//Button panicButton = (Button) findViewById(R.id.panicButton);
		//panicButton.setOnClickListener(panicButtonClicked);
		
		//Button shareOrbotButton = (Button) findViewById(R.id.shareOrbotButton);
		//shareOrbotButton.setOnClickListener(shareOrbotButtonClicked);
		//Button shareMuteswanButton = (Button) findViewById(R.id.shareMuteswanButton);
		//shareMuteswanButton.setOnClickListener(shareMuteswanButtonClicked);
        
       
        final ImageView mManageCirclesButton = (ImageView) findViewById(R.id.mManageCircles);
        mManageCirclesButton.setOnClickListener(mManageCircles);
        
        //final Button mReadMsgsButton = (Button) findViewById(R.id.mReadMsgs);
        //mReadMsgsButton.setOnClickListener(mReadMsgs); 
        
        //final Button mWriteMsgButton = (Button) findViewById(R.id.mWriteMsg);
        //mWriteMsgButton.setOnClickListener(mWriteMsg); 
        
        //final Button mShareCircleButton = (Button) findViewById(R.id.mShare);
        //mShareCircleButton.setOnClickListener(mShareCircle); 
        
        //final Button mScanCircleButton = (Button) findViewById(R.id.mScan);
        //mScanCircleButton.setOnClickListener(mScanCircle); 
        
        //final Button mCreateCircleButton = (Button) findViewById(R.id.mCreateCircle);
        //mCreateCircleButton.setOnClickListener(mCreateCircle); 
      
        
        
             
        //if (!isBound) {
        //	Log.v("Muteswan", "IMessageService is not bound.");
        //} else {
        //	Log.v("Muteswan", "IMessageService is bound.");
        //}
     
	    
        PackageInfo pinfo = null;
        String versionNameString = null;
		try {
			pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			versionNameString = pinfo.versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        
        final TextView versionName = (TextView) findViewById(R.id.versionName);
        if (versionNameString != null)
          versionName.setText(versionNameString);
	    
    }
    
    private void showCheckTorDialog() {
    	
    	if (checkTorDialog == null) {
    	  checkTorDialog = ProgressDialog.show(this, "", "Verifying secure connection to Tor network..", true);
    	  checkTorDialog.setCancelable(true);
    	  
    	}
	}






	public View.OnClickListener postClicked = new View.OnClickListener() {
    	public void onClick(View v) {
    		Intent intent = new Intent(getApplicationContext(),WriteMsg.class);
    		
    		startActivity(intent);
    	}
    };
    
    public View.OnClickListener panicButtonClicked = new View.OnClickListener() {
    	public void onClick(View v) {
    		Intent intent = new Intent(Intent.ACTION_DELETE);
    		String packageName = "org.muteswan.client";
    		Uri data = Uri.fromParts("package", packageName, null);
    		intent.setData(data);
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
        //menu.add("Create Circle");
        
        //menu.add("Options");
       
        
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	
		//if (item.toString().equals("Create Circle")) {
		//	startActivity(new Intent(this, CreateCircle.class));
		//	return true;
		if (item.toString().equals("Identities")) {
			startActivity(new Intent(this,IdentityList.class));
			return true;
		//} else if (item.toString().equals("Create Identity")) {
		//	startActivity(new Intent(this,GenerateIdentity.class));
		//	return true;
		} else if (item.toString().equals("Share Muteswan")) {
			Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
			intent.putExtra("ENCODE_DATA","http://muteswan.org/android/muteswan-latest.apk");
			intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
			startActivity(intent);
		} else if (item.toString().equals("Share Orbot")) {
			Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
			intent.putExtra("ENCODE_DATA","market://search?q=pname:org.torproject.android");
			intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
			startActivity(intent);
		} else if (item.toString().equals("Reset Muteswan")) {
			Intent intent = new Intent(Intent.ACTION_DELETE);
    		String packageName = "org.muteswan.client";
    		Uri data = Uri.fromParts("package", packageName, null);
    		intent.setData(data);
    		startActivity(intent);
		} else if (item.toString().equals("Options")) {
			startActivity(new Intent(this,Preferences.class));
			return true;
		}


		return true;

	}
    
    
    
    
    private void selectMsg(Circle r) {
    	Intent intent = new Intent(this,LatestMessages.class);
    	intent.putExtra("circle",r.getFullText());
    	return;
    }
    
    private void showCircles(Integer action) {
    	Intent intent = new Intent(this,CircleList.class);
    	intent.putExtra("action", action);
    	
    	
    	startActivity(intent);
    	return;
    }
    
    private void createCircle() {
    	startActivity(new Intent(this,CreateCircle.class));
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
    
   
    
    public Button.OnClickListener mManageCircles = new Button.OnClickListener() {
	    public void onClick(View v) {
	        showCircles(CircleList.ANY);
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
        		Log.e("Muteswan", "torService is null in mTorConn Service Connection callback.");
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
        	try {
				newMsgService.setUserChecking(true);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	Log.v("Muteswan", "onServiceConnected called.");
        	if (newMsgService == null) {
        		Log.e("Muteswan", "newMsgService is null ");
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
	
	
	
	 private final ITorVerifyResult.Stub torResultCallback = new ITorVerifyResult.Stub() {

			@Override
			public void torFailure() throws RemoteException {
				dialogTorNotAvailable.sendEmptyMessage(0);
			}

			@Override
			public void torSuccess() throws RemoteException {
				//dialogTorAvailable.sendEmptyMessage(0);
			}
			
	};
	
	

	private class TorNotAvailableReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	dialogTorNotAvailable.sendEmptyMessage(0);
	    }
	}

	private TorNotAvailableReceiver torNotAvailableReceiver;
    
}
