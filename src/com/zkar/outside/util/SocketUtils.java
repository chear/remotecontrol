package com.zkar.outside.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class SocketUtils {
	private static String msg;

	/**
	 * 有返回值的socket，没有在线程里
	 */
	public static String send(String content, String ip, int port) {

		Socket socket = null;
		OutputStream os = null;
		InputStream is = null;
		BufferedReader in = null;

		try {
			socket = new Socket();
			SocketAddress address = new InetSocketAddress(ip, port);
			socket.connect(address,700);
			os = socket.getOutputStream();
			byte buf[] = content.getBytes();
			os.write(buf, 0, buf.length);
			socket.shutdownOutput();
			
			// 得到返回信息
			is = socket.getInputStream();
			in = new BufferedReader(new InputStreamReader(is));
			
//			if (is.available() > 0) {
				msg = in.readLine();
//			}
			socket.close();
			return msg;
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		} finally {
			try {
				if (os != null) {
					os.flush();
					os.close();
				}
				if (is != null)
					is.close();
				if (in != null)
					in.close();
				if (socket != null)
					socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 没有返回值Socket，在线程里面
	 */
	public static void sendMessageInThread(final String message,final String serverIp,final int portNumber){
		new Thread(){
			@Override
			public void run() {
				super.run();
				Socket socket = null;
				OutputStream os = null;
				InputStream is = null;
				BufferedReader in = null;
				try {
					socket = new Socket();
					// 生成SocketAddress对象，该对象代表网络当中的一个地址
					SocketAddress address = new InetSocketAddress(serverIp,portNumber);
					// 调用connect方法连接服务器端
					socket.connect(address,4000);
					// 连接成功之后，从Socket当中获取OutputStream对象，该对象用于写出数据
					os = socket.getOutputStream();
					byte buf[] = message.getBytes();
					os.write(buf, 0, buf.length);
					os.flush();
					socket.shutdownOutput();  
					
					// 接收服务器信息    
					/*is = socket.getInputStream();
					in = new BufferedReader(new InputStreamReader(is));
					is.wait(500);//等一下
					String returnmsg = "";
					if (is.available() > 0) {
						returnmsg = in.readLine();
					}
					System.out.println("开门sockeServer返回 :"+returnmsg);*/
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						if (os != null) {
							os.flush();
							os.close();
						}
						if (is != null)
							is.close();
						if (in != null)
							in.close();
						if (socket != null)
							socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}
}
