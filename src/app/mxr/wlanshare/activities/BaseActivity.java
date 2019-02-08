package app.mxr.wlanshare.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import com.github.ghmxr.wlanshare.R;
import app.mxr.wlanshare.receivers.APStateChangedReceiver;
import app.mxr.wlanshare.receivers.APStateChangedReceiver.APStateListener;
import app.mxr.wlanshare.receivers.WifiStateChangedReceiver;
import app.mxr.wlanshare.receivers.WifiStateChangedReceiver.WifiStateListener;

@SuppressLint("InlinedApi")
public abstract class BaseActivity extends Activity{
	
	public static final int  AP_DISABLING=10;
	public static final int  AP_DISABLED =11;
	public static final int  AP_ENABLING =12;
	public static final int  AP_ENABLED  =13;
	public static final int  NET_CONNECTIVITY_CHANGED=14;
	public static final String KEY_FIRSTUSE  ="firstuse";
	public static final String KEY_FIRSTUSE_THIS_VERSION = "firstuse6";
	public static final String KEY_APID      ="apid";
	public static final String KEY_DEVICENAME="devicename";
	
	public long apid=0;
	
	public String deviceName="";
	@SuppressLint("NewApi")
	protected void onCreate(Bundle mybundle){
		super.onCreate(mybundle);
		String deviceName;
		SharedPreferences settings = this.getSharedPreferences("settings", Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		BluetoothAdapter badapter = BluetoothAdapter.getDefaultAdapter();
		deviceName=badapter.getName();
		if(deviceName==null||deviceName.trim()==""||deviceName.trim().equals("")){
			deviceName=Build.BRAND+" "+Build.MODEL;
		}
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){
			Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(this.getResources().getColor(R.color.color_actionbar));
		}
		if(settings.getBoolean(KEY_FIRSTUSE, true)){
			long apid = System.currentTimeMillis();
			editor.putBoolean(KEY_FIRSTUSE, false);
			editor.putLong(KEY_APID,apid);
			editor.putString(KEY_DEVICENAME, deviceName);
			editor.commit();
			this.apid =apid; 
			this.deviceName=deviceName;
		}
		else{
			this.apid = settings.getLong(KEY_APID, 0);
			this.deviceName = settings.getString(KEY_DEVICENAME, deviceName);
		}
		if(settings.getBoolean(KEY_FIRSTUSE_THIS_VERSION, true)){
			editor.putBoolean(KEY_FIRSTUSE_THIS_VERSION, false);
			editor.commit();
			
		}
		
		WifiStateChangedReceiver.wifistatelistener =new WifiStateListener(){

			@Override
			public void onWifiStateChanged(int state) {
				// TODO Auto-generated method stub
				BaseActivity.this.onWifiStateChanged(state);
			}
		};
		
		APStateChangedReceiver.apstatelistener=new APStateListener(){

			@Override
			public void onAPStateChanged(int state) {
				// TODO Auto-generated method stub
				BaseActivity.this.onAPStateChanged(state);
			}
			
		};
	}
	
	
	
	public abstract void onWifiStateChanged(int state);
	public abstract void onAPStateChanged(int state);
}
