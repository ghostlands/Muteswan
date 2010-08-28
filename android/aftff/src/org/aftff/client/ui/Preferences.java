package org.aftff.client.ui;

import android.R;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(org.aftff.client.R.xml.preferences);
			
		
	}
	
}
