package com.zkar.outside.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XmlUtils {
    public static String ReadValue(String content, String paraName)
    {	     
		try {
	    	ByteArrayInputStream encXML = new ByteArrayInputStream(content.getBytes("UTF8"));
	    	  
		     InputSource inputSource = new InputSource(encXML); 
		 	 XPath xpath = XPathFactory.newInstance().newXPath();
			XPathExpression expression = xpath.compile("//" + paraName);
			Node node = (Node)expression.evaluate(inputSource,XPathConstants.NODE);
		    return node.getTextContent();
		}  catch(Exception e){
			e.printStackTrace();
		}
		return null;
    }
    
    public static ArrayList<String> ReadValues(String content, String paraName)
    {    	 

    	ArrayList<String> list = new ArrayList<String>();
		try {
			ByteArrayInputStream encXML = new ByteArrayInputStream(content.getBytes("UTF8"));
			InputSource inputSource = new InputSource(encXML);  
			XPath xpath = XPathFactory.newInstance().newXPath();
			 XPathExpression expression = xpath.compile("//" + paraName);
			 NodeList nodeList = (NodeList)expression.evaluate(inputSource,XPathConstants.NODESET);
		     for(int i = 0; i < nodeList.getLength(); i++){
		    	 list.add(nodeList.item(i).getTextContent());
 	        }
		     return list;
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		return null;
    }
    
    public static String ReadFile(String path){
		File file=new File(path);
		
		StringBuffer contents = new StringBuffer();
		BufferedReader reader = null;
		try {
			String count;
			FileInputStream stream = new FileInputStream(file);
			InputStreamReader isr = new InputStreamReader(stream, "UTF-8");
			reader = new BufferedReader(isr);
			
			while((count=reader.readLine())!=null){
				contents.append(count);
			}
			stream.close();
			isr.close(); 
			return contents.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return contents.toString();
		
	}
}
