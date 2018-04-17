package com.zkar.monitoringfacilities;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.apache.http.conn.util.InetAddressUtils;
import android.content.ContentResolver;
import android.content.Context;
//import android.net.ethernet.EthernetDevInfo;
//import android.net.ethernet.EthernetManager;
import android.net.ethernet.EthernetManager;
import android.os.AsyncTask;
import android.os.SystemProperties;
import android.preference.Preference;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.util.Log;
import com.zkar.pis.remotecontrol.MyService;
import com.zkar.pis.remotecontrol.R;
import static com.zkar.outside.util.PackageUtils.TAG;

public class SetUpIpUtils {

    private final String TAG = "RemoteControl_SetupIP";

    private Context myContext;
    private static SetUpIpUtils setUpIpUtils;
//    private EthernetDevInfo mDevInfo;

    public final static boolean DEBUG = false;

    /**
     * Whether to use static IP and other static network attributes.
     * @hide
     * Set to 1 for true and 0 for false.
     */
    public static final String ETHERNET_USE_STATIC_IP = "ethernet_use_static_ip";

    /**
     * The static IP address.
     * @hide
     * Example: "192.168.1.51"
     */
    public static final String ETHERNET_STATIC_IP = "ethernet_static_ip";

    /**
     * If using static IP, the gateway's IP address.
     * @hide
     * Example: "192.168.1.1"
     */
    public static final String ETHERNET_STATIC_GATEWAY = "ethernet_static_gateway";

    /**
     * If using static IP, the net mask.
     * @hide
     * Example: "255.255.255.0"
     */
    public static final String ETHERNET_STATIC_NETMASK = "ethernet_static_netmask";

    /**
     * If using static IP, the primary DNS's IP address.
     * @hide
     * Example: "192.168.1.1"
     */
    public static final String ETHERNET_STATIC_DNS1 = "ethernet_static_dns1";

    /**
     * If using static IP, the secondary DNS's IP address.
     * @hide
     * Example: "192.168.1.2"
     */
    public static final String ETHERNET_STATIC_DNS2 = "ethernet_static_dns2";

    public static final String ETHERNET_ON = "ethernet_on";

    public static final String ETHERNET_SERVICE = "ethernet";

    private String nullIpInfo = "0.0.0.0";
//    //TODO htt
    private final EthernetManager mEthManager = null;

