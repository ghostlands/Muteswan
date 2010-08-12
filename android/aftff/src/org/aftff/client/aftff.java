package org.aftff.client;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;

import uk.ac.cam.cl.dtg.android.tor.TorProxyLib.SocksProxy;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;



import android.os.Bundle;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.conn.scheme.Scheme;
import org.apache.http.entity.BasicHttpEntity;




public class aftff extends Activity {
	Store store = null;
	static Ring activeRing = null;
	
	final static String PREFS = "AftffPrefs"; 
	
	
	
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
  
        
        
        
        
        store = getStore();
        setContentView(R.layout.main);
        final Button button = (Button) findViewById(R.id.mScan);
        button.setOnClickListener(mScan);
                                
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.clear();
        store = getStore();
           	
    	
    	
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        
        for (Ring r : store) {
        	MenuItem menuItem = menu.add(r.getShortname());
         }
        
        menu.add("Clear Saved Keys");
        
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	
		if (item.toString().equals("Clear Saved Keys")) {
			SharedPreferences prefs = getSharedPreferences(PREFS,0);
			SharedPreferences.Editor prefsEd = prefs.edit();
			prefsEd.clear();
			prefsEd.commit();
			TextView txt = new TextView(this);
			txt.setText("Saved keys cleared.");
			setContentView(txt);
			return true;
		}
		
		for (Ring r : store) {
			if (r.getShortname().equals(item.toString())) {
			  aftff.activeRing = r;
			}
		}
		TextView txt = new TextView(this);
		txt.setText("Loading..." + aftff.activeRing.getShortname());
		setContentView(txt);
			
		
		startActivity(new Intent( this, MsgList.class));

		return true;

	}
    
    
    
    
    private void showRingOptions(Ring ring) {
    	
    }
    
    private void selectMsg() {
    	startActivity(new Intent( this, MsgList.class));
    	return;
    }
    
    private void testEncrypt(Ring ring) {


		String encMsg = "";

        try {
        	byte[] keyBytes;
			try {
				keyBytes = ring.getKey().getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
		    
	        
	        
	        try {

	        	String[] msg = ring.getMsg("1");
		        
				TextView txt2 = new TextView(this);
		        txt2.setText("We got this: " + msg[1]);
		        setContentView(txt2);
		        
		        //setContentView(new MsgList());
				
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				//e1.printStackTrace();
				TextView txt2 = new TextView(this);
		        txt2.setText("Unknown host exception: " + ring.getServer());
		        setContentView(txt2);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				//e1.printStackTrace();
				TextView txt2 = new TextView(this);
		        txt2.setText("IO exception to host: " + ring.getServer() + " " + e1.toString());
		        setContentView(txt2);
			}

	
	        
	        
			
		} catch (IllegalBlockSizeException e) {
			TextView txt = new TextView(this);
            txt.setText("Illegal block size exception: " + e.toString() + " data:\n-" + encMsg + " with length of " + encMsg.length());
            setContentView(txt);
            
            DataOutputStream fh;
			try {
				fh = new DataOutputStream(new FileOutputStream("/sdcard/encMessage.txt"));
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return;
			}
			
            try {
				fh.writeBytes(encMsg);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
            
            
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }

    private Store getStore() {
    	
    	Store newStore = new Store();    	
    	
    	SharedPreferences prefs = getSharedPreferences(PREFS,0);
        String storeString = prefs.getString("store", null);
        if (storeString == null || storeString.equals(""))
        	return newStore;
        
        String[] storeArr = storeString.split("---");
      
        
        for (String keyStr : storeArr) {
        	if (keyStr == null)
        		continue;
        	Ring r = new Ring(keyStr);
        	if (r == null)
        		continue;
        	newStore.add(r);
        }
        
        return(newStore);
    	
    }
    
    private void updateStore(String contents) {
    	boolean haveRing = false;
        for (Ring r : store) {
        	if (r.getFullText().equals(contents)) {
        		haveRing = true;
        		return;
        	}
        }
  
        String storeString;
        if (haveRing == false) {
        	SharedPreferences prefs = getSharedPreferences(PREFS,0);
        	
        	if (store.isEmpty()) {
        		 storeString = contents + "---";
        	} else {
        	     storeString = store.getAsString() + contents + "---";
        	}
        	
        	SharedPreferences.Editor prefEd = prefs.edit();
        	prefEd.putString("store", storeString);
        	prefEd.commit();
        }
    }
    
    
    
    public Button.OnClickListener mScan = new Button.OnClickListener() {
    	    public void onClick(View v) {
    	        Intent intent = new Intent("com.google.zxing.client.android.SCAN");
    	        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
    	        startActivityForResult(intent, 0);
    	 }
    };
    

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	  if (requestCode == 0) {
    	    if (resultCode == RESULT_OK) {
    	    	    //Handle successful scan
    	            String contents = intent.getStringExtra("SCAN_RESULT");
    	            String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
    	            
    	            Ring ring = new Ring(contents);
    	            updateStore(contents);
    	            
    	            
    	            
    	            this.activeRing = ring;
    	            selectMsg();
    	            
    	            
    	            
            } else if (resultCode == RESULT_CANCELED) {
            	//final String testSite = "forest+0df46018575f1656@tckwndlytrphlpyo.onion";
            	//final String testSite = "2ndset+1522c03e8b9bae5d@tckwndlytrphlpyo.onion";
            	final String testSite = "testsite+dba4fe6ef22b494d@tckwndlytrphlpyo.onion";

            	Ring ring = new Ring(testSite);
 	            updateStore(testSite);
            	            	
            	this.activeRing = ring;
            	selectMsg();
            
            }
    	  }
    }

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
	// BLECH! not used
    static public String getGetBody(String host, String getline) throws UnknownHostException, IOException {
    	  // Get the proxy port
        SocksProxy proxy = new SocksProxy(9050);

          
        // Create a socket to the destination through the
        // anonymous proxy
        Socket s = proxy.connectSocksProxy(null, host, 80, 0);
                   //Socket s = new Socket(HOST, PORT);

        PrintWriter writer = new PrintWriter(s.getOutputStream());
        InputStreamReader reader = new InputStreamReader(s.getInputStream());
        InputStream  is = s.getInputStream();

        
        // Very simple HTTP GET
        writer.println("GET " + getline + " HTTP/1.1");
        writer.println("Host: " + host);
        writer.println("Connection: close");
        writer.println();
        writer.flush();
        
        BasicHttpEntity resp = new BasicHttpEntity();
        resp.setContent(is);

        String body = EntityUtils.toString(resp);
        return(body);
        
//        // Get the result
//        StringBuilder result = new StringBuilder();
//        char[] buffer = new char[1024];
//        int read = 0;
//        do {
//                  result.append(buffer, 0, read);
//                  read = reader.read(buffer, 0, buffer.length);
//           } while (read > -1);
//        

        
        //return(result.toString());
    }
    
}