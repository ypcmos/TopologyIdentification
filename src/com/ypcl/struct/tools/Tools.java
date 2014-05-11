package com.ypcl.struct.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Tools {
	public static String objectToUTF8String(Object o) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();  
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);  
        objectOutputStream.writeObject(o);    
        String arg = byteArrayOutputStream.toString("ISO-8859-1");  
        arg = java.net.URLEncoder.encode(arg, "UTF-8");    
        objectOutputStream.close();  
        byteArrayOutputStream.close();  
        return arg;
	}
	
	public static Object utf8StringToObject(String s) throws ClassNotFoundException, IOException {
		s = java.net.URLDecoder.decode(s, "UTF-8");
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(s.getBytes("ISO-8859-1"));  
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);   
        Object o = objectInputStream.readObject();
        objectInputStream.close();  
        byteArrayInputStream.close();
        return o;
	}
	
	public static String binary(int i) {
		assert(i >= 0);
		
		if (i == 0) {
			return "0";
		}
		
		StringBuilder ret = new StringBuilder();
		
		while (i != 0) {
			ret.insert(0, (i & 1) == 1 ? '1' : '0');
			i = i >> 1;
		}
		
		return ret.toString();
	}
	
	public static String fillZero(String s, int len) {		
		int l = len - s.length();
		
		if (l <= 0) {
			return s;
		}
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < l; i++) {
			sb.append('0');
		}
		return sb.append(s).toString();
	}
	
	static public void main(String[] args) {
		for (int i = 0; i <= 64; i++) {
			System.out.println(binary(i));
		}
	}
}
