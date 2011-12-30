package org.muteswan.client.test;

import java.util.ArrayList;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.jayway.android.robotium.solo.Solo;
import org.muteswan.client.Main;


import junit.framework.TestCase;

public class StressInitialCircleAdd extends ActivityInstrumentationTestCase2<Main>
 {
        
        private Solo solo;

        public StressInitialCircleAdd() {
                super("org.muteswan.client",Main.class);
        }

        protected void setUp() throws Exception {
                super.setUp();
                solo = new Solo(getInstrumentation(), getActivity());
        }

        protected void tearDown() throws Exception {
                solo.finishOpenedActivities();
        }
        
        
        public void testStressInitialCircleAdd() {
        	solo.clickOnImage(2);
        	solo.sleep(1000);
     
        	
        	// find 'testsite' in the circle list
        	View targetView = getTestSiteView();
        
        	// if testsite is there, delete it
        	if (targetView != null) {
        		deleteTestSite(targetView);
        	}
        	
        
        	// try to scan and fail, declining to install barcode scanner
        	// select the first in list (will be testsite)
        	solo.clickOnButton(1);
        	solo.sleep(1000);
        	solo.clickOnButton(1);
        	
        	// view testsite and go back repeatedly
        	int waitSeconds = 8000;
        	for (int i=0; i<20; i++) {
        	  gotoToMain();
        	  gotoAllMessages();
        	  //solo.clickInList(0);
        	  //solo.sleep(5000);
        	  //solo.goBack();
        	  solo.sleep(waitSeconds);
        	  if (waitSeconds <= 1000)
        		  waitSeconds = 9000;
        	  waitSeconds = waitSeconds - 1000;
        	}
        	
        }
        
        private void gotoToMain() {
        	solo.clickOnImage(0);
        }
        
        private void gotoAllMessages() {
        	solo.clickOnImage(1);
        }
        
        private void deleteTestSite(View v) {
        	View delView = v.findViewById(org.muteswan.client.R.id.circleListDelete);
        	solo.clickOnView(delView);
        	solo.clickOnButton(0);
		}

		private View getTestSiteView() {
        	View targetView = null;
        
        
        	while (targetView == null) {
        	  ArrayList<ListView> listView = solo.getCurrentListViews();
        	  for (int i = 0; i<listView.get(0).getChildCount(); i++) {
        		View v = listView.get(0).getChildAt(i);
        		TextView circleName = (TextView) v.findViewById(org.muteswan.client.R.id.android_circleListName);
       			Log.v("StressInitialCircle","circleName: " + circleName.getText());
       			if (circleName.getText().equals("testsite")) {
       				targetView = v;
       			}
        
       			if (solo.scrollDownList(0)) {
       				break;
       			}
        	  }
        	}
        	return(targetView);
        }
}
