package com.zkar.outside.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

public class DetectionEquipmentUtils {
	private static DetectionEquipmentUtils detectionEquipmentUtils;
	private final String TAG = "ZKAR.Detection";

	public static DetectionEquipmentUtils getInstance() {
		if (detectionEquipmentUtils == null) {
			detectionEquipmentUtils = new DetectionEquipmentUtils();
		}
		return detectionEquipmentUtils;
	}

	/**
	 * 获取CPU最大频率（单位KHZ） "/system/bin/cat" 命令行
	 * "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq" 存储最大频率的文件的路径
	 */
	public String getMaxCpuFreq() {
		String result = "";
		ProcessBuilder cmd;
		try {
			String[] args = { "/system/bin/cat",
					"/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq" };
			cmd = new ProcessBuilder(args);
			Process process = cmd.start();
			InputStream in = process.getInputStream();
			byte[] re = new byte[24];
			while (in.read(re) != -1) {
				result = result + new String(re);
			}
			in.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			result = "N/A";
		}
		return result.trim();
	}

	/**
	 * 获取CPU最小频率（单位KHZ）
	 * 
	 * @return
	 */
	public String getMinCpuFreq() {
		String result = "";
		ProcessBuilder cmd;
		try {
			String[] args = { "/system/bin/cat",
					"/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq" };
			cmd = new ProcessBuilder(args);
			Process process = cmd.start();
			InputStream in = process.getInputStream();
			byte[] re = new byte[24];
			while (in.read(re) != -1) {
				result = result + new String(re);
			}
			in.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			result = "N/A";
		}
		return result.trim();
	}

