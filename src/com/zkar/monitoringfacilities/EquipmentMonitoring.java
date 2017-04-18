package com.zkar.monitoringfacilities;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Formatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.net.TrafficStats;
import android.util.Log;

//设备检测
public class EquipmentMonitoring {

	// 系统CPU文件
	public final String CPU_FILE = "/proc/stat";

	private Context context;

	public EquipmentMonitoring(Context context) throws IOException {
		// TODO 自动生成的构造函数存根
		this.context = context;
	}

	private String pingNet(String serviceurl) {
		float l = 0;
		String servicerulsub = serviceurl.substring(
				serviceurl.lastIndexOf("/") + 1, serviceurl.length());
		// StringBuilder stringBuilder = new StringBuilder();
		Runtime run = Runtime.getRuntime();
		String str = "/system/bin/ping -c 1 -w 5 " + servicerulsub;
		try { 
			Process proc = run.exec(str);
			BufferedReader buf = new BufferedReader(new InputStreamReader(
					proc.getInputStream()));
			String string = new String();
			while ((string = buf.readLine()) != null) {
//				string = string /*+ "\r\n"*/;
//				System.out.println("string%% :"+string);
				String[] strArray = string.split("\\:");
				System.out.println("strArray :"+Arrays.toString(strArray));
				// Log.i("ceshi", strArray.length + "");
				if (strArray.length == 2 && strArray[1].length() > 0
						&& strArray[1].trim().startsWith("i")) {
					l = count(countValue(strArray[1].split("ms")));
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return l + " ";
	}

	public String[] countValue(String[] value) {
		String[] strArray = new String[5];
		String regex = "\\d+\\.*\\d*";
		int index = 0;
		Pattern p = Pattern.compile(regex);
		if (value[0] != null) {
			Matcher m = p.matcher(value[0]);
			while (m.find()) {
				if (!"".equals(m.group())) {
					strArray[index] = m.group();
//					System.out.println("m.group() :"+m.group());
					index++;
				}
			}
		}
		return strArray;
	}

	public float count(String[] strArray) {
//		float l = 0f;
		float l3 = 0f;
		if (strArray.length == 0) {
//			return l;
			return l3;
		} else {
//			float l1 = Float.valueOf(strArray[0]);
//			float l2 = Float.valueOf(strArray[1]);
//			System.out.println("l2 :"+l2);
			l3 = Float.valueOf(strArray[2]);
//			System.out.println("l3 :"+l3);
//			if (l3 != 0) {
//				l = (l2 / l3) * 1000;
//			}
		}
//		return l;
		return l3;
	}

	public String getInfo(String ip) {

		StringBuffer sBuffer = new StringBuffer();
		String cpuString = getProcessCpuRate();
		String memoryString = getAvailMemory(context);// 获取android当前可用内存大小

		String totalmemoryString = getTotalMemory();// 总大小
		// DecimalFormat mDecimalFormat = new DecimalFormat(".##");
		String string = pingNet(ip);
//		String string = null;

		String str = sBuffer.append(cpuString).append(";")
				.append(totalmemoryString).append(";").append(memoryString)
				.append(";").append("ping time:"+string).append("ms").append(";")
				.toString();
		Log.i("ceshi", str);
		return str;
	}

	private long getTotalCpuTime() { // 获取系统总CPU使用时间
		String[] cpuInfos = null;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(CPU_FILE)), 1000);
			String load = reader.readLine();
			reader.close();
			cpuInfos = load.split(" ");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		long totalCpu = Long.parseLong(cpuInfos[2])
				+ Long.parseLong(cpuInfos[3]) + Long.parseLong(cpuInfos[4])
				+ Long.parseLong(cpuInfos[6]) + Long.parseLong(cpuInfos[5])
				+ Long.parseLong(cpuInfos[7]) + Long.parseLong(cpuInfos[8]);
		return totalCpu;
	}

	/***
	 * 1.获取android当前可用内存大小
	 * 
	 * @param activity
	 * @return
	 */
	private String getAvailMemory(Context context) {

		ActivityManager am = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		MemoryInfo mi = new MemoryInfo();

		am.getMemoryInfo(mi);
		// mi.availMem; 当前系统的可用内存
		long m = mi.availMem;
		return bytes2kb(m);// 将获取的内存大小规格化
	}

	/***
	 * 3.获取内存总大小
	 * 
	 * @param activity
	 * @return
	 */
	private String getTotalMemory() {
		String str1 = "/proc/meminfo";// 系统内存信息文件
		String str2;
		String[] arrayOfString;
		long initial_memory = 0;

		try {
			FileReader localFileReader = new FileReader(str1);
			BufferedReader localBufferedReader = new BufferedReader(
					localFileReader, 8192);
			str2 = localBufferedReader.readLine();// 读取meminfo第一行，系统总内存大小

			arrayOfString = str2.split("\\s+");

			initial_memory = Integer.valueOf(arrayOfString[1]).intValue() * 1024;// 获得系统总内存，单位是KB，乘以1024转换为Byte
			localBufferedReader.close();

		} catch (IOException e) {
		}
		return bytes2kb(initial_memory);// Byte转换为KB或者MB，内存大小规格化
	}

	private String bytes2kb(long bytes) {
		BigDecimal filesize = new BigDecimal(bytes);
		BigDecimal megabyte = new BigDecimal(1024 * 1024);
		float returnValue = filesize.divide(megabyte, 2, BigDecimal.ROUND_UP)
				.floatValue();
		if (returnValue > 1)
			return (returnValue + "MB");
		BigDecimal kilobyte = new BigDecimal(1024);
		returnValue = filesize.divide(kilobyte, 2, BigDecimal.ROUND_UP)
				.floatValue();
		return (returnValue + "KB");
	}

	/***
	 * 获取CPU使用率
	 * 
	 * @return
	 */
	private String getProcessCpuRate() {

		float totalCpuTime1 = getTotalCpuTime();
		float processCpuTime1 = getAppCpuTime();
		try {
			Thread.sleep(360);

		} catch (Exception e) {
		}

		float totalCpuTime2 = getTotalCpuTime();
		float processCpuTime2 = getAppCpuTime();

		long cpuRate = (long) (100 * (processCpuTime2 - processCpuTime1) / (totalCpuTime2 - totalCpuTime1));

		return cpuRate + "";
	}

	private long getAppCpuTime() { // 获取应用占用的CPU时间
		String[] cpuInfos = null;
		try {
			int pid = android.os.Process.myPid();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream("/proc/" + pid + "/stat")), 1000);
			String load = reader.readLine();
			reader.close();
			cpuInfos = load.split(" ");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		long appCpuTime = Long.parseLong(cpuInfos[13])
				+ Long.parseLong(cpuInfos[14]) + Long.parseLong(cpuInfos[15])
				+ Long.parseLong(cpuInfos[16]);
		return appCpuTime;
	}

}
