package com.zkar.monitoringfacilities;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
//import java.util.Arrays;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
//import java.util.Set;

import org.apache.http.conn.util.InetAddressUtils;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.ethernet.EthernetDevInfo;
import android.net.ethernet.EthernetManager;
import android.os.AsyncTask;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.util.Log;

import com.zkar.pis.remotecontrol.MyService;
import com.zkar.pis.remotecontrol.R;

import static com.zkar.outside.util.PackageUtils.TAG;

public class SetUpIpUtils {
    private final String TAG = "RemoteControl_SetupIP";
    private static SetUpIpUtils setUpIpUtils;
    private EthernetDevInfo mDevInfo;

    //TODO htt
    private List<EthernetDevInfo> mListDevices = new ArrayList<EthernetDevInfo>();
    private final EthernetManager mEthManage;

    //    private PreferenceCategory mEthDevices;
    private SetUpIpUtils() {
//        mEthDevices = (PreferenceCategory) findPreference(KEY_DEVICES_TITLE);
//        mEthDevices.setOrderingAsAdded(false);
        mEthManage = EthernetManager.getInstance();

    }

    /**
     * 获取SetUpIpUtils实例 ,单例模式
     */
    public static SetUpIpUtils getInstance() {
        if (setUpIpUtils == null) {
            setUpIpUtils = new SetUpIpUtils();
        }
        return setUpIpUtils;
    }

    /**
     * 重启机器
     */
    public void restartSystem() {
        Process localProcess = null;
        try {
            localProcess = Runtime.getRuntime().exec("su");
            DataOutputStream localDataOutputStream = new DataOutputStream(
                    localProcess.getOutputStream());
            // localDataOutputStream.writeBytes("chmod 755 "+ "reboot" + "\n");
            localDataOutputStream.writeBytes("reboot\n");
            // localDataOutputStream.writeBytes("poweroff");
            localDataOutputStream.flush();
            localDataOutputStream.close();
            localProcess.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (localProcess != null) {
                localProcess.destroy();
            }
        }
    }

