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

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import app.mxr.wlanshare.activities.Activity_Send;
import app.mxr.wlanshare.data.Devices;
import app.mxr.wlanshare.utils.IPUtils;


public class NetSendThread  implements Runnable {
	
	public static final String TAG = "NetThread";
	private static final int BUFFERLENGTH = 1024; //缓冲大小
	private String sendername,sendergroup;
	private byte[] sendBuffer = null;
	private DatagramPacket udpSendPacket = null,udpResPacket = null;	//用于发送的udp数据包
	public static DatagramSocket udpSocket = null;	//用于接收和发送udp数据的socket
	private boolean onWork=false;
	private Thread udpThread = null,udp2=null;
	private byte[] resBuffer = new byte[BUFFERLENGTH];	//接收数据的缓存
	private HashMap<String,Devices> devices;	//当前所有用户的集合，以IP为KEY
	//protected List<Devices> devices;
	private static LinkedList<NetSendThread> queue = new LinkedList<NetSendThread>();
	private boolean canCloseUdpSocket=false;
	private static final int Message_Send_Up_Data_Complete=1;
//	public static final int MESSAGE_SEND_REFUSE_INFO=0x2;
	private static Handler handler=new Handler(){
		public void handleMessage(Message msg) {
			switch(msg.what){
			default:if(queue.size()>0){				
						queue.getLast().processMessage(msg);
					} break;
			}
			
		}
	};
	
