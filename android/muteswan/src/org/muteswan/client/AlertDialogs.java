package org.muteswan.client;

import android.app.AlertDialog;
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
	    noTorDialog.setTitle("Install BarcodeScanner?");
	    noTorDialog.setMessage("BarcodeScanner is not currently installed. Do you want to install it from the market?");
	    noTorDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	      public void onClick(DialogInterface dialogInterface, int i) {
	    	Uri uri = Uri.parse("market://search?q=pname:com.google.zxing.client.android");
	    	Intent intent = new Intent(Intent.ACTION_VIEW, uri);
	        context.startActivity(intent);
	      }
	    });
	    noTorDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
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
	 
	 private void offerToStartTor() {
		  AlertDialog.Builder noTorDialog = new AlertDialog.Builder(context);
	      noTorDialog.setTitle("Tor Unavailable");
	      noTorDialog.setMessage("Tor is not available at this time. Please start or restart Tor or ensure it is running properly. Only cached data will be available otherwise.");
	      noTorDialog.setPositiveButton("Start Tor?", new DialogInterface.OnClickListener() {
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
	    noTorDialog.setNegativeButton("No, thanks", new DialogInterface.OnClickListener() {
	      public void onClick(DialogInterface dialogInterface, int i) {}
	    });
	    noTorDialog.create();
	    noTorDialog.show();
		}
		
		private void offerToInstallTor() {
			AlertDialog.Builder noTorDialog = new AlertDialog.Builder(context);
	        noTorDialog.setTitle("Install Tor?");
	        noTorDialog.setMessage("Tor is not currently installed. Do you want to install it from the market?");
	        noTorDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
	      public void onClick(DialogInterface dialogInterface, int i) {
	        Uri uri = Uri.parse("market://search?q=pname:org.torproject.android");
	    	Intent intent = new Intent(Intent.ACTION_VIEW, uri);
	        context.startActivity(intent);
	       }
	      });
	      noTorDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialogInterface, int i) {}
	      });
	      noTorDialog.create();
	      noTorDialog.show();
		}
		

}