	/**
	 * 实时获取CPU当前频率（单位KHZ）
	 * 
	 * @return
	 */
	public String getCurCpuFreq() {
		String result = "N/A";
		try {
			FileReader fr = new FileReader(
					"/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
			BufferedReader br = new BufferedReader(fr);
			String text = br.readLine();
			result = text.trim();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 获取总的cpu使用率,包括User和System
	 * 
	 * @return
	 */
	public String getTotalProcessCpuRate() {
		String result = null;
		Process p = null;
		try {
			p = Runtime.getRuntime().exec("top -m 1 -n 1");
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(p.getInputStream()));
			String cpu = null;
			String userCpu = null;
			String systemCpu = null;
			while ((cpu = bufferedReader.readLine()) != null) {
				if (cpu.trim().length() < 1) {
					continue;
				} else {
					userCpu = cpu.substring(5, cpu.indexOf("%,"));
					systemCpu = cpu.substring(cpu.indexOf("System ") + 7,
							cpu.indexOf("%,", 11));
					break;
				}
			}
			bufferedReader.close();
//			System.out.println("cpu useage :" + cpu);
			Log.i(TAG,"cpu useage :" + cpu);
			if (userCpu != null) {
				result = Integer.parseInt(userCpu)
						+ Integer.parseInt(systemCpu) + "";
			}
			/*
			 * while ((Result = br.readLine()) != null) { if
			 * (Result.trim().length() < 1) { continue; } else { String[] CPUusr
			 * = Result.split("%"); sb.append("USER:" + CPUusr[0] + "\n");
			 * String[] CPUusage = CPUusr[0].split("User"); String[] SYSusage =
			 * CPUusr[1].split("System"); sb.append("CPU:" + CPUusage[1].trim()
			 * + " length:" + CPUusage[1].trim().length() + "\n");
			 * sb.append("SYS:" + SYSusage[1].trim() + " length:" +
			 * SYSusage[1].trim().length() + "\n"); sb.append(Result + "\n");
			 * break; } }
			 */
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (p != null) {
				p.destroy();
			}
		}
		return result;
	}

	/**
	 * 获取当前应用程序的cpu使用率
	 * 
	 * @return
	 */
	public float getProcessCpuRate() {
		float totalCpuTime1 = getTotalCpuTime();
		float processCpuTime1 = getAppCpuTime();
		try {
			Thread.sleep(160);
		} catch (Exception e) {
		}
		float totalCpuTime2 = getTotalCpuTime();
		float processCpuTime2 = getAppCpuTime();
		float cpuRate = 100 * (processCpuTime2 - processCpuTime1)
				/ (totalCpuTime2 - totalCpuTime1);
		return cpuRate;
	}

	/**
	 * 获取系统总CPU使用时间
	 * 
	 * @return
	 */
	public long getTotalCpuTime() {
		String[] cpuInfos = null;
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream("/proc/stat")), 1000);
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

	/**
	 * 获取当前应用占用的CPU时间
	 * 
	 * @return
	 */
	public long getAppCpuTime() {
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

	/**
	 * 获取CPU名字
	 * 
	 * @return
	 */
	public String getCpuName() {
		try {
			FileReader fr = new FileReader("/proc/cpuinfo");
			BufferedReader br = new BufferedReader(fr);
			String text = br.readLine();
			String[] array = text.split(":\\s+", 2);
			for (int i = 0; i < array.length; i++) {
			}
			return array[1];
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 获取Rom大小
	 * 
	 * @return
	 */
	public long[] getRomMemroy() {
		long[] romInfo = new long[2];
		// Total rom memory
		romInfo[0] = getTotalInternalMemorySize();

		// Available rom memory
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		romInfo[1] = blockSize * availableBlocks;
		getVersion();
		return romInfo;
	}

	public long getTotalInternalMemorySize() {
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long totalBlocks = stat.getBlockCount();
		return totalBlocks * blockSize;
	}

	/**
	 * sdCard大小
	 * 
	 * @return
	 */
	public long[] getSDCardMemory() {
		long[] sdCardInfo = new long[2];
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			File sdcardDir = Environment.getExternalStorageDirectory();
			StatFs sf = new StatFs(sdcardDir.getPath());
			long bSize = sf.getBlockSize();
			long bCount = sf.getBlockCount();
			long availBlocks = sf.getAvailableBlocks();

			sdCardInfo[0] = bSize * bCount;// 总大小
			sdCardInfo[1] = bSize * availBlocks;// 可用大小
		}
		return sdCardInfo;
	}

	/**
	 * 系统的版本信息
	 * 
	 * @return
	 */
	public String[] getVersion() {
		String[] version = { "null", "null", "null", "null" };
		String str1 = "/proc/version";
		String str2;
		String[] arrayOfString;
		try {
			FileReader localFileReader = new FileReader(str1);
			BufferedReader localBufferedReader = new BufferedReader(
					localFileReader, 8192);
			str2 = localBufferedReader.readLine();
			arrayOfString = str2.split("\\s+");
			version[0] = arrayOfString[2];// KernelVersion
			localBufferedReader.close();
		} catch (IOException e) {
		}
		version[1] = Build.VERSION.RELEASE;// firmware version
		version[2] = Build.MODEL;// model
		version[3] = Build.DISPLAY;// system version
		return version;
	}

	/**
	 * 获取可用运存大小
	 * 
	 * @param context
	 * @return
	 */
	public long getAvailMemory(Context context) {
		// 获取android当前可用内存大小
		ActivityManager am = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		MemoryInfo mi = new MemoryInfo();
		am.getMemoryInfo(mi);
		// mi.availMem; 当前系统的可用内存
		// return Formatter.formatFileSize(context, mi.availMem);// 将获取的内存大小规格化
//		System.out.println("可用内存---->>>" + mi.availMem / (1024 * 1024));
		Log.i(TAG, "free memory ---->>>" + mi.availMem / (1024 * 1024));
		return mi.availMem / (1024 * 1024);
	}

	/**
	 * 获取总运存大小
	 * 
	 * @param context
	 * @return
	 */
	public long getTotalMemory(Context context) {
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
			for (String num : arrayOfString) {
				Log.i(str2, num + "\t");
			}
			initial_memory = Integer.valueOf(arrayOfString[1]).intValue() * 1024;// 获得系统总内存，单位是KB，乘以1024转换为Byte
			localBufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// return Formatter.formatFileSize(context, initial_memory);//
		// Byte转换为KB或者MB，内存大小规格化
		return initial_memory / (1024 * 1024);
	}

	/**
	 * 获取当前sleepTime毫秒内，总的平均每秒的上传速度和下载速度，包含Mobile和WiFi等，需要停sleepTime毫秒
	 * sleepTime单位是毫秒，返回值单位是KB，上传速度;下载速度
	 * 
	 * @param sleepTime
	 * @return
	 */
	public String[] getUploadDownloadSpeed(int sleepTime) {
		long ttb1 = getTotalTxBytes();// 上传
		long trb1 = getTotalRxBytes();// 下载
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long ttb2 = getTotalTxBytes();
		long trb2 = getTotalRxBytes();
		String[] str = new String[2];
		str[0] = (ttb2 - ttb1) / (sleepTime / 1000) + "";//上传
		str[1] = (trb2 - trb1) / (sleepTime / 1000) + "";//下载
		return str;
	}

	/**
	 * 获取当前sleepTime毫秒内，总的平均每秒的下载速度，包含Mobile和WiFi等，需要停sleepTime毫秒
	 * sleepTime单位是毫秒，返回值单位是KB
	 * 
	 * @param sleepTime
	 * @return
	 */
	public long getDownloadSpeed(int sleepTime) {
		long trb1 = getTotalRxBytes();
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long trb2 = getTotalRxBytes();
		return (trb2 - trb1) / (sleepTime / 1000);
	}

	/**
	 * 获取当前sleepTime毫秒内，总的平均每秒的上传速度，包含Mobile和WiFi等，需要停sleepTime毫秒
	 * sleepTime单位是毫秒，返回值单位是KB
	 * 
	 * @param sleepTime
	 * @return
	 */
	public long getUploadSpeed(int sleepTime) {
		long ttb1 = getTotalTxBytes();
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		long ttb2 = getTotalTxBytes();
		return (ttb2 - ttb1) / (sleepTime / 1000);
	}

	/**
	 * 获取总的接收字节数，包含Mobile和WiFi等 返回值单位是KB
	 * 
	 * @return
	 */
	public long getTotalRxBytes() {
		return TrafficStats.getTotalRxBytes() == TrafficStats.UNSUPPORTED ? 0
				: (TrafficStats.getTotalRxBytes() / 1024);
	}

	/**
	 * 总的发送字节数，包含Mobile和WiFi等 返回值单位是KB
	 * 
	 * @return
	 */
	public long getTotalTxBytes() {
		return TrafficStats.getTotalTxBytes() == TrafficStats.UNSUPPORTED ? 0
				: (TrafficStats.getTotalTxBytes() / 1024);
	}

	/**
	 * 获取通过Mobile连接收到的字节总数，不包含WiFi 返回值单位是KB
	 * 
	 * @return
	 */
	public long getMobileRxBytes() {
		return TrafficStats.getMobileRxBytes() == TrafficStats.UNSUPPORTED ? 0
				: (TrafficStats.getMobileRxBytes() / 1024);
	}
}

/*
 * 总的Cpu使用率计算 计算方法： 1、 采样两个足够短的时间间隔的Cpu快照，分别记作t1,t2，其中t1、t2的结构均为：
 * (user、nice、system、idle、iowait、irq、softirq、stealstolen、guest)的9元组; 2、
 * 计算总的Cpu时间片totalCpuTime a) 把第一次的所有cpu使用情况求和，得到s1; b) 把第二次的所有cpu使用情况求和，得到s2; c)
 * s2 - s1得到这个时间间隔内的所有时间片，即totalCpuTime = j2 - j1 ; 3、计算空闲时间idle
 * idle对应第四列的数据，用第二次的idle - 第一次的idle即可 idle=第二次的idle - 第一次的idle 4、计算cpu使用率 pcpu
 * =100* (total-idle)/total
 */

/**
 * mac地址和开机时间
 * 
 * @return
 */
/*
 * public String[] getOtherInfo(){ String[] other={"null","null"}; WifiManager
 * wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
 * WifiInfo wifiInfo = wifiManager.getConnectionInfo();
 * if(wifiInfo.getMacAddress()!=null){ other[0]=wifiInfo.getMacAddress(); } else
 * { other[0] = "Fail"; } other[1] = getTimes(); return other; } private String
 * getTimes() { long ut = SystemClock.elapsedRealtime() / 1000; if (ut == 0) {
 * ut = 1; } int m = (int) ((ut / 60) % 60); int h = (int) ((ut / 3600)); return
 * h + " " + mContext.getString(R.string.info_times_hour) + m + " " +
 * mContext.getString(R.string.info_times_minute); }
 */

/**
 * 
 */
// public static void getTotalMemory() {
// String str1 = "/proc/meminfo";
// String str2 = "";
// try {
// FileReader fr = new FileReader(str1);
// BufferedReader localBufferedReader = new BufferedReader(fr, 8192);
// while ((str2 = localBufferedReader.readLine()) != null) {
// // Log.i(TAG, "---" + str2);
// }
// } catch (IOException e) {
// }
// }

/**
 * 电池电量
 */
/*
 * private BroadcastReceiver batteryReceiver=new BroadcastReceiver(){
 * 
 * @Override public void onReceive(Context context, Intent intent) { int level =
 * intent.getIntExtra("level", 0); // level加%就是当前电量了 } };
 * registerReceiver(batteryReceiver, new
 * IntentFilter(Intent.ACTION_BATTERY_CHANGED));
 */