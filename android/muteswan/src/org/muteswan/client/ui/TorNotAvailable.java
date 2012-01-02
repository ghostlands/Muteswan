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

import org.muteswan.client.R;

import android.app.Activity;
import android.os.Bundle;

public class TorNotAvailable extends Activity {

	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tornotavailable);
	}
	
	

    //public void onResume() {
    //	startActivity(new Intent(this,muteswan.class));
    //}
	
}
