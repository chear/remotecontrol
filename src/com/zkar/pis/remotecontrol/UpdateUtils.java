package com.zkar.pis.remotecontrol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;

import com.zkar.monitoringfacilities.SetUpIpUtils;
import com.zkar.outside.util.CopyFileUtils;
import com.zkar.outside.util.DownLoad;
import com.zkar.outside.util.HttpDownload;
import com.zkar.outside.util.PackageUtils;
import com.zkar.outside.util.XmlUtils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class UpdateUtils {
	private final static String SAVE_PATH = "/sdcard/plugins/";

	/**
	 * 自动检测更新程序
	 * 
	 * @param context
	 * @param baseUrl
	 */
	public static void downloadUpdate(Context context, String baseUrl) {
		downloadUpdate(context, baseUrl, null, null);
	}

	/**
	 * 下载更新程序
	 * 
	 * @param context
	 * @param baseUrl
	 * @param sendAddress
	 * @param getSocket
	 */
	public static void downloadUpdate(Context context, String baseUrl,
			SocketAddress sendAddress, DatagramSocket getSocket) {
		String benjiIP = SetUpIpUtils.getInstance().getIp();
		String versionNumber = "";
		SharedPreferences versionSP = context.getSharedPreferences("version",
				Context.MODE_PRIVATE);
		if (getSocket == null) {// 自动检测更新
			versionNumber = versionSP.getString("versionNumber", "0.0");
			Log.i("System.out", "versionNumber:" + versionNumber);
		}
		// 获取更新文件
		String updateXml = "";
		try {
			// 同步下载
			updateXml = HttpDownload.download(new URL(baseUrl + "update.xml"));
			String version = XmlUtils.ReadValue(updateXml, "version");
			if (getSocket == null && versionNumber.equals(version)) {// 用于自动检测更新
				return;
			}
			// 释放更新watchDog
			boolean updateWithWatchDog = false;
			boolean setSystemFont = false;

			ArrayList<String> files = XmlUtils.ReadValues(updateXml, "file");
			for (String file : files) {
				String downloadUrl = baseUrl + file;
				boolean uninstallResult = false;
				boolean installResult = false;
				boolean downloadResult = false;
				try {// 同步下载文件
					int result;
					if ("DeviceTypeSN.config".equals(file)) {
						result = DownLoad.download("/sdcard/", new URL(
								downloadUrl));
					} else {
						result = DownLoad.download(SAVE_PATH, new URL(
								downloadUrl));
					}
					downloadResult = result == 1;
				} catch (Exception e) {
					e.printStackTrace();
					downloadResult = false;
				}
				Log.i("autoInstall", "下载文件:" + file + ";" + downloadResult);

				if (getSocket != null) {// 返回下载结果(非自动检测更新)
					sendDatagramPacketMessage(sendAddress, getSocket, "489;"
							+ benjiIP + ";" + file + ";"
							+ (downloadResult ? "true" : "false"));
				}
				if (!downloadResult) {// 没下载到
					continue;
				}
				// 成功下载文件
				if ("com.zkar.pis.remotecontrol.apk".equals(file)) {
					updateWithWatchDog = true;
				} else if ("DroidSansFallback.ttf".equals(file)) {
					setSystemFont = true;
				}
				// 如果是apk文件 安装
				if (file.endsWith("apk")
						&& !"com.zkar.pis.remotecontrol.apk".equals(file)) {
					Thread.sleep(800);// 等一下
					int result = -1;
					try {
						if (file.contains("eyesmart")) {// 先卸载再安装
							result = PackageUtils.uninstall(context,
									"cn.com.eyesmart.irisidentification");
							uninstallResult = result == 1;
							Log.i("autoInstall", "虹膜程序卸载结果:" + file + ";"
									+ uninstallResult);
						}
						result = PackageUtils
								.install(context, SAVE_PATH + file);
						installResult = result == 1;
						Log.i("autoInstall", "安装结果:" + file + ";"
								+ installResult);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				if (getSocket != null) {// 返回下载结果(非自动检测更新)
					sendDatagramPacketMessage(sendAddress, getSocket, "490;"
							+ benjiIP + ";" + file + ";"
							+ (installResult ? "true" : "false"));
				}
			}

			Thread.sleep(1000);
			try {
				// PIS.Outside
				Intent intentInsideLockScreenActivity = new Intent();
				intentInsideLockScreenActivity
						.setComponent(new ComponentName("com.zkar.pis",
								"com.zkar.pis.InsideLockScreenActivity"));
				intentInsideLockScreenActivity
						.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intentInsideLockScreenActivity);
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				// PIS.Outside
				Intent intentOutSideLockscreenActivity = new Intent();
				intentOutSideLockscreenActivity.setComponent(new ComponentName(
						"com.zkar.pis",
						"com.zkar.pis.OutSideLockscreenActivity"));
				intentOutSideLockscreenActivity
						.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intentOutSideLockscreenActivity);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (getSocket == null) {// 自动检测更新
				// 将新版本号存入sp
				Editor editor = versionSP.edit();
				editor.putString("versionNumber", version);
				editor.commit();
				Log.i("System.out", "更新程序version存入sp :" + version);
			}

			if (updateWithWatchDog) {
				// 安装WatchDog(更新自己)
				int result = PackageUtils.install(context, SAVE_PATH
						+ "com.zkar.pis.remotecontrol.apk");
				Log.i("autoInstall", "安装结果:com.zkar.pis.remotecontrol.apk;"
						+ (result == 1));
			} else if (setSystemFont) {// 设置系统字体然后重启
				boolean isok = CopyFileUtils.copyFile(SAVE_PATH
						+ "DroidSansFallback.ttf",
						"/system/fonts/DroidSansFallback.ttf");
				if (isok) {// 复制成功
					Thread.sleep(800);
					SetUpIpUtils.getInstance().restartSystem();// 重启
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 给发送方返回消息
	 * */
	private static void sendDatagramPacketMessage(SocketAddress Address,
			DatagramSocket getSocket, String backmessage) {
		// 确定要反馈发送方的消息内容，并转换为字节数组
		System.out.println("返回消息backmessage :" + backmessage);
		byte[] backBuf = backmessage.getBytes();
		// 创建发送类型的数据报
		try {
			DatagramPacket sendPacket = new DatagramPacket(backBuf,
					backBuf.length, Address);
			// 通过套接字发送数据
			getSocket.send(sendPacket);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
