package com.zkar.monitoringfacilities;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class VideoThread extends Thread {
    private static final int FRAME_RATE = 15;
    private static final int FRAME_SIZE = 1024 * 40;
    private SharedPreferences settings;
    private MediaRecorder mMediaRecorder;
    private LocalSocket receiver;
    private LocalSocket sender;
    private LocalServerSocket lss;
    // 显示视频的控件
    private SurfaceView mSurfaceView;
    // 用来显示视频的一个接口，我靠不用还不行,也就是说用mediarecorder录制视频还得给个界面看,想偷偷录视频的同学可以考虑别的办法。。嗯需要实现这个接口的Callback接口
    private SurfaceHolder mSurfaceHolder;
    private File mRecVideoFile;
    // boolean bIfNativeORRemote;
    private Thread recordThread;
    private String serverAddr;
    private DatagramSocket client = null;
    private DataOutputStream stream = null;

    public VideoThread(SharedPreferences settings, SurfaceView surfaceview,
                       SurfaceHolder surfaceHolder, String serverIp) {
        // TODO 自动生成的构造函数存根
        serverAddr = serverIp;
        this.settings = settings;
        this.mSurfaceView = surfaceview;
        this.mSurfaceHolder = surfaceHolder;
        Log.i("ceshi", "VideoThread:" + "构造方法");
        initLocalSocket();
        initializeVideo();
    }

    @Override
    public void run() {
        // TODO 自动生成的方法存根
        byte[] buffer = new byte[FRAME_SIZE * 64];
        int num, number = 0;
        InputStream fis = null;
        try {
            fis = receiver.getInputStream();
        } catch (Exception e) {
            return;
        }
        SharedPreferences.Editor editor = settings.edit();
        String ppsString = settings.getString("pps", "");
        String spsString = settings.getString("sps", "");
        int mdatPosition = settings.getInt("mdatPosition", -1);
        byte[] sps = null;
        byte[] pps = null;
        number = 0;
        if (ppsString == "" || spsString == "" || mdatPosition == -1) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            releaseRecorder();

            try {
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }

            while (true) {
                try {
                    num = fis.read(buffer, number, FRAME_SIZE);
                    number += num;
                    if (num < FRAME_SIZE) {
                        break;
                    }
                } catch (Exception e) {
                    break;
                }
            }
            for (int ix = 0; ix < buffer.length; ix++) {
                if (buffer[ix] == 0x6d && buffer[ix + 1] == 0x64
                        && buffer[ix + 2] == 0x61 && buffer[ix + 3] == 0x74) {
                    // 找到avcC，则记录avcRecord起始位置，然后退出循环。
                    mdatPosition = ix + 4;
                    break;
                }
            }
            if (mdatPosition != 0) {
                editor.putInt("mdatPosition", mdatPosition);
            }
            // avcC的起始位置
            int avcRecord = 0;
            for (int ix = 0; ix < buffer.length; ix++) {
                if (buffer[ix] == 0x61 && buffer[ix + 1] == 0x76
                        && buffer[ix + 2] == 0x63 && buffer[ix + 3] == 0x43) {
                    // 找到avcC，则记录avcRecord起始位置，然后退出循环。
                    avcRecord = ix + 4;
                    break;
                }
            }
            if (0 != avcRecord) {
                int spsStartPos = avcRecord + 6;
                byte[] spsbt = new byte[]{buffer[spsStartPos],
                        buffer[spsStartPos + 1]};
                int spsLength = bytes2Int(spsbt);
                sps = new byte[spsLength];

                spsStartPos += 2;
                System.arraycopy(buffer, spsStartPos, sps, 0, spsLength);

                int ppsStartPos = spsStartPos + spsLength + 1;
                byte[] ppsbt = new byte[]{buffer[ppsStartPos],
                        buffer[ppsStartPos + 1]};
                int ppsLength = bytes2Int(ppsbt);
                pps = new byte[ppsLength];
                ppsStartPos += 2;
                System.arraycopy(buffer, ppsStartPos, pps, 0, ppsLength);

                editor.putString("pps", Base64.encodeToString(pps, 0,
                        ppsLength, Base64.NO_WRAP));
                editor.putString("sps", Base64.encodeToString(sps, 0,
                        spsLength, Base64.NO_WRAP));
            }

            editor.commit();
            number = 0;
            // initializeVideo();
        } else {
            pps = Base64.decode(ppsString, Base64.NO_WRAP);
            sps = Base64.decode(spsString, Base64.NO_WRAP);
        }
        initSocket();
        DataInputStream dis = new DataInputStream(fis);

        // 跳过的字节数
        try {
            dis.read(buffer, 0, mdatPosition);
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] h264head = {0, 0, 0, 1};
        byte[] info = null;
        if (sps != null && pps != null) {
            info = new byte[h264head.length * 2 + sps.length + pps.length];

            System.arraycopy(h264head, 0, info, 0, h264head.length);
            System.arraycopy(sps, 0, info, h264head.length, sps.length);
            System.arraycopy(h264head, 0, info, h264head.length + sps.length,
                    h264head.length);
            System.arraycopy(pps, 0, info, h264head.length * 2 + sps.length,
                    h264head.length);

            sendDataToServer(info, 0, info.length);
        }

        byte[] sendBuffer = new byte[FRAME_SIZE];

        int numCount = 0;

        while (true) {
            try {

                number = 0;

                //读取每场的长度
                int h264length = dis.readInt();
                System.arraycopy(h264head, 0, sendBuffer, number, h264head.length);

                boolean first = true;
                while (number < h264length) {
                    int lost = h264length - number;

                    num = fis.read(buffer, 0, FRAME_SIZE < lost ? FRAME_SIZE : lost);
                    number += num;

                    System.arraycopy(buffer, 0, sendBuffer, first ? h264head.length : number + h264head.length, num);
                    first = false;
                }
                if (numCount >= 1) {
                    sendDataToServer(sendBuffer, 0, number + numCount * h264head.length);
                    numCount = 0;
                }

                numCount++;
            } catch (Exception e) {
                break;
            }
        }
    }

    private int bytes2Int(byte[] bt) {
        int ret = bt[0];
        ret <<= 8;
        ret |= bt[1];
        return ret;
    }

    private void initSocket() {
        if (serverAddr == null || serverAddr == "")
            return;

        // 与服务端建立连接
        try {
            client = new DatagramSocket();
            InetAddress addr = InetAddress.getByName(serverAddr);
            client.connect(addr, 9085);
            Log.i("video", "连接服务器:" + serverAddr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendDataToServer(final byte[] buffer, final int offset,
                                  final int length) {
        if (buffer == null || client == null)
            return;
        // 建立连接后就可以往服务端写数据了
        try {
            DatagramPacket sendPacket = new DatagramPacket(buffer, length);

            client.send(sendPacket);
            Log.i("video", "发送数据:" + length);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化流输出
     */
    private void initLocalSocket() {
        receiver = new LocalSocket();
        try {
            lss = new LocalServerSocket("H264");
            receiver.connect(new LocalSocketAddress("H264"));
            receiver.setReceiveBufferSize(500000);
            receiver.setSendBufferSize(500000);
            sender = lss.accept();
            sender.setReceiveBufferSize(500000);
            Log.i("ceshi", "VideoThread:" + "initLocalSocket");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化视频
     *
     * @return
     */
    private boolean initializeVideo() {
        try {
            // 〇state: Initial 实例化MediaRecorder对象
            if (mSurfaceView == null) {
                return false;
            }
            if (mMediaRecorder == null)
                mMediaRecorder = new MediaRecorder();
            else
                mMediaRecorder.reset();

            // 〇state: Initial=>Initialized
            // set audio source as Microphone, video source as camera
            // specified before settings Recording-parameters or encoders，called only before setOutputFormat
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            //mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            // 〇state: Initialized=>DataSourceConfigured
            // 设置錄製視頻输出格式
            //     THREE_GPP:    3gp格式，H263视频ARM音频编码
            //    MPEG-4:        MPEG4 media file format
            //    RAW_AMR:    只支持音频且音频编码要求为AMR_NB
            //    AMR_NB:
            //    ARM_MB:
            //    Default:
            // 3gp or mp4
            //Android支持的音频编解码仅为AMR_NB；支持的视频编解码仅为H263，H264只支持解码；支持对JPEG编解码；输出格式仅支持.3gp和.mp4
            // mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            // 设置視頻/音频文件的编码：AAC/AMR_NB/AMR_MB/Default
            //    video: H.263, MP4-SP, or H.264
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
            //mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);

            // audio: AMR-NB
            //mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            // 设置视频录制的分辨率。必须放在设置编码和格式的后面，否则报错
            mMediaRecorder.setVideoSize(640, 480);

            mMediaRecorder.setAudioEncodingBitRate(1024 * 64);

            // mMediaRecorder.setVideoSize(1024, 768);

            // 设置录制的视频帧率。必须放在设置编码和格式的后面，否则报错
            mMediaRecorder.setVideoFrameRate(FRAME_RATE);

            // 预览
            mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

            // 设置输出文件方式： 直接本地存储   or LocalSocket远程输出
//            if(bIfNativeORRemote)    //Native
//            {
//                mMediaRecorder.setOutputFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/test.mp4");    //called after set**Source before prepare
//            }
//            else    //Remote
//            {
//                // 设置以流方式输出
//                mMediaRecorder.setOutputFile(sender.getFileDescriptor());
//            }

            // 设置以流方式输出
            mMediaRecorder.setOutputFile(sender.getFileDescriptor());


            //
            mMediaRecorder.setMaxDuration(0);//called after setOutputFile before prepare,if zero or negation,disables the limit
            mMediaRecorder.setMaxFileSize(0);//called after setOutputFile before prepare,if zero or negation,disables the limit
            try {
                // 〇state: DataSourceConfigured => prepared
                mMediaRecorder.prepare();
                // 〇state: prepared => recording
                mMediaRecorder.start();
            } catch (Exception e) {
                releaseLocalSocket();
                releaseRecorder();
                e.printStackTrace();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void releaseLocalSocket() {
        try {
            lss.close();
            receiver.close();
            sender.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void releaseRecorder() {
        try {
            if (mMediaRecorder != null) {
                boolean isStoped = false;
                try {
                    // 停止录制
                    mMediaRecorder.stop();
                    isStoped = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    if (!isStoped) {
                        // 停止录制
                        mMediaRecorder.stop();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                mMediaRecorder.reset();

                // 释放资源
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
