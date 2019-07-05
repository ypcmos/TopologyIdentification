package com.ypcl.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlowDataV2 extends FlowData {

	private Map<Integer, List<Double>> loadCurver = new HashMap<Integer, List<Double>>();
	public FlowDataV2(String text, double d, double baddp, double badmin) {
		super(text, d, baddp, badmin);
		LoadData ld = new LoadData();
		try {
			ld.parse("E:/data/1#Ö÷±ä_500kv.csv");
		} catch (IOException e) {
			e.printStackTrace();
		}
		String b = "2014-";
		String [] ds = {"01-01", "01-15", "01-18", "02-15", "02-28", "03-14", "05-01",
				"05-18", "06-25", "07-11", "08-30", "09-18", "11-10", "12-20"};
		int i = 0, size = 0, s = 10;;
		for (Integer id : pBuses.keySet()) {
			List<Double> list = ld.getDateData(b + ds[i]);
			if (list == null) {
				throw new RuntimeException("no date data.");
			}
			if (!check(list)) {
				throw new RuntimeException(ds[i]);
			}
			size = list.size();
			
			List<Double> l = new ArrayList<Double>(size / s);
			for (int j = 0; j < size; j++) {
				if (j % s == 0) {
					l.add(list.get(j));
				}
			}
			loadCurver.put(id, l);
			i++;
		}
		N = size / s - 1;
	}
	
	public boolean check(List<Double> list) {
		for (double v : list) {
			if (v < 0) {
				return false;
			}
		}
		return true;
	}
	
	public double cal(int id) {
		double v = pBeginBuses.get(id);
		return v * loadCurver.get(id).get(index);
	}

}
