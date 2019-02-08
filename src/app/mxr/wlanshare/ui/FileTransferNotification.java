package app.mxr.wlanshare.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;


public class FileTransferNotification {
	
	public Notification.Builder notification;
	public NotificationManager manager;
	
	int progress=0;
	
	public FileTransferNotification(Context context){
		this.progress=0;
		manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		this.notification = new Notification.Builder(context).setProgress(100, this.progress, false);
	}
	
	public void setProgress(int progress,int id){
		if(progress!=this.progress){
			this.progress=progress;
			notification.setProgress(100, this.progress, false);
			notify(id);
		}
		
	}
	
	
	public void notify(int id){		
		manager.notify(id, notification.build());
	}

}
