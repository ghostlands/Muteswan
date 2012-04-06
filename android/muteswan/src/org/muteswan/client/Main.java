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
package org.muteswan.client;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;

import org.muteswan.client.data.Circle;
import org.muteswan.client.data.CircleStore;
import org.muteswan.client.data.MigrateToSqlCipher;
import org.muteswan.client.ui.CircleList;
import org.muteswan.client.ui.IdentityList;
import org.muteswan.client.ui.LatestMessages;
import org.muteswan.client.ui.Preferences;
import org.muteswan.client.ui.WriteMsg;
import org.torproject.android.service.ITorService;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import net.sqlcipher.database.SQLiteOpenHelper;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteStatement;




public class Main extends Activity implements Runnable {
	//Store store = null;
	//public static Circle activeCircle = null;

	public final static int TOR_STATUS_OFF = -1;
	public final static int TOR_STATUS_READY = 0;
	public final static int TOR_STATUS_ON = 1;
	public final static int TOR_STATUS_CONNECTING = 2;
	
	public final static String TOR_NOT_AVAILABLE = "tornotavailable";
	public final static String TOR_AVAILABLE = "toravailable";

	
	public final static String PREFS = "MuteswanPrefs";
	public static final String UPGRADING_DATABASE = "upgrading_database";
	public static final String FINISHED_UPGRADING_DATABASE = "finished_upgrading_database";
	
	

    IMessageService newMsgService;

