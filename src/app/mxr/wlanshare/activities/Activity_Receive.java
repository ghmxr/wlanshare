package app.mxr.wlanshare.activities;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.wlanshare.R;
import app.mxr.wlanshare.net.IpMessageConst;
import app.mxr.wlanshare.net.IpMessageProtocol;
import app.mxr.wlanshare.net.NetReceiveThread;
import app.mxr.wlanshare.net.NetTcpFileReceiveThread;
import app.mxr.wlanshare.ui.FileTransferDialog;
import app.mxr.wlanshare.ui.FileTransferNotification;
import app.mxr.wlanshare.ui.NotificationManagement;
import app.mxr.wlanshare.utils.IPUtils;
import app.mxr.wlanshare.utils.StorageUtil;
import app.mxr.wlanshare.utils.WifiManagement;

public class Activity_Receive extends BaseActivity{
	ImageView img_done;
	ProgressBar pg;
	TextView attention,ssidinfo;
	RelativeLayout bottomarea;
	boolean  isConnected=false;
	public static boolean isAPMode=false;
	private FileTransferDialog receivingFileDialog;
	AlertDialog ad_receivefile;
	WifiManagement wmanage;
	protected static LinkedList<Activity_Receive> queue=new LinkedList<Activity_Receive>();
	private static final int NO_WIFI_AVALIABLE =   0x1;
	private static final int WIFI_CONNECTED    =   0x2;
	private static final String SSID_INDEX="mxr-wlanshare-";
	private Thread wait10s=null;
	private boolean ifneedCloseWifi=false;
	protected static NetReceiveThread netThread=null;
	private String senderip;
	Thread fileReceiveThread;
	List<String[]> filevalues;
	long totalBytes=10;
	public static boolean isReceivingFileSuccess=true;
	private  boolean ifcancallclose=true;
	private boolean isinReceivingProcess=false;
	private boolean isatFront=false;
	private FileTransferNotification fnotification;
	private NotificationManagement notificationmanagement;
	public static final int 
			MESSAGE_WIFI_CLOSED_MANNUAL			=0x00019,			//wifi被手动关闭，本界面所有内容停止
			MESSAGE_OPEN_WIFI_FAILED			=0x00020, 			//打开wifi失败
			MESSAGE_REFRESH_FILEDIALOG_PROGRESS =0x00025,          //刷新传输文件对话框的进度
			MESSAGE_START_RECEIVING_FILES		=0x00024,             //开始接收文件
			MESSAGE_REFRESH_FILEDIALOG_SPEED	=0X00023,
			MESSAGE_REFUSED_RECEIVING_FILES		=0X00022,		//拒绝接收文件
			MESSAGE_RECEIVING_FILES_COMPLETE	=0x00026,		//接收文件完成
			MESSAGE_RECEIVINGFILES_INTERRUPT	=0x00027,      //接收中途中断
			MESSAGE_RECEIVING_CURRENTFILE		=0x00028,        //当前接收的文件
			MESSAGE_DISMISS_REQUESTDIALOG		=0x00030,       //  对方等待本端回复时取消了发送请求，本端关闭请求对话框
			MESSAGE_RECEIVINGFILES_INTERRUPT_SELF=0x00031;		//本端手动停止接收
			
	
	private static Handler handler=new Handler(){
		
		public void handleMessage(Message msg) {
			switch(msg.what){
			default:if(queue.size() > 0)
				queue.getLast().processMessage(msg);
		//	Log.e("Activity_Receive", "Activity_Receive.handler received message");
			break;
			}
		}
	};
	
	
	
	
	protected void onCreate(Bundle myBundle){
		super.onCreate(myBundle);
		this.getActionBar().setDisplayHomeAsUpEnabled(true);
		this.getActionBar().setDisplayShowHomeEnabled(true);
		this.getActionBar().setIcon(R.drawable.ic_launcher);
		if(Activity_Send.queue.size()>0){
			new AlertDialog.Builder(this)
			.setTitle(this.getResources().getString(R.string.dialog_main_receive_warn_title))
			.setMessage(this.getResources().getString(R.string.dialog_main_receive_warn_message))
			.setIcon(R.drawable.icon_alertdialog_warn)
			.setCancelable(false)
			.setPositiveButton(this.getResources().getString(R.string.button_turnto), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					Intent i = new Intent();
					i.setClass(Activity_Receive.this, Activity_Send.class);
					Activity_Receive.this.startActivity(i);
					Activity_Receive.this.finish();
				}
			})
			.setNegativeButton(this.getResources().getString(R.string.button_negative_cancel), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					Activity_Receive.this.finish();
				}
			})
			.show();
		}
		else{
			if(!queue.contains(this)){
				queue.add(this);
			}
			
			wmanage=new WifiManagement(this);
			wmanage.setSSIDIndex(SSID_INDEX);
		//	BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
		//	this.deviceName = myDevice.getName();
			
			if(!wmanage.isWifiEnabled()){
				ifneedCloseWifi=true;
			/*	AlertDialog ad_openwifi = new AlertDialog.Builder(this).setTitle(Activity_Receive.this.getResources().getString(R.string.dialog_openwifi_title))
						.setMessage(Activity_Receive.this.getResources().getString(R.string.dialog_openwifi_message))
						.setCancelable(false)
						.setPositiveButton(Activity_Receive.this.getResources().getString(R.string.button_possitive_confirm), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								
								
							}
						})
						.setNegativeButton(Activity_Receive.this.getResources().getString(R.string.button_negative_cancel), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								Activity_Receive.this.finish();
							}
						})
						.create();  */
			//	ad_openwifi.show();
				setView();
				//setViewofSearching();
				Activity_Receive.this.connectToOpenWifi();
						
			}
			else{	
				if(wmanage.isWifiConnected()){
				/*	new AlertDialog.Builder(this)
					.setTitle(this.getResources().getString(R.string.dialog_attention_connecttosamenet_title))
					.setMessage(this.getResources().getString(R.string.dialog_attention_connecttosamenet_message))
					.setPositiveButton(this.getResources().getString(R.string.button_possitive_confirm), new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
						//	setView();
						//	setViewofSearching();
						//	Activity_Receive.this.connectToOpenWifi();
							
						}
					})				
					.setCancelable(false)
					.create();   */
					
					
					setView();						
					Activity_Receive.sendEmptyMessage(WIFI_CONNECTED);
				}
				else{
					setView();
				//	setViewofSearching();
					Activity_Receive.this.connectToOpenWifi();
				}
							
			}   
			
		}
		
	}
	
	
	protected void onResume(){
		super.onResume();
		this.isatFront=true;
	}
	
	protected void onPause(){
		super.onPause();
		this.isatFront=false;
	}
	
	protected void setView(){
		setContentView(R.layout.layout_receive);
		pg=(ProgressBar)findViewById(R.id.progressBar_receive);
		img_done=(ImageView)findViewById(R.id.imageview_receive);
		attention=(TextView)findViewById(R.id.receive_att);
		ssidinfo = (TextView)findViewById(R.id.ssidinfo);
		bottomarea=(RelativeLayout)findViewById(R.id.bottomarea);
	}
	
	private void setViewofSearching(){
		pg.setVisibility(View.VISIBLE);
		img_done.setVisibility(View.INVISIBLE);
		bottomarea.setVisibility(View.INVISIBLE);
		attention.setText(Activity_Receive.this.getResources().getString(R.string.textview_searchingwifi));
	}
	
	private void setViewofConnected(boolean isAPMode){		
		pg.setVisibility(View.INVISIBLE);
		bottomarea.setVisibility(View.VISIBLE);
		if(isAPMode){
			img_done.setImageResource(R.drawable.icon_apreceive);
			attention.setText(this.getResources().getString(R.string.textview_ap_wificonnected));
			ssidinfo.setText(this.getResources().getString(R.string.text_ssid_info)+wmanage.getSSID()+"\n"+this.getResources().getString(R.string.text_receive_apssid_att));
		}
		else{
			img_done.setImageResource(R.drawable.icon_wifimode);
			attention.setText(Activity_Receive.this.getResources().getString(R.string.textview_wificonnected));
			ssidinfo.setText(this.getResources().getString(R.string.text_ssid_info)+wmanage.getSSID()+"\n"+this.getResources().getString(R.string.text_receive_normalssid_att));
		}
		img_done.setVisibility(View.VISIBLE);
		
	}
	
	private void setViewofNull(){
		pg.setVisibility(View.GONE);
		img_done.setVisibility(View.GONE);
		attention.setVisibility(View.GONE);
		ssidinfo.setVisibility(View.GONE);
	}
	
	private void connectToOpenWifi(){
	//	wmanage.setWifiEnabled(true);
		setViewofSearching();
		wmanage.connecttoOpenWifi();	
		wait10s=new Thread(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				try{
					Thread.sleep(10*1000);
					
					if(!wmanage.isWifiConnected()){
						Activity_Receive.sendEmptyMessage(NO_WIFI_AVALIABLE);
						Log.i("Activity_Receive", "当前未连接wifi，取得的SSID值是  "+wmanage.getSSID());
					}
					else{
						if(wmanage.isWifiConnected()){
							Activity_Receive.sendEmptyMessage(WIFI_CONNECTED);
						}
						
					}
					
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			
		});
		
		wait10s.start();
		
	}
	
	private void startNet(){
		netThread=new NetReceiveThread(deviceName,this);
		netThread.connectSocket();
		netThread.noticeOnline();
		if(!Activity_Receive.isAPMode){
			attention.setText(this.getResources().getString(R.string.textview_wificonnected)+"\n"
					+this.getResources().getString(R.string.text_send_wifimode_ip)+IPUtils.getLocalIpAddress(this));
		}
		else{
			attention.setText(this.getResources().getString(R.string.textview_ap_wificonnected)+"\n"
					+this.getResources().getString(R.string.text_send_wifimode_ip)+IPUtils.getLocalIpAddress(this));
		}
		
		
		if(!wmanage.isWifiConnected()){
			this.ifcancallclose=false;
			closeAllOperationsAndExit();
			setViewofNull();
			new AlertDialog.Builder(this)
			.setTitle(this.getResources().getString(R.string.dialog_receive_wificlosed_title))
			.setMessage(this.getResources().getString(R.string.dialog_receive_wificlosed_message))
			.setCancelable(false)
			.setPositiveButton(this.getResources().getString(R.string.button_possitive_confirm), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					Activity_Receive.this.finish();
				}
			})
			.show();
		}
	}
	
	
	public  void processMessage(Message msg){
		switch(msg.what){
		case MESSAGE_OPEN_WIFI_FAILED:
			setViewofNull();
			if(wait10s!=null){
				wait10s.interrupt();
				wait10s=null;
			}
			new AlertDialog.Builder(this).setIcon(R.drawable.icon_alertdialog_warn)
			.setTitle(this.getResources().getString(R.string.dialog_openwifi_failed_title))
			.setMessage(this.getResources().getString(R.string.dialog_openwifi_failed_message))
			.setCancelable(false)
			.setPositiveButton(this.getResources().getString(R.string.button_retry), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub					
					Activity_Receive.this.connectToOpenWifi();
					setView();
					setViewofSearching();
				}
			})
			.setNegativeButton(this.getResources().getString(R.string.button_negative_cancel), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					Activity_Receive.this.finish();
				}
			})
			.show();
			break;
		case NO_WIFI_AVALIABLE:
			this.isConnected=false;
			wmanage.stopConnecttoOpenWifi();
			Log.e("Activity_Send", "connected value"+wmanage.isWifiConnected());
			AlertDialog ad_retry = new AlertDialog.Builder(Activity_Receive.this)
					.setIcon(R.drawable.icon_face_ops)
					.setTitle(Activity_Receive.this.getResources().getString(R.string.dialog_nowifiavaliable_title))
			.setMessage(Activity_Receive.this.getResources().getString(R.string.dialog_nowifiavaliable_message))
			.setCancelable(false)
			.setPositiveButton(Activity_Receive.this.getResources().getString(R.string.button_retry), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					if(wait10s!=null){
						wait10s.interrupt();
						wait10s=null;
					}
					Activity_Receive.this.connectToOpenWifi();
					setView();
					setViewofSearching();
				}
			})
			.setNegativeButton(Activity_Receive.this.getResources().getString(R.string.button_negative_cancel), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					Activity_Receive.this.finish();
				}
			})
			.create();
			setViewofNull();
			ad_retry.show();
			
			
			break;
			
		case WIFI_CONNECTED: 
			if(wait10s!=null){
				wait10s.interrupt();
				wait10s=null;
			}
			wmanage.stopConnecttoOpenWifi();
			this.isConnected=true;
			
			if(wmanage.getSSID().indexOf(SSID_INDEX)!=-1){
				isAPMode=true;
			}
			else{
				isAPMode=false;
			}
			setViewofConnected(isAPMode);
			Log.i("Activity_Receive", "当前连接的wifi的SSID是  "+wmanage.getSSID());
			startNet();
			break;
			
			
		case IpMessageConst.IPMSG_SENDMSG | IpMessageConst.IPMSG_FILEATTACHOPT:{  
			//收到发送文件请求  
			if(!this.isinReceivingProcess){
				this.isinReceivingProcess=true;
				final String[] extraMsg = (String[]) msg.obj;	//得到附加文件信息,字符串数组，分别放了  IP，附加文件信息,发送者名称，包ID
				Log.d("receive file....", "receive file from :" + extraMsg[2] + "(" + extraMsg[0] +")");
				Log.d("receive file....", "receive file info:" + extraMsg[1]);
				byte[] bt = {0x07};		//用于分隔多个发送文件的字符
				String splitStr = new String(bt);
				final String[] fileInfos = extraMsg[1].split(splitStr);	//使用分隔字符进行分割
				this.senderip=extraMsg[0];
				Log.d("Activity_Send", "收到文件传输请求,共有" + fileInfos.length + "个文件");
				notificationmanagement=new NotificationManagement(this);
				notificationmanagement.notification.setSmallIcon(R.drawable.icon_download);
				notificationmanagement.setDefaults(NotificationManagement.DEFAULT_ALL);
				notificationmanagement.notification.setContentTitle(this.getResources().getString(R.string.notification_receive_receiverequest));
				notificationmanagement.notification.setContentText("发送者IP："+extraMsg[0]+"\n"+"发送者名称："+extraMsg[2]);
				notificationmanagement.notification.setAutoCancel(true);								
				notificationmanagement.notification.setOngoing(false);
				notificationmanagement.setTargetActivity(Activity_Receive.class);
						
				if(Build.VERSION.SDK_INT>=21){
					notificationmanagement.setFullSreenIntent(Activity_Receive.class);
				}
				if(!this.isatFront){
					notificationmanagement.notify(2);
					Toast.makeText(this, Activity_Receive.this.getResources().getString(R.string.notification_receive_receiverequest)+"  共有" + fileInfos.length + "个文件", Toast.LENGTH_SHORT).show();	
				}
								
			//	String infoStr = "发送者IP:\t" + extraMsg[0] + "\n" + 
			//					 "发送者名称:\t" + extraMsg[2] + "\n" +
			//					 "文件总数:\t" + fileInfos.length +"个";
				
				String fileinfostotal = "";
				
				for(int f=0;f<fileInfos.length;f++){
					String fileval[]=fileInfos[f].split(":");
					String filename=fileval[1];
					long filelength = Long.parseLong(fileval[2],16);
					fileinfostotal+=filename+"  "+Formatter.formatFileSize(Activity_Receive.this, filelength)+"\n\n";
					
				}
				
				if(this.ad_receivefile!=null){
					this.ad_receivefile.cancel();
					this.ad_receivefile=null;
				}
				 ad_receivefile = new AlertDialog.Builder(Activity_Receive.this).setIcon(R.drawable.icon_files_small)
						.setTitle(this.getResources().getString(R.string.dialog_receivefilerequest_title))
						.setMessage(this.getResources().getString(R.string.dialog_receivefilerequest_message1)+
								this.getResources().getString(R.string.dialog_receivefilerequest_message2)+extraMsg[0]+"\n"
								+this.getResources().getString(R.string.dialog_receivefilerequest_message3)+extraMsg[2]+"\n"
								+this.getResources().getString(R.string.dialog_receivefilerequest_message4)+fileInfos.length+"\n"
								+"文件信息如下：\n\n"+fileinfostotal)
						.setPositiveButton(this.getResources().getString(R.string.button_receive),new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								filevalues=new ArrayList<String[]>();
								long totalBytes = 0;
								for(int i =0;i<fileInfos.length;i++){
									String[] val = fileInfos[i].split(":");
									filevalues.add(val);
									totalBytes+=Long.parseLong(val[2],16);
								}	
								Activity_Receive.this.totalBytes = totalBytes;
								if(totalBytes+10*1024*1024>StorageUtil.getSDAvaliableSize()){   //存储不足
									//发送拒绝报文
									Activity_Receive.this.isinReceivingProcess=false;
									Activity_Receive.this.senupUDP(IpMessageConst.IPMSG_RELEASEFILES, extraMsg[0]);
									
									//显示弹框
									new AlertDialog.Builder(Activity_Receive.this)
									.setTitle(Activity_Receive.this.getResources().getString(R.string.dialog_storage_notenough_title))
									.setMessage(Activity_Receive.this.getResources().getString(R.string.dialog_storage_notenough_message))
									.setPositiveButton(Activity_Receive.this.getResources().getString(R.string.button_possitive_confirm), new DialogInterface.OnClickListener() {
										
										@Override
										public void onClick(DialogInterface dialog, int which) {
											// TODO Auto-generated method stub
										
										}
									})
									.show();
								}
								else{
									Activity_Receive.isReceivingFileSuccess=true;
									NetReceiveThread.sendEmptyMessage(NetReceiveThread.MESSAGE_START_RECEIVING_FILES);
									fileReceiveThread = new Thread(new NetTcpFileReceiveThread(extraMsg[3], extraMsg[0],fileInfos));	//新建一个接受文件线程
									fileReceiveThread.start();	//启动线程	
									Activity_Receive.this.isinReceivingProcess=true;
									//totalKBytes=0;
																		
									receivingFileDialog = new FileTransferDialog(Activity_Receive.this);
									receivingFileDialog.setTitle(Activity_Receive.this.getResources().getString(R.string.dialog_receivingfiles_title));
									receivingFileDialog.setIcon(R.drawable.icon_files_small);
									receivingFileDialog.setMax(totalBytes);
									receivingFileDialog.setButton(AlertDialog.BUTTON_NEGATIVE, Activity_Receive.this.getResources().getString(R.string.button_stop), 
											new DialogInterface.OnClickListener() {
										
										@Override
										public void onClick(DialogInterface dialog, int which) {
											// TODO Auto-generated method stub
											Activity_Receive.isReceivingFileSuccess=false;
											Activity_Receive.this.isinReceivingProcess=false;
											if(fileReceiveThread!=null){
												fileReceiveThread.interrupt();
												fileReceiveThread=null;
												Activity_Receive.this.senupUDP(IpMessageConst.IPMSG_INTERRUPT_FILETRANSFER, extraMsg[0]);
												try{
													if(NetTcpFileReceiveThread.socket!=null){
														NetTcpFileReceiveThread.socket.close();
														NetTcpFileReceiveThread.socket=null;
													}											
												}catch(Exception e){
													e.printStackTrace();
												}	
											}
											Activity_Receive.sendEmptyMessage(MESSAGE_RECEIVINGFILES_INTERRUPT_SELF);
											
										}
									});
									receivingFileDialog.setCancelable(false);
									
									fnotification=new FileTransferNotification(Activity_Receive.this);
									fnotification.notification.setSmallIcon(R.drawable.icon_download);
									fnotification.notification.setContentTitle(Activity_Receive.this.getResources().getString(R.string.notification_receive_receiving_title));
									fnotification.notification.setContentText("已完成：");
									fnotification.notification.setOngoing(true);
									Intent intent = new Intent();
									intent.setClass(Activity_Receive.this, Activity_Receive.class);
									PendingIntent pi = PendingIntent.getActivity(Activity_Receive.this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
									fnotification.notification.setContentIntent(pi);
									
									
									Activity_Receive.this.senupUDP(IpMessageConst.IPMSG_ACCEPT_RECEIVINGFILES, extraMsg[0]);

								//	IpMessageProtocol ipmsgSend = new IpMessageProtocol();
								//	ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
								//	ipmsgSend.setSenderName("android");
								//	ipmsgSend.setSenderHost("none");
								//	ipmsgSend.setCommandNo(IpMessageConst.IPMSG_ACCEPT_RECEIVINGFILES);	//回送报文命令
								//	ipmsgSend.setAdditionalSection(extraMsg[3] + "\0");	//附加信息里加入用户名和分组信息
								//	InetAddress sendAddress = null;
								//	try {
								//		sendAddress = InetAddress.getByName(extraMsg[0]);
								//	} catch (UnknownHostException e) {
										// TODO Auto-generated catch block
								//		e.printStackTrace();
								//	}
								//	netThread.sendUdpData(ipmsgSend.getProtocolString(), sendAddress, IpMessageConst.PORT);	//发送数据
									Activity_Receive.sendEmptyMessage(Activity_Receive.MESSAGE_START_RECEIVING_FILES);
									Toast.makeText(Activity_Receive.this, "开始接收文件", Toast.LENGTH_SHORT).show();
								}
								
								
								
							}
						}).setNegativeButton(this.getResources().getString(R.string.button_refuse), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								//发送拒绝报文
					 			//构造拒绝报文
								Activity_Receive.this.isinReceivingProcess=false;
								Activity_Receive.this.senupUDP(IpMessageConst.IPMSG_RELEASEFILES, extraMsg[0]);
								
							//	IpMessageProtocol ipmsgSend = new IpMessageProtocol();
							//	ipmsgSend.setVersion("" +IpMessageConst.VERSION);	//拒绝命令字
							//	ipmsgSend.setCommandNo(IpMessageConst.IPMSG_RELEASEFILES);
							//	ipmsgSend.setSenderName("android飞鸽");
							//	ipmsgSend.setSenderHost("android");
							//	ipmsgSend.setAdditionalSection(extraMsg[3] + "\0");	//附加信息里是确认收到的包的编号
					 			
							//	InetAddress sendAddress = null;
							//	try {
							//		sendAddress = InetAddress.getByName(extraMsg[0]);
							//	} catch (UnknownHostException e) {
									// TODO Auto-generated catch block
							//		e.printStackTrace();
							//	}
								
							//	netThread.sendUdpData(ipmsgSend.getProtocolString(), sendAddress, IpMessageConst.PORT);
							}
						}).create();
				 ad_receivefile.setCancelable(false);
				 
				 ad_receivefile.show(); 
				 
			}
			
			
		}break;
		case Activity_Receive.MESSAGE_START_RECEIVING_FILES:{
			if(this.ad_receivefile!=null){
				this.ad_receivefile.cancel();
			}
			if(this.receivingFileDialog!=null){
				this.receivingFileDialog.show();
			}
			if(this.fnotification!=null){
				fnotification.notification.setOngoing(true);
				fnotification.notification.setAutoCancel(false);
				fnotification.notify(2);
			}
		
		}
		break;
		case Activity_Receive.MESSAGE_REFRESH_FILEDIALOG_SPEED:{
			long value_speed[]=(long[])msg.obj;
			if(this.receivingFileDialog!=null){
				this.receivingFileDialog.setSpeed(value_speed[0]);
			}
			
		}
		break;
		case Activity_Receive.MESSAGE_REFRESH_FILEDIALOG_PROGRESS:{
			long value_progress[]=(long[])msg.obj;
			if(this.receivingFileDialog!=null){
				this.receivingFileDialog.setProgress(value_progress[0]);
			}			
			DecimalFormat dm=new DecimalFormat("#.00");	
			int percent =(int) (Double.valueOf(dm.format((double)value_progress[0]/this.totalBytes))*100);
			if(this.fnotification!=null){
				fnotification.notification.setContentText("已完成"+percent+"%");
				fnotification.setProgress(percent, 2);
			}
			
		}
		break;
		case Activity_Receive.MESSAGE_RECEIVING_CURRENTFILE:{
			String value_filename[]=(String []) msg.obj;
			if(this.receivingFileDialog!=null){
				this.receivingFileDialog.setTitle(this.getResources().getString(R.string.dialog_receivingfiles_title)+value_filename[0]+"/"+value_filename[1]);
				this.receivingFileDialog.setFilename(this.getResources().getString(R.string.dialog_receivingfiles_title)+": "+value_filename[2]);
			}			
		}
		break;
		case Activity_Receive.MESSAGE_DISMISS_REQUESTDIALOG:
			String ip[] =(String[]) msg.obj;
			String requestip=ip[0];
			if(requestip.equals(this.senderip)){
				this.isinReceivingProcess=false;
				if(this.ad_receivefile!=null){
					ad_receivefile.cancel();
				}
				
				if(this.notificationmanagement!=null){
					this.notificationmanagement.manager.cancel(2);
				}
				
				Toast.makeText(this, "对方取消了发送文件", Toast.LENGTH_SHORT).show();
			}
			break;
		case Activity_Receive.MESSAGE_RECEIVING_FILES_COMPLETE:
			this.isinReceivingProcess=false;
			String[] newpath=(String[])msg.obj;
			if(this.receivingFileDialog!=null){
				this.receivingFileDialog.cancel();
			}
			if(this.fnotification!=null){
				fnotification.notification.setContentTitle(this.getResources().getString(R.string.notification_receive_complete_title));
				fnotification.notification.setProgress(100, 100, false);
				fnotification.notification.setContentText("已完成");
				fnotification.notification.setOngoing(false);
				fnotification.notification.setAutoCancel(true);
				fnotification.notify(2);
			}
			if(this.notificationmanagement!=null&&!this.isatFront){
				this.notificationmanagement.notification.setContentTitle(this.getResources().getString(R.string.notification_receive_complete_title));
				this.notificationmanagement.notification.setContentText("文件接收完成");
				this.notificationmanagement.notification.setOngoing(false);
				this.notificationmanagement.notification.setAutoCancel(true);
				this.notificationmanagement.notify(2);
				Toast.makeText(this, this.getResources().getString(R.string.notification_receive_complete_title), Toast.LENGTH_SHORT).show();
			}
			new AlertDialog.Builder(this).setTitle("接收完成").setIcon(R.drawable.icon_files_small)
			.setMessage("文件已接收至 本机存储/WiFiRev/"+newpath[0])
			.setPositiveButton(this.getResources().getString(R.string.button_possitive_confirm), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					   
				}
			})
			.setCancelable(true)
			.show();
			
			break;
			
		case	MESSAGE_RECEIVINGFILES_INTERRUPT:
			if(this.receivingFileDialog!=null){
				this.receivingFileDialog.cancel();
			}
			
			if(fileReceiveThread!=null){
				fileReceiveThread.interrupt();
				fileReceiveThread=null;
			}
			if(NetTcpFileReceiveThread.socket!=null){
				try{
					NetTcpFileReceiveThread.socket.close();
				}catch(Exception e){
					e.printStackTrace();
				}
				NetTcpFileReceiveThread.socket=null;
			}
			this.isinReceivingProcess=false;
			if(this.fnotification!=null){
				fnotification.notification.setContentTitle(this.getResources().getString(R.string.notification_receive_stopreceiving_title));
				fnotification.notification.setContentText("对方停止了发送");
				fnotification.notification.setOngoing(false);
				fnotification.notification.setAutoCancel(true);
				fnotification.notify(2);
			}
			if(this.notificationmanagement!=null&&!this.isatFront){
				this.notificationmanagement.notification.setContentTitle(this.getResources().getString(R.string.notification_receive_stopreceiving_title));
				this.notificationmanagement.notification.setContentText("对方停止了发送");
				this.notificationmanagement.notification.setOngoing(false);
				this.notificationmanagement.notification.setAutoCancel(true);
				this.notificationmanagement.notify(2);				
			}	
			
			Toast.makeText(this, this.getResources().getString(R.string.notification_receive_stopreceiving_title)+" 对方终止了发送文件", Toast.LENGTH_LONG).show();
			
			break;
		case  NET_CONNECTIVITY_CHANGED:
			if(this.ifcancallclose&&this.isConnected&&!wmanage.isWifiConnected()&&queue.size()>0){
				this.ifcancallclose=false;
				closeAllOperationsAndExit();
				setViewofNull();
				
				this.notificationmanagement=new NotificationManagement(this);
				this.notificationmanagement.notification.setSmallIcon(R.drawable.icon_alertdialog_warn);
				this.notificationmanagement.notification.setContentTitle(this.getResources().getString(R.string.notification_connectivity_broken));
				this.notificationmanagement.notification.setContentText("网络连接断开，收发终止");	
				this.notificationmanagement.setTargetActivity(Activity_Receive.class);
				this.notificationmanagement.notification.setAutoCancel(true);
				this.notificationmanagement.notification.setOngoing(false);
				this.notificationmanagement.notify(2);
				
				new AlertDialog.Builder(this)
				.setTitle(this.getResources().getString(R.string.dialog_receive_wificlosed_title))
				.setMessage(this.getResources().getString(R.string.dialog_receive_wificlosed_message))
				.setCancelable(false)
				.setPositiveButton(this.getResources().getString(R.string.button_possitive_confirm), new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						Activity_Receive.this.finish();
					}
				})
				.show();
			}
			
		
		break;
		case MESSAGE_RECEIVINGFILES_INTERRUPT_SELF:{
			if(Activity_Receive.this.fnotification!=null){
				Activity_Receive.this.fnotification.notification.setOngoing(false);
				Activity_Receive.this.fnotification.notification.setContentTitle(this.getResources().getString(R.string.notification_receive_stopreceiving_title));
				Activity_Receive.this.fnotification.notification.setContentText("已停止接收");
				Activity_Receive.this.fnotification.notification.setAutoCancel(true);
				Activity_Receive.this.fnotification.notify(2);
			}
		}
		break;
		default:break;
		}
	}
	
	public static void sendEmptyMessage(int what) {
		handler.sendEmptyMessage(what);
	}
	
	public static void sendMessage(Message msg) {
		handler.sendMessage(msg);
	}
	
	private void closeAllOperationsAndExit(){
		
			if(this.receivingFileDialog!=null){
				this.receivingFileDialog.cancel();
			}
			if(wait10s!=null){
				wait10s.interrupt();
				wait10s=null;
			}
			
			if(this.wmanage!=null){
				this.wmanage.stopConnecttoOpenWifi();
			}
			
			if(fileReceiveThread!=null){
				fileReceiveThread.interrupt();
				fileReceiveThread=null;
			}
			if(NetTcpFileReceiveThread.socket!=null){
				try{
					NetTcpFileReceiveThread.socket.close();
				}catch(Exception e){
					e.printStackTrace();
				}
				NetTcpFileReceiveThread.socket=null;
			}
			if(netThread!=null){
				netThread.disconnectSocket();
			}
		
		
	}
	
	public void onWifiStateChanged(int state){
		if(state==NET_CONNECTIVITY_CHANGED&&queue.size()>0){
			Activity_Receive.sendEmptyMessage(NET_CONNECTIVITY_CHANGED);			
		}
	}
	
	public void onAPStateChanged(int state){
		
	}
	
	public void finish(){
		super.finish();
		if(queue.contains(this)){
			queue.remove(this);
		}
		if(wait10s!=null){
			wait10s.interrupt();
			wait10s=null;
		}
		if(wmanage!=null){
			wmanage.stopConnecttoOpenWifi();
		}
		
		if(ifneedCloseWifi){
		//	wmanage.setWifiEnabled(false);
		}
		if(this.isConnected){	
			netThread.noticeOffline();
		//	netThread.disconnectSocket();			
			Log.e("Activity_Receive", "已尝试发送下线数据包和关闭端口");
		}
	
	}
	
	private void senupUDP(int command,String IP){
		IpMessageProtocol ipmsgSend = new IpMessageProtocol();
		ipmsgSend.setVersion(String.valueOf(IpMessageConst.VERSION));
		ipmsgSend.setSenderName("android");
		ipmsgSend.setSenderHost("none");
		ipmsgSend.setCommandNo(command);	//回送报文命令
		ipmsgSend.setAdditionalSection("");	//附加信息里加入用户名和分组信息
		InetAddress sendAddress = null;
		try {
			sendAddress = InetAddress.getByName(IP);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		netThread.sendUdpData(ipmsgSend.getProtocolString(), sendAddress, IpMessageConst.PORT);	//发送数据
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			this.finish();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	

}
