package app.mxr.wlanshare.utils;

import java.io.File;
import java.io.FileInputStream;

import android.util.Log;

public class FileSize {
	
	public static long getFileSizeofBytes(File file) throws Exception{
		
		if(file!=null){
			long size = 0;
			if (file.exists()){
				FileInputStream fis = null;
				fis = new FileInputStream(file);
				size = fis.available();
				fis.close();
			}
			else{
				//file.createNewFile();
				Log.e("获取文件大小","文件不存在!");
			}
			return size;
		}
		
		else{
			return 0;
		}
		
		
	}
	
	public static long getMultiFileSizeofBytes(String [] paths){
		
		if(paths!=null){
			long total=0;
			for(int i=0;i<paths.length;i++){
				try{
					File file = new File(paths[i]);
					total+=getFileSizeofBytes(file);
				}catch(Exception e){
					e.printStackTrace();
				}		
			}
			return total;
		}
		else{
			return 0;
		}
		
	}
}
