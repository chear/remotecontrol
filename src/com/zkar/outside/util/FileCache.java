package com.zkar.outside.util;

import java.io.File;

import android.content.Context;

public class FileCache {
	private static FileCache fileCache;
	private static File cacheDir;  
	  
    public static FileCache getInstance(Context context,String fileDirectory) {
    	if(fileCache==null){
    		fileCache = new FileCache();
    	}
        // 找到保存缓存的图片目录  
        if (android.os.Environment.getExternalStorageState().equals(  
                android.os.Environment.MEDIA_MOUNTED)) {
        	cacheDir = new File(  
        			android.os.Environment.getExternalStorageDirectory(),  
        			fileDirectory);  
        } else {
        	cacheDir = context.getCacheDir();  
        } 
        if (!cacheDir.exists())  
            cacheDir.mkdirs();  
//        System.out.println("cacheDir.changdu :"+cacheDir.getFreeSpace()/1024/1024);
//        if(cacheDir.listFiles().length>20){
////        	Toast.makeText(context, "正在清除缓存请稍后..", 0).show();
//        	System.out.println("正在清除缓存请稍后..");
//        	clear();
//        }
        return fileCache;
    }  
  
    public File getFile(String url) {  
        String filename = String.valueOf(url.hashCode());  
        File f = new File(cacheDir, filename);  
        return f;  
    }  
  
    public void clear() {  
        File[] files = cacheDir.listFiles();  
        for (File f : files){
//        	System.out.println("f :"+f.getName());
        	f.delete();  
        }  
    }  
}
