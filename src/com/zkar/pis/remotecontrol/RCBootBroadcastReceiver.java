package com.zkar.pis.remotecontrol;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.zkar.monitoringfacilities.SetUpIpUtils;

public class RCBootBroadcastReceiver extends BroadcastReceiver {
    private final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    private final String DETECTION_UPDATES = "android.intent.action.DETECTION_UPDATES";
    private final String PACKAGE_ADDED = "android.intent.action.PACKAGE_ADDED";
    private final String TAG = "ZKAR.RCBoot";
//	private final String START_SERVICE = "android.intent.action.START_WATCHDOG_SERVICE";

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        if (BOOT_COMPLETED.equals(action)) {
            Log.i(TAG , "WatchDog received system power up");
            remoteControlInit(context);
        } else if (DETECTION_UPDATES.equals(action)) {
            final String serviceurl = intent.getStringExtra("serviceurl");
            final String label = intent.getStringExtra("label");
            Log.i(TAG, "watchdog received broadcast :" + serviceurl + " label:"
                    + label);
            new Thread() {
                @Override
                public void run() {
                    super.run();
                    downloadAndInstall(context, serviceurl, label);
                }
            }.start();
        } else if (PACKAGE_ADDED.equals(action)) {
            String packageName = intent.getDataString();
            Log.e(TAG, "---------------" + packageName + " installed..");
            // try {
            // Thread.sleep(500);
            // } catch (InterruptedException e) {
            // e.printStackTrace();
            // }
            // rootCommand("pm uninstall "+packageName);
            // uninstall(packageName);
            // PackageUtils.uninstall(context, packageName);
        }/* else if (START_SERVICE.equals(action)) {// 启动服务的广播
            Log.i("WatchDog", "启动服务的广播");
			startWatchDog(context);
		}*/

    }

	/*private void startWatchDog(Context context) {
		Intent intent = new Intent(context, MyService.class);
		context.startService(intent);
	}*/

    /**
     * 每次开机设置保存的ipֵ
     *
     * @param context
     */
    private void remoteControlInit(Context context) {
        Intent service = new Intent(context, MyService.class);
        context.startService(service);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //获取本机ipֵ
        MyApplication myapp = (MyApplication) context.getApplicationContext();
        myapp.setLocalIP(SetUpIpUtils.getInstance().getLocalIpAddress());
    }

	/*
	 * private void rootCommand(final String command){ new Thread() {
	 * 
	 * @Override public void run() { super.run(); Process process = null;
	 * DataOutputStream os = null; try { process =
	 * Runtime.getRuntime().exec("su"); os = new
	 * DataOutputStream(process.getOutputStream()); os.writeBytes(command +
	 * "\n"); os.writeBytes("exit\n"); os.flush(); process.waitFor(); } catch
	 * (Exception e) { System.out.println("1111111"); e.getMessage(); } finally
	 * { try { if (os != null) { os.close(); } process.destroy(); } catch
	 * (Exception e) { } } System.out.println("222222222"); } }.start(); }
	 */

    /**
     * 从服务器上下载更新
     */
    private void downloadAndInstall(Context context, String baseUrl,
                                    String label) {
        if (baseUrl == null)
            return;
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        if ("in".equals(label)) {
            baseUrl += "update/inside/";
        } else if ("out".equals(label)) {
            baseUrl += "update/outside/";
        }
        UpdateUtils.downloadUpdate(context, baseUrl);
    }
}
