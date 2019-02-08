package app.mxr.wlanshare.utils;

import java.lang.reflect.Method;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;




public class APManager{
	
	public Context context;
	public String SSID,password;
	public WifiManager wifiManager;
	final String TAG="APManager";
	
	public APManager(Context context){
		this.context=context;
		wifiManager=(WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	}
	
	public void setSSID(String SSID){
		this.SSID=SSID;
	}
	
	public void setPassword(String password){
		this.password=password;
	}

	 public void startWifiAp(Context context,String SSID,String Password) {
		
		  WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	         if (wifiManager.isWifiEnabled()) {
	            wifiManager.setWifiEnabled(false);
	        }

	        Method method = null;
	        try {
	            method = wifiManager.getClass().getMethod("setWifiApEnabled",
	                    WifiConfiguration.class, boolean.class);
	            method.setAccessible(true);
	            WifiConfiguration netConfig = new WifiConfiguration();
	            netConfig.SSID = SSID;
	            netConfig.preSharedKey = Password;
	            netConfig.allowedAuthAlgorithms
	                    .set(WifiConfiguration.AuthAlgorithm.OPEN);
	            netConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
	            netConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
	            netConfig.allowedKeyManagement
	                    .set(WifiConfiguration.KeyMgmt.WPA_PSK);
	            netConfig.allowedPairwiseCiphers
	                    .set(WifiConfiguration.PairwiseCipher.CCMP);
	            netConfig.allowedPairwiseCiphers
	                    .set(WifiConfiguration.PairwiseCipher.TKIP);
	            netConfig.allowedGroupCiphers
	                    .set(WifiConfiguration.GroupCipher.CCMP);
	            netConfig.allowedGroupCiphers
	                    .set(WifiConfiguration.GroupCipher.TKIP);

	            method.invoke(wifiManager, netConfig,true);

	        } catch (Exception e) {
	            Log.i(TAG, "startWifiAp: "+e.getMessage());
	        }
	    }
	 
	 public  boolean setApEnabled(boolean enabled) {
		
	        if (enabled) { // disable WiFi in any case  
	            //wifi和热点不能同时打开，所以打开热点的时候需要关闭wifi  
	            wifiManager.setWifiEnabled(false);  
	        }  
	        try {  
	            //热点的配置类  
	            WifiConfiguration apConfig = new WifiConfiguration();  
	            //配置热点的名称(可以在名字后面加点随机数什么的)  
	            apConfig.SSID = this.SSID;  	           
	            //通过反射调用设置热点  
	            Method method = wifiManager.getClass().getMethod(  
	                    "setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);  
	            //返回热点打开状态  
	            return (Boolean) method.invoke(wifiManager, apConfig, enabled);  
	        } catch (Exception e) {  
	            return false;  
	        }  
	    }  
}