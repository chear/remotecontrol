package com.zkar.pis.remotecontrol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

import com.zkar.monitoringfacilities.DeviceUuidFactory;
import com.zkar.monitoringfacilities.EquipmentMonitoring;
import com.zkar.monitoringfacilities.LogCatThread;
import com.zkar.monitoringfacilities.SetUpIpUtils;
import com.zkar.monitoringfacilities.TestEquipmentInformationThread;
import com.zkar.monitoringfacilities.VideoThread;
import com.zkar.outside.util.CopyFileUtils;
import com.zkar.outside.util.DownLoad;
import com.zkar.outside.util.FileCache;
import com.zkar.outside.util.HttpDownload;
import com.zkar.outside.util.HttpUtils;
import com.zkar.outside.util.PackageUtils;
import com.zkar.outside.util.ShellUtils;
import com.zkar.outside.util.SocketUtils;
import com.zkar.outside.util.XmlUtils;

public class MyService extends Service implements SurfaceHolder.Callback {
	private final int VIDEO_RECORDING = 11111;
	private final int GET_ORDER = 2;
	public static ServerSocket serverSocket = null;
	private LogCatThread mLogcatThread;
	private Socket socket;
	private DatagramSocket getSocket;
	private BufferedReader socketinput;
	private SurfaceView surfaceView;
	private static final String PREFS_NAME = "VideoDemo";
	private final String UPDATE = "5";//更新程序
	private final String KILL_ALL_PIS = "8";//杀死所有pis进程
	private final String LOGSTART = "10";//开始返回logcat
	private final String LOGSTOP = "11";//停止返回logcat
	private final String VIDEO_RECORDING_START = "12";//开始录视频
	private final String STOPVIDEO = "13";//停止录制视频
	private final String POWEROFF = "22";//重启机器
	private final String SENDIP = "101";//返回本机器的ip等网络配置
	private final String EDITIP = "102";;//修改ip等，不返回
	private final String EDITIP2 = "104";//修改ip等，返回“1001”
	private final String EDIT_DEVICE_NAME = "105";//只修改存入devicepath.xml的监室名称
	private final String QUERY_VERSION_NUMBER = "106";//查询当前程序的版本号
	private final String MACHINECODE = "413";//返回机器码
	private final String INFO = "414";//获取机器的硬件信息
	private final String SET_MACHINE = "454";//在屏幕上显示出写入devicepath.xml的监室名称
	private final String FINISH_MACHINE = "455";//关闭设置页面
	private final String DYNAMIC_IP = "456";//设置动态获取ip
	private final String RETURN_DESKTOP = "478";//使机器返回到系统桌面
	private final String CLEAR_PICTURE = "479";//清除Imageload文件夹里缓存的图片
	private final String CLEAR_PLUGINS = "480";//清除plugins文件夹里文件
	private final String RUNNING_STATE = "1103";//返回当前屏幕显示的activity类名和包名
	private final String RESTART_ADB = "1104";//重启机器的adb服务
//	private final String SHOW_THEM = "1105";//在屏幕上显示出写入devicepath.xml的监室名称
	private SharedPreferences settings;
	private WindowManager windowManager;
//	private String machineCode,serviceurl;
	private boolean detectionIpblock = false;//监测子网掩码变不变
	private boolean getMessageQueueThreadblock = false;//等待接收指令的线程

	private final String TAG = "ZKAR";

	public final Handler handle = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case VIDEO_RECORDING:
				// Start foreground service to avoid unexpected kill
				Notification.Builder notification = new Notification.Builder(
						MyService.this).setContentTitle("Background Video Recorder")
						.setContentText("").setSmallIcon(R.drawable.ic_launcher);
				startForeground(1234, notification.getNotification());
				settings = getSharedPreferences(PREFS_NAME, 0);
				windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
				surfaceView = new SurfaceView(MyService.this);
				LayoutParams layoutParams = new WindowManager.LayoutParams(1,
						1, WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
						WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
						PixelFormat.TRANSLUCENT);
				layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
				windowManager.addView(surfaceView, layoutParams);
				surfaceView.getHolder().addCallback(MyService.this);
				break;
			case GET_ORDER://接收指令

				break;
			}
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	int i = 0;

	@Override
	public void onCreate() {
		super.onCreate();
		ShellUtils.execCommand("setprop service.adb.tcp.port 5555", true);
		ShellUtils.execCommand("stop adbd", true);
		ShellUtils.execCommand("start adbd", true);

//		new DetectionIpAndMaskThread().start();
		new UdpServerThread().start();
//		new TestEquipmentInformationThread(getApplicationContext()).start();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

	}

	private boolean isInstalling = false;

	// 组播地址ַ
