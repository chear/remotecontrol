package com.zkar.outside.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;

public class ReadAndWriteFileUtils {
	
	/**
	 * 写入此软件的“密钥”
	 * */
	public static void writeThePiskey() {
		File file = new File("/mnt/", "piskey.xml");
		String name = "true";
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
	 * 读出此软件的“密钥”
	 * */
	public static String readThePiskey() {
		StringBuffer sb = new StringBuffer();;
		try {
			File file = new File("/mnt/", "piskey.xml");
			BufferedReader br = new BufferedReader(new FileReader(file));
			String readline = "";
			while ((readline = br.readLine()) != null) {
				System.out.println("piskey.xmlreadline:" + readline);
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
}
