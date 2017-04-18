package com.zkar.pis.remotecontrol;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public class ReadFilesName {
	public static LinkedHashMap ReadAllFilesName(String filePath) {
//		LinkedList list = new LinkedList();
		LinkedHashMap map = new LinkedHashMap();
		String fileName = "";
//		File tmp;
		File dir = new File(filePath);
		File file[] = dir.listFiles();
		//String[] file = dir.list();
		// 遍历文件夹下的所有文件
		for (int i = 0; i < file.length; i++) {
//			if (file[i].isDirectory()) {// 如果是文件夹，暂放集合中去
//				list.add(file[i]);
//			} else {
				// System.out.println(file[i].getAbsolutePath());
				fileName = file[i].getName();
				map.put(fileName + i, fileName);
//			}// end if
		}// end for
//			// 开始循环遍历，次级文件夹下的所有文件
//		while (!list.isEmpty()) {
//			tmp = (File) list.removeFirst();
//			if (tmp.isDirectory()) {
//				file = tmp.listFiles();
//				if (file == null)
//					continue;
//				for (int i = 0; i < file.length; i++) {
//					if (file[i].isDirectory()) {
//						list.add(file[i]);
//					} else {
//						// System.out.println(file[i].getAbsolutePath());
//						fileName = file[i].getName();
//						map.put(fileName + i, fileName);
//					}// end if
//				}// end for
//			} else {
//				fileName = tmp.getName();
//				map.put(fileName, fileName);
//			}
//		}// end while
		return map;
	}
}
