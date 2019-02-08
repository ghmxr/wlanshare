package app.mxr.wlanshare.ui;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class NotificationManagement {
	public Notification.Builder notification;
	public NotificationManager manager;
	public static final int DEFAULT_ALL=Notification.DEFAULT_ALL;
	private Context context;
	private PendingIntent pi;
	
	public NotificationManagement(Context context){
		this.context=context;
		manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		notification=new Notification.Builder(context).setAutoCancel(true).setOngoing(false);		
	}
	
	public void setDefaults(int value){
		this.notification.setDefaults(value);
	}
	
	public void setTargetActivity(Class<?> cls){
		Intent intent = new Intent();
		intent.setClass(this.context, cls);
		PendingIntent pi = PendingIntent.getActivity(this.context, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		this.pi=pi;
		this.notification.setContentIntent(pi);	
	}
	
	@SuppressLint("NewApi")
	public void setFullSreenIntent(Class<?>cls){				
		if(Build.VERSION.SDK_INT>=21){
			this.notification.setPriority(Notification.PRIORITY_DEFAULT);
			this.notification.setVisibility(Notification.VISIBILITY_PUBLIC);
			this.notification.setFullScreenIntent(this.pi, true);
		}								
	}
	
	public void notify(int id){
		manager.notify(id,notification.build());
	}

}
