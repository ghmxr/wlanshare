package app.mxr.wlanshare.utils;

import java.io.File;

import android.os.Environment;
import android.os.StatFs;

public class StorageUtil {
	
	public static long getSDAvaliableSize(){
		   File path = Environment.getExternalStorageDirectory();  
	       StatFs stat = new StatFs(path.getPath());  
	       long blockSize = stat.getBlockSize();  
	       long availableBlocks = stat.getAvailableBlocks();   
	       return  blockSize * availableBlocks;
	}
	
	
	public static String getSDPath(){ 
		try{
			return Environment.getExternalStorageDirectory().getAbsolutePath();
		}catch(Exception e){
			e.printStackTrace();
		}
		return "";
	}
	
	

}
