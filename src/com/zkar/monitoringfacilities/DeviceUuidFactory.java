package com.zkar.monitoringfacilities;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class DeviceUuidFactory {
	private static final String PREFS_FILE = "zkar_authentication.xml";
	private static final String PREFS_DEVICE_ID = "macaddress";
	private volatile static String ID;
	private static DeviceUuidFactory INSTANCE = null;// DeviceUuidFactory实例

	/** 获取DeviceUuidFactory实例 ,单例模式 */
	public static DeviceUuidFactory getInstance(Context context) {
		if (INSTANCE == null)
			INSTANCE = new DeviceUuidFactory(context);
		return INSTANCE;
	}
	/** 获取DeviceUuidFactory实例 ,单例模式 */
	public static DeviceUuidFactory getInstance() {
		if (INSTANCE == null)
			INSTANCE = new DeviceUuidFactory();
		return INSTANCE;
	}
	private DeviceUuidFactory(){};

	public String callCmd(String cmd, String filter) {
		String result = "";
		String line = "";
		try {
			Process proc = Runtime.getRuntime().exec(cmd);
			InputStreamReader is = new InputStreamReader(proc.getInputStream());
			BufferedReader br = new BufferedReader(is);
			// 执行命令cmd，只取结果中含有filter的这一行
			while ((line = br.readLine()) != null
					&& line.contains(filter) == false) {
				// result += line;
				Log.i("test", "line :" + line);
//				System.out.println("line :" + line);
			}
			result = line;
			Log.i("test", "result :" + result);
//			System.out.println("result :" + result);
		}catch (Exception e) {
			e.printStackTrace();
		}

		return result;

	}

	public String getMacAddress() {
		String result = "";
		String Mac = "";
		result = callCmd("busybox ifconfig", "HWaddr");
		// 如果返回的result == null，则说明网络不可取
		if (result == null) {
			return "网络出错，请检查网络";
		}
		// 对该行数据进行解析
		// 例如：eth0 Link encap:Ethernet HWaddr 00:16:E8:3E:DF:67
		if (result.length() > 0 && result.contains("HWaddr") == true) {
			Mac = result.substring(result.indexOf("HWaddr") + 7,
					result.length() - 1);
//			Log.i("test", "Mac:" + Mac + " Mac.length: " + Mac.length());
			System.out.println("Mac:" + Mac + " Mac.length: " + Mac.length());
			if (Mac.length() > 1) {
				Mac = Mac.replaceAll(" ", "");
				result = "";
				String[] tmp = Mac.split(":");
				for (int i = 0; i < tmp.length; ++i) {
					result += tmp[i];
				}
			}
//			Log.i("test", result + " result.length :" + result.length());
			System.out.println(result + " result.length :" + result.length());
		}

		return result;

	}

	private DeviceUuidFactory(Context context) {
		if (ID == null) {
			synchronized (DeviceUuidFactory.class) {
				if (ID == null) {
					final SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE, 0);
					final String id = prefs.getString(PREFS_DEVICE_ID, null);
//					final String id = null;
					if (id != null) {
						// Use the ids previously computed and stored in the prefs file
						ID = id;
					} else {
						
						ID = Md5(getMacAddress());
						// 获取CPU信息
						// final String androidId = Secure
						// .getString(context.getContentResolver(),
						// Secure.ANDROID_ID);
						//
						// // 使用Android ID，如果无效,在这种情况返回deviceId,
						// // 如果还是无法获取,然后在一个随机UUID存储sp文件
						// try {
						// if (!"9774d56d682e549c".equals(androidId)) {
						// id = UUID.nameUUIDFromBytes(androidId
						// .getBytes("utf8"));
						// } else {
						// final String deviceId = ((TelephonyManager) context
						// .getSystemService(Context.TELEPHONY_SERVICE))
						// .getDeviceId();
						// uuid = deviceId != null ? UUID
						// .nameUUIDFromBytes(deviceId
						// .getBytes("utf8")) : UUID
						// .randomUUID();
						// }
						// } catch (UnsupportedEncodingException e) {
						// throw new RuntimeException(e);
						// }
						// Write the value out to the prefs file
						 prefs.edit().putString(PREFS_DEVICE_ID, ID).commit();
					}
				}
			}
		}
	}

	/**
	 * 获取mac地址
	 * @return
	 */
	public String getDeviceUuid() {
		return ID;
	}

	private String Md5(String plainText) {
		StringBuffer buf = new StringBuffer("");
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(plainText.getBytes());
			byte b[] = md.digest();
			int i;
			for (int offset = 0; offset < b.length; offset++) {
				i = b[offset];
				if (i < 0)
					i += 256;
				if (i < 16)
					buf.append("0");
				buf.append(Integer.toHexString(i));
			}
			// return buf.toString();
			// System.out.println("result: " + buf.toString());// 32位的加密

			// System.out.println("result: " + buf.toString().substring(8,
			// 24));// 16位的加密

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
		return buf.toString().substring(0, 24); //截取长度为24位
	}
}
