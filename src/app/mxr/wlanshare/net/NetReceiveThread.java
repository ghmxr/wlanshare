package app.mxr.wlanshare.net;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.IllegalBlockingModeException;
import java.util.HashMap;
import java.util.LinkedList;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import app.mxr.wlanshare.activities.Activity_Receive;
import app.mxr.wlanshare.data.Devices;
import app.mxr.wlanshare.utils.IPUtils;


public class NetReceiveThread  implements Runnable {
	
	private static final int Message_Send_Up_Data_Complete=1;
	public static final int MESSAGE_START_RECEIVING_FILES = 0x0002;
							
	
	public static final String TAG = "NetThread";
	private static final int BUFFERLENGTH =65500; //缓冲大小
	private String sendername,sendergroup;
	private byte[] sendBuffer = null;
	private DatagramPacket udpSendPacket = null,udpResPacket = null;	//用于发送的udp数据包
	public static  DatagramSocket udpSocket = null;	//用于接收和发送udp数据的socket
	private boolean onWork=false;
	private Thread udpThread = null,udp2=null;
	private byte[] resBuffer = new byte[BUFFERLENGTH];	//接收数据的缓存
	private HashMap<String,Devices> devices;	//当前所有用户的集合，以IP为KEY
	//protected List<Devices> devices;
	private Context context;
	private boolean canCloseUdpSocket=false;	
	private static LinkedList<NetReceiveThread> queue = new LinkedList<NetReceiveThread>();
	private static Handler handler=new Handler(){
		public void handleMessage(Message msg) {
			switch(msg.what){
			default:if(queue.size()>0){				
						queue.getLast().processMessage(msg);
					} break;
			}
			
		}
	};
	
	public NetReceiveThread(String sendername,Context context){
		if(!queue.contains(this)){
			queue.add(this);
		}
		this.sendername=sendername;
		this.sendergroup="";
		devices = new HashMap<String,Devices>();
		this.context=context;
	//	this.resBuffer=new byte[Bufferlength];
	//	devices = new ArrayList<Devices>();
	//	this.isAPMode=isAPMode;
	}
	
	
	public boolean connectSocket(){	//监听端口，接收UDP数据
		boolean result = false;
		
		try {
			if(udpSocket == null){
				udpSocket = new DatagramSocket(IpMessageConst.PORT);	//绑定端口
				Log.e("", "connectSocket()....绑定UDP端口" + IpMessageConst.PORT + "成功");
			}
			if(udpResPacket == null)
				udpResPacket = new DatagramPacket(resBuffer, BUFFERLENGTH);
			onWork = true;  //设置标识为线程工作
			startThread();	//启动线程接收udp数据
			result = true;
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			disconnectSocket();
			Log.e(TAG, "connectSocket()....绑定UDP端口" + IpMessageConst.PORT + "失败");
		}
		
		return result;
	}
	
	public void disconnectSocket(){	// 停止监听UDP数据
		onWork = false;	// 设置线程运行标识为不运行
		if(udpResPacket != null){
			udpResPacket = null;
		}
		
		if(udpSocket != null){
			udpSocket.close();
			udpSocket = null;
			Log.e(TAG, "UDP收发端口已关闭");
		}
		stopThread();
		if(udp2!=null){
			udp2.interrupt();
			udp2=null;
		}
		
		
	}
	
	
	
