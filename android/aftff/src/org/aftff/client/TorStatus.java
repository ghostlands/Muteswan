package org.aftff.client;

import org.torproject.android.service.ITorService;

import android.content.ServiceConnection;
import android.os.RemoteException;
import android.widget.Button;
import android.widget.TextView;

public class TorStatus {

	boolean on = false;
	ITorService torService = null;

	public TorStatus(ITorService torService) {
		this.torService = torService;
		checkStatus();
	}
	
	public boolean checkStatus() {
		try {
			if (torService != null && torService.getStatus() == aftff.TOR_STATUS_ON)
				on = true;
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return(on);
	}
	
	public void checkButton(Button button) {
		if (checkStatus() == false) {
			button.setClickable(false);
			button.setVisibility(Button.INVISIBLE);
		} else {
			button.setClickable(true);
			button.setVisibility(Button.VISIBLE);
		}
	}
	
	public void checkView(TextView view) {
		if (checkStatus() == false) {
			view.setClickable(false);
			view.setVisibility(Button.INVISIBLE);
		} else {
			view.setClickable(true);
			view.setVisibility(Button.VISIBLE);
		}
	}
	
}
