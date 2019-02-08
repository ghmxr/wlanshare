package app.mxr.wlanshare.activities;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.wlanshare.R;
import app.mxr.wlanshare.net.NetSendThread;
import app.mxr.wlanshare.net.IpMessageConst;
import app.mxr.wlanshare.net.IpMessageProtocol;
import app.mxr.wlanshare.net.NetTcpFileSendThread;
import app.mxr.wlanshare.ui.FileTransferDialog;
import app.mxr.wlanshare.ui.FileTransferNotification;
import app.mxr.wlanshare.ui.NotificationManagement;
import app.mxr.wlanshare.utils.APManager;
import app.mxr.wlanshare.utils.FileSize;
import app.mxr.wlanshare.utils.IPUtils;
import app.mxr.wlanshare.utils.StorageUtil;
import app.mxr.wlanshare.utils.WifiManagement;

public class Activity_Send extends BaseActivity {
	
	//String deviceName="";
	Intent shareIntent;
	String filePaths[];
	ProgressBar pg_refreshing;
	ImageView modeins,icon_nofound;
	TextView tv_onlinenum,tv_mode,tv_nofound,tv_fileinfo1,tv_fileinfo2;
	Button refresh;
	ArrayList<Uri> filePathList;	
//	private HashMap<String,Devices> devices;
	protected static NetSendThread netThread;
	ListView devicelist;
	protected static LinkedList<Activity_Send> queue=new LinkedList<Activity_Send>();
	public static Context context_send;
	private WifiManagement wm;
	private boolean isnetConnected=false;
	private boolean ifneedRestartWifi=false;
	private boolean isatFront=false;
	public static  boolean isAPEnabled=false;
	private static final int WAIT_AP_COMPLETE=511,MESSAGE_GETPATH_ERROR=512;
	public static final int MESSAGE_REFRESH_FILEDIALOG_PROGRESS       =0x00025,
							MESSAGE_START_SENDING_FILES				  =0x00024,
							MESSAGE_REFRESH_FILEDIALOG_SPEED		  =0X00023,
							MESSAGE_REFUSED_SENDING_FILES			  =0X00022,
							MESSAGE_SENDING_FILES_COMPLETE			  =0x00026,
							MESSAGE_SENDINGFILES_INTERRUPT			  =0x00027,
							MESSAGE_SENDING_CURRENTFILE				  =0x00028,
							MESSAGE_NO_DEVICE_FOUND					  =0x00029,
							MESSAGE_SENDINGFILES_INTERRUPT_SELF		  =0x00030,
							MESSAGE_OPENAP_FAILED					  =0X00031,
							MESSAGE_OPENAP_USERMANNUAL				  =0x00032;
							
							
	private  FileTransferDialog sendingFileDialog;
	private AlertDialog sendFileRequestDialog,dialog_openap_mannual;
	Thread netTcpFileSendThread;
	public static boolean isSendFileSuccess=true;
	List<Map<String,Object>> iplist=new ArrayList<Map<String,Object>>();
	private boolean ifcancallclose=true;
	private long totalBytes=10;
	Thread searching_wait=null;
	
	FileTransferNotification fnotification;
	NotificationManagement nm;
	
	private static Handler handler=new Handler(){
		
		public void handleMessage(Message msg) {
			switch(msg.what){
			default:if(queue.size() > 0)
				queue.getLast().processMessage(msg);
			//Log.e("Activity_send", "接受到信息并刷新列表");
			break;
			}
		}
	};	
	
