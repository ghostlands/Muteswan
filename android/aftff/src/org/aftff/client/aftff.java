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
		//	Log.v("AFTFF", "new message service still null.");
		}
		//Log.v("AFTFF", "new message service not null.");

	  try {
		Log.v("AFFF", "Getting tor status");
		if (torService.getStatus() != TOR_STATUS_ON) {
			   Log.v("AFTFF", "Tor is not on.");
		       //dialog.dismiss();
		       torService.setProfile(TOR_STATUS_ON);
		       //dialog.setMessage("Starting Tor...");
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
		       //startActivity(new Intent(this,TorNotAvailable.class));
		  }
		
		  dialogDismiss.sendEmptyMessage(0);
			   
		  if (justCreated) {
			   justCreated = false;
			   //migratePrefRings();
			   fetchLatestMessageData();
		  }
			
		  
	} catch (RemoteException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	}
	
	
	
	private void fetchLatestMessageData() {
		try {
			Log.v("Aftff", "Running newMsgService updateMessages now");
			
			newMsgService.updateLastMessage();
			newMsgService.downloadMessages();
			while (newMsgService.isWorking()) {
				Thread.currentThread().sleep(500);
			}
			stopTitleProgressBar.sendEmptyMessage(0);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void primeTorOld() {
		RingStore rs = new RingStore(getApplicationContext(),true);
		for (Ring r : rs) {
			Log.v("PrimeTor", r.getShortname() + ": fetching last message.");
			Integer lastMessage = r.getMsgIndex();
			if (lastMessage != null) {
				Log.v("PrimeTor", r.getShortname() + ": updating lastMessage.");
				Integer numMsgDownload = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("numMsgDownload","5"));
				MSG: for (Integer i=lastMessage;i>lastMessage-numMsgDownload;i--) {
					try {
						if (i == 0)
							break MSG;
						
						Log.v("PrimeTor","Downloading message id " + i + " for ring " + r.getShortname());
						r.getMsg(i.toString());
					} catch (ClientProtocolException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				r.updateLastMessage(lastMessage);
			}
			
		}
		stopTitleProgressBar.sendEmptyMessage(0);
	}
	

	// destroy soon, used to automatically migrate rings to sql
	private void migratePrefRings() {
		
        SharedPreferences prefs = getSharedPreferences(PREFS,0);
        RingStore rs = new RingStore(getApplicationContext(),true);
        String[] storeArr = prefs.getString("store", "").split("---");
        
        for (String keyStr : storeArr) {
        	if (keyStr == null)
        		continue;
        	Ring r;
        	r = new Ring(getApplicationContext(),rs.getOpenHelper(),keyStr);
        	if (r == null)
        		continue;
        	rs.updateStore(keyStr);
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
           	    setTitle("aftff");
		        setProgressBarIndeterminateVisibility(false);
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
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        
        startService(new Intent(this,NewMessageService.class));
        
        // Check tor status
        Intent torServiceIntent = new Intent();
        torServiceIntent.setAction("org.torproject.android.service.ITorService");
        boolean isBoundTor = bindService(torServiceIntent,mTorConn,Context.BIND_AUTO_CREATE);
        
        
        Intent serviceIntent = new Intent(this,NewMessageService.class);
        boolean isBound = bindService(serviceIntent,mNewMsgConn,Context.BIND_AUTO_CREATE);
       
        
        // indicate we were just created
        justCreated = true;
        


        setContentView(R.layout.main);
  	    setTitle("aftff (checking for new messages)");
        setProgressBarIndeterminateVisibility(true);

        
        final Button button = (Button) findViewById(R.id.mScan);
        button.setOnClickListener(mScan);
        
        final Button mLatestMessagesButton = (Button) findViewById(R.id.mLatestMessages);
        mLatestMessagesButton.setOnClickListener(mLatestMessages);
        
       /* final Button shareButton = (Button) findViewById(R.id.mShare);
        shareButton.setOnClickListener(mShare);
        
        final Button readMsgsButton = (Button) findViewById(R.id.mReadMsgs);
        readMsgsButton.setOnClickListener(mReadMsgs);
        
        final Button writeMsgButton = (Button) findViewById(R.id.mWriteMsg);
        writeMsgButton.setOnClickListener(mWriteMsg); */
        
        final Button mManageRingsButton = (Button) findViewById(R.id.mManageRings);
        mManageRingsButton.setOnClickListener(mManageRings); 
        
        final Button createRingButton = (Button) findViewById(R.id.mCreateRing);
        createRingButton.setOnClickListener(mCreateRing);
        
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
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.clear();
    
    	
    	
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        
     
        
        menu.add("Create Identity");
        menu.add("List Identities");
    
        
        menu.add("Options");
       
        
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	
		if (item.toString().equals("Create Ring")) {
			startActivity(new Intent(this, CreateRing.class));
			return true;
		} else if (item.toString().equals("List Identities")) {
			startActivity(new Intent(this,IdentityList.class));
			return true;
		} else if (item.toString().equals("Create Identity")) {
			startActivity(new Intent(this,GenerateIdentity.class));
			return true;
		} else if (item.toString().equals("Options")) {
			startActivity(new Intent(this,Preferences.class));
			return true;
		}


		return true;

	}
    
    
    
    
    private void showRingOptions(Ring ring) {
    	
    }
    
    private void selectMsg(Ring r) {
    	Intent intent = new Intent(this,MsgList.class);
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
    
    
 
    
  
    
    
    
    public Button.OnClickListener mScan = new Button.OnClickListener() {
    	    public void onClick(View v) {
    	        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
    	        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
    	        startActivityForResult(intent, 0);
    	 }
    };
    
    public Button.OnClickListener mShare = new Button.OnClickListener() {
	    public void onClick(View v) {
	        showRings(RingList.SHARE);
	 }
    };
    
    public Button.OnClickListener mReadMsgs = new Button.OnClickListener() {
	    public void onClick(View v) {
	        
	        showRings(RingList.READ);

	 }
    };
    
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
    
    
    
    
    public Button.OnClickListener mWriteMsg = new Button.OnClickListener() {
	    public void onClick(View v) {
	        showRings(RingList.WRITE);
	 }
    };
    
    public Button.OnClickListener mCreateRing = new Button.OnClickListener() {
	    public void onClick(View v) {
	        createRing();
	 }
    };
    

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
    	            
      	              RingStore store = new RingStore(getApplicationContext(),true);
    	              Ring ring = new Ring(getApplicationContext(),store.getOpenHelper(),contents);
    	              store.updateStore(contents);
    	               
    	              //this.activeRing = ring;
    	              selectMsg(ring);
    	            
    	            // IDENTITY
    	            } else {
    	            	String[] parts = contents.split(":");
    	            	Identity identity = new Identity(parts[0],parts[1],parts[2]);
    	            	IdentityStore idStore = new IdentityStore(getApplicationContext());
    	            	idStore.addToDb(identity);
    	            }
    	            
    	            
    	            
            } else if (resultCode == RESULT_CANCELED) {
            	//final String testSite = "forest+0df46018575f1656@tckwndlytrphlpyo.onion";
            	//final String testSite = "2ndset+1522c03e8b9bae5d@tckwndlytrphlpyo.onion";
            	final String testSite = "testsite+dba4fe6ef22b494d@tckwndlytrphlpyo.onion";

	            RingStore store = new RingStore(getApplicationContext(),true);
            	Ring ring = new Ring(getApplicationContext(),store.getOpenHelper(),testSite);
 	            //updateStore(testSite);
	            store.updateStore(testSite);

            	            	
            	//this.activeRing = ring;
            	selectMsg(ring);
            
            }
    	  }
    }

    
    
    
    
    
    
    
    
    
    
    
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


	// BLECH! not used
    static public String getGetBody(String host, String getline) throws UnknownHostException, IOException {
    	  // Get the proxy port
        SocksProxy proxy = new SocksProxy(9050);

          
        // Create a socket to the destination through the
        // anonymous proxy
        Socket s = proxy.connectSocksProxy(null, host, 80, 0);
                   //Socket s = new Socket(HOST, PORT);

        PrintWriter writer = new PrintWriter(s.getOutputStream());
        InputStreamReader reader = new InputStreamReader(s.getInputStream());
        InputStream  is = s.getInputStream();

        
        // Very simple HTTP GET
        writer.println("GET " + getline + " HTTP/1.1");
        writer.println("Host: " + host);
        writer.println("Connection: close");
        writer.println();
        writer.flush();
        
        BasicHttpEntity resp = new BasicHttpEntity();
        resp.setContent(is);

        String body = EntityUtils.toString(resp);
        return(body);
        
//        // Get the result
//        StringBuilder result = new StringBuilder();
//        char[] buffer = new char[1024];
//        int read = 0;
//        do {
//                  result.append(buffer, 0, read);
//                  read = reader.read(buffer, 0, buffer.length);
//           } while (read > -1);
//        

        
        //return(result.toString());
    }


	
    
}