    private SetUpIpUtils() {
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
//        Process localProcess = null;
        try {
//            localProcess = Runtime.getRuntime().exec("getprop net.dns1");
//            BufferedReader bufferedReader = new BufferedReader(
//                    new InputStreamReader(localProcess.getInputStream()));
//            localDns = bufferedReader.readLine();
//            // System.out.println("localDns :"+ localDns);
//            bufferedReader.close();

            if(isUsingStaticIp()) {
                //chear get from Static dns
                localDns = Settings.System.getString(MyService.CONTEXT.getContentResolver(), ETHERNET_STATIC_DNS1);
            } else {
                //chear get from Dhcp
                String tempIpInfo = SystemProperties.get("dhcp.eth0.dns1");
                if ((tempIpInfo != null) && (!tempIpInfo.equals("")) ){
                    localDns = tempIpInfo;
                } else {
                    localDns = nullIpInfo;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
//            if (localProcess != null) {
//                localProcess.destroy();
//            }
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

    private boolean isUsingStaticIp() {
        if(MyService.CONTEXT == null)
            System.out.println("Myservice CONTENT its null");
        return Settings.System.getInt(MyService.CONTEXT.getContentResolver(), ETHERNET_USE_STATIC_IP, 0) == 1 ? true : false;
    }

    /**
     * 获取mac地址,ip,子网掩码,,返回数组
     */
    public String[] getIpMac() {
        String[] str = new String[3];
//        Process localProcess = null;
        String line = "";
        try {
//            localProcess = Runtime.getRuntime().exec("busybox ifconfig > //data//ifconfig.txt");
//            BufferedReader bufferedReader = new BufferedReader(
//                    new InputStreamReader(localProcess.getInputStream()));

            // get Mac address from sysdev
            BufferedReader bufferedReader = new BufferedReader(new FileReader("sys/class/net/eth0/address"));
            try {
                // get mac address
                line = bufferedReader.readLine();
                System.out.println("getIpMac line  :" + line);
                str[0] = line.toUpperCase();
                System.out.println("chear: mac = " + str[0]);

                // get broadcost ip
                line = getIp();
                str[1] = line;
                System.out.println("chear: broadcast = " + str[1]);

                // get net mask
                str[2] = getMask();
                System.out.println("chear: mask = " + str[2]);

            }catch (Exception e) {
                Log.e(TAG, "open sys/class/net/eth0/address failed : " + e);
            }finally {
                try {
                    if (bufferedReader != null)
                        bufferedReader.close();
                } catch (IOException e) {
                    Log.e(TAG, "close sys/class/net/eth0/address failed : " + e);
                }
            }



//            String line = "";
//            line = bufferedReader.readLine();
//            System.out.println("getIpMac line  :" + line);
//            // mac
//            str[0] = line.substring(line.indexOf("HWaddr") + 7,
//                    line.length() - 2);
//            // System.out.println("str[0] :"+str[0]);
//            // System.out.println("line :"+line);
//            line = bufferedReader.readLine();
//            // ip
//            // System.out.println("line :"+line);
//            str[1] = line.trim()
//                    .substring(10, line.trim().indexOf("Bcast") - 2);
//            // System.out.println("str[1] :"+str[1]);
//            // 子网掩码
//            str[2] = line.substring(line.indexOf("Mask") + 5, line.length());
//            // System.out.println("line :"+line);
//            // System.out.println("str[1] :"+str[1]+'\n'+"str[2] :"+str[2]);
//            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
//            if (localProcess != null) {
//                localProcess.destroy();
//            }
        }
        return str;
    }

    /**
     * 获取子网掩码
     */
    public String getMask() {
        String mask = "";
//        Process localProcess = null;
        try {
//            localProcess = Runtime.getRuntime().exec("busybox ifconfig");
//            BufferedReader bufferedReader = new BufferedReader(
//                    new InputStreamReader(localProcess.getInputStream()));
//            String line = "";
//            line = bufferedReader.readLine();
//            line = bufferedReader.readLine();
//            // 子网掩码
//            mask = line.substring(line.indexOf("Mask") + 5, line.length());
//            bufferedReader.close();

            //chear: this used in android 4.4.4 rk3288
            if(isUsingStaticIp()) {
                //chear get eth Info  From Static IP ,
                mask = Settings.System.getString(MyService.CONTEXT.getContentResolver(), ETHERNET_STATIC_NETMASK);
            } else {
                //chear get eth from Dhcp
                String tempIpInfo = SystemProperties.get("dhcp.eth0.mask");
                if ((tempIpInfo != null) && (!tempIpInfo.equals("")) ){
                    mask = tempIpInfo;
                } else {
                    mask = nullIpInfo;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
//            if (localProcess != null) {
//                localProcess.destroy();
//            }
        }
        return mask;
    }

    /**
     * 修改ip
     */
    public String getIp() {
        String ip = "";
//        Process localProcess = null;
        try {
//            localProcess = Runtime.getRuntime().exec("busybox ifconfig");
//            BufferedReader bufferedReader = new BufferedReader(
//                    new InputStreamReader(localProcess.getInputStream()));
//            String line = "";
//            line = bufferedReader.readLine();
//            line = bufferedReader.readLine();
//            ip = line.trim().substring(10, line.trim().indexOf("Bcast") - 2);
//            bufferedReader.close();


            //chear: this used in android 4.4.4 rk3288
            if(isUsingStaticIp()) {
                //chear get eth Info  From Static IP ,
                ip = Settings.System.getString(MyService.CONTEXT.getContentResolver(), ETHERNET_STATIC_IP);
            } else {
                //chear get eth from Dhcp
                String tempIpInfo = SystemProperties.get("dhcp.eth0.ipaddress");
                if ((tempIpInfo != null) && (!tempIpInfo.equals("")) ){
                    ip = tempIpInfo;
                } else {
                    ip = nullIpInfo;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
//            if (localProcess != null) {
//                localProcess.destroy();
//            }
        }
        return ip;
    }

    /**
     * update static up to 3188 plutform
     */
    public void editEtherByContentResolver(ContentResolver contentResolver,String ip, String dns, String gateway, String mask ){
        final ContentResolver asycContent = contentResolver;
        final String asycIP = ip;
        final String asycMask = mask;
        final String asycDNS = dns;
        final String asycGateWay = gateway;
        new AsyncTask<Void, Void, Void>() {
            protected void onPreExecute() {
            }
            @Override
            protected Void doInBackground(Void... unused) {
                try {
                    //TODO : setting the static ip
                    android.provider.Settings.System.putString(asycContent, ETHERNET_STATIC_IP, asycIP);

                    //TODO : setting the mask
                    android.provider.Settings.System.putString(asycContent, ETHERNET_STATIC_NETMASK, asycMask);

                    //TODO : setting the dns
                    android.provider.Settings.System.putString(asycContent, ETHERNET_STATIC_DNS1, asycDNS);

                    //TODO : setting the gateway
                    android.provider.Settings.System.putString(asycContent, ETHERNET_STATIC_GATEWAY, asycGateWay);

                    //TODO : use the static ip address for settings
                    android.provider.Settings.System.putInt(asycContent, ETHERNET_USE_STATIC_IP, 1);

                    // disable ethernet
                    boolean enable = Secure.getInt(asycContent, ETHERNET_ON, 1) == 1;
                    Log.i(TAG,"notify Secure.ETHERNET_ON changed. enable = " + enable);
                    if(enable) {
                        Log.i(TAG, "first disable");
                        Secure.putInt(asycContent, ETHERNET_ON, 0);
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                        }
                        Log.i(TAG, "second enable");
                        Secure.putInt(asycContent, ETHERNET_ON, 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
            protected void onProgressUpdate(Void... unused) {
            }
            protected void onPostExecute(Void unused) {
            }
        }.execute();
    }

    /**
     * 设置ip获取方式为动态获取
     */
    public void setDynamicAcquisitionIP() {

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

}
