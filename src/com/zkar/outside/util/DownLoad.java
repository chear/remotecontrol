package com.zkar.outside.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.os.AsyncTask;
import android.util.Log;

public class DownLoad {
	
	public static Integer download(String _baseSavePath,URL param) {
		String downloadUrl = param.toString();
		String newFilename = _baseSavePath
				+ downloadUrl.substring(downloadUrl.lastIndexOf("/") + 1);

		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 25 * 1000);
		HttpConnectionParams.setSoTimeout(httpParams, 25 * 1000);

		DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);
		// GET
		HttpGet httpGet = new HttpGet(downloadUrl);
		try {
			HttpResponse response = httpClient.execute(httpGet);
			//HttpStatus.SC_OK=200
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				String length = response.getFirstHeader("Content-Length").getValue();
				if(length==null || Integer.parseInt(length)<=0){
					Log.i("autoInstall", "文件:" + newFilename + "检测此文件是空文件,不下载!");
					return 0;
				}
				
				File destDir = new File(_baseSavePath);
				if (!destDir.exists()) {
					destDir.mkdirs();
				}
				File file = new File(newFilename);
				// 如果目标文件已经存在，则删除。产生覆盖旧文件的效果
				if (file.exists()) {
					file.delete();
					Log.i("autoInstall", "文件:"+newFilename+"删除完成!");
				}
				try {
					file.createNewFile();
					file.setWritable(Boolean.TRUE);
				} catch (IOException e) {
					e.printStackTrace();
					Log.i("autoInstall", "文件:" + newFilename + "创建失败!");
					return -1;
				}
				Log.i("autoInstall", "文件:" + newFilename + "重新创建完成!");

				// 4K的数据缓冲
				byte[] bs = new byte[4096];
				// 读取到的数据长度
				int len;
				InputStream is = response.getEntity().getContent();
				int sum = 0;
				// 输出的文件流
				OutputStream os = new FileOutputStream(newFilename);
				// 开始读取
				while ((len = is.read(bs)) != -1) {
					os.write(bs, 0, len);
					sum += len;
				}
				// 完毕，关闭所有链接
				os.close();
				is.close();
				// 下载文件失败
				if (!(sum + "").equals(length)) {
					return 0;
				}
				Log.i("autoInstall", "文件:" + newFilename + "下载完成!");
			}else{
				Log.i("autoInstall", "文件:" + newFilename + "服务器上没找到此文件!");
				return 0;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
		return 1;
	}
}
