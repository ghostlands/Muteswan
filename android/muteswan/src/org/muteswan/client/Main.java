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

import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.json.JSONException;
import org.json.JSONObject;
import org.muteswan.client.data.Circle;
import org.muteswan.client.data.CircleStore;
import org.muteswan.client.data.MigrateToEncPrefs;
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
import android.content.SharedPreferences.Editor;
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

import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

public class Main extends Activity implements Runnable {
	// Store store = null;
	// public static Circle activeCircle = null;

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

		
		SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		try {
			while (newMsgService == null || cipherSecret == null) {
				Thread.currentThread();
				Thread.sleep(15);
				String prefSecret = defPrefs.getString("cipherSecret", null);
				if (cipherSecret != null && prefSecret != null)
					cipherSecret = prefSecret;
			}
			if (cipherSecret != null) {
				migrateDatabase();
				newMsgService.setCipherSecret(cipherSecret);
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
	
	private Handler finishButtonHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			finish();
		}

	};
	

	private AlertDialogs alertDialogs;
	private boolean oiSafeNotInstalled = false;
	

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

		
		
		
		
		
		
		SharedPreferences defPrefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		String storedCipherSecret = defPrefs.getString("cipherSecret", null);
		Boolean verbose = defPrefs.getBoolean("verbose",false);
		boolean useoisafe = defPrefs.getBoolean("useoisafe", false);
		
		
		
		if (storedCipherSecret == null && verbose) {
			final ImageView noStoredSecret = (ImageView) findViewById(R.id.noStoredSecret);
			noStoredSecret.setImageResource(android.R.drawable.ic_secure);
			noStoredSecret.setVisibility(View.VISIBLE);
		}
		
		if (verbose) {
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
			}

		
		if (useoisafe && !oiSafeNotInstalled) {
			MuteLog.Log("Main","About to call getsafesecret");
			getSafeSecret();
		}
		
	}

	public void onDestroy() {

		super.onDestroy();

		checkTorDialog = null;
		if (newMsgService != null) {
			unbindService(mNewMsgConn);
		}
	}

	private void getSafeSecret() {
		
		  Intent intent = new Intent("org.openintents.action.GET_PASSWORD");
		  intent.putExtra("org.openintents.extra.UNIQUE_NAME", "muteswan");
		  MuteLog.Log("Main", "Calling getsafe secret ");
		  
		  try {
			  startActivityForResult(intent,0);
		  } catch (ActivityNotFoundException e) {
			  //alertDialogs.offerToInstallOISafe();
			  oiSafeNotInstalled = true;
		  } catch (java.lang.SecurityException e) {
			  MuteLog.Log("Main", "Security exception " + e); 
		  }
		
	}

	
	
	private void setSafeSecret() {
		
		Intent intent = new Intent("org.openintents.action.SET_PASSWORD");
		intent.putExtra("org.openintents.extra.UNIQUE_NAME", "muteswan");
		intent.putExtra("org.openintents.extra.PASSWORD", cipherSecret);

		MuteLog.Log("Main", "Calling setsafe secret "); 
		
		try {
		  startActivityForResult(intent,0);
		} catch (ActivityNotFoundException e) {
		  //alertDialogs.offerToInstallOISafe();
			oiSafeNotInstalled = true;
		} catch (java.lang.SecurityException e) {
			MuteLog.Log("Main", "Security exception " + e); 
		}
		
	

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		SharedPreferences defPrefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		
		// this is sort of necessarily complicated. we want to make sure
		// that unless oisafe certainly has our key we do not delete our
		// stored key. this should allow people who want security to
		// use oisafe, but protect against any number of problems otherwise
		// like oisafe not working, not installed, key changed, etc.
		
		if (resultCode == RESULT_OK
				&& intent.getAction().equals(
						"org.openintents.action.GET_PASSWORD")) {
			MuteLog.Log("Main", "Got intent back " + intent.getAction());
			String secret = intent
					.getStringExtra("org.openintents.extra.PASSWORD");
			MuteLog.Log("Main", "Get password success with " + resultCode);
			
			
			
			//boolean backgroundMessageCheck = defPrefs.getBoolean("backgroundMessageCheck", false);
			boolean keepSecret = defPrefs.getBoolean("keepsecret", false);
			
			// this means we got the secret from oisafe and it is the same as
			// currently stored one. this means we are good, and we can remove the
			// cipherSecret from the preferences. now we are safe.
			if (!keepSecret
					&& cipherSecret != null && cipherSecret.equals(secret)) {
			   defPrefs.edit().remove("cipherSecret").commit();
			   MuteLog.Log("Main", "Cipher secret is synced with oi safe and removed from muteswan.");
			   MuteLog.Log("Main", "Ciphers old: " + cipherSecret + " new: " + secret);
			// store the secret if we are supposed to
			} else if (keepSecret && secret != null) {
			   defPrefs.edit().putString("cipherSecret",secret).commit();
				
			} else if (secret != null && cipherSecret != null && !cipherSecret.equals("secret")) {
				MuteLog.Log("Main", "Cipher is different, we should reset it. old: " + cipherSecret + " new: " + secret);
				setSafeSecret();
			}
			
			if (cipherSecret == null) {
				  cipherSecret = secret;
				  MuteLog.Log("Main", "Set cipher!!");
			}
			
		
		} else if (resultCode == RESULT_OK
				&& intent.getAction().equals(
						"org.openintents.action.SET_PASSWORD")) {
			defPrefs.edit().putBoolean("hasGeneratedSecret", true).commit();
		} else if (intent != null && intent.getAction().equals(
				"org.openintents.action.SET_PASSWORD")) {
			MuteLog.Log("Main", "Set password success with " + resultCode);
			String secret = intent
					.getStringExtra("org.openintents.extra.PASSWORD");
			if (secret == null) {
				defPrefs.edit().putBoolean("hasGeneratedSecret", false)
						.commit();
				MuteLog.Log(
						"Main",
						"We did not get a secret back for some reason when setting password, so setting hasGeneratedSecret to false");
				//finish();
				//alertDialogs.noCipherSecretAvailable(finishButtonHandler);
			} else {
				defPrefs.edit().putBoolean("hasGeneratedSecret", true).commit();
				
			}
		} else if (intent != null && intent.getAction().equals(
				"org.openintents.action.GET_PASSWORD")) {
			MuteLog.Log("Main", "Get password failed with " + resultCode);
			setSafeSecret();
			//alertDialogs.noCipherSecretAvailable(finishButtonHandler);
			//finish();
		}
		
		
		// if we do not have a cipherSecret after all this we are stewed and need to notify the user of that reality.
		//if (cipherSecret == null) {
			//alertDialogs.noCipherSecretAvailable(finishButtonHandler);
			//finish();
		//}
		
	}

	
	private void firstRunInit(SharedPreferences defPrefs) {
		Editor editor = defPrefs.edit();
		editor.putBoolean("firstrun", false);
		editor.putBoolean("keepsecret", true);
		editor.putBoolean("useoisafe", false);

		if (cipherSecret == null)
		  cipherSecret = Crypto.generateCipherSecret();
		editor.putString("cipherSecret", cipherSecret).commit();
		
		// why?
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// fdo we want this?
		CircleStore cs = new CircleStore(cipherSecret,this,true,false);
		 cs.updateStore("dd85381ac8acc1a7", "Feedback",
		 "circles.muteswan.org");
	}
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	

		// SERVICE BIND
		Intent serviceIntent = new Intent(this, NewMessageService.class);
		bindService(serviceIntent, mNewMsgConn, Context.BIND_AUTO_CREATE);
		

		SharedPreferences defPrefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		
		// initialize alert dialogs
		alertDialogs = new AlertDialogs(this);
		
		
	    cipherSecret = defPrefs.getString("cipherSecret", null);
		boolean useoisafe = defPrefs.getBoolean("useoisafe", false);
	
		migrate = new MigrateToEncPrefs();
		if (migrate.needsMigration(getApplicationContext())) {
		  cipherSecret = Crypto.generateCipherSecret();
		  defPrefs.edit().putString("cipherSecret", cipherSecret).commit();
		}
		

		boolean firstRun = defPrefs.getBoolean("firstrun", true);
		if (firstRun) {
				firstRunInit(defPrefs);
		}
		
		
		
		
		
		

		

		// start work activities
		Thread thread = new Thread(this);
		thread.start();

		

		

		setContentView(R.layout.main);
		
		
		// if we are supposed to use oi safe, get the secret
		// even if we aren't supposed to use oisafe, if we don't
		// have a secret in prefs we have no other choice, so try
		// to get it. otherwise we just save the secret if we
		// should
		if (useoisafe) {
			getSafeSecret();
		}
				
		

		final ImageView mLatestMessagesButton = (ImageView) findViewById(R.id.mLatestMessages);
		mLatestMessagesButton.setOnClickListener(mLatestMessages);

		final ImageView mManageCirclesButton = (ImageView) findViewById(R.id.mManageCircles);
		mManageCirclesButton.setOnClickListener(mManageCircles);

				
		
		
		
		

	}

	public View.OnClickListener postClicked = new View.OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent(getApplicationContext(), WriteMsg.class);

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

		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// if (item.toString().equals("Create Circle")) {
		// startActivity(new Intent(this, CreateCircle.class));
		// return true;
		if (item.toString().equals("Identities")) {
			startActivity(new Intent(this, IdentityList.class));
			return true;
			// } else if (item.toString().equals("Create Identity")) {
			// startActivity(new Intent(this,GenerateIdentity.class));
			// return true;
		} else if (item.toString().equals("Share Muteswan")) {
			Intent intent = new Intent("com.google.zxing.client.android.ENCODE");
			intent.putExtra("ENCODE_DATA",
					"market://search?q=pname:org.muteswan.client");
			intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
			try {
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
				alertDialogs.offerToInstallBarcodeScanner();
			}
			// } else if (item.toString().equals("Share Orbot")) {
			// Intent intent = new
			// Intent("com.google.zxing.client.android.ENCODE");
			// intent.putExtra("ENCODE_DATA",getString(R.string.orbot_market_uri));
			// intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
			// try {
			// startActivity(intent);
			// } catch (ActivityNotFoundException e) {
			// alertDialogs.offerToInstallBarcodeScanner();
			// }
		} else if (item.toString().equals("Uninstall Muteswan")) {
			Intent intent = new Intent(Intent.ACTION_DELETE);
			String packageName = "org.muteswan.client";
			Uri data = Uri.fromParts("package", packageName, null);
			intent.setData(data);
			startActivity(intent);
		} else if (item.toString().equals("About")) {
			showAbout();
		} else if (item.toString().equals("Settings")) {
			Intent intent = new Intent(this, Preferences.class);
			intent.putExtra("secret", cipherSecret);
			startActivity(intent);
			return true;
		}

		return true;

	}

	private void showCircles(Integer action) {
		Intent intent = new Intent(this, CircleList.class);
		intent.putExtra("action", action);
		intent.putExtra("secret", cipherSecret);

		startActivity(intent);
		return;
	}

	private void showLatestMessages() {
		Intent intent = new Intent(this, LatestMessages.class);
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

		public void onServiceConnected(ComponentName className, IBinder service) {
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

		TextView versionName = (TextView) view.findViewById(R.id.versionName);
		versionName.setText(version);

		new AlertDialog.Builder(this).setTitle("About Muteswan").setView(view)
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
		for (int i = 0; i < messageDigest.length; i++) {
			String hex = Integer.toHexString(0xFF & messageDigest[i]);
			if (hex.length() == 1)
				hexString.append('0');
			hexString.append(hex);
		}
		return (new String(hexString));

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
	private MigrateToEncPrefs migrate;

	private boolean migrateDatabase() {

		
		if (!migrate.needsMigration(this))
			return (false);

		// sendBroadcast(new Intent(Main.UPGRADING_DATABASE));
		alertDialogs.upgradingDatabase.sendEmptyMessage(0);
		/*cipherSecret = Crypto.generateCipherSecret();
		SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		defPrefs.edit().putString("cipherSecret", cipherSecret).commit();*/
		
		SharedPreferences prefs = getSharedPreferences("circles", 0);

		File oldDb = new File(
				"/data/data/org.muteswan.client/databases/muteswandb");
		oldDb.renameTo(new File(
				"/data/data/org.muteswan.client/databases/muteswandbOld"));

		File dbDir = new File("/data/data/org.muteswan.client/databases/");
		File[] files = dbDir.listFiles();
		for (int i = 0; i < files.length; i++) {
			MuteLog.Log("NewMessageService", "File is " + files[i].getName());
			if (files[i].getName().equals("muteswandbOld"))
				continue;
			files[i].delete();
		}

	
		LinkedList<String[]> circles = migrate.getOldCircleData();
		

		for (String[] s : circles) {
			MuteLog.Log("NewMessageService", "On Migration got: " + s[0]);
			
			Circle newCircle = new Circle(cipherSecret, this, s[1], s[0], s[2]);
			newCircle.createLastMessage(0);
			MuteLog.Log("NewMessageService",
					"full text: " + newCircle.getFullText());

			JSONObject jsonObject = newCircle.getCryptJSON(cipherSecret);
			prefs.edit()
					.putString(genHexHash(newCircle.getFullText()),
							jsonObject.toString()).commit();

		

		}

		MuteLog.Log("CircleStore", "Done loading migration data.");

		MuteLog.Log("CircleStore", "Done migrating.");
		// sendBroadcast(new Intent(Main.FINISHED_UPGRADING_DATABASE));
		alertDialogs.finishedUpgradingDatabase.sendEmptyMessage(0);
		return true;
	}

}
