package app.mxr.wlanshare.utils;

import java.util.List;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.text.TextUtils;
import android.util.Log;
import app.mxr.wlanshare.activities.Activity_Receive;

public class WifiManagement implements Runnable{
	
	private WifiManager wifimanager;
	private List<ScanResult> scanresultlist; 
	 
	 WifiLock wifilock;
	 Thread thread_openwifi=null;
	 String index,SSID;
	 
	 public static final int WIFI_TYPE_OPEN   =   0x1;
	 public static final int WIFI_TYPE_WEP    =   0x2;
	 public static final int WIFI_TYPE_WPA    =   0x3;
	 public static final int WIFI_TYPE_UNKNOWN=   0x4;
	 
	
	public WifiManagement(Context context){
		wifimanager=(WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		this.SSID="";
		this.index="";
	}
	
	public boolean setWifiEnabled(boolean isEnabled){
		if(isEnabled){
			if(!wifimanager.isWifiEnabled()){
				return wifimanager.setWifiEnabled(true);
			}
		}
		else{
			if(wifimanager.isWifiEnabled()){
				return wifimanager.setWifiEnabled(false);
			}
			
		}
		return true;
		
	}
	
	public boolean isWifiEnabled(){
		return wifimanager.isWifiEnabled();
	}
	
	public int getWifiState(){
		return wifimanager.getWifiState();
	}
	
	public void acquireWifiLock(String tag){
		wifilock=wifimanager.createWifiLock(tag);
		wifilock.acquire();
	}
	
	public void releaseWifiLock(){
		wifilock.release();
	}
	
	public String getWifiList() {	
		StringBuilder stringBuilder = new StringBuilder();  
		for (int i = 0; i < scanresultlist.size(); i++) {  
		stringBuilder.append("Index_" + Integer.valueOf(i + 1).toString() + ":");  
		// 将ScanResult信息转换成一个字符串包   
		// 其中把包括：BSSID、SSID、capabilities、frequency、level   
		stringBuilder.append((scanresultlist.get(i)).toString());  
		stringBuilder.append("\n");  
		}  
		        
		return stringBuilder.toString(); 	
	} 
	
	/**
	 * 获取当前连接的wifi的SSID
	 */
	public String getSSID(){
		return wifimanager.getConnectionInfo().getSSID().toString();
	}
	
	/**
	 * 判断当前是否已经连接上可用的wifi
	 * @return boolean 是否已连接上可用wifi
	 * 
	 */
	public boolean isWifiConnected(){
	/*	if(wifimanager.getConnectionInfo().getSSID()==null
				||wifimanager.getConnectionInfo().getSSID().trim().equals("<unknown ssid>")
				||wifimanager.getConnectionInfo().getSSID().trim().equals("")
				||wifimanager.getConnectionInfo().getSSID().length()==0
				||!wifimanager.isWifiEnabled()){
			return false;
		 }
		 else{
			 Log.i("WifiManagement", "通过Wifimanagement的isWifiConnected方法判断当前已经连接了Wifi，SSID值为  "+getSSID());
			return true;
		 }     */
		
		WifiInfo wifiInfo=wifimanager.getConnectionInfo();
		int ipAddress = wifiInfo == null ? 0 : wifiInfo.getIpAddress();
		if (isWifiEnabled() && ipAddress != 0){
			return true;
		}
		else{
			return false;
		}
		
	}
	
	/**
	 * 指定要连接的WIFI的SSID中要包含的关键字
	 * @param index : 关键字
	 * 
	 */
	public void setSSIDIndex(String index){
		this.index=index;
	}
	
	/**
	 * 连接到已设置包含关键字的SSID的wifi
	 * 执行此方法前必须先通过 执行setSSIDIndex(String index)方法设置关键字
	 */
	public void connecttoOpenWifi(){
		if(thread_openwifi!=null){
			thread_openwifi.interrupt();
			thread_openwifi=null;
		}
		thread_openwifi=new Thread(this);	
		thread_openwifi.start();
	}
	
	public void stopConnecttoOpenWifi(){
		if(thread_openwifi!=null){
			thread_openwifi.interrupt();
			thread_openwifi=null;
		}
	}
	
	
	@Override
	public void run(){
		// TODO Auto-generated method stub
		this.SSID="";
		try{
			if(!setWifiEnabled(true)){
				Activity_Receive.sendEmptyMessage(Activity_Receive.MESSAGE_OPEN_WIFI_FAILED);
				if(thread_openwifi!=null){
					thread_openwifi.interrupt();
					thread_openwifi=null;
				}
			}
			Thread.currentThread();
			Thread.sleep(200);
			while(wifimanager.getWifiState()==WifiManager.WIFI_STATE_ENABLING){				
					Thread.sleep(100);												
			}			
			while(this.SSID==""){
				if(wifimanager.getWifiState()==WifiManager.WIFI_STATE_ENABLED){					
					Thread.sleep(200);
					this.scanresultlist=wifimanager.getScanResults();														
				}
				
				for(int i=0;i<scanresultlist.size();i++){
					String scannedSSID=scanresultlist.get(i).SSID;
					
					if(scannedSSID.indexOf(index)!=-1){
						this.SSID=scannedSSID;
						break;						
					}
				}
				
				Thread.sleep(200);				
			}
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
		if(this.SSID!=""){
			WifiConfiguration wifiConfig = createWifiInfo(this.SSID, "123456789000",
	                WifiManagement.WIFI_TYPE_OPEN);
			if (wifiConfig == null) {
	           // sendMsg("wifiConfig is null!");
	            return;
	        }
	        
			WifiConfiguration tempConfig= isConfigExsits(this.SSID);
			if (tempConfig != null) {
	            wifimanager.removeNetwork(tempConfig.networkId);
	            Log.e("WifiManagement", "已移除已存在的网络配置"+this.SSID);
	        }
			int netID = wifimanager.addNetwork(wifiConfig);
			
			wifimanager.disconnect();
			wifimanager.enableNetwork(netID, true);
			Log.e("WifiManagement", "已尝试连接到网络"+this.SSID);
			wifimanager.reconnect();
			Log.e("WifiManagement", "已尝试重连到网络"+this.SSID);
			while(!isWifiConnected()){
				try{
					Thread.sleep(1000);
					Log.i("WifiManagerment", "正在等待wifi连接");
				}catch(Exception e){
					e.printStackTrace();
				}
				if(isWifiConnected()){					
					Activity_Receive.sendEmptyMessage(0x2);   //0x2值在Activity_Receive中是private 
					break;
				}
			}
			
	
		}             
       	
	}
	
	 private WifiConfiguration createWifiInfo(String SSID, String Password,
	            int Type) {
	        WifiConfiguration config = new WifiConfiguration();
	        config.allowedAuthAlgorithms.clear();
	        config.allowedGroupCiphers.clear();
	        config.allowedKeyManagement.clear();
	        config.allowedPairwiseCiphers.clear();
	        config.allowedProtocols.clear();
	        config.SSID = "\"" + SSID + "\"";
	        // nopass
	        if (Type ==WifiManagement.WIFI_TYPE_OPEN) {
	            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
	        }
	        // wep
	        if (Type == WifiManagement.WIFI_TYPE_WEP) {
	            if (!TextUtils.isEmpty(Password)) {
	                if (isHexWepKey(Password)) {
	                    config.wepKeys[0] = Password;
	                } else {
	                    config.wepKeys[0] = "\"" + Password + "\"";
	                }
	            }
	            config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
	            config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
	            config.allowedKeyManagement.set(KeyMgmt.NONE);
	            config.wepTxKeyIndex = 0;
	        }
	        // wpa
	        if (Type == WifiManagement.WIFI_TYPE_WPA) {
	            config.preSharedKey = "\"" + Password + "\"";
	            config.hiddenSSID = true;
	            config.allowedAuthAlgorithms
	                    .set(WifiConfiguration.AuthAlgorithm.OPEN);
	            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
	            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
	            config.allowedPairwiseCiphers
	                    .set(WifiConfiguration.PairwiseCipher.TKIP);
	            // 此处需要修改否则不能自动重联
	             config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
	            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
	            config.allowedPairwiseCiphers
	                    .set(WifiConfiguration.PairwiseCipher.CCMP);
	            config.status = WifiConfiguration.Status.ENABLED;
	        }
	        return config;
	    }
	 
	
	 private static boolean isHexWepKey(String wepKey) {
	        final int len = wepKey.length();

	        // WEP-40, WEP-104, and some vendors using 256-bit WEP (WEP-232?)
	        if (len != 10 && len != 26 && len != 58) {
	            return false;
	        }

	        return isHex(wepKey);
	    }

	 private static boolean isHex(String key) {
	        for (int i = key.length() - 1; i >= 0; i--) {
	            final char c = key.charAt(i);
	            if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a'
	                    && c <= 'f')) {
	                return false;
	            }
	        }

	        return true;
	}
	 private WifiConfiguration isConfigExsits(String SSID) {
	        List<WifiConfiguration> existingConfigs = wifimanager
	                .getConfiguredNetworks();
	        for (WifiConfiguration existingConfig : existingConfigs) {
	            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
	                return existingConfig;
	            }
	        }
	        return null;
	    }
	 
	  
	
}