	private ProgressDialog checkTorDialog;
	
	
	public void run() {
		
		try {
			while (newMsgService == null || cipherSecret == null) {
				Thread.currentThread();
				Thread.sleep(15);
			}
			if (cipherSecret != null) {
			  migrateDatabase();
			  newMsgService.setSQLCipherSecret(cipherSecret);
			  newMsgService.checkTorStatus(torResultCallback);
			}
			
		} catch (RemoteException e) {
			
		} catch (InterruptedException e) {
			
		}
	}
	
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}
	

	@SuppressWarnings("unused")
	private Handler checkTorDialogDismiss = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	        	  if (checkTorDialog != null) {
	              	
	        		
	        		  
	              	
	        		checkTorDialog.dismiss();
	        	  }
	        }

			
	 };
	 
	 
		 

	 
	
	 
	
	 
	 
	private AlertDialogs alertDialogs;
	
	 
	
	public void onPause() {
		super.onPause();
		if (torNotAvailableReceiver != null)
			unregisterReceiver(torNotAvailableReceiver);
		

	}
	public void onResume() {
		 super.onResume();

			
		 if (torNotAvailableReceiver == null)
			 torNotAvailableReceiver = new TorNotAvailableReceiver();
		 IntentFilter intentFilter = new IntentFilter(Main.TOR_NOT_AVAILABLE);
		 registerReceiver(torNotAvailableReceiver, intentFilter);
  
		 
		 
		 //callSafeGetSecret();
		 
	 }
	
	public void onDestroy() {
		
		super.onDestroy();
		
		checkTorDialog = null;
		if (newMsgService != null) {
			unbindService(mNewMsgConn);
		}
	}
	
	private void getSafeSecret() {
		if (cipherSecret == null) {
		  Intent intent = new Intent("org.openintents.action.GET_PASSWORD");
		  intent.putExtra("org.openintents.extra.UNIQUE_NAME", "muteswan");
		  startActivityForResult(intent,0);
		}
	}
	
	private void setSafeSecret() {
		cipherSecret = Crypto.generateSQLSecret();
		Intent intent = new Intent("org.openintents.action.SET_PASSWORD");
		intent.putExtra("org.openintents.extra.UNIQUE_NAME", "muteswan");
		intent.putExtra("org.openintents.extra.PASSWORD", cipherSecret);
		
		startActivityForResult(intent,0);
		
		
		//try {
		//	newMsgService.setSQLCipherSecret(secret);
		//} catch (RemoteException e) {
		//	e.printStackTrace();
		//}
		
	}
	
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if (resultCode == RESULT_OK && intent.getAction().equals("org.openintents.action.GET_PASSWORD")) {
			MuteLog.Log("Main", "Got intent back " + intent.getAction());
			 String secret = intent.getStringExtra("org.openintents.extra.PASSWORD");
			 //try {
				//newMsgService.setSQLCipherSecret(secret);
				 //BOOK
				cipherSecret = secret;
				MuteLog.Log("Main", "Set SQL cipher!!");
			//} catch (RemoteException e) {
			//	e.printStackTrace();
			//}
		} else if (resultCode == RESULT_OK && intent.getAction().equals("org.openintents.action.SET_PASSWORD")) {
			defPrefs.edit().putBoolean("hasGeneratedSecret", true).commit();
		} else if (intent.getAction().equals("org.openintents.action.SET_PASSWORD")){
		 //	setSafePassword();
		 //	callSafeGetSecret();
			String secret = intent.getStringExtra("org.openintents.extra.PASSWORD");
			if (secret == null) {
			  defPrefs.edit().putBoolean("hasGeneratedSecret", false).commit();
			  MuteLog.Log("Main","We did not get a secret back for some reason when setting password, so setting hasGeneratedSecret to false");
			  finish();
			} else {
				defPrefs.edit().putBoolean("hasGeneratedSecret", true).commit();
			}
		} else if (intent.getAction().equals("org.openintents.action.GET_PASSWORD")) {
			MuteLog.Log("Main", "Get password failed with " + resultCode);
			setSafeSecret();
			finish();
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
        bindService(serviceIntent,mNewMsgConn,Context.BIND_AUTO_CREATE);
        //,Context.BIND_AUTO_CREATE);
        
        
        
        SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		boolean firstRun = defPrefs.getBoolean("firstrun", true);
		if (firstRun) {
			// FIXME better flag and duped in NewMessageService.java
			File isUpgraded = new File(getFilesDir() + "/" + "is_upgraded");
			File db = new File("/data/data/org.muteswan.client/databases/muteswandb");
			
			
				if (!db.exists()) {
					try {
					  isUpgraded.createNewFile();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			
			defPrefs.edit().putBoolean("firstrun", false).commit();
			
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//CircleStore cs = new CircleStore(cipherSecret,this,true,false);
			//cs.updateStore("dd85381ac8acc1a7", "Feedback", "circles.muteswan.org");
		}
		
		//Boolean hasGeneratedSecret = defPrefs.getBoolean("hasGeneratedSecret", false);
		cipherSecret = defPrefs.getString("cipherSecret",null);
		//if (!hasGeneratedSecret) {
		//	setSafeSecret();
		if (cipherSecret == null) {
			getSafeSecret();
		}
        
		
        // start work activities
        Thread thread = new Thread(this); 
	    thread.start();
    
	    //setSafePassword();
	    
	   
	    
	    // initialize alert dialogs
	    alertDialogs = new AlertDialogs(this);

        setContentView(R.layout.main);
        
        
        final ImageView mLatestMessagesButton = (ImageView) findViewById(R.id.mLatestMessages);
        mLatestMessagesButton.setOnClickListener(mLatestMessages);
        
       
        
		
        final ImageView mManageCirclesButton = (ImageView) findViewById(R.id.mManageCircles);
        mManageCirclesButton.setOnClickListener(mManageCircles);
        
        
             
        PackageInfo pinfo = null;
        String versionNameString = null;
		try {
			pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			versionNameString = pinfo.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
        
        
        final TextView versionName = (TextView) findViewById(R.id.versionName);
        if (versionNameString != null)
          versionName.setText(versionNameString);
        
        
        
        /*if (cipherSecret != null) {
        	try {
        		int count = 0;
        		while (newMsgService == null && count <= 10) {
        			try {
						Thread.sleep(200);
						count++;
					} catch (InterruptedException e) {
					}
        		}
				newMsgService.setSQLCipherSecret(cipherSecret);
			} catch (RemoteException e) {
				
			}
        } else {
            getSafeSecret();
        }*/
       
        
        
        
	    
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
	private String cipherSecret;
    
    
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
			intent.putExtra("ENCODE_DATA","market://search?q=pname:org.muteswan.client");
			intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
			try {
			  startActivity(intent);
			} catch (ActivityNotFoundException e) {
			  alertDialogs.offerToInstallBarcodeScanner();
			}
		//} else if (item.toString().equals("Share Orbot")) {
		//	Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
		//	intent.putExtra("ENCODE_DATA",getString(R.string.orbot_market_uri));
		//	intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
		//	try {
		//	  startActivity(intent);
		//	} catch (ActivityNotFoundException e) {
		//	  alertDialogs.offerToInstallBarcodeScanner();
		//	}
		} else if (item.toString().equals("Uninstall Muteswan")) {
			Intent intent = new Intent(Intent.ACTION_DELETE);
    		String packageName = "org.muteswan.client";
    		Uri data = Uri.fromParts("package", packageName, null);
    		intent.setData(data);
    		startActivity(intent);
		} else if (item.toString().equals("About")) {
			showAbout();
		} else if (item.toString().equals("Settings")) {
			startActivity(new Intent(this,Preferences.class));
			return true;
		}


		return true;

	}
    
    
    
    
    private void showCircles(Integer action) {
    	Intent intent = new Intent(this,CircleList.class);
    	intent.putExtra("action", action);
    	intent.putExtra("secret", cipherSecret);
    	
    	
    	startActivity(intent);
    	return;
    }
    
    private void showLatestMessages() {
    	Intent intent = new Intent(this,LatestMessages.class);
    	intent.putExtra("secret", cipherSecret);
    	startActivity(intent);
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
    
    
    
    
    
    
    
    private ServiceConnection mNewMsgConn = new ServiceConnection() {

		public void onServiceConnected(ComponentName className,
                IBinder service) {
        	newMsgService = IMessageService.Stub.asInterface(service);
        	try {
				newMsgService.setSkipNextCheck(true);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
        	MuteLog.Log("Muteswan", "onServiceConnected called.");
        	if (newMsgService == null) {
        		Log.e("Muteswan", "newMsgService is null ");
        	}

        }

        public void onServiceDisconnected(ComponentName className) {
           newMsgService = null;
        }
    };
    
    private void showAbout() {

            LayoutInflater li = LayoutInflater.from(this);
            View view = li.inflate(R.layout.about, null);

            String version = "";

            try {
                    version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            } catch (NameNotFoundException e) {
                    version = "Version Not Found";
            }

            TextView versionName = (TextView)view.findViewById(R.id.versionName);
            versionName.setText(version);

                    new AlertDialog.Builder(this)
            .setTitle("About Muteswan")
            .setView(view)
            .show();
    }

    
    
	public static String genHexHash(String data) {
		MessageDigest sha = null;
		try {
			sha = MessageDigest.getInstance("SHA");
		} catch (NoSuchAlgorithmException e) {
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

			public void torFailure() throws RemoteException {
				alertDialogs.dialogTorNotAvailable.sendEmptyMessage(0);
			}

			public void torSuccess() throws RemoteException {
			}
			
	};
	
	

	private class TorNotAvailableReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
			alertDialogs.dialogTorNotAvailable.sendEmptyMessage(0);
	    }
	}
	
	
	private TorNotAvailableReceiver torNotAvailableReceiver;
	

	
	
	
private boolean migrateDatabase() {
		
		MigrateToSqlCipher migrate = new MigrateToSqlCipher();
		if (!migrate.needsMigration(this))
			return(false);
	
		//sendBroadcast(new Intent(Main.UPGRADING_DATABASE));
		alertDialogs.upgradingDatabase.sendEmptyMessage(0);
		
		File oldDb = new File("/data/data/org.muteswan.client/databases/muteswandb");
		oldDb.renameTo(new File("/data/data/org.muteswan.client/databases/muteswandbOld"));
		
		
		File dbDir = new File("/data/data/org.muteswan.client/databases/");
		File[] files = dbDir.listFiles();
		for (int i = 0; i<files.length;i++) {
			MuteLog.Log("NewMessageService", "File is " + files[i].getName());
			if (files[i].getName().equals("muteswandbOld"))
				continue;
			files[i].delete();
		}
		
		//if (true) return true;
		//File newDb = new File("/data/data/org.muteswan.client/databases/muteswandb");
		//oldDb.delete();
		//newDb.renameTo(new File("/data/data/org.muteswan.client/databases/muteswandb"));
		
		
		LinkedList<String[]> circles = migrate.getOldCircleData();
		SQLiteDatabase.loadLibs(this);
		
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase("/data/data/org.muteswan.client/databases/muteswandb", cipherSecret, null);
		db.execSQL("CREATE TABLE rings (id INTEGER PRIMARY KEY, shortname TEXT, key TEXT, server TEXT);");
		
		
		
		for (String[] s : circles) {
			MuteLog.Log("NewMessageService", "On Migration got: " + s[0]);
			db.execSQL("INSERT INTO rings (shortname,key,server) VALUES('"+s[0]+"','"+s[1]+"','"+s[2]+"');");
			Circle newCircle = new Circle(cipherSecret,this,s[0],s[1],s[2]);
			newCircle.createLastMessage(0, true);
			//SQLiteStatement insert = db.compileStatement("INSERT INTO " + Circle.OpenHelper.LASTMESSAGES + " (ringHash,lastMessage,lastCheck) VALUES(?,?,datetime('now'))");
			 //insert.bindString(1,Main.genHexHash(getFullText()));
			 //insert.bindLong(2, 0);
			 //insert.executeInsert();
			
		}
		db.close();
		//return true;
		
		
		/*
		SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase("/data/data/org.muteswan.client/databases/muteswandbEnc", "", null);
		
		db.execSQL("PRAGMA KEY = '" + cipherSecret + "';");
		db.execSQL("CREATE TABLE rings (id INTEGER PRIMARY KEY, shortname TEXT, key TEXT, server TEXT);");
		db.execSQL("ATTACH DATABASE '/data/data/org.muteswan.client/databases/muteswandb' AS plaintext KEY '';");
		db.execSQL("INSERT INTO rings SELECT * FROM plaintext.rings;");
		db.execSQL("DETACH DATABASE plaintext;");
		*/
		MuteLog.Log("CircleStore", "Done loading migration data.");
		
		
		// FIXME better flag and duped in Main.java
		File isUpgraded = new File(getFilesDir() + "/" + "is_upgraded");
		try {
			isUpgraded.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
				
		
		
		MuteLog.Log("CircleStore", "Done migrating.");
		//sendBroadcast(new Intent(Main.FINISHED_UPGRADING_DATABASE));
		alertDialogs.finishedUpgradingDatabase.sendEmptyMessage(0);
		return true;
	}
	
    
}
