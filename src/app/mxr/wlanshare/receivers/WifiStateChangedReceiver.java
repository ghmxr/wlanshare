package app.mxr.wlanshare.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

public class WifiStateChangedReceiver extends BroadcastReceiver {
	public static  WifiStateListener wifistatelistener;

	
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub	
		 if(intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){
			 int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
			 if(wifistatelistener!=null){
				 wifistatelistener.onWifiStateChanged(state);
			 }			
		 }
		 
		 if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {  
	          if(wifistatelistener!=null){
	        	  wifistatelistener.onWifiStateChanged(14);
	          }
	     }  
	
		 
	}
	
	public interface WifiStateListener{
		public void onWifiStateChanged(int state);
	}

}
