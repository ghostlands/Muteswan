package org.muteswan.client;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;

public class AlertDialogs {

	Context context;
	private boolean useOISafe = true;
	
	public AlertDialogs(Context context) {
		this.context = context;
	}
	
	private Handler useOISafeHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			useOISafe = false;
		}

	};
	
	public boolean getUseOISafe() {
		return useOISafe;
	}
	
	
	public void offerToInstallBarcodeScanner() {
		AlertDialog.Builder noTorDialog = new AlertDialog.Builder(context);
	    noTorDialog.setTitle(R.string.q_install_barcodescanner);
	    noTorDialog.setMessage(R.string.q_intall_barcodescanner);
	    noTorDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	      public void onClick(DialogInterface dialogInterface, int i) {
	    	Uri uri = Uri.parse(context.getString(R.string.barcode_scanner_market_uri));
	    	Intent intent = new Intent(Intent.ACTION_VIEW, uri);
	        context.startActivity(intent);
	      }
	    });
	    noTorDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
	      public void onClick(DialogInterface dialogInterface, int i) {}
	    });
	    noTorDialog.create();
	    noTorDialog.show();
	}
	
	
	public void offerToInstallOISafe() {
		final SharedPreferences defPrefs = PreferenceManager
  				.getDefaultSharedPreferences(context);
		boolean keepSecret = defPrefs.getBoolean("keepsecret", false);
		
		if (keepSecret)
			return;
		
		AlertDialog.Builder oiSafe = new AlertDialog.Builder(context);
	    oiSafe.setTitle("Install OI Safe?");
	    oiSafe.setMessage("Muteswan uses OI Safe to store encryption passwords. OI Safe provides a secure way to store and provide passwords to other applications, like Muteswan. If you don't want to use OI Safe, your circle information will be easily recoverable by someone who has access to your device.");
	    oiSafe.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	      public void onClick(DialogInterface dialogInterface, int i) {
	    	Uri uri = Uri.parse(context.getString(R.string.oisafe_market_uri));
	    	Intent intent = new Intent(Intent.ACTION_VIEW, uri);
	        context.startActivity(intent);
	      }
	    });
	    oiSafe.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
	      public void onClick(DialogInterface dialogInterface, int i) {
	    	  useOISafeHandler.sendEmptyMessage(0);
	    	 
			  MuteLog.Log("Main","Not using oi safe.");
			  //String cipherSecret = Crypto.generateSQLSecret();
			  
			  Editor editor = defPrefs.edit();
			  editor.putBoolean("keepsecret", true).commit();
			  //editor.putString("cipherSecret",cipherSecret).commit();
			  
			 
	      }
	    });
	    oiSafe.create();
	    oiSafe.show();
	}
	
	 public Handler dialogTorNotAvailable = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	        		  offerToStartTor();
	        }		
	 };
	 
	 public Handler upgradingDatabase = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	        		  upgradingDatabase();
	        }	
	 };
	 
	 public Handler finishedUpgradingDatabase = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	        		  finishedUpgradingDatabase();
	        }	
	 };
	 
	 private ProgressDialog progressDialog;
	 private void upgradingDatabase() {
		MuteLog.Log("AlertDialogs", "Upgrading!");
	    progressDialog = ProgressDialog.show(context, "", "Upgrading database...", true);
	 }
	 
	 private void finishedUpgradingDatabase() {
		MuteLog.Log("AlertDialogs", "Done Upgrading!");
		progressDialog.cancel();
		progressDialog = null;
     }
	 
	 private void offerToStartTor() {
		  AlertDialog.Builder noTorDialog = new AlertDialog.Builder(context);
	      noTorDialog.setTitle(R.string.t_tor_unavailable);
	      noTorDialog.setMessage(R.string.n_tor_not_available);
	      noTorDialog.setPositiveButton(R.string.q_start_tor, new DialogInterface.OnClickListener() {
	      public void onClick(DialogInterface dialogInterface, int i) {
	      
	    	Intent intent = null;
	    	try {
	    	  intent = new Intent("org.torproject.android.START_TOR");
	    	  context.startActivity(intent);
	    	} catch (ActivityNotFoundException e) {
	    	  offerToInstallTor();
	          
	    	}
	      }
	    });
	    noTorDialog.setNegativeButton(R.string.q_start_tor_confirm_no, new DialogInterface.OnClickListener() {
	      public void onClick(DialogInterface dialogInterface, int i) {}
	    });
	    noTorDialog.create();
	    noTorDialog.show();
		}
		
		private void offerToInstallTor() {
			AlertDialog.Builder noTorDialog = new AlertDialog.Builder(context);
	        noTorDialog.setTitle(R.string.t_install_tor);
	        noTorDialog.setMessage(R.string.q_install_tor);
	        noTorDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
	      public void onClick(DialogInterface dialogInterface, int i) {
	        Uri uri = Uri.parse(context.getString(R.string.orbot_market_uri));
	    	Intent intent = new Intent(Intent.ACTION_VIEW, uri);
	        context.startActivity(intent);
	       }
	      });
	      noTorDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialogInterface, int i) {}
	      });
	      noTorDialog.create();
	      noTorDialog.show();
		}
		

}
