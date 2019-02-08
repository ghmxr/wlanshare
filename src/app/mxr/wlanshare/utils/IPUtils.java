package app.mxr.wlanshare.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

//import org.apache.http.conn.util.InetAddressUtils;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

public class IPUtils {
	
	/**
     * wifi获取 路由ip地址
     *
     * @param context
     * @return String 路由器IP地址
     */
	public static String getWifiRouteIPAddress(Context context) {
		WifiManager manager=(WifiManager)context.getSystemService(Context.WIFI_SERVICE);
	    DhcpInfo dhcpInfo = manager.getDhcpInfo();
//	    WifiInfo wifiinfo = manager.getConnectionInfo();	        
//	        System.out.println("Wifi info----->" + wifiinfo.getIpAddress());
//	        System.out.println("DHCP info gateway----->" + Formatter.formatIpAddress(dhcpInfo.gateway));
//	        System.out.println("DHCP info netmask----->" + Formatter.formatIpAddress(dhcpInfo.netmask));
	    //DhcpInfo中的ipAddress是一个int型的变量，通过Formatter将其转化为字符串IP地址
	    String routeIp = Formatter.formatIpAddress(dhcpInfo.gateway);
	    Log.e("WifiManagerment Print out", "wifi route ip：" + routeIp);

	    return routeIp;
	}
	  
	/**
	 * 获取本机Ip
	 * @return String 本机IP地址
	 */
	public static String getLocalIpAddress(Context context){
		WifiManager wifimanager=(WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		DhcpInfo dhcpinfo=wifimanager.getDhcpInfo();
		return Formatter.formatIpAddress(dhcpinfo.ipAddress);
		
		
		
		/*	try{
			Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); 
			while(en.hasMoreElements()){
				NetworkInterface nif = en.nextElement();
				Enumeration<InetAddress> enumIpAddr = nif.getInetAddresses();
				while(enumIpAddr.hasMoreElements()){
					InetAddress mInetAddress = enumIpAddr.nextElement();
					if(!mInetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(mInetAddress.getHostAddress())){
						return mInetAddress.getHostAddress().toString();
					}
				}
			}
		}catch(SocketException ex){
			Log.e("IPUtils", "获取本地IP地址失败");
		}  */
			
		//return null;
	}
	/**
	 * 获取已连接本机热点的所有设备的其中一个设备的IP地址
	 * 	
	 */
	
	public static  String getanIPfromARP(){
		String anIP="";
		BufferedReader reader = null;
		try {
		    reader = new BufferedReader(new FileReader("/proc/net/arp"));
		    String line = reader.readLine();
		    //读取第一行信息，就是IP address HW type Flags HW address Mask Device
		    while ((line = reader.readLine()) != null) {
		        String[] tokens = line.split("[ ]+");
		        if (tokens.length < 6) {
		            continue;
		        }
		        String ip = tokens[0]; //ip
		        anIP=ip;
		    //    String mac = tokens[3];  //mac 地址
		     //  String flag = tokens[2];//表示连接状态
		    }
		} catch (FileNotFoundException e) {
		} catch (IOException e) {  
		} finally {
		    try {
		        if (reader != null) {
		            reader.close();
		        }
		    }
		    catch (IOException e) {
		    }
		}
		return anIP;
	}
}