//    private static final String MulticastAddr = "234.8.1.2";

	private static final int REMOTE_SERVER_PORT = 9077;

	class UdpServerThread extends Thread {
		@Override
		public void run() {
			super.run();
			// 确定接受方的IP和端口号，IP地址为本地机器地址
			// InetAddress ip = InetAddress.getLocalHost();
			// System.out.println("ip :"+ip);
			Log.i(TAG, "remotecontrol服务启动..");
			int port = 9075;
//			try {
//				InetAddress inetRemoteAddr = InetAddress.getByName(MulticastAddr);
//				getSocket = new MulticastSocket(port);
//				// 加入组播组
//				getSocket.joinGroup(inetRemoteAddr);
//			} catch (SocketException e) 0{
//				e.printStackTrace();
//			} catch (UnknownHostException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
			InetAddress address = null;
			try {
				address = InetAddress.getByName("255.255.255.255");
				getSocket = new DatagramSocket(port);
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// 确定数据报接受的数据的数组大小
			byte[] buf = new byte[1024];

			// 创建接受类型的数据报，数据将存储在buf中
			DatagramPacket getPacket = new DatagramPacket(buf, buf.length);

			while (true) {
				// 通过套接字接收数据
				try {
					getSocket.receive(getPacket);

					// 解析发送方传递的消息，并打印
					String getMes = new String(buf, 0, getPacket.getLength());
					System.out.println("remotecontrol接收的消息：" + getMes);
					String[] commandMess = getMes.toLowerCase().split(";");
					String commandMess0 = commandMess[0];

					// 通过数据报得到发送方的IP和端口号
					InetAddress sendIP = getPacket.getAddress();
					int sendPort = getPacket.getPort();
					System.out.println("对方的IP地址是：" + sendIP.getHostAddress());
					System.out.println("对方的端口号是：" + sendPort);
					// 通过数据报得到发送方的套接字地址
					SocketAddress sendAddress = getPacket.getSocketAddress();

					if (SENDIP.equals(commandMess0)) {
						String deviceName = "无";
						try {
							deviceName = readTheDeviceName();
						} catch (Exception e) {
							e.printStackTrace();
						}
						// 返回本机的以太网配置
//						sendDatagramPacketMessage(sendAddress,
//								getIPConfiguration() + ";" + deviceName + ";");

						byte[] backbuffer = (getIPConfiguration() + ";" + deviceName + ";").getBytes();
						// 组播返回
						getSocket.send(new DatagramPacket(backbuffer, backbuffer.length, address, REMOTE_SERVER_PORT));
					} else if (EDITIP.equals(commandMess0) || EDITIP2.equals(commandMess0)) {
						Log.i(TAG,"htt!!! -- Start to set IP Configration");
						setIPConfiguration(commandMess,sendAddress);
						Log.i(TAG,"htt!!! -- Finishing to set IP Configration");
					} else if(EDIT_DEVICE_NAME.equals(commandMess0)){
						if(commandMess.length>1){
							writeTheDeviceName(commandMess[1]);// 修改设备名称
						}
					}else if (RUNNING_STATE.equals(commandMess0)) {
						// 用Linux命令查看服务运行状态
						// boolean isRun =
						// SetUpIpUtils.getInstance().isRunning("com.zkar.pis");
						// 用ActivityManager查看程序是否处于活动状态
						// boolean isRun = AppActiveState.
						// isRunningApp(getApplicationContext(),"com.zkar.pis");
						// System.out.println("isRun :"+isRun);

						// 获取当前显示的Activity
						ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
						ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
						Log.d(TAG, "pkg:" + cn.getPackageName());
						Log.d(TAG, "cls:" + cn.getClassName());
						sendDatagramPacketMessage(sendAddress,cn.getClassName() + "");
					} else if (POWEROFF.equals(commandMess0)) { // 重启指令
						SetUpIpUtils.getInstance().restartSystem();
					} else if (UPDATE.equals(commandMess0) && commandMess.length > 1) {
						String baseUrl = commandMess[1];
						// 如果没有开始下载
						if(!isInstalling){
							downloadAndInstall(baseUrl, sendAddress);// 下载安装app,并启动主程序
						}
					} else if (INFO.equals(commandMess0)) {
						// 获取硬件信息
						EquipmentMonitoring monitoring = new EquipmentMonitoring(
								MyService.this);
						String hardwareInformation = monitoring
								.getInfo(getServerIp());
						System.out.println("硬件信息 :" + hardwareInformation);
						sendDatagramPacketMessage(sendAddress,
								hardwareInformation);
					} else if (MACHINECODE.equals(commandMess0)) {
						// 获取当前机器的机器码
						String machineCode = DeviceUuidFactory.getInstance(
								MyService.this).getDeviceUuid();
						sendDatagramPacketMessage(sendAddress, machineCode);
					} else if (VIDEO_RECORDING_START.equals(commandMess0)) {
						handle.obtainMessage(VIDEO_RECORDING, "500").sendToTarget();
					} else if (STOPVIDEO.equals(commandMess0)) {
						vt.releaseLocalSocket();
						vt.releaseRecorder();
						vt.stop();
					} else if (LOGSTART.equals(commandMess0)) {
						String command1 = commandMess[1];
						String socketIP = commandMess[2];
						int socketport = Integer.parseInt(commandMess[3]);
						Log.i(TAG, i++ + "");
						// socket.shutdownInput();
						Log.i(TAG, "客户端发过来的消息mString:" + command1);
						String logcommand = command1 == null ? "v" : command1;
						Log.i(TAG, logcommand);

						if (mLogcatThread != null
								&& mLogcatThread.isRunning == true) {
							mLogcatThread.stopLogCat();
							Log.i(TAG, mLogcatThread.isRunning + "");
						}
//						DatagramSocket mSocket = new DatagramSocket();
//						mLogcatThread = new LogCatThread(mSocket,
//								socketIP, socketport);
						mLogcatThread = new LogCatThread(getSocket,
								sendIP.getHostAddress(), sendPort);
						String mFilter = mLogcatThread.getLogCat(logcommand);
						if (mFilter != null) {
							mLogcatThread.setFilter(mFilter);
						}
						Log.i(TAG, mFilter);
						mLogcatThread.start();
					} else if (LOGSTOP.equals(commandMess0)) {
						if (mLogcatThread != null) {
							Log.i(TAG, "stopLogCat");
							mLogcatThread.stopLogCat();
						}
					} else if(RETURN_DESKTOP.equals(commandMess0)) {
						Intent intent = new Intent(Intent.ACTION_MAIN);
						// 如果是服务里调用，必须加入new task标识
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intent.addCategory(Intent.CATEGORY_HOME);
						startActivity(intent);
					} else if(SET_MACHINE.equals(commandMess0)){
						Intent intent = new Intent("android.intent.action.SET_MACHINE");
						sendBroadcast(intent);
					} else if(FINISH_MACHINE.equals(commandMess0)){
						Intent intent = new Intent("android.intent.action.FINISH_MACHINE");
						sendBroadcast(intent);
					}else if(RESTART_ADB.equals(commandMess0)) {
						ShellUtils.execCommand("stop adbd", true);
						ShellUtils.execCommand("start adbd", true);
					} else if(KILL_ALL_PIS.equals(commandMess0)){
						killAllPis(MyService.this);
					} else if(DYNAMIC_IP.equals(commandMess0)){
						setDynamicIp();
					} else if(CLEAR_PICTURE.equals(commandMess0)) {
						FileCache.getInstance(getApplicationContext(),"ImageLoader").clear();
						System.out.println("清除缓存..");
					} else if(CLEAR_PLUGINS.equals(commandMess0)){
						FileCache.getInstance(getApplicationContext(),"plugins").clear();
					} else if(QUERY_VERSION_NUMBER.equals(commandMess0)){//查询当前程序的版本号
						SharedPreferences versionSP = MyService.this.getSharedPreferences(
								"version", Context.MODE_PRIVATE);
						String nowVersion = versionSP.getString("versionNumber", "0.0");
						sendDatagramPacketMessage(sendAddress, nowVersion);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				// 关闭套接字
				// getSocket.close();
			}
		}
	}

	/**
	 * 监测子网掩码是否错误，启动一直等待获取命令的线程
	 * 给室内或室外屏发socket消息根据是否有返回判断是否程序假死并传给服务器
	 * @author Administrator
	 */
	/**
	class DetectionIpAndMaskThread extends Thread {
		@Override
		public void run() {
			super.run();
			int count = 0;
			SharedPreferences ipParameters = getSharedPreferences(
					"ipparameters", Context.MODE_MULTI_PROCESS);
			while (true) {
				if (count == 50) {// 过5分钟检查一次ip
					count = 0;
					if(!detectionIpblock){
						String mask = ipParameters.getString("mask",
								"255.255.255.0");
						String getMask = SetUpIpUtils.getInstance().getMask();
						System.out.println("监测mask mask:" + mask + " getMask:"
								+ getMask);
						if (!mask.equals(getMask)) {
							System.out.println("mask被篡改..");
							String ip = ipParameters.getString("ip", null);
							String dns = ipParameters.getString("dns", null);
							String gateway = ipParameters.getString("gateway", null);
							SetUpIpUtils.getInstance().editEthernet( ip, dns,gateway, mask);
						}
					}
					//一直等待获取命令的线程
					if(!getMessageQueueThreadblock){
						new GetMessageQueueThread().start();
					}
				}
				try {
					sleep(1000*6);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				count++;
//				surveyMessageSending();
			}
		}
	}
	**/

	/**
	 * 设置ip动态获取
	 * */
	private void setDynamicIp(){
		SharedPreferences ipParameters = getSharedPreferences(
				"ipparameters", Context.MODE_MULTI_PROCESS);
		Editor editor = ipParameters.edit();
		editor.putBoolean("DYNAMICIP",true);
		editor.commit();
		detectionIpblock= true;//不用再监测ip
		SetUpIpUtils.getInstance().setDynamicAcquisitionIP();
	}

	/**
	 * 设置修改ip、网关、子网掩码等配置
	 * */
	private void setIPConfiguration(String[] commandMess,SocketAddress sendAddress){
		SharedPreferences ipParameters = getSharedPreferences(
				"ipparameters", Context.MODE_MULTI_PROCESS);
		Editor editor = ipParameters.edit();
		editor.putString("ip", commandMess[1]);
		editor.putString("dns", commandMess[2]);
		editor.putString("gateway", commandMess[3]);
		editor.putString("mask", commandMess[4]);
		editor.putBoolean("DYNAMICIP",false);//重启时是否设置ip动态获取，false否
		editor.commit();
		// 修改ip等
		SetUpIpUtils.getInstance().editEthernet(this, commandMess[1],
				commandMess[2], commandMess[3], commandMess[4]);
		// 修改设备名称
		writeTheDeviceName(commandMess[5]);
		if (EDITIP2.equals(commandMess[0])) {
			sendDatagramPacketMessage(sendAddress, "1001");
		}
		detectionIpblock=false;//监测ip
	}

	/**
	 * 给室内或室外屏发socket消息根据是否有返回判断是否程序假死并传给服务器
	 * */
	/*private void surveyMessageSending(){
		String isok = SocketUtils.send("11","127.0.0.1", 9078);
		Log.i("remotecontrol", "监测程序是否运行isok :"+isok);
		if("ok".equals(isok)){
			sendSurveyMessage("true");
		}else{
			sendSurveyMessage("false");
		}
	}*/
	/**
	 * 监测程序的结果发给服务器
	 * */
	/*private void sendSurveyMessage(String isok){
		MyApplication myapp = (MyApplication) getApplication();
		String machineCode = myapp.getMachineCode();
		String serviceurl = myapp.getServiceurl();
		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair("key", machineCode));
		urlParameters.add(new BasicNameValuePair("isappok",isok));
		HttpUtils.doPost(serviceurl+ "/device/keepalive", urlParameters);
	}*/

	/**
	 * 给发送方返回消息
	 * */
	private void sendDatagramPacketMessage(SocketAddress Address,
			String backmessage) {
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

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
	}

	VideoThread vt;

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		String serverIp = getServerIp();
//		String serverIp = "192.168.0.178";
	  	vt = new VideoThread(settings, surfaceView, holder,serverIp);
		vt.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

	}

	private void socketResponse(String msg) {
		try {
			PrintWriter socketoutput = new PrintWriter(
					socket.getOutputStream(), true);
			socketoutput.println(msg);
			socket.shutdownOutput();
			socketoutput.close();
			socketinput.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 读取serverIp
	 * */
	private String getServerIp() {
		String serverIp = null;
		try {
			Context contextPIS = createPackageContext("com.zkar.pis",
					Context.CONTEXT_IGNORE_SECURITY);
			SharedPreferences roomPreferences = contextPIS
					.getSharedPreferences("room", Context.MODE_MULTI_PROCESS);
			serverIp = roomPreferences.getString("serverIp", null);
			System.out.println("MyService serverIp :" + serverIp);
		} catch (NameNotFoundException e2) {
			e2.printStackTrace();
		}
		return serverIp;
	}

	/**
	 * 获取本机以太网配置
	 */
	private String getIPConfiguration() {
		// 获取mac,ip,子网掩码
		String[] ipMac = SetUpIpUtils.getInstance().getIpMac();
		// ip`
		String localip = ipMac[1];
		// System.out.println("localip :"+localip);
		// 获取dns
		String dns = SetUpIpUtils.getInstance().getDns();
		// mac地址ַ
		String mac = ipMac[0];
		// 获取网关
		String gateway = SetUpIpUtils.getInstance().getGateway();
		// 子网掩码
		String mask = ipMac[2];
		// 获取当前机器的机器码
		String machineCode = DeviceUuidFactory.getInstance(MyService.this)
				.getDeviceUuid();
		// System.out.println("RemoteControl--machineCode :"+ machineCode);

		String ipConfiguration = localip + ";" + dns + ";" + gateway + ";"
				+ mask + ";" + mac + ";" + machineCode;

		return ipConfiguration;
	}

	/**
	 * 下载安装app,并启动主程序
	 */
	private void downloadAndInstall(final String url,final SocketAddress sendAddress) {
		new Thread(){
			@Override
			public void run() {
				super.run();
				isInstalling = true;
				SetUpIpUtils.getInstance().remountDirectory("/mnt/sdcard");//将sdcard挂载为可读写
				String baseUrl = url;
				if (baseUrl == null)
					return;
				if (!baseUrl.endsWith("/")) {
					baseUrl += "/";
				}
				UpdateUtils.downloadUpdate(getApplicationContext(), baseUrl, sendAddress, getSocket);
				isInstalling = false;//重置下载状态
			}
		}.start();
	}

	/**
	 * 修改devicename.xml文件里的设备名
	 * */
	private void writeTheDeviceName(String name) {
		File file = new File("/sdcard/", "devicename.xml");
		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(name.getBytes());
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * 读出devicename.xml文件里的设备名
	 * */
	private String readTheDeviceName() {
		StringBuffer sb = new StringBuffer();;
		try {
			File file = new File("/sdcard/", "devicename.xml");
			BufferedReader br = new BufferedReader(new FileReader(file));
			String readline = "";
			while ((readline = br.readLine()) != null) {
				System.out.println("readline:" + readline);
				sb.append(readline);
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
	/**
	 * 杀死所有后台进程
	 * */
	private void killAllPis(Context context) {
		// 获取一个ActivityManager 对象
		ActivityManager activityManager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		// 获取系统中所有正在运行的进程
		List<RunningAppProcessInfo> appProcessInfos = activityManager
				.getRunningAppProcesses();
		// 获取当前activity所在的进程
		// String currentProcess=context.getApplicationInfo().processName;
		String currentProcess = "com.zkar.pis";
		// 对系统中所有正在运行的进程进行迭代，如果进程名不是当前进程，则Kill掉
		for (RunningAppProcessInfo appProcessInfo : appProcessInfos) {
			String processName = appProcessInfo.processName;
			if (processName.contains(currentProcess)) {
				System.out.println("ApplicationInfo-->" + processName);
				android.os.Process.killProcess(appProcessInfo.pid);
				activityManager.killBackgroundProcesses(processName);
				System.out.println("Killed -->PID:" + appProcessInfo.pid
						+ "--ProcessName:" + processName);
			}
		}
	}

	/**
	 *一直等待获取命令
	 * @author Administrator
	 */
	private class GetMessageQueueThread extends Thread{
		@Override
		public void run() {
			super.run();
			getMessageQueueThreadblock = true;
			MyApplication myapp = (MyApplication) getApplication();
			String machineCode = myapp.getMachineCode();
			String serviceurl = myapp.getServiceurl();
			List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
			urlParameters.add(new BasicNameValuePair("key", machineCode));
			urlParameters.add(new BasicNameValuePair("Name",myapp.getLocalIP()));
			urlParameters.add(new BasicNameValuePair("Type","command"));
			String getOrder =HttpUtils.doPostLongTime(serviceurl+ "/MessageQueue/Get",
					urlParameters,18*60*1000);
			handle.obtainMessage(GET_ORDER,getOrder).sendToTarget();
			getMessageQueueThreadblock = false;
		}
	}
}
