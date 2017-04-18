package com.zkar.monitoringfacilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import android.util.Log;

//LogCat UDP输出
public class LogCatThread extends Thread {
	private static final String TAG = "LogcatThread";
	private DatagramSocket mSocket = null;
	public static boolean isRunning = false;
	private String IP;
	private int Port;
	private String procString;

	public LogCatThread(DatagramSocket socket, String IP, int Port) {
		if (socket == null) {
			throw new NullPointerException();
		}
		isRunning = true;
		mSocket = socket; 

		this.IP = IP;
		this.Port = Port;

	}

	public String getLogCat(String string) {
		String mString = " *:S -f -v time ";
		if (string == null) {
			throw new NullPointerException();
		} else if (string.length() == 1) {
			if (string.equals("d")) {
				mString += "*:D";
			} else if (string.equals("i")) {
				mString += "*:I";
			} else if (string.equals("w")) {
				mString += "*:W";
			} else if (string.equals("e")) {
				mString += "*:E";
			} else {
				mString += "*:V";
			}
		} else {
			mString = string;
		} 
		return mString;
	}

	public void setFilter(String config) {
		procString = "logcat";
		if (config.trim().length() > 0) {
			procString += config;
		}
	}

	public void run() {
		Log.d(TAG, "started");
		try {
			Log.i("ceshi", procString);
			Process process = Runtime.getRuntime().exec(procString);
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));
			String logLine;
			while (isRunning) {
				try {
					LogCatThread.sleep(100);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				String sendingLine = "491;";
				// assume that log writes whole lines
				if (bufferedReader.ready()) {
					logLine = bufferedReader.readLine();
					sendingLine += logLine+ System.getProperty("line.separator");
					DatagramPacket packet = new DatagramPacket(
							sendingLine.getBytes(), sendingLine.length(),
							InetAddress.getByName(IP),// IP地址
							Port);// 端口號

//					 System.out.print(new String(packet.getData()));
					// Log.i("ceshi", "logstart" + ":" + "到这6" + ":"
					// + new String(packet.getData()));
					// Log.i("ceshi", "1");
					mSocket.send(packet);
					sendingLine = "491;";
				}
				if (isInterrupted()) {
					Log.d(TAG, "interupted.");
					break;
				}
			}
		} catch (Exception e) {
			isRunning=false;
			e.printStackTrace();
		} finally {
			Log.d(TAG, "stopped.");
		}
	}

	public void stopLogCat() {
		isRunning = false;
		LogCatThread.this.interrupt();
		try {
			LogCatThread.this.join(1000);
			if (LogCatThread.this.isAlive()) {
				// TODO: Display
				// "force close/wait" dialog
				LogCatThread.this.join();

			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}
}
