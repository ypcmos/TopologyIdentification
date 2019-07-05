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
}
