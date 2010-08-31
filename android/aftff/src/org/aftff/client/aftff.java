package org.aftff.client;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.aftff.client.data.Identity;
import org.aftff.client.data.IdentityStore;
import org.aftff.client.data.Ring;
import org.aftff.client.data.RingStore;
import org.aftff.client.ui.CreateRing;
import org.aftff.client.ui.GenerateIdentity;
import org.aftff.client.ui.IdentityList;
import org.aftff.client.ui.MsgList;
import org.aftff.client.ui.Preferences;
import org.aftff.client.ui.RingList;
import org.aftff.client.ui.TorNotAvailable;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;

import uk.ac.cam.cl.dtg.android.tor.TorProxyLib.SocksProxy;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.app.Service;
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

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.conn.scheme.Scheme;
import org.apache.http.entity.BasicHttpEntity;
import org.torproject.android.service.ITorService;




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
			   migratePrefRings();
			   primeTor();
		  }
			   //AftffHttp aHttp = new AftffHttp();
			   //HttpHead httpHead = new HttpHead("http://www.google.com");
		       //HttpResponse resp = aHttp.httpClient.execute(httpHead);
			   //dialog.setMessage("Poking tor...");
		       
		  
	} catch (RemoteException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	}
	
	
	
	private void primeTor() {
		RingStore rs = new RingStore(getApplicationContext(),true);
		for (Ring r : rs) {
			Log.v("PrimeTor", r.getShortname() + ": fetching last message.");
			Integer lastMessage = r.getMsgIndex();
			if (lastMessage != null) {
				Log.v("PrimeTor", r.getShortname() + ": updating lastMessage.");
				r.updateLastMessage(lastMessage);
			}
		}
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
	 
	 
	public void onResume() {
		 super.onResume();
		 
		// Thread thread = new Thread(this);
		// thread.start();
		 
		 dialog = ProgressDialog.show(this, "", "Connecting to Tor service...", true);
	     Thread thread = new Thread(this); 
	     thread.start();
	        
	        
	        
	       
	       
	        
		 
		 
	 }
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        
        // Check tor status
        Intent torServiceIntent = new Intent();
        torServiceIntent.setAction("org.torproject.android.service.ITorService");
        boolean isBound = bindService(torServiceIntent,mTorConn,Context.BIND_AUTO_CREATE);
        
        
        // indicate we were just created
        justCreated = true;
        
       
        setContentView(R.layout.main);
        
        final Button button = (Button) findViewById(R.id.mScan);
        button.setOnClickListener(mScan);
        
        final Button shareButton = (Button) findViewById(R.id.mShare);
        shareButton.setOnClickListener(mShare);
        
        final Button readMsgsButton = (Button) findViewById(R.id.mReadMsgs);
        readMsgsButton.setOnClickListener(mReadMsgs);
        
        final Button writeMsgButton = (Button) findViewById(R.id.mWriteMsg);
        writeMsgButton.setOnClickListener(mWriteMsg);
        
        final Button createRingButton = (Button) findViewById(R.id.mCreateRing);
        createRingButton.setOnClickListener(mCreateRing);
        
        if (!isBound) {
        	Log.v("AFTFF", "failed to bind, service conn definitely busted.\n");
        } else if (torService == null) {
        	Log.v("AFTFF", "Service is bound but torService is null.");
        } else {
        	Log.v("AFTFF", "Hey, we are bound to the tor service\n");
        }   
        
       
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.clear();
    
    	
    	
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        
     
        
        menu.add("Generate Identity");
        menu.add("List Identities");
        //menu.add("Create Ring");
    
        boolean serviceRunning = false;
        ActivityManager actMan = (ActivityManager) getSystemService( ACTIVITY_SERVICE );
        List<RunningServiceInfo> runningServices = actMan.getRunningServices(50);
        for (RunningServiceInfo service : runningServices) {
        	//menu.add("srvc: " + service.service.getClassName());
        	//menu.add("msgservc: " + NewMessageService.class.getName());
        	if (service.service.getClassName().equals(NewMessageService.class.getName())) {
        		serviceRunning = true;
        		break;
        	}
        }
        
        
        if (serviceRunning == false) {
        	menu.add("Start Service");
        } else {
        	menu.add("Stop Service");
        }
        
       // menu.add("Check Tor Connectivity");
        menu.add("Options");
       
        
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	
		if (item.toString().equals("Create Ring")) {
			startActivity(new Intent(this, CreateRing.class));
			return true;
		} else if (item.toString().equals("Start Service")) {
			//Intent intent = new Intent(this,NewMessageService.class));
			startService(new Intent(this,NewMessageService.class));
			return true;
		} else if (item.toString().equals("Stop Service")) {
			stopService(new Intent(this,NewMessageService.class));
			return true;
		} else if (item.toString().equals("List Identities")) {
			startActivity(new Intent(this,IdentityList.class));
			return true;
		} else if (item.toString().equals("Generate Identity")) {
			startActivity(new Intent(this,GenerateIdentity.class));
			return true;
		} else if (item.toString().equals("Options")) {
			startActivity(new Intent(this,Preferences.class));
			return true;
		}
//		} else if (item.toString().equals("Check Tor Connectivity")) {
//			if (torService != null) {
//				TextView txt = new TextView(this);
//				try {
//					if (torService.getStatus() == TOR_STATUS_READY) {
//					  txt.setText("Tor is connected with status ready.");
//					} else if (torService.getStatus() == TOR_STATUS_OFF) {
//					  txt.setText("Tor is disconnected.");
//					} else if (torService.getStatus() == TOR_STATUS_CONNECTING) {
//						txt.setText("Tor is currently establishing a connection.");
//					} else if (torService.getStatus() == TOR_STATUS_ON) {
//						txt.setText("Tor is connected with status on.");
//					}
//				} catch (RemoteException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				setContentView(txt);
//				//Toast.makeText(getApplicationContext(), "Tor connected.", Toast.LENGTH_LONG);
//			
//			} else {
//				TextView txt = new TextView(this);
//				txt.setText("Not connected to tor service.");
//				setContentView(txt);
//				//Toast.makeText(getApplicationContext(),"Tor is not connected.", Toast.LENGTH_LONG);
//			}
		
	
//		Ring selectedRing = null;
//		for (Ring r : store) {
//			if (r.getShortname().equals(item.toString())) {
//			  selectedRing = r;
//			}
//		}
//		TextView txt = new TextView(this);
//		String contextInfo;
//		if (selectedRing.context == null) {
//			contextInfo = " is null.";
//		} else {
//			contextInfo = " is not null.";
//		}
//		txt.setText("Loading..." + selectedRing.getShortname() + " context " + contextInfo);
//		setContentView(txt);
//		
		
	//	startActivity(new Intent( this, MsgList.class));

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
    	//Bundle bundle = new Bundle();
    	//Bundle extr = intent.getExtras();
    	//extr.putInt("action", action);
    	
    	startActivity(intent);
    	return;
    }
    
    private void createRing() {
    	startActivity(new Intent(this,CreateRing.class));
    	return;
    
    }
    
    
    
    
    //FIXME: get rid of this duplication in Store.java
//    public static Store getStore(SharedPreferences prefs) {
//    	
//    	Store newStore = new Store();   	
//    	
//        String storeString = prefs.getString("store", null);
//        if (storeString == null || storeString.equals(""))
//        	return newStore;
//        
//        String[] storeArr = storeString.split("---");
//      
//        
//        for (String keyStr : storeArr) {
//        	if (keyStr == null)
//        		continue;
//        	Ring r = new Ring(keyStr);
//        	if (r == null)
//        		continue;
//        	newStore.add(r);
//        }
//        return(newStore);
//    	
//    }
    
  
    
    
    
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
    	            
      	              RingStore store = new RingStore(getApplicationContext());
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

	            RingStore store = new RingStore(getApplicationContext());
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
