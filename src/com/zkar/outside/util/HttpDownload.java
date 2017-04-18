package com.zkar.outside.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.os.AsyncTask;

public class HttpDownload {

	public static String download(URL param) {
		String downloadUrl = param.toString();
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 25*1000);// ���ӳ�ʱʱ��25��
		HttpConnectionParams.setSoTimeout(httpParams, 25*1000); // �ȴ���ݳ�ʱʱ��

		DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);
		// GET
		HttpGet httpGet = new HttpGet(downloadUrl);
		StringBuffer sb = new StringBuffer();
		try {
			HttpResponse response = httpClient.execute(httpGet);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

				InputStream is = response.getEntity().getContent();

				BufferedReader br = new BufferedReader(new InputStreamReader(
						is, "UTF-8"));
				String line = "";
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
		return sb.toString();
	}
}
