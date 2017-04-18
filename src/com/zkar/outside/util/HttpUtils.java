package com.zkar.outside.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

public class HttpUtils {

	public static String doGet(String url) {
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 3000);
		HttpConnectionParams.setSoTimeout(httpParams, 3000);

		DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);
		// GET
		HttpGet httpGet = new HttpGet(url);
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

	/**
	 * 返回值小于204就return ""
	 * @param url
	 * @param urlParameters
	 * @return
	 */
	public static String doPost(String url, List<NameValuePair> urlParameters) {
		try {
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(url); // add header
			post.setHeader("Content-Type",
					"application/x-www-form-urlencoded; charset=utf-8");
			post.setEntity(new UrlEncodedFormEntity(urlParameters, HTTP.UTF_8));
			// 请求超时
			client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 6*1000);
			// 读取超时
            client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 6*1000);
			            
			HttpResponse response = client.execute(post);
			int responseCode = response.getStatusLine().getStatusCode();
			System.out.println(url+" Response Code :"+ responseCode);
			if(responseCode<204){
				BufferedReader rd = new BufferedReader(new InputStreamReader(
						response.getEntity().getContent()));
				StringBuffer result = new StringBuffer();
				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}
				return result.toString();
			}else{
				return "";
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return "";
		}
	}
	
	/**
	 * post请求，返回值大于204，return "",timeOut秒超时
	 * @param url
	 * @param urlParameters
	 * @param timeOut
	 * @return
	 */
	public static String doPostLongTime(String url, List<NameValuePair> urlParameters,int timeOut) {
		try {
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(url); // add header
			post.addHeader("keep-alive", "true");
//			post.setHeader("Content-Type",
//					"application/x-www-form-urlencoded; charset=utf-8");
			post.setEntity(new UrlEncodedFormEntity(urlParameters, HTTP.UTF_8));
			// 请求超时
			client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, timeOut);
			// 读取超时
			client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, timeOut);
			
			HttpResponse response = client.execute(post);
			int responseCode = response.getStatusLine().getStatusCode();
			System.out.println("Response Code : "+ responseCode);
			if(responseCode<204){
				BufferedReader rd = new BufferedReader(new InputStreamReader(
						response.getEntity().getContent()));
				StringBuffer result = new StringBuffer();
				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}
				return result.toString();
			}else{
				return "";
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			return "";
		}
	}
}