	public void noticeOnline(){	// 发送上线广播
		IpMessageProtocol ipmsgSend = new IpMessageProtocol();
		ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
		ipmsgSend.setSenderName(sendername);
		ipmsgSend.setSenderHost(sendergroup);
		ipmsgSend.setCommandNo(IpMessageConst.IPMSG_ANSENTRY);	//上线命令
		ipmsgSend.setAdditionalSection(sendername + "\0" );	//附加信息里加入用户名和分组信息
		
		InetAddress broadcastAddr;
		try {
			if(Activity_Receive.isAPMode){
				broadcastAddr = InetAddress.getByName(IPUtils.getWifiRouteIPAddress(context));	//广播地址	--热点模式
			}
			else{
				broadcastAddr = InetAddress.getByName("255.255.255.255");	//广播地址	-- 普通路由模式
			}
			
			sendUdpData(ipmsgSend.getProtocolString()+"\0", broadcastAddr, IpMessageConst.PORT);	//发送数据
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "noticeOnline()....广播地址有误");
		}
		
	}
	
	public void noticeOffline(){	//发送下线广播
		IpMessageProtocol ipmsgSend = new IpMessageProtocol();
		ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
		ipmsgSend.setSenderName(sendername);
		ipmsgSend.setSenderHost(sendergroup);
		ipmsgSend.setCommandNo(IpMessageConst.IPMSG_DEVICE_OFFLINE);	//下线命令
		ipmsgSend.setAdditionalSection(sendername + "\0" + sendergroup);	//附加信息里加入用户名和分组信息
		
		InetAddress broadcastAddr;
		try {			
			if(Activity_Receive.isAPMode){
				broadcastAddr = InetAddress.getByName(IPUtils.getWifiRouteIPAddress(context));	//广播地址	--热点模式
			}
			else{
				broadcastAddr = InetAddress.getByName("255.255.255.255");	//广播地址	-- 普通路由模式
			}		
			canCloseUdpSocket=true;
			sendUdpData(ipmsgSend.getProtocolString() + "\0", broadcastAddr, IpMessageConst.PORT);	//发送数据
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "noticeOnline()....广播地址有误");
			
		}

	}
	
	private void startThread() {	//启动线程
		// TODO Auto-generated method stub
		if(udpThread == null){
			udpThread = new Thread(this);
			udpThread.start();
			Log.e(TAG, "正在监听UDP数据");
		}
	}
	
	private void stopThread() {	//停止线程
		// TODO Auto-generated method stub
		
		if(udpThread != null){			
			udpThread.interrupt();	//若线程堵塞，则中断
		}
		Log.i("", "停止监听UDP数据");
	}
	
	public synchronized void sendUdpData(final String sendStr, final InetAddress sendto, final int sendPort){	//发送UDP数据包的方法
		if(udp2!=null){
			udp2.interrupt();
			udp2=null;
		}
		udp2=new Thread(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					sendBuffer = sendStr.getBytes();
					// 构造发送的UDP数据包
					udpSendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, sendto, sendPort);
					if(udpSocket!=null){
						udpSocket.send(udpSendPacket);	//发送udp数据包	
						Log.e(TAG, "成功向IP为" + sendto.getHostAddress() + "发送UDP数据：" + sendStr);
					}
					udpSendPacket = null;		
					handler.sendEmptyMessage(Message_Send_Up_Data_Complete);
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.e(TAG, "sendUdpData(String sendStr, int port)....系统不支持编码");
				} catch (IOException e) {	//发送UDP数据包出错
					// TODO Auto-generated catch block
					e.printStackTrace();
					udpSendPacket = null;
					//udpSendPacket_Host=null;
					Log.e(TAG, "sendUdpData(String sendStr, int port)....发送UDP数据包失败");
				}
				catch(SecurityException s){
					
				}
				catch(IllegalBlockingModeException ib){
					
				}
				catch(IllegalArgumentException ia){
					
				}
			}
			
		});
		udp2.start();
		
		
	}
	
	

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(onWork){
			
			try {
				udpSocket.receive(udpResPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				onWork = false;
				
				if(udpResPacket != null){
					udpResPacket = null;
				}
				
				if(udpSocket != null){
					udpSocket.close();
					udpSocket = null;
				}
				
				udpThread = null;
				Log.e("", "UDP数据包接收失败！线程停止");
				break;
			} 
			
			if(udpResPacket.getLength() == 0){
				Log.i(TAG, "无法接收UDP数据或者接收到的UDP数据为空");
				continue;
			}
			String ipmsgStr = "";
			try {
				ipmsgStr = new String(resBuffer, 0, udpResPacket.getLength());
			} catch (Exception e){//UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(TAG, "接收数据时，系统不支持编码");
			}//截取收到的数据
			Log.i(TAG, "接收到的UDP数据内容为:" + ipmsgStr);
			IpMessageProtocol ipmsgPro = new IpMessageProtocol(ipmsgStr);	//
			int commandNo = ipmsgPro.getCommandNo();
			int commandNo2 = 0x000000FF & commandNo;	//获取命令字
			switch(commandNo2){
				case  IpMessageConst.IPMSG_REQUEST_ONLINE_DEVICES:{
					//下面构造回送报文内容
					IpMessageProtocol ipmsgSend = new IpMessageProtocol();
					ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
					ipmsgSend.setSenderName(sendername);
					ipmsgSend.setSenderHost(sendergroup);
					ipmsgSend.setCommandNo(IpMessageConst.IPMSG_ANSENTRY);	//回送报文命令
					ipmsgSend.setAdditionalSection(sendername + "\0" );	//附加信息里加入用户名和分组信息
					
					sendUdpData(ipmsgSend.getProtocolString(), udpResPacket.getAddress(), udpResPacket.getPort());	//发送数据
				}
				break;
				case IpMessageConst.IPMSG_ANSENTRY_SENDER:	{	//收到发送端回馈
					//	addUser(ipmsgPro);
					//	Activity_Send.sendEmptyMessage(IpMessageConst.IPMSG_ANSENTRY);
				}	
				break;
				
				//case IpMessageConst.IPMSG_RELEASEFILES:{ //拒绝接受文件
				//	MyFeiGeBaseActivity.sendEmptyMessage(IpMessageConst.IPMSG_RELEASEFILES);
				//	}
				//		break;
				case IpMessageConst.IPMSG_SENDMSG:{ //收到消息，处理
					String senderIp = udpResPacket.getAddress().getHostAddress();	//得到发送者IP
					String senderName = ipmsgPro.getSenderName();	//得到发送者的名称
					String additionStr = ipmsgPro.getAdditionalSection();	//得到附加信息
					//String msgTemp;		//直接收到的消息，根据加密选项判断是否是加密消息
					String[] splitStr = additionStr.split("\0"); //使用"\0"分割，若有附加文件信息，则会分割出来
				//	msgTemp = splitStr[0]; //将消息部分取出
					if((commandNo & IpMessageConst.IPMSG_FILEATTACHOPT) == IpMessageConst.IPMSG_FILEATTACHOPT){	
						//下面进行发送文件相关处理
					
						Message msg = new Message();
						msg.what = (IpMessageConst.IPMSG_SENDMSG | IpMessageConst.IPMSG_FILEATTACHOPT);
						//字符串数组，分别放了  IP，附加文件信息,发送者名称，包ID
						String[] extraMsg = {senderIp, splitStr[1],senderName,ipmsgPro.getPacketNo()};	
						msg.obj = extraMsg;	//附加文件信息部分
						Activity_Receive.sendMessage(msg);
					
						break;
					}
				
				
				}
				break;
				
				case IpMessageConst.IPMSG_SENDPORT_INTERRUPT:
					Log.i(TAG, "发送端停止了发送请求");
					Message msg_dis = new Message();
					msg_dis.what=Activity_Receive.MESSAGE_DISMISS_REQUESTDIALOG;
					String ip[]=new String [1];
					ip[0]=this.udpResPacket.getAddress().getHostAddress().toString();
					msg_dis.obj=ip;
					Activity_Receive.sendMessage(msg_dis);
					break;
				case IpMessageConst.IPMSG_INTERRUPT_FILETRANSFER:{
					Activity_Receive.isReceivingFileSuccess=false;
					Activity_Receive.sendEmptyMessage(Activity_Receive.MESSAGE_RECEIVINGFILES_INTERRUPT);
				}
				break;
				default:break;	
			}	//end of switch
			
			if(udpResPacket != null){	//每次接收完UDP数据后，重置长度。否则可能会导致下次收到数据包被截断。
				udpResPacket.setLength(BUFFERLENGTH);
			}
			
		}
		
		if(udpResPacket != null){
			udpResPacket = null;
		}
		
		if(udpSocket != null){
			udpSocket.close();
			udpSocket = null;
		}
		
		udpThread = null;
	}
	
	private  void processMessage(Message msg){
		switch(msg.what){
		default:break;
		case Message_Send_Up_Data_Complete:  
			if(canCloseUdpSocket){
				disconnectSocket();
			}
			break;
			
		case MESSAGE_START_RECEIVING_FILES:
			IpMessageProtocol ipmsgSend = new IpMessageProtocol();
			ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
			ipmsgSend.setSenderName(sendername);
			ipmsgSend.setSenderHost(sendergroup);
			ipmsgSend.setCommandNo(IpMessageConst.IPMSG_ACCEPT_RECEIVINGFILES);	//回送报文命令
			ipmsgSend.setAdditionalSection(sendername + "\0" );	//附加信息里加入用户名和分组信息
			
		//	sendUdpData(ipmsgSend.getProtocolString(), udpResPacket.getAddress(), udpResPacket.getPort());	//发送数据
			break;
			
		
		}
	}
	
	public HashMap<String,Devices> getDevices(){
		return devices;
	}
	
	public static void sendEmptyMessage(int what){
		handler.sendEmptyMessage(what);
	}

	
	
}