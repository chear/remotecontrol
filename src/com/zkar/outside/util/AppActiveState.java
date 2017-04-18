package com.zkar.outside.util;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;

public class AppActiveState {
	
	
	/**
     * 
     *  @Description    : �������ĳ����Ƿ�������
     *  @Method_Name    : isRunningApp
     *  @param context ������
     *  @param packageName �жϳ���İ���
     *  @return ������ص�Ȩ��
     *      <uses-permission android:name="android.permission.GET_TASKS"> 
     *  @return         : boolean
     */
	 public static boolean isRunningApp(Context context, String packageName) {
	        boolean isAppRunning = false;
	        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
	        List<RunningTaskInfo> list = am.getRunningTasks(100);
	        for (RunningTaskInfo info : list) {
	            if (info.topActivity.getPackageName().equals(packageName) && info.baseActivity.getPackageName().equals(packageName)) {
	                isAppRunning = true;
	                // find it, break
	                break;
	            }
	        }
	        return isAppRunning;
	    }
	 
}
