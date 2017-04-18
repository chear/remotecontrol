package com.zkar.pis.remotecontrol;

import com.zkar.monitoringfacilities.DeviceUuidFactory;
import com.zkar.outside.util.XmlUtils;

import android.app.Application;

public class MyApplication extends Application {
	private String localIP;
	private String machineCode;
	private String serviceurl;

	/*public void init() {
		// ���ø�CrashHandlerΪ�����Ĭ�ϴ�����
		System.out.println("PIS---application");
		CrashHandlerUtils chu = CrashHandlerUtils.getInstance();
		chu.init(getApplicationContext(), CrashHandlerUtils.MSG_FILE);
	}*/
	
	@Override
	public void onCreate() {
		super.onCreate();
		if(machineCode==null || machineCode==""){
			machineCode = DeviceUuidFactory.getInstance(this).getDeviceUuid();
		}
		if(serviceurl==null || serviceurl==""){
			try{
				String xml = XmlUtils.ReadFile("/sdcard/plugins/service.dat");
				serviceurl = XmlUtils.ReadValue(xml, "dataservice").trim();
			}catch (Exception e) {
				e.printStackTrace();
				serviceurl = "";
			}
		}
	}

	public String getLocalIP() {
		return localIP;
	}

	public void setLocalIP(String localIP) {
		this.localIP = localIP;
	}

	public String getMachineCode() {
		return machineCode;
	}

	public void setMachineCode(String machineCode) {
		this.machineCode = machineCode;
	}

	public String getServiceurl() {
		return serviceurl;
	}

	public void setServiceurl(String serviceurl) {
		this.serviceurl = serviceurl;
	}
}
