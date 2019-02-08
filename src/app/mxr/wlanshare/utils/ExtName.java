package app.mxr.wlanshare.utils;

import java.io.File;

public class ExtName {
	
	/**
	 * 获取一个文件的扩展名
	 * 例如 abc.png  返回 png
	 * @param   文件路径
	 * @return  String  文件的扩展名
	 */
	
	public static String  getFileExtName(String filepath) {
        File file = new File(filepath);
        String fileName = file.getName();
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
     //   System.out.println(suffix);
        return suffix;
    }
	
	
	/**
	 * 获取一个文件的扩展名
	 * 例如 abc.png  返回 png
	 * @param File file  文件
	 * @return  String  文件的扩展名
	 */
	public static String  getFileExtName(File file){
		return file.getName().substring(file.getName().lastIndexOf(".")+1);
	}

}
