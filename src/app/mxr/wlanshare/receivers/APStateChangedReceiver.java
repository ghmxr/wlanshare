package app.mxr.wlanshare.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class APStateChangedReceiver extends BroadcastReceiver {

//	private static final int  AP_DISABLING=10;
//	private static final int  AP_DISABLED =11;
//	private static final int  AP_ENABLING =12;
//	private static final int  AP_ENABLED  =13;
	public static APStateListener apstatelistener;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		
		if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equals(intent.getAction())) {  
            //便携式热点的状态为：10---正在关闭；11---已关闭；12---正在开启；13---已开启             
           if(apstatelistener!=null){
        	   int state = intent.getIntExtra("wifi_state",  0);
        	   apstatelistener.onAPStateChanged(state);
           }
        }  
	}
	
	public interface APStateListener{
		public void onAPStateChanged(int state);
	}

}
