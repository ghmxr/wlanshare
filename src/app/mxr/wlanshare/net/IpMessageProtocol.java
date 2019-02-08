package app.mxr.wlanshare.net;

import java.util.Date;


/**
 * IPMSG协议抽象类
 * 
 */
public class IpMessageProtocol {
	private String version;	//版本号 目前都为1
	private String packetNo;//数据包编号
	private String senderName;	//发送者昵称（若是PC，则为登录名）
	private String senderHost;	//发送主机名
	private int commandNo;	//命令
	private String additionalSection;	//附加数据
	
	public IpMessageProtocol(){
		this.packetNo = getSeconds();
	}
	
	// 根据协议字符串初始化
	public IpMessageProtocol(String protocolString){
		String[] args = protocolString.split(":");	// 以:分割协议串
		version = args[0];
		packetNo = args[1];
		senderName = args[2];
		senderHost = args[3];
		commandNo = Integer.parseInt(args[4]);
		if(args.length >= 6){	//是否有附加数据
			additionalSection = args[5];
		}else{
			additionalSection = "";
		}
		for(int i = 6; i < args.length; i++){	//处理附加数据中有:的情况
			additionalSection += (":" + args[i]);
		}
		
	}
	
	public IpMessageProtocol(
			String senderName, String senderHost, int commandNo,
			String additionalSection) {
		super();
		this.version = "1";
		this.packetNo = getSeconds();
		this.senderName = senderName;
		this.senderHost = senderHost;
		this.commandNo = commandNo;
		this.additionalSection = additionalSection;
	}


	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getPacketNo() {
		return packetNo;
	}
	public void setPacketNo(String packetNo) {
		this.packetNo = packetNo;
	}
	public String getSenderName() {
		return senderName;
	}
	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}
	public String getSenderHost() {
		return senderHost;
	}
	public void setSenderHost(String senderHost) {
		this.senderHost = senderHost;
	}
	public int getCommandNo() {
		return commandNo;
	}
	public void setCommandNo(int commandNo) {
		this.commandNo = commandNo;
	}
	public String getAdditionalSection() {
		return additionalSection;
	}
	public void setAdditionalSection(String additionalSection) {
		this.additionalSection = additionalSection;
	}
	
	//得到协议串
	public String getProtocolString(){
		StringBuffer sb = new StringBuffer();
		sb.append(version);
		sb.append(":");
		sb.append(packetNo);
		sb.append(":");
		sb.append(senderName);
		sb.append(":");
		sb.append(senderHost);
		sb.append(":");
		sb.append(commandNo);
		sb.append(":");
		sb.append(additionalSection);
		
		return sb.toString();
	}
	
	//得到数据包编号，毫秒数
	private String getSeconds(){
		Date nowDate = new Date();
		return Long.toString(nowDate.getTime());
	}
	
}