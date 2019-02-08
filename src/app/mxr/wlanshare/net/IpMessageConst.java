package app.mxr.wlanshare.net;

/**
 * 协议常量
 */

public class IpMessageConst {
	public static final int VERSION = 0x001;		// 版本号
	public static final int PORT = 0x097a;			// 端口号   默认端口2426
	
	
	public static final int IPMSG_NOOPERATION		 		 = 0x00000000;	//不进行任何操作

	public static final int IPMSG_REQUEST_ONLINE_DEVICES	 = 0x00000001;	//发送寻求在线设备命令
	public static final int IPMSG_DEVICE_OFFLINE		 	 = 0x00000002;	//用户退出
	public static final int IPMSG_ANSENTRY			         = 0x00000003;	//通报在线
	public static final int IPMSG_ANSENTRY_SENDER			 = 0x00000004;	//发送端给予的反馈
	public static final int IPMSG_SENDPORT_INTERRUPT 		 = 0x00000005;  //发送端在等待对方接收确认时取消了发送
	public static final int IPMSG_FILE_REGULAR 			 	 = 0x00000006;
	
	public static final int IPMSG_SENDMSG 			         = 0x00000020;	//发送消息
	public static final int IPMSG_RECVMSG 			 		 = 0x00000021;	//通报收到消息
	
	public static final int IPMSG_GETFILEDATA		 		 = 0x00000060;	//文件传输请求
	public static final int IPMSG_RELEASEFILES		 		 = 0x00000061;	//接收端拒绝接收文件
	public static final int IPMSG_ACCEPT_RECEIVINGFILES 	 = 0x00000062;  //接收端同意接收文件
	public static final int IPMSG_INTERRUPT_FILETRANSFER     = 0x00000063;  //文件传输中途其中一端终止了发送或接收
	
	public static final int IPMSG_SENDCHECKOPT 				 = 0x00000100;	//传送验证
	
	
	public static final int IPMSG_FILEATTACHOPT 	         = 0x00200000;	//附加文件
	

}
