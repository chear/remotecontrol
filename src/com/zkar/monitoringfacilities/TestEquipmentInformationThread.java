package com.zkar.monitoringfacilities;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;

import com.zkar.outside.util.DetectionEquipmentUtils;
import com.zkar.outside.util.HttpUtils;
import com.zkar.pis.remotecontrol.MyApplication;

public class TestEquipmentInformationThread extends Thread {
	private Context myContext;
	
	public TestEquipmentInformationThread(Context myContext){
		this.myContext = myContext;
	}

	@Override
	public void run() {
		super.run();
		int count = 0;
		MyApplication myapp = (MyApplication) myContext.getApplicationContext();
		String machineCode = myapp.getMachineCode();
		String localIP = myapp.getLocalIP();
		String serviceurl = myapp.getServiceurl();
		while (true) {
			if (count == 8) {//16秒钟
				count = 0;
				//总的cpu使用率
				String cpu = DetectionEquipmentUtils.getInstance().getTotalProcessCpuRate();
				//一秒内的上传和下载速度
				String[] sx = DetectionEquipmentUtils.getInstance().getUploadDownloadSpeed(1000);
				//获取可用运存大小剩余内存
				long availram=DetectionEquipmentUtils.getInstance().getAvailMemory(myContext);
				//获取总运存大小
				long allram = DetectionEquipmentUtils.getInstance().getTotalMemory(myContext);
				//已用运存
				long usedram = allram - availram;
				List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
				urlParameters.add(new BasicNameValuePair("key", machineCode));
				urlParameters.add(new BasicNameValuePair("ip",localIP));
				urlParameters.add(new BasicNameValuePair("cpu",cpu));//cpu使用率
				urlParameters.add(new BasicNameValuePair("usedMemory",usedram+""));//已用运存
				urlParameters.add(new BasicNameValuePair("totalMemory",allram+""));//总的运存
				urlParameters.add(new BasicNameValuePair("uploadSpeed",sx[0]));//上传速度
				urlParameters.add(new BasicNameValuePair("downloadSpeed",sx[1]));//下载速度
				urlParameters.add(new BasicNameValuePair("isAppOk",""));
				urlParameters.add(new BasicNameValuePair("appVersion",""));//当前程序版本
				HttpUtils.doPost(serviceurl+ "/Device/Info", urlParameters);
			}
			try {
				sleep(1000*2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			count++;
		}
	}
}
