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
        
        String[] testCircles = new String[] { "test1+aabcdefghijklmno@tckwndlytrphlpyo.onion",
        									  "test2+a1bcdefghijklmno@tckwndlytrphlpyo.onion", 
        									  "test3+aa1cdefghijklmno@tckwndlytrphlpyo.onion", 
        									  "test4+aab1defghijklmno@tckwndlytrphlpyo.onion", 
        									  "test5+aabc1efghijklmno@tckwndlytrphlpyo.onion", 
        									  "test6+aabcd1fghijklmno@tckwndlytrphlpyo.onion", 
        									  "test7+aabcde1ghijklmno@tckwndlytrphlpyo.onion", 
        									  "test8+aabcdef1hijklmno@tckwndlytrphlpyo.onion", 
        									  "test9+aabcdefg1ijklmno@tckwndlytrphlpyo.onion", 
        									  "test10+aabcdefgh1jklmno@tckwndlytrphlpyo.onion", 
        									  "test11+aabcdefghi1klmno@tckwndlytrphlpyo.onion", 
        									  "test12+aabcdefghij1lmno@tckwndlytrphlpyo.onion", 
        									  "test13+aabcdefghijk1mno@tckwndlytrphlpyo.onion" 
        		};

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
   
        	ArrayList<ListView> listView = solo.getCurrentListViews();
        	while (listView.size() != 0) {
        	  //for (int i = 0; i<listView.size(); i++) {
        		  
        	   //Log.v("StressInitialCircleAdd", "ListViewSize " + listView.size());
        		View v = listView.get(0);
        		if (!deleteCircle(v))
        			break;
        		solo.sleep(750);
        		solo.goBack();
        	    solo.clickOnImage(2);
        	    listView = solo.getCurrentListViews();
        		
        	  //}
        	}
        	
        	for (int i = 0; i<testCircles.length; i++) {
        		  //Integer plusIndx = testCircles[i].indexOf("+");
                  //String name = testCircles[i].substring(0,plusIndx);
        	      //View targetView = getCircleView(name);
       
        	   //Log.v("StressInitialCircleAdd", "Deleting " + name);
        	   //if (targetView != null) {
        	//	deleteCircle(targetView);
        	 // }
        	
       
        	   // join manually
        	   solo.pressMenuItem(0);
        	   
        	   solo.enterText(0, testCircles[i]);
        	   solo.clickOnButton(0);
        	}
        	
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
        
        private boolean deleteCircle(View v) {
        	View delView = v.findViewById(org.muteswan.client.R.id.circleListDelete);
        	TextView nameView = (TextView) v.findViewById(org.muteswan.client.R.id.android_circleListName);
        	
        	if (!nameView.getText().toString().contains("test")) {
        		return false;
        	}
        	
        	if (delView == null)
        		return false;
        	solo.clickOnView(delView);
        	solo.clickOnButton(0);
        	return true;
		}

		private View getCircleView(String circleString) {
        	View targetView = null;
        
        
        	while (targetView == null) {
        	  ArrayList<ListView> listView = solo.getCurrentListViews();
        	  for (int i = 0; i<listView.get(0).getChildCount(); i++) {
        		View v = listView.get(0).getChildAt(i);
        		TextView circleName = (TextView) v.findViewById(org.muteswan.client.R.id.android_circleListName);
       			Log.v("StressInitialCircle","circleName: " + circleName.getText());
       			if (circleName.getText().equals(circleString)) {
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