	private View.OnClickListener listener_refresh = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub				
			netThread.refreshDevices();
			Activity_Send.this.refreshList();
		}
	};
	
	
	protected void onCreate(Bundle myBundle){
		super.onCreate(myBundle);
		this.getActionBar().setIcon(android.R.drawable.ic_menu_upload);
		this.getActionBar().setDisplayShowHomeEnabled(true);
		this.getActionBar().setDisplayHomeAsUpEnabled(true);
		if(Activity_Receive.queue.size()>0){
			new AlertDialog.Builder(this)
			.setTitle(this.getResources().getString(R.string.dialog_send_receivealreadyrun_title))
			.setMessage(this.getResources().getString(R.string.dialog_send_receivealreadyrun_message))
			.setIcon(R.drawable.icon_alertdialog_warn)
			.setCancelable(false)
			.setPositiveButton(this.getResources().getString(R.string.button_turnto),new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					Intent i = new Intent();
					i.setClass(Activity_Send.this, Activity_Receive.class);
					Activity_Send.this.startActivity(i);
					Activity_Send.this.finish();
				}
			})
			.setNegativeButton(this.getResources().getString(R.string.button_negative_cancel), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					Activity_Send.this.finish();
				}
			})
			.show();
		}
		else{
			context_send=this;
			if(!queue.contains(this)){
				queue.add(this);
			}
			
			this.nm=new NotificationManagement(this);
			this.nm.notification.setSmallIcon(R.drawable.icon_upload);
			this.nm.notification.setContentTitle("");
			this.nm.notification.setContentText("");
			this.nm.setDefaults(NotificationManagement.DEFAULT_ALL);
			
			nm.setTargetActivity(Activity_Send.class);						
			
			if(Build.VERSION.SDK_INT>=21){
				nm.setFullSreenIntent(Activity_Send.class);
			}
									
			shareIntent=this.getIntent();
			String action = shareIntent.getAction();
			if(action==null){
				action="";
			}
			
		  //  String type = shareIntent.getType(); 
		    
			if(action.equals(Intent.ACTION_SEND)||action.equals(Intent.ACTION_SEND_MULTIPLE)){
				wm = new WifiManagement(this);
				if(!wm.isWifiConnected()){    //当前没有连接wifi，打开热点来继续操作
					if(wm.isWifiEnabled()){
						this.ifneedRestartWifi=true;
					}
					
					AlertDialog ad_openap = new AlertDialog.Builder(this)
							.setCancelable(false)
							.setIcon(R.drawable.icon_apmode_small)
							.setTitle(Activity_Send.this.getResources().getString(R.string.dialog_openap_title))
							.setMessage(Activity_Send.this.getResources().getString(R.string.dialog_openap_message))
							.setPositiveButton(Activity_Send.this.getResources().getString(R.string.button_possitive_confirm), new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub	
									Activity_Send.isAPEnabled=true;
									Activity_Send.this.setview();
									
									new Thread(new Runnable(){
										@Override
										public void run() {
											// TODO Auto-generated method stub
											//try{
											
											if(Build.VERSION.SDK_INT<=23){
												if(!Activity_Send.this.openAP()){
													Activity_Send.sendEmptyMessage(Activity_Send.MESSAGE_OPENAP_FAILED);
												}
											}
											else{
												if(!Activity_Send.this.initializeAP()){
													Activity_Send.sendEmptyMessage(Activity_Send.MESSAGE_OPENAP_FAILED);
												}
												else{
													Activity_Send.sendEmptyMessage(Activity_Send.MESSAGE_OPENAP_USERMANNUAL);
												}
											}
												
											//	Thread.sleep(5000);
											//	Activity_Send.this.startNet();
											//	Activity_Send.sendEmptyMessage(WAIT_AP_COMPLETE);
											//}catch(Exception e){
												
										//	}
										}
										
									}).start();
								}
							}).setNegativeButton(Activity_Send.this.getResources().getString(R.string.button_negative_cancel), new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub
									Activity_Send.this.finish();
								}
							}).create();
					
					ad_openap.show();
					
				}
				
				else{ //当前已连接wifi，提示用户接受者也连接至同一局域网。
					isAPEnabled=false;
					AlertDialog ad_wifi= new AlertDialog.Builder(Activity_Send.this)
							.setIcon(R.drawable.icon_wifilogo_small)
							.setTitle(Activity_Send.this.getResources().getString(R.string.dialog_confirmnet_title))
							.setMessage(Activity_Send.this.getResources().getString(R.string.dialog_confirmnet_message))
							.setPositiveButton("使用当前WiFi", new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub
									Activity_Send.this.setview();
									Activity_Send.this.startNet();
								}
							})
							.setNegativeButton(Activity_Send.this.getResources().getString(R.string.button_openap), new DialogInterface.OnClickListener() {
								
								@Override
								public void onClick(DialogInterface dialog, int which) {
									// TODO Auto-generated method stub
									Activity_Send.this.ifneedRestartWifi=true;
									Activity_Send.isAPEnabled=true;
									setview();
									new Thread(new Runnable(){

										@Override
										public void run() {
											// TODO Auto-generated method stub
										//	try{
											if(Build.VERSION.SDK_INT<=23){
												if(!Activity_Send.this.openAP()){
													Activity_Send.sendEmptyMessage(Activity_Send.MESSAGE_OPENAP_FAILED);
												}
											}
											else{
												if(!Activity_Send.this.initializeAP()){
													Activity_Send.sendEmptyMessage(Activity_Send.MESSAGE_OPENAP_FAILED);
												}
												else{
													Activity_Send.sendEmptyMessage(Activity_Send.MESSAGE_OPENAP_USERMANNUAL);
												}
											}
												
											//	Thread.sleep(5000);
											//	Activity_Send.this.startNet();
											//	Activity_Send.sendEmptyMessage(WAIT_AP_COMPLETE);
										//	}catch(Exception e){
												
										//	}
										}
										
									}).start();
								}
							})
							.setCancelable(false)
							.create();
					ad_wifi.show();
					
					
				}
				//startNet();
				if(action.equals(Intent.ACTION_SEND)){
					Uri uri = (Uri) shareIntent.getParcelableExtra(Intent.EXTRA_STREAM);
					if(uri==null){
						Activity_Send.sendEmptyMessage(MESSAGE_GETPATH_ERROR);
					}
					else{
						setfilePaths(uri);
					}
					
					
				}
				else{
					if(action.equals(Intent.ACTION_SEND_MULTIPLE)){
						ArrayList<Uri> uris=shareIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
						if(uris==null){
							Activity_Send.sendEmptyMessage(MESSAGE_GETPATH_ERROR);
						}
						else{
							setfilePaths(uris);
						}
						
					}
					else{
						new AlertDialog.Builder(Activity_Send.this)
						.setTitle(this.getResources().getString(R.string.dialog_send_systemmode_att_title))
						.setIcon(R.drawable.icon_alertdialog_warn)
						.setMessage(this.getResources().getString(R.string.dialog_send_systemmode_att_message))
						.setCancelable(false)
						.setPositiveButton(this.getResources().getString(R.string.button_possitive_confirm), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								Activity_Send.this.finish();
							}
						})
						.show();
					}
				}
				
			}
			else{
			//  在非ACTION_SEND下打开本Activity，不做处理
				new AlertDialog.Builder(Activity_Send.this)
				.setTitle(this.getResources().getString(R.string.dialog_send_systemmode_att_title))
				.setIcon(R.drawable.icon_alertdialog_warn)
				.setMessage(this.getResources().getString(R.string.dialog_send_systemmode_att_message))
				.setCancelable(false)
				.setPositiveButton(this.getResources().getString(R.string.button_possitive_confirm), new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						Activity_Send.this.finish();
					}
				})
				.show();
				
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
	
	protected void setview(){
		this.setContentView(R.layout.layout_send);
		modeins=(ImageView)findViewById(R.id.img_mode);
		pg_refreshing = (ProgressBar)findViewById(R.id.progressBarDetect);
		tv_onlinenum=(TextView)findViewById(R.id.textatt);
		tv_mode=(TextView)findViewById(R.id.textmode);
		devicelist=(ListView)findViewById(R.id.devicelist);
		refresh=(Button)findViewById(R.id.refreshDevices);
		icon_nofound=(ImageView)findViewById(R.id.icon_nofound);
		tv_nofound=(TextView)findViewById(R.id.text_nofound);
		tv_fileinfo1=(TextView)findViewById(R.id.text_fileinfos1);
		tv_fileinfo2=(TextView)findViewById(R.id.text_fileinfos2);
		tv_onlinenum.setText(this.getResources().getString(R.string.text_online_opening));
		String fileinfo1="";
		if(this.filePaths[0].indexOf("/")!=-1){
			fileinfo1=this.filePaths[0].substring(this.filePaths[0].lastIndexOf("/")+1, this.filePaths[0].length());
		}
		if(this.filePaths.length>1){
			fileinfo1+="等"+this.filePaths.length+"个文件";
		}
		tv_fileinfo2.setText("  查看");
		tv_fileinfo2.setClickable(true);
		tv_fileinfo2.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String fileinfos = "要发送的文件信息如下：\n\n";
				for(int i=0;i<Activity_Send.this.filePaths.length;i++){
					try{
						fileinfos+=Activity_Send.this.filePaths[i].toString()+"  "+Formatter.formatFileSize(Activity_Send.this, FileSize.getFileSizeofBytes(new File(Activity_Send.this.filePaths[i].toString())));
					}catch(Exception e){
						e.printStackTrace();
					}
					fileinfos+="\n\n";
				}
				
				new AlertDialog.Builder(Activity_Send.this)
				.setTitle("查看")
				.setMessage(fileinfos)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						
					}
				})
				.show();
			
			}
		});
		tv_fileinfo1.setText(fileinfo1);
		
		refresh.setOnClickListener(null);	
		
		if(isAPEnabled){
			modeins.setImageResource(R.drawable.icon_apmode);
			modeins.setVisibility(View.VISIBLE);
			tv_mode.setText(this.getResources().getString(R.string.text_send_apmode));
		}
		else{
			modeins.setImageResource(R.drawable.icon_wifimode);
			modeins.setVisibility(View.VISIBLE);
			tv_mode.setText(this.getResources().getString(R.string.text_send_wifimode)+"\n"
					+this.getResources().getString(R.string.text_send_wifimode_ip)+IPUtils.getLocalIpAddress(this));
		}
	}
	
	private void setViewofNull(){
	//	this.setContentView(null);
		
	}
	
	
	protected void startNet(){
		this.isnetConnected=true;
		netThread = new NetSendThread(this.deviceName);
		netThread.connectSocket();	//开始监听数据
	    netThread.noticeOnline();	//广播上线
	    refresh.setOnClickListener(listener_refresh);
	    refreshList();
	    if(!wm.isWifiConnected()&&!isAPEnabled){
	    	ifcancallclose=false;
			closeAllOperationsAndExit();
			setViewofNull();
			this.showConnectivityBrokenNotification();
			new AlertDialog .Builder(this)
			.setTitle(this.getResources().getString(R.string.dialog_send_connection_changed_title))
			.setMessage(this.getResources().getString(R.string.dialog_send_connection_changed_message))
			.setCancelable(false)
			.setPositiveButton(this.getResources().getString(R.string.button_possitive_confirm), new DialogInterface.OnClickListener() {
			
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					Activity_Send.this.finish();
				}
			})
			.show();
	    }
	}
	
	
	protected boolean openAP(){
		APManager am=new APManager(this);
		am.setApEnabled(false);
		am.setSSID("mxr-wlanshare-"+this.apid);
	//	am.setPassword("123456789000");
		return	am.setApEnabled(true);
	}
	
	private boolean initializeAP(){
		APManager am=new APManager(this);		
		am.setSSID("mxr-wlanshare-"+this.apid);
	//	am.setPassword("123456789000");
		return	am.setApEnabled(false);			
	}
	
	private void turntoAPPage(){
	dialog_openap_mannual=	new AlertDialog.Builder(this)
		.setTitle(this.getResources().getString(R.string.dialog_send_openap_mannual_title))
		.setMessage(this.getResources().getString(R.string.dialog_send_openap_mannual_message))
		.setIcon(R.drawable.icon_face_ops)
		.setCancelable(false)
		.setPositiveButton(this.getResources().getString(R.string.button_possitive_confirm), null)
		.setNegativeButton(this.getResources().getString(R.string.button_negative_cancel), null)
		.show();
	
	dialog_openap_mannual.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Intent intent = new Intent();
			ComponentName cm = new ComponentName("com.android.settings",
	                "com.android.settings.TetherSettings");
	        intent.setComponent(cm);
	        intent.setAction("android.intent.action.VIEW");
	        Activity_Send.this.startActivity(intent);
	        Toast.makeText(Activity_Send.this,Activity_Send.this.getResources().getString(R.string.toast_send_openap_mannual) , Toast.LENGTH_LONG).show();
		}
	});
	
	dialog_openap_mannual.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			Activity_Send.this.dialog_openap_mannual.cancel();
			Activity_Send.this.finish();
		}
	});
	}  
	
	protected void closeAP(){
		APManager am=new APManager(this);
	//	am.setSSID("mxr-wlanshare-"+System.currentTimeMillis());
	//	am.setPassword("123456789000");
		am.setApEnabled(false);
	}
	
	private void setfilePaths(ArrayList<Uri> uris){
		this.filePathList=new ArrayList<Uri>();
		this.filePathList=uris;
		this.filePaths=new String[uris.size()];
		
		boolean isGetFilesSuccess=true;
		
		for(int i=0;i<uris.size();i++){
			String realpath=getRealPathFromURI(uris.get(i));
			if(realpath.equals("")||realpath.length()==0){
				isGetFilesSuccess=false;
				Activity_Send.sendEmptyMessage(MESSAGE_GETPATH_ERROR);
			}
			else{
				this.filePaths[i]=realpath;
			}
	            		   
		}
		if(isGetFilesSuccess){
			this.totalBytes=FileSize.getMultiFileSizeofBytes(this.filePaths);
		}
		else{
			this.totalBytes=10;
		}
		
	}
	private void setfilePaths(Uri uri){
		String filePath=getRealPathFromURI(uri);
		boolean isGetFilesSuccess=true;
        if(filePath.equals("")||filePath.length()==0){
        	Activity_Send.sendEmptyMessage(MESSAGE_GETPATH_ERROR);
        }
        else{
        	this.filePaths=new String[1];
    		this.filePaths[0]=filePath;
        }
        
        if(isGetFilesSuccess){
			this.totalBytes=FileSize.getMultiFileSizeofBytes(this.filePaths);
		}
        else{
        	this.totalBytes=10;
        }
       
		
	}
	
	private void refreshList(){
		this.devicelist.setAdapter(null);
		List<Map<String, Object>> listitems = new ArrayList<Map<String, Object>>(); 
		Set<String>set = netThread.getDevices().keySet();
		Object []IPs=new Object[set.size()];
		IPs=set.toArray();
		
	//	final Object [] IPArray=IPs ;
		
	//	while(netThread.getDevices().)
		
		for(int i=0;i<IPs.length;i++){  //用i遍历无效，key是设备的IP，此循环无效
			Map<String, Object> item = new HashMap<String, Object>();
			if(!isAPEnabled){
				if(!IPUtils.getLocalIpAddress(this).trim().toString().equals(netThread.getDevices().get(IPs[i].toString()).getIp())){
					item.put("devicename", netThread.getDevices().get(IPs[i].toString()).getUserName());
					item.put("deviceip", netThread.getDevices().get(IPs[i].toString()).getIp());
				}		
				listitems.add(item);
			}
			else{
				item.put("devicename", netThread.getDevices().get(IPs[i].toString()).getUserName());
				item.put("deviceip", netThread.getDevices().get(IPs[i].toString()).getIp());
				listitems.add(item);
			}
			
		}
		SimpleAdapter listadaptor = new SimpleAdapter(this, listitems,  
                R.layout.item_devicelist, new String[] { "devicename", "deviceip" },  
                new int[] {R.id.devicename,R.id.deviceip}); 
		if(listitems.size()>0){
			devicelist.setAdapter(listadaptor);
		}
		else{
			devicelist.setAdapter(null);
		}
		
		this. iplist = listitems;
		
		if(listitems.size()>0){
			if(searching_wait!=null){
				searching_wait.interrupt();
				searching_wait=null;
			}
			Log.e("Activity_Send", "listitems.size()>0,listitems="+listitems.size());
			devicelist.setVisibility(View.VISIBLE);
			pg_refreshing.setVisibility(View.INVISIBLE);
			icon_nofound.setVisibility(View.INVISIBLE);
			tv_nofound.setVisibility(View.INVISIBLE);
			tv_onlinenum.setText(this.getResources().getString(R.string.text_online_devices)+listitems.size());
		}
		else{
			Log.e("Activity_Send", "listitems.size()<=0,listitems.size()="+listitems.size());
			devicelist.setVisibility(View.VISIBLE);
			pg_refreshing.setVisibility(View.VISIBLE);
			icon_nofound.setVisibility(View.INVISIBLE);
			tv_nofound.setVisibility(View.INVISIBLE);
			tv_onlinenum.setText(this.getResources().getString(R.string.text_online_searching));
			
			if(searching_wait==null){
				searching_wait=new Thread(new Runnable(){

					@Override
					public void run() {
						// TODO Auto-generated method stub
						try{
							Thread.sleep(15*1000);
						}catch(Exception e){
							e.printStackTrace();
						}
						if(iplist.size()<1){
							Log.e("Activity_Send", "开始发送无设备的message,iplist.size()="+iplist.size());
							Activity_Send.sendEmptyMessage(MESSAGE_NO_DEVICE_FOUND);
						}
					}
					
				});
				searching_wait.start();
			}
			
		}
		
		devicelist.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				// TODO Auto-generated method stub
			//	String IP=IPArray[arg2].toString();
				String IP = Activity_Send.this.iplist.get(arg2).get("deviceip").toString();
				sendFileRequest(IP);
			}
		});
		
		
	}
	
	
	
	
	private String getRealPathFromURI(Uri pathUri) {
		String uriPath="";
		if(pathUri!=null){
			uriPath=pathUri.getPath();
			File file =new File(uriPath);
			if(file.exists()&&!file.isDirectory()){
				return uriPath;
			}
			else{			
			/*	Object [] pathsegments=contentUri.getPathSegments().toArray();
				
				String realpath="";
				int cutval=0;
				while(cutval<pathsegments.length){
					for(int p=0;p<pathsegments.length;p++){
						realpath="";
						if(!pathsegments[p].toString().equals("")&&pathsegments[p]!=null){
							realpath+=pathsegments[p].toString();
							if(p<(pathsegments.length-1)){
								realpath+="/";
							}
							
						}
						
						
					}
					pathsegments[cutval]=null;
					cutval++;
					String paththisloop=StorageUtil.getSDPath()+"/"+realpath;
					File filethisloop = new File(paththisloop);
					if(filethisloop.exists()&&!filethisloop.isDirectory()){
						realpath=paththisloop;
						return realpath;					
					}
				}     */
				
				//get the realpath from media store
					

				String[] proj = {MediaStore.MediaColumns.DATA};  
		        Cursor cursor=getContentResolver().query(pathUri, proj, null, null, null);  
		        if(cursor!=null&&cursor.moveToNext()){
		        	String imgpath=cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
		        	File imgfile = new File(imgpath);
		        	if(imgfile.exists()&&!imgfile.isDirectory()){
		        		 return imgpath ;  
		        	}	           
		        }  
		        if(cursor!=null){
		        	cursor.close();
		        }
		        
		        //get the real path by trying to cut off the header and get the path  with the storage util
		        
		        String path_process = uriPath;
		        while(path_process.indexOf("/")!=-1){
		        	String path_cutoff=path_process.substring(path_process.indexOf("/")+1, path_process.length());
		        	path_process=path_cutoff;
		        	File testfile = new File(StorageUtil.getSDPath()+"/"+path_process);	        	
		        	if(testfile.exists()&&!testfile.isDirectory()){
		        		break;
		        	}
		        		        		        	
		        }
		        
		        File new_test_file = new File(StorageUtil.getSDPath()+"/"+path_process);
		        
		        if(new_test_file.exists()&&!new_test_file.isDirectory()){
	        		return StorageUtil.getSDPath()+"/"+path_process;
	        	}
		        	        	        	        
		        return ""; 
		        
			}
		}
		
		
		else{
			return "";
		}				
    
    } 
	
	public void finish(){
		super.finish();
		if(queue.contains(this)){
			queue.remove(this);
		}
		closeAllOperationsAndExit();
				
		if(this.searching_wait!=null){
			this.searching_wait.interrupt();
			this.searching_wait=null;
		}
						
		new Thread(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				try{
					if(Activity_Send.isAPEnabled){
						Activity_Send.this.closeAP();
						Thread.sleep(5000);
					}
					if(Activity_Send.this.ifneedRestartWifi){
						WifiManagement wm = new WifiManagement(Activity_Send.this);
						wm.setWifiEnabled(true);
					}
					
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			
		}).start();
		
		
	}
	
	public  void processMessage(Message msg){
		switch(msg.what){
		case IpMessageConst.IPMSG_ANSENTRY:refreshList();break;
		case IpMessageConst.IPMSG_DEVICE_OFFLINE:refreshList();break;
		case Activity_Send.MESSAGE_REFUSED_SENDING_FILES:
			if(this.sendFileRequestDialog!=null){
				Activity_Send.this.sendFileRequestDialog.cancel();
			}			
			Toast.makeText(this, "对方拒绝了接收文件", Toast.LENGTH_SHORT).show();
			
			if(Activity_Send.this.netTcpFileSendThread!=null){	
				Activity_Send.isSendFileSuccess=false;
				netTcpFileSendThread.interrupt();
				netTcpFileSendThread=null;
				try{
					if(NetTcpFileSendThread.server!=null){
						NetTcpFileSendThread.server.close();
						NetTcpFileSendThread.server=null;
					}
					if(NetTcpFileSendThread.socket!=null){
						NetTcpFileSendThread.socket.close();
						NetTcpFileSendThread.socket=null;
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			
		break;
		case Activity_Send.MESSAGE_START_SENDING_FILES:{
			if(this.sendFileRequestDialog!=null){
				this.sendFileRequestDialog.cancel();
			}
			if(this.sendingFileDialog!=null){
				this.sendingFileDialog.show();
			}
			if(this.fnotification!=null){
				this.fnotification.notification.setOngoing(true);
				this.fnotification.notification.setAutoCancel(false);
				fnotification.notify(1);
			}
			
		}
		break;
		case Activity_Send.MESSAGE_SENDING_CURRENTFILE:{
			String [] info = (String[])msg.obj;
			if(this.sendingFileDialog!=null){
				this.sendingFileDialog.setTitle("正在发送"+info[0]+"/"+info[1]);
				this.sendingFileDialog.setFilename("正在发送："+info[2]);
			}
			
		}
		break;
		case Activity_Send.MESSAGE_REFRESH_FILEDIALOG_SPEED:{
			long values[]=(long[])msg.obj;
			if(this.sendingFileDialog!=null){
				this.sendingFileDialog.setSpeed(values[0]);
			}
						
		}
		break;
		case Activity_Send.MESSAGE_REFRESH_FILEDIALOG_PROGRESS:{
			long values_progress[]=(long[])msg.obj;
			if(this.sendingFileDialog!=null){
				this.sendingFileDialog.setProgress(values_progress[0]);
			}
			
			DecimalFormat dm=new DecimalFormat("#.00");	
			int percent =(int) (Double.valueOf(dm.format((double)values_progress[0]/this.totalBytes))*100);
			if(this.fnotification!=null){
				fnotification.notification.setContentText("已完成"+percent+"%");
				fnotification.setProgress(percent, 1);
			}
			
			
		}
		break;
		case Activity_Send.MESSAGE_SENDING_FILES_COMPLETE:
			this.sendingFileDialog.cancel();
			//Toast.makeText(this, "文件发送完成", Toast.LENGTH_LONG).show();
			if(this.fnotification!=null){
				fnotification.notification.setContentTitle(this.getResources().getString(R.string.notification_send_complete_title));
				fnotification.notification.setProgress(100, 100, false);
				fnotification.notification.setContentText("已完成");
				fnotification.notification.setOngoing(false);
				fnotification.notification.setAutoCancel(true);
				fnotification.notify(1);
			}
			
			if(this.nm!=null&&!this.isatFront){
				this.nm.notification.setContentTitle(this.getResources().getString(R.string.notification_send_complete_title));
				this.nm.notification.setContentText("文件发送完成");
				this.nm.notification.setOngoing(false);
				this.nm.notification.setAutoCancel(true);
				this.nm.notify(1);
				
			}	
			Toast.makeText(this, this.getResources().getString(R.string.notification_send_complete_title), Toast.LENGTH_SHORT).show();
			
		break;
		
		case MESSAGE_SENDINGFILES_INTERRUPT:
			this.sendingFileDialog.cancel();
			//Toast.makeText(this, "对方终止了接收文件", Toast.LENGTH_LONG).show();
			if(this.fnotification!=null){
				fnotification.notification.setContentTitle(this.getResources().getString(R.string.notification_send_stopsending_title));
				fnotification.notification.setContentText("对方停止了接收");
				fnotification.notification.setOngoing(false);
				fnotification.notification.setAutoCancel(true);
				fnotification.notify(1);
			}
			
			if(this.nm!=null&&!this.isatFront){
				nm.notification.setContentTitle(this.getResources().getString(R.string.notification_send_stopsending_title));
				nm.notification.setContentText("对方停止了接收");
				nm.notification.setOngoing(false);
				nm.notification.setAutoCancel(true);
				nm.notify(1);
				
			}
			Toast.makeText(Activity_Send.this, Activity_Send.this.getResources().getString(R.string.notification_send_stopsending_title)+" 对方停止了接收", Toast.LENGTH_SHORT).show();
			
			if(Activity_Send.this.netTcpFileSendThread!=null){	
				Activity_Send.isSendFileSuccess=false;
				netTcpFileSendThread.interrupt();
				netTcpFileSendThread=null;
				try{
					if(NetTcpFileSendThread.server!=null){
						NetTcpFileSendThread.server.close();
						NetTcpFileSendThread.server=null;
					}
					if(NetTcpFileSendThread.socket!=null){
						NetTcpFileSendThread.socket.close();
						NetTcpFileSendThread.socket=null;
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			break;
		case WAIT_AP_COMPLETE:{
			if(this.dialog_openap_mannual!=null){
				this.dialog_openap_mannual.cancel();
			}
			Activity_Send.this.startNet();				
		} break;
		
		case MESSAGE_NO_DEVICE_FOUND:{
			if(searching_wait!=null){
				searching_wait.interrupt();
				searching_wait=null;
			}
			if(this.iplist.size()<1){
				Log.e("Activity_Send", "在线设备为0，开始设置无设备的view");
				pg_refreshing.setVisibility(View.INVISIBLE);
				devicelist.setVisibility(View.INVISIBLE);
				tv_onlinenum.setText(this.getResources().getString(R.string.text_online_searching_complete));
				icon_nofound.setVisibility(View.VISIBLE);
				tv_nofound.setVisibility(View.VISIBLE);
			}
			
		}
		break;	
		case MESSAGE_SENDINGFILES_INTERRUPT_SELF:{
			if(Activity_Send.this.fnotification!=null){
				Activity_Send.this.fnotification.notification.setOngoing(false);
				Activity_Send.this.fnotification.notification.setAutoCancel(true);
				Activity_Send.this.fnotification.notification.setContentTitle(this.getResources().getString(R.string.notification_send_stopsending_title));
				Activity_Send.this.fnotification.notification.setContentText("已停止发送");
				Activity_Send.this.fnotification.notify(1);
			}
		}
		break;
		case MESSAGE_OPENAP_FAILED:{
			new AlertDialog.Builder(this)
			.setTitle(this.getResources().getString(R.string.dialog_send_openap_error_title))
			.setMessage(R.string.dialog_send_openap_error_message)
			.setCancelable(false)
			.setIcon(R.drawable.icon_alertdialog_warn)
			.setPositiveButton(this.getResources().getString(R.string.button_retry), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					new Thread(new Runnable(){

						@Override
						public void run() {
							// TODO Auto-generated method stub
							if(!Activity_Send.this.openAP()){
								Activity_Send.sendEmptyMessage(Activity_Send.MESSAGE_OPENAP_FAILED);
							}
						}
						
					}).start();
				}
			})
			.setNegativeButton(this.getResources().getString(R.string.button_negative_cancel), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					Activity_Send.this.finish();
				}
			})
			.show();
		}

		break;
		case MESSAGE_OPENAP_USERMANNUAL:{
			turntoAPPage();
		}
		break;
		
		case MESSAGE_GETPATH_ERROR:{
			Activity_Send.this.closeAllOperationsAndExit();
			
			new AlertDialog.Builder(this)
			.setTitle(this.getResources().getString(R.string.dialog_send_urierror_title))
			.setIcon(R.drawable.icon_alertdialog_warn)
			.setCancelable(false)
			.setMessage(this.getResources().getString(R.string.dialog_send_urierror_message))
			.setPositiveButton(this.getResources().getString(R.string.button_possitive_confirm), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					
					Activity_Send.this.finish();
				}
			})
			.show();
		}
		break;
		default:break;
		}
	}
	
	private void showConnectivityBrokenNotification(){
		this.nm=new NotificationManagement(this);
		this.nm.notification.setSmallIcon(R.drawable.icon_alertdialog_warn);
		this.nm.notification.setContentTitle(this.getResources().getString(R.string.notification_connectivity_broken));
		this.nm.notification.setContentText("网络连接断开，收发终止");
		this.nm.setTargetActivity(Activity_Send.class);
		this.nm.notification.setOngoing(false);
		this.nm.notification.setAutoCancel(true);
		this.nm.notify(1);
	}
	
	public void onAPStateChanged(int state){
		switch(state){
		default:break;
		case Activity_Send.AP_DISABLING:{
				if(this.isnetConnected&&isAPEnabled&&queue.size()>0){
					closeAllOperationsAndExit();
					setViewofNull();
					showConnectivityBrokenNotification();
					new AlertDialog .Builder(this)
					.setTitle(this.getResources().getString(R.string.dialog_send_apclosed_title))
					.setMessage(this.getResources().getString(R.string.dialog_send_apclosed_message))
					.setCancelable(false)
					.setPositiveButton(this.getResources().getString(R.string.button_possitive_confirm), new DialogInterface.OnClickListener() {
				
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							Activity_Send.this.finish();
						}
					})
					.show();
				}		
			}  break;
		case Activity_Send.AP_ENABLED:{
			if(!this.isnetConnected&&queue.size()>0&&isAPEnabled){
				try{
					if(this.wm!=null){
						wm.setWifiEnabled(false);
					}
					Thread.sleep(1000);
					Activity_Send.sendEmptyMessage(WAIT_AP_COMPLETE);
				}catch(Exception e){
					e.printStackTrace();
				}				
			}
		}
		break;
			
		}
	}
	
	public void onWifiStateChanged(int state){
		if(state==NET_CONNECTIVITY_CHANGED){
			
			if(ifcancallclose&&!isAPEnabled&&this.isnetConnected&&!wm.isWifiConnected()){
				ifcancallclose=false;
				closeAllOperationsAndExit();
				setViewofNull();
				showConnectivityBrokenNotification();
				if(queue.size()>0){
					new AlertDialog .Builder(this)
						.setTitle(this.getResources().getString(R.string.dialog_send_connection_changed_title))
						.setMessage(this.getResources().getString(R.string.dialog_send_connection_changed_message))
						.setCancelable(false)
						.setPositiveButton(this.getResources().getString(R.string.button_possitive_confirm), new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								Activity_Send.this.finish();
							}
						})
						.show();					
				}
			}
			
		}
	}
	
	private void closeAllOperationsAndExit(){
		if(this.isnetConnected){
			if(this.sendFileRequestDialog!=null){
				this.sendFileRequestDialog.cancel();
			}
			if(this.sendingFileDialog!=null){
				this.sendingFileDialog.cancel();
			}
		
			try{
				
				if(netThread!=null){
					netThread.disconnectSocket();
				}
				
				//if(NetSendThread.udpSocket!=null){
				//	NetSendThread.udpSocket.close();
				//	NetSendThread.udpSocket=null;
			//	}
				
				
				if(this.netTcpFileSendThread!=null){
					this.netTcpFileSendThread.interrupt();
					this.netTcpFileSendThread=null;
				}
				if(NetTcpFileSendThread.socket != null){						
					NetTcpFileSendThread.socket.close();					
					NetTcpFileSendThread.socket = null;
				}
				if(NetTcpFileSendThread.server != null){					
					NetTcpFileSendThread.server.close();
					NetTcpFileSendThread.server = null;
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		
			
		}
	}
	
	public static void sendEmptyMessage(int what) {
		handler.sendEmptyMessage(what);
	}
	
	public static void sendMessage(Message msg) {
		handler.sendMessage(msg);
	}
	
	protected void sendFileRequest(final String receiverIP){
		Activity_Send.isSendFileSuccess=true;
		String[] filePathArray = this.filePaths;
		sendFileRequestDialog= new AlertDialog.Builder(this).setIcon(R.drawable.icon_files_small).setTitle(this.getResources().getString(R.string.dialog_sendfilerequest_title))
				.setMessage(this.getResources().getString(R.string.dialog_sendfilerequest_message1)+receiverIP+this.getResources().getString(R.string.dialog_sendfilerequest_message2))
				.setNegativeButton(this.getResources().getString(R.string.button_negative_cancel), new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
					//	NetSendThread.sendEmptyMessage(NetSendThread.MESSAGE_SEND_REFUSE_INFO);
						senupUDP(IpMessageConst.IPMSG_SENDPORT_INTERRUPT,receiverIP);
						if(Activity_Send.this.netTcpFileSendThread!=null){	
							Activity_Send.isSendFileSuccess=false;
							netTcpFileSendThread.interrupt();
							netTcpFileSendThread=null;
							try{
								if(NetTcpFileSendThread.server!=null){
									NetTcpFileSendThread.server.close();
									NetTcpFileSendThread.server=null;
								}
								if(NetTcpFileSendThread.socket!=null){
									NetTcpFileSendThread.socket.close();
									NetTcpFileSendThread.socket=null;
								}
							}catch(Exception e){
								e.printStackTrace();
							}
						}
					}
				}).create();
		sendFileRequestDialog.setCancelable(false);
		sendFileRequestDialog.show();
		//set dialog
		sendingFileDialog=new FileTransferDialog(this);
		sendingFileDialog.setIcon(R.drawable.icon_files_small);
		sendingFileDialog.setTitle(this.getResources().getString(R.string.dialog_sendingfile_title));
		sendingFileDialog.setMax(FileSize.getMultiFileSizeofBytes(this.filePaths));
		sendingFileDialog.setCancelable(false);
		sendingFileDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "停止", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				if(Activity_Send.this.netTcpFileSendThread!=null){
					Activity_Send.isSendFileSuccess=false;
					netTcpFileSendThread.interrupt();
					netTcpFileSendThread=null;
					senupUDP(IpMessageConst.IPMSG_INTERRUPT_FILETRANSFER,receiverIP);
					try{
						if(NetTcpFileSendThread.server!=null){
							NetTcpFileSendThread.server.close();
							NetTcpFileSendThread.server=null;
						}
						if(NetTcpFileSendThread.socket!=null){
							NetTcpFileSendThread.socket.close();
							NetTcpFileSendThread.socket=null;
						}
					}catch(Exception e){
						e.printStackTrace();
					}
				}
				Activity_Send.sendEmptyMessage(MESSAGE_SENDINGFILES_INTERRUPT_SELF);
				
			}
		});
		
		//set notification
		fnotification = new FileTransferNotification(this);
		fnotification.notification.setSmallIcon(R.drawable.icon_upload);
		fnotification.notification.setContentTitle(this.getResources().getString(R.string.notification_send_sending_title));
		fnotification.notification.setContentText("已完成：");	
		fnotification.notification.setOngoing(true);
		Intent intent = new Intent();
		intent.setClass(Activity_Send.this, Activity_Send.class);
		PendingIntent pi = PendingIntent.getActivity(Activity_Send.this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		fnotification.notification.setContentIntent(pi);
		
		//发送传送文件UDP数据报
		IpMessageProtocol sendPro = new IpMessageProtocol();
		sendPro.setVersion("" +IpMessageConst.VERSION);
		sendPro.setCommandNo(IpMessageConst.IPMSG_SENDMSG | IpMessageConst.IPMSG_FILEATTACHOPT);
		sendPro.setSenderName(this.deviceName);
		sendPro.setSenderHost("");
		String msgStr = "";	//发送的消息
		
		StringBuffer additionInfoSb = new StringBuffer();	//用于组合附加文件格式的sb
		for(String path:filePathArray){
			File file = new File(path);
			additionInfoSb.append("0:");
			additionInfoSb.append(file.getName() + ":");
			additionInfoSb.append(Long.toHexString(file.length()) + ":");		//文件大小十六进制表示
			additionInfoSb.append("" + ":");	//文件创建时间，现在暂时已最后修改时间替代  Long.toHexString(file.lastModified())
			additionInfoSb.append(IpMessageConst.IPMSG_FILE_REGULAR + ":");
			byte[] bt = {0x07};		//用于分隔多个发送文件的字符
			String splitStr = new String(bt);
			additionInfoSb.append(splitStr);
		}
		
		sendPro.setAdditionalSection(msgStr + "\0" + additionInfoSb.toString() + "\0");
		
		InetAddress sendto = null;
		try {
			sendto = InetAddress.getByName(receiverIP);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			Log.e("Activity_Send", "发送地址有误");
		}
		if(sendPro.getProtocolString().length()>=65500){
			//error,send bytes too long
			new AlertDialog.Builder(this)
			.setTitle("Error")
			.setMessage("Error:send buffer too long\n请尝试减少要发送的文件的数量，然后重试。")
			.setPositiveButton("确定", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					
				}
			})
			.show();
			
		}
		else{
			if(sendto != null){
				netThread.sendUdpData(sendPro.getProtocolString(), sendto, IpMessageConst.PORT);
			}
				
			
			//监听2425端口，准备接受TCP连接请求
			netTcpFileSendThread = new Thread(new NetTcpFileSendThread(filePathArray));
			netTcpFileSendThread.start();	//启动线程
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