	public NetSendThread(String sendername){
		this.sendername=sendername;
		this.sendergroup="";
		devices = new HashMap<String,Devices>();
		if(!queue.contains(this)){
			queue.add(this);
		}
	//	devices = new ArrayList<Devices>();
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
	
	
	
	public void noticeOnline(){	// 发送上线广播，用来请求已连接的设备
		IpMessageProtocol ipmsgSend = new IpMessageProtocol();
		ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
		ipmsgSend.setSenderName(sendername);
		ipmsgSend.setSenderHost(sendergroup);
		ipmsgSend.setCommandNo(IpMessageConst.IPMSG_REQUEST_ONLINE_DEVICES);	//上线命令
		ipmsgSend.setAdditionalSection(sendername + "\0" );	//附加信息里加入用户名和分组信息
		
		InetAddress broadcastAddr;
		try {
			if(!Activity_Send.isAPEnabled){
				broadcastAddr = InetAddress.getByName("255.255.255.255");	//广播地址	
				sendUdpData(ipmsgSend.getProtocolString()+"\0", broadcastAddr, IpMessageConst.PORT);	//发送数据
			}
			else{								
				String initalIP = IPUtils.getanIPfromARP();
				Log.i("通过读取文件获得的IP", initalIP);
				if(initalIP.length()>=8){
					String broadcastadress = initalIP.substring(0, initalIP.lastIndexOf("."))+".255";
					broadcastAddr = InetAddress.getByName(broadcastadress);	//广播地址	
					sendUdpData(ipmsgSend.getProtocolString()+"\0", broadcastAddr, IpMessageConst.PORT);	//发送数据
				}
				
															
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "noticeOnline()....广播地址有误");
		}
		
	}
	
	public void noticeOffline(){	//发送下线广播，，这个方法在发送端用不到
		IpMessageProtocol ipmsgSend = new IpMessageProtocol();
		ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
		ipmsgSend.setSenderName(sendername);
		ipmsgSend.setSenderHost(sendergroup);
		ipmsgSend.setCommandNo(IpMessageConst.IPMSG_DEVICE_OFFLINE);	//下线命令
		ipmsgSend.setAdditionalSection(sendername + "\0" + sendergroup);	//附加信息里加入用户名和分组信息
		
		InetAddress broadcastAddr;
		try {			
			broadcastAddr = InetAddress.getByName("255.255.255.255");	//广播地址			
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
			udpThread=null;
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
					}
					Log.e(TAG, "成功向IP为" + sendto.getHostAddress() + "发送UDP数据：" + sendStr);
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
	
	private synchronized void addUser(IpMessageProtocol ipmsgPro){ //添加用户到Users的Map中
		String userIp = udpResPacket.getAddress().getHostAddress();
		Devices device = new Devices();
//		user.setUserName(ipmsgPro.getSenderName());
		device.setAlias(ipmsgPro.getSenderName());	//别名暂定发送者名称
		device.setUserName(ipmsgPro.getSenderName());
		String extraInfo = ipmsgPro.getAdditionalSection();
		String[] userInfo = extraInfo.split("\0");	//对附加信息进行分割,得到用户名和分组名
		if(userInfo.length < 1){
			device.setUserName(ipmsgPro.getSenderName());
			
		}else if (userInfo.length == 1){
			device.setUserName(userInfo[0]);
			
		}else{
			device.setUserName(userInfo[0]);
			
		}
			
		device.setIp(userIp);
		device.setHostName(ipmsgPro.getSenderHost());
		device.setMac("");	//暂时没用这个字段
		devices.put(userIp, device);
		//devices.add(device);
		Log.i(TAG, "成功添加ip为" + userIp + "的用户");
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
				Log.e(TAG, "接收数据时，系统不支持GBK编码");
			}//截取收到的数据
			Log.i(TAG, "接收到的UDP数据内容为:" + ipmsgStr);
			IpMessageProtocol ipmsgPro = new IpMessageProtocol(ipmsgStr);	//
			int commandNo = ipmsgPro.getCommandNo();
			int commandNo2 = 0x000000FF & commandNo;	//获取命令字
			switch(commandNo2){
			case IpMessageConst.IPMSG_ANSENTRY:	{	//收到上线数据包，添加用户，并回送IPMSG_ANSENTRY应答。
				addUser(ipmsgPro);	//添加用户
				
				Activity_Send.sendEmptyMessage(IpMessageConst.IPMSG_ANSENTRY);    
				
				//下面构造回送报文内容
				IpMessageProtocol ipmsgSend = new IpMessageProtocol();
				ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
				ipmsgSend.setSenderName(sendername);
				ipmsgSend.setSenderHost(sendergroup);
				ipmsgSend.setCommandNo(IpMessageConst.IPMSG_ANSENTRY_SENDER);	//回送报文命令
				ipmsgSend.setAdditionalSection(sendername + "\0" );	//附加信息里加入用户名和分组信息
				
				sendUdpData(ipmsgSend.getProtocolString(), udpResPacket.getAddress(), udpResPacket.getPort());	//发送数据
			}	
				break;
			
		//	case IpMessageConst.IPMSG_ANSENTRY:	{	//收到上线应答，更新在线用户列表
		//		addUser(ipmsgPro);
		//	Activity_Send.sendEmptyMessage(IpMessageConst.IPMSG_ANSENTRY);
		//	}	
		//		break;
			
			case IpMessageConst.IPMSG_DEVICE_OFFLINE:{	//收到下线广播，删除users中对应的值
				String userIp = udpResPacket.getAddress().getHostAddress();
				devices.remove(userIp);
				Activity_Send.sendEmptyMessage(IpMessageConst.IPMSG_DEVICE_OFFLINE);
				
				Log.i(TAG, "根据下线报文成功删除ip为" + userIp + "的用户");
			}	
				break;
			
			case IpMessageConst.IPMSG_SENDMSG:{ //收到消息，处理
				String senderIp = udpResPacket.getAddress().getHostAddress();	//得到发送者IP
				String senderName = ipmsgPro.getSenderName();	//得到发送者的名称
				String additionStr = ipmsgPro.getAdditionalSection();	//得到附加信息
			//	Date time = new Date();	//收到信息的时间
			//	String msgTemp;		//直接收到的消息，根据加密选项判断是否是加密消息
			//	String msgStr;		//解密后的消息内容
				
				//以下是命令的附加字段的判断
				
				//若有命令字传送验证选项，则需回送收到消息报文
				if( (commandNo & IpMessageConst.IPMSG_SENDCHECKOPT) == IpMessageConst.IPMSG_SENDCHECKOPT){
					//构造通报收到消息报文
					IpMessageProtocol ipmsgSend = new IpMessageProtocol();
					ipmsgSend.setVersion("" +IpMessageConst.VERSION);	//通报收到消息命令字
					ipmsgSend.setCommandNo(IpMessageConst.IPMSG_RECVMSG);
					ipmsgSend.setSenderName(sendername);
					ipmsgSend.setSenderHost(sendergroup);
					ipmsgSend.setAdditionalSection(ipmsgPro.getPacketNo() + "\0");	//附加信息里是确认收到的包的编号
					
					sendUdpData(ipmsgSend.getProtocolString(), udpResPacket.getAddress(), udpResPacket.getPort());	//发送数据
				}
				
				String[] splitStr = additionStr.split("\0"); //使用"\0"分割，若有附加文件信息，则会分割出来
			//	msgTemp = splitStr[0]; //将消息部分取出
				
				//是否有发送文件的选项.若有，则附加信息里截取出附带的文件信息
				if((commandNo & IpMessageConst.IPMSG_FILEATTACHOPT) == IpMessageConst.IPMSG_FILEATTACHOPT){	
					//下面进行发送文件相关处理
					
					Message msg = new Message();
					msg.what = (IpMessageConst.IPMSG_SENDMSG | IpMessageConst.IPMSG_FILEATTACHOPT);
					//字符串数组，分别放了  IP，附加文件信息,发送者名称，包ID
					String[] extraMsg = {senderIp, splitStr[1],senderName,ipmsgPro.getPacketNo()};	
					msg.obj = extraMsg;	//附加文件信息部分
					Activity_Send.sendMessage(msg);
					
					break;
				}
																
				
			}
				break;
				
			case IpMessageConst.IPMSG_RELEASEFILES:{ //拒绝接受文件
			//	MyFeiGeBaseActivity.sendEmptyMessage(IpMessageConst.IPMSG_RELEASEFILES);
				Activity_Send.sendEmptyMessage(Activity_Send.MESSAGE_REFUSED_SENDING_FILES);
			}
				break;
				
			case IpMessageConst.IPMSG_ACCEPT_RECEIVINGFILES:{
				Activity_Send.sendEmptyMessage(Activity_Send.MESSAGE_START_SENDING_FILES);
			}
			break;
			
			case IpMessageConst.IPMSG_INTERRUPT_FILETRANSFER:{
				Activity_Send.isSendFileSuccess=false;
				Activity_Send.sendEmptyMessage(Activity_Send.MESSAGE_SENDINGFILES_INTERRUPT);
			}
			break;
				
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
	
	public HashMap<String,Devices> getDevices(){
		return devices;
	}
//	public List<Devices>getDevices(){
//		return devices;
//	}
	public void refreshDevices(){	//刷新在线用户
		devices.clear();	//清空在线用户列表
		noticeOnline(); //发送上线通知
	//	Activity_Send.sendEmptyMessage(IpMessageConst.IPMSG_ANSENTRY);
	}
	
	private  void processMessage(Message msg){
		switch(msg.what){
		default:break;
		case Message_Send_Up_Data_Complete:  
			if(canCloseUdpSocket){
				disconnectSocket();
			}
			break;
					
		}
	}
	
	public static void sendEmptyMessage(int what){
		handler.sendEmptyMessage(what);
	}
	
	public static void sendMessage(Message msg){
		handler.sendMessage(msg);
	}

}
