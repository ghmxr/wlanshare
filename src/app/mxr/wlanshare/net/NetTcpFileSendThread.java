package app.mxr.wlanshare.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;

import android.os.Message;
import android.util.Log;
import app.mxr.wlanshare.activities.Activity_Send;

/**
 * Tcp发送文件线程
 */
public class NetTcpFileSendThread implements Runnable{
	private final static String TAG = "NetTcpFileSendThread";
	private String[] filePathArray;	//保存发送文件路径的数组
	
	public static  ServerSocket server;	
	public static Socket socket;	
	private byte[] readBuffer = new byte[1024];
	BufferedOutputStream bos;
	BufferedInputStream bis,fbis;
	
	
	public NetTcpFileSendThread(String[] filePathArray){
		this.filePathArray = filePathArray;
		Activity_Send.isSendFileSuccess=true;
		try {
			server = new ServerSocket(IpMessageConst.PORT);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG, "监听tcp端口失败");
		}
	}
	

	@Override
	public void run() {
		// TODO Auto-generated method stub
		long writesum_bytes=0;
		for(int i = 0; i < filePathArray.length; i ++){
			try {
				socket = server.accept();
				Log.i(TAG, "与IP为" + socket.getInetAddress().getHostAddress() + "的用户建立TCP连接");	
			//	Activity_Send.sendEmptyMessage(Activity_Send.MESSAGE_START_SENDING_FILES);
				this.bos = new BufferedOutputStream(socket.getOutputStream());
				
				this.bis = new BufferedInputStream(socket.getInputStream());
				
				int mlen = bis.read(readBuffer);
				//String ipmsgStr = new String(readBuffer,0,mlen,"gbk");
				String ipmsgStr = new String(readBuffer,0,mlen);
				
				Log.d(TAG, "收到的TCP数据信息内容是：" + ipmsgStr);
				
				IpMessageProtocol ipmsgPro = new IpMessageProtocol(ipmsgStr);
				String fileNoStr = ipmsgPro.getAdditionalSection();
				String[] fileNoArray = fileNoStr.split(":");
				int sendFileNo = Integer.valueOf(fileNoArray[1]);
				
				Log.d(TAG, "本次发送的文件具体路径为" + filePathArray[sendFileNo]);
				Message filename = new Message();
				String[] arrange=new String[3];
				int arr=i+1;
				arrange[0]=""+arr;
				arrange[1] = ""+this.filePathArray.length;
				arrange[2] = this.filePathArray[i];
				filename.what=Activity_Send.MESSAGE_SENDING_CURRENTFILE;
				filename.obj = arrange;
				Activity_Send.sendMessage(filename);
				File sendFile = new File(filePathArray[sendFileNo]);	//要发送的文件
				this.fbis = new BufferedInputStream(new FileInputStream(sendFile));
				Log.i(TAG, "准备开始发送数据");
				int rlen = 0;
				long writesum=0;
				long writesum_bytes_thisloop=0;
				long temp_Bytes_thisloop=0;
				long     writeBytesperSecond=0;
				long 	startTime=System.currentTimeMillis();
				while((rlen = fbis.read(readBuffer)) != -1){
				//	int temp_all_files_KBytes=writesum_k_thisloop;
					bos.write(readBuffer, 0, rlen);					
					writesum+=rlen;
					writeBytesperSecond+=rlen;
					writesum_bytes_thisloop=writesum;					
					if(writesum_bytes_thisloop>(temp_Bytes_thisloop+100*1024)){  //每写100K发送一次更新的message
						temp_Bytes_thisloop=writesum_bytes_thisloop;
						Message message_progress = new Message();
						long[] values_progress = new long[1];
						values_progress[0]= writesum_bytes+writesum_bytes_thisloop;//write_all_files_KBytes;
						message_progress.what= Activity_Send.MESSAGE_REFRESH_FILEDIALOG_PROGRESS;
						message_progress.obj = values_progress;
					//	Activity_Send.sendingFileDialog.setProgress(writesum_k_thisloop+writesum_k);
						Activity_Send.sendMessage(message_progress);
					}
					long endTime=System.currentTimeMillis();
					if((endTime-startTime)>1000){
						startTime=System.currentTimeMillis();
						long speedofBytes=writeBytesperSecond;
						writeBytesperSecond=0;
						Log.i("Speed....", "发送速度"+speedofBytes+"Bytes/秒");
						Message msg = new Message();
						msg.what=Activity_Send.MESSAGE_REFRESH_FILEDIALOG_SPEED;
						long values [] = new long[1];
						values[0]=speedofBytes;
						msg.obj= values;
						Activity_Send.sendMessage(msg);
					//	Activity_Send.sendingFileDialog.setSpeed(speedofKBytes);
					}
					bos.flush();
				}
				writesum_bytes+=writesum_bytes_thisloop;
				bos.flush();
				Log.i(TAG, "文件发送成功");				
				
				
				if(i == (filePathArray.length -1)){
						//文件发送成功
				}
				
			}catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				//Activity_Send.isSendFileSuccess=false;
				e.printStackTrace();
				Log.e(TAG, "接收数据时，系统不支持GBK编码");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Activity_Send.isSendFileSuccess=false;
				e.printStackTrace();
				Log.e(TAG, "发生IO错误");
				break;
			} finally{
				
				try{
					if(bis != null){
						bis.close();
						bis = null;
					}
					
					if(fbis != null){
						fbis.close();
						fbis = null;
					}
					
					if(bos != null){
						bos.close();
						bos = null;
					}
				}catch(IOException ioe){
					ioe.printStackTrace();
				}
				
				if(socket != null){
					try {
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					socket = null;
				}
												
			}
			
		}
		
		if(server != null){
			try {
				server.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			server = null;
		}
		
		if(Activity_Send.isSendFileSuccess){
			Activity_Send.sendEmptyMessage(Activity_Send.MESSAGE_SENDING_FILES_COMPLETE);
		}
		
	}

}