    /**
     * 挂载目录为可读写
     * 注:sdcard路径要写/mnt/sdcard
     */
    public void remountDirectory(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        Process localProcess = null;
        try {
            localProcess = Runtime.getRuntime().exec("su");
            DataOutputStream localDataOutputStream = new DataOutputStream(
                    localProcess.getOutputStream());
            localDataOutputStream.writeBytes("mount -o remount rw " + path);
            localDataOutputStream.flush();
            localDataOutputStream.close();
            localProcess.waitFor();
//			System.out.println("挂载目录:"+path);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (localProcess != null) {
                localProcess.destroy();
            }
        }
    }

    /**
     * 获取本机ip
     */
    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()
                            && InetAddressUtils.isIPv4Address(inetAddress
                            .getHostAddress())) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException e) {
            Log.i("SetUpIpUtils", "获取本机ip出错!");
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取dns
     */
    public String getDns() {
        String localDns = "";
        Process localProcess = null;
        try {
            localProcess = Runtime.getRuntime().exec("getprop net.dns1");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(localProcess.getInputStream()));
            localDns = bufferedReader.readLine();
            // System.out.println("localDns :"+ localDns);
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (localProcess != null) {
                localProcess.destroy();
            }
        }
        return localDns;
    }

    /**
     * 获取网关
     */
    public String getGateway() {
        String gateway = "";
        Process localProcess = null;
        try {
            localProcess = Runtime.getRuntime().exec("ip route");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(localProcess.getInputStream()));
            String getewayReaderLine = bufferedReader.readLine();
            System.out.println("getewayReaderLine :" + getewayReaderLine);
            gateway = getewayReaderLine.substring(12,
                    getewayReaderLine.indexOf("dev") - 1);
            System.out.println("gateway :" + gateway);
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (localProcess != null) {
                localProcess.destroy();
            }
        }
        return gateway;
    }

    /**
     * 获取mac地址,ip,子网掩码,,返回数组
     */
    public String[] getIpMac() {
        String[] str = new String[3];
        Process localProcess = null;
        try {
            localProcess = Runtime.getRuntime().exec("busybox ifconfig");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(localProcess.getInputStream()));
            String line = "";
            line = bufferedReader.readLine();
            // mac
            str[0] = line.substring(line.indexOf("HWaddr") + 7,
                    line.length() - 2);
            // System.out.println("str[0] :"+str[0]);
            // System.out.println("line :"+line);
            line = bufferedReader.readLine();
            // ip
            // System.out.println("line :"+line);
            str[1] = line.trim()
                    .substring(10, line.trim().indexOf("Bcast") - 2);
            // System.out.println("str[1] :"+str[1]);
            // 子网掩码
            str[2] = line.substring(line.indexOf("Mask") + 5, line.length());
            // System.out.println("line :"+line);
            // System.out.println("str[1] :"+str[1]+'\n'+"str[2] :"+str[2]);
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (localProcess != null) {
                localProcess.destroy();
            }
        }
        return str;
    }

    /**
     * 获取子网掩码
     */
    public String getMask() {
        String mask = "";
        Process localProcess = null;
        try {
            localProcess = Runtime.getRuntime().exec("busybox ifconfig");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(localProcess.getInputStream()));
            String line = "";
            line = bufferedReader.readLine();
            line = bufferedReader.readLine();
            // 子网掩码
            mask = line.substring(line.indexOf("Mask") + 5, line.length());
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (localProcess != null) {
                localProcess.destroy();
            }
        }
        return mask;
    }

    /**
     * 修改ip
     */
    public String getIp() {
        String ip = "";
        Process localProcess = null;
        try {
            localProcess = Runtime.getRuntime().exec("busybox ifconfig");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(localProcess.getInputStream()));
            String line = "";
            line = bufferedReader.readLine();
            line = bufferedReader.readLine();
            ip = line.trim().substring(10, line.trim().indexOf("Bcast") - 2);
            bufferedReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (localProcess != null) {
                localProcess.destroy();
            }
        }
        return ip;
    }

    /**
     * 修改ip,dns,网关等设置
     */
    public void editEthernet(Context context, String ip, String dns, String gateway, String mask) {
        if (context == null || ip == null || ip.isEmpty() || gateway == null ||
                gateway.isEmpty() || mask == null || mask.isEmpty()) {
            return;
        }

        mDevInfo = new EthernetDevInfo();
        List<EthernetDevInfo> list = mEthManage.getDeviceNameList();
        for (EthernetDevInfo deviceInfo : list) {
            if ("eth0".equals(deviceInfo.getIfName())) {
                mDevInfo = deviceInfo;
                break;
            }
        }
        mDevInfo.setConnectMode(EthernetDevInfo.ETHERNET_CONN_MODE_MANUAL);
        //mDevInfo.setIfName("eth0");
        mDevInfo.setIpAddress(ip);
        mDevInfo.setNetMask(mask);
        mDevInfo.setDnsAddr(dns);
        mDevInfo.setGateWay(gateway);

        new AsyncTask<Void, Void, Void>() {
            protected void onPreExecute() {
            }

            @Override
            protected Void doInBackground(Void... unused) {
                try {
                    mEthManage.updateDevInfo(mDevInfo);
                    Thread.sleep(500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.e(TAG, "--------new:{" + mDevInfo.getIpAddress() + "}---------");
//                mDevInfo = null;
                return null;
            }

            protected void onProgressUpdate(Void... unused) {
            }

            protected void onPostExecute(Void unused) {
            }
        }.execute();
        /*  finally, we must enable ethernet whatever */
        enableEth();

        /*
        EthernetDevInfo oldInfo = EthernetManager.getInstance().getSavedConfig();
		Log.e("TAG", "--------old:{"+oldInfo.getIpAddress()+"}---------");

		oldInfo.setConnectMode(EthernetDevInfo.ETHERNET_CONN_MODE_MANUAL);
		oldInfo.setIpAddress(ip);
		oldInfo.setNetMask(mask);
		oldInfo.setDnsAddr(dns);
		oldInfo.setGateWay(gateway);
		try{
			EthernetManager.getInstance().updateDevInfo(oldInfo);
			Thread.sleep(500);
		}catch(Exception e){
			e.printStackTrace();
		}
		enableEth();

		EthernetDevInfo newInfo = EthernetManager.getInstance().getSavedConfig();
		Log.e("TAG", "--------new:{"+newInfo.getIpAddress()+"}---------");
		*/
    }

    /**
     * 设置ip获取方式为动态获取
     */
    public void setDynamicAcquisitionIP() {

    }

    private void enableEth() {
        try {
            EthernetManager.getInstance().setEnabled(false);
//            BackupManager.dataChanged("com.android.providers.settings");

            new Thread() {
                public void run() {
                    try {
                        EthernetManager.getInstance().setEnabled(true);
//                        BackupManager.dataChanged("com.android.providers.settings");
                        Thread.sleep(500);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                ;
            }.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取应用是否在运行
     */
    public boolean isRunning(String packageName) {
        boolean isRun = false;
        Process localProcess = null;
        try {
            localProcess = Runtime.getRuntime().exec(
                    "ps grep " + packageName + "\n");
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(localProcess.getInputStream()));
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                System.out.println("isrunning :"
                        + line.substring(line.indexOf("S") + 2, line.length()));
                if (line.substring(line.indexOf("S") + 2, line.length()).trim()
                        .equals(packageName)) {
                    isRun = true;
                    break;
                }
            }
            bufferedReader.close();
            localProcess.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (localProcess != null) {
                localProcess.destroy();
            }
        }
        return isRun;
    }


    private class EthPreference extends Preference {
        private EthernetDevInfo mEthConf;
        private int mState = -1;

        EthPreference(Context context, EthernetDevInfo ethConfigure) {
            super(context);
            setPersistent(false);
            setOrder(0);
//            setOnPreferenceClickListener(EthernetSettings.this);

            mEthConf = ethConfigure;
            update();
        }

        public EthernetDevInfo getConfigure() {
            return mEthConf;
        }

        public void update() {
            Context context = getContext();
            String mode = null;
            String hwaddr = null;

            if (mEthConf == null)
                return;

            setTitle(mEthConf.getIfName());
            if (mEthConf.getConnectMode() == EthernetDevInfo.ETHERNET_CONN_MODE_DHCP) {
                mode = "DHCP";
            } else {
                mode = "MANUAL";
            }
            hwaddr = mEthConf.getHwaddr().toUpperCase();
            setSummary("MAC: " + hwaddr + " -- IP Mode:" + mode);
            setKey(mEthConf.getIfName());
        }

        public void update(EthernetDevInfo info) {
            mEthConf = info;
            update();
        }

        @Override
        public int compareTo(Preference preference) {
            int result = -1;
            if (preference instanceof EthPreference) {
                EthPreference another = (EthPreference) preference;
                EthernetDevInfo otherInfo = another.getConfigure();
                if (mEthConf.getIfName() == otherInfo.getIfName())
                    result = 0;
                if (mEthConf.getHwaddr() == otherInfo.getHwaddr())
                    result = 0;
            }
            return result;
        }
    }

}
