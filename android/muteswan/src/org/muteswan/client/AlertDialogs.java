package org.muteswan.client;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;

public class AlertDialogs {

	Context context;
	
	public AlertDialogs(Context context) {
		this.context = context;
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
