package com.ypcl.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class LoadData {
	private Map<String, List<Double>> data = new HashMap<String, List<Double>>();
	
	public LoadData parse(String path) throws IOException {
		BufferedReader br;
		br = new BufferedReader(new FileReader(path));
		String str = null;
		
		while ((str = br.readLine()) != null) {
			str = str.trim();
			List<Double> list = parseBody(str);
			String label = str.split(",")[0];
			if (list != null) {
				data.put(label, normalingByFirst(list));
			}
		}
		br.close();
		return this;
	}
	
	private static List<Double> parseBody(String data) {
		if (data.isEmpty()) {
			return null;
		}
		
		if (data.charAt(0) == '#') {
			return null;
		}
		
		String[] datas = data.split(",", -1);
		List<Double> vl = new LinkedList<Double>();
		for (int i = 1; i < datas.length; i++) {
			String v = datas[i].trim();
			if (v.isEmpty()) {
				return null;
			} else {
				vl.add(Double.parseDouble(v));
			}
		}
		return vl;
	}
	
	private static List<Double> normalingByFirst(List<Double> data) {
		List<Double> vl = new ArrayList<Double>(data.size());
		double v0 = data.get(0);
		for (double v : data) {
			vl.add(v / v0);
		}
		return vl;
	}
	
	public List<Double> getDateData(String label) {
		return data.get(label);
	}

}
