/*
Copyright 2011-2012 James Unger, Rob Wolffe, Chris Churnick.
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
package org.muteswan.client.ui;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import org.muteswan.client.R;
import org.muteswan.client.data.Identity;
import org.muteswan.client.data.IdentityStore;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class GenerateIdentity extends Activity {
	
	
	public void onCreate(Bundle savedInstanceState) {
	       super.onCreate(savedInstanceState);
	       setContentView(R.layout.generateidentity);
    
	       final Button genIdentityButton = (Button) findViewById(R.id.genIdentityButton);
	       genIdentityButton.setOnClickListener(genIdentity);
	       
	}  
	       
	       
	
	public Button.OnClickListener genIdentity = new Button.OnClickListener() {
	    public void onClick(View v) {
	    	EditText txtEdit = (EditText) findViewById(R.id.newIdentityName);
	    	if (txtEdit.length() == 0) 
	    		return;
	    	String name = txtEdit.getText().toString();
	    	Identity identity = new Identity();
	    	identity.setName(name);
	    	try {
				identity.genKeyPair();
				IdentityStore idStore = new IdentityStore(getApplicationContext());
				idStore.addToDb(identity);
				
				TextView txt = new TextView(getApplicationContext());
				txt.setText("Identity created and stored. Formatting for pub is " + identity.formatPub + " and " + identity.formatPriv + " for private.");

				setContentView(txt);
				
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchProviderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	};
	       
	       
	
	
	
}
