package com.ypcl.data;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.ypcl.identification.algorithm.simulator.ITopologySimulator;
import com.ypcl.ieee.StandardFile;
import com.ypcl.math.Constant;
import com.ypcl.struct.IntPair;
import com.ypcl.visibility.Picture;


public class FlowData {
	public Map<Integer, Double> pBuses = new TreeMap<Integer, Double>();
	public Map<Integer, Double> pBusesVoltage = new TreeMap<Integer, Double>();
	public Map<Integer, Double> pBusesQ = new TreeMap<Integer, Double>();
	public Map<IntPair, Double> pBranches = new TreeMap<IntPair, Double>();
	public Map<IntPair, Double> pBranchesQ = new TreeMap<IntPair, Double>();
	
	public Map<Integer, Double> pBeginBuses = new TreeMap<Integer, Double>();
	public Map<IntPair, Double> pBeginBranches = new TreeMap<IntPair, Double>();
	
	public Map<String, List<Double>> pr = new TreeMap<String, List<Double>>();
	public Map<Integer, List<Double>> rawBus = new TreeMap<Integer, List<Double>>();
	public Map<IntPair, List<Double>> rawBranch = new TreeMap<IntPair, List<Double>>();
	
	public Map<Integer, List<Double>> afterBus = new TreeMap<Integer, List<Double>>();
	public Map<IntPair, List<Double>> afterBranch = new TreeMap<IntPair, List<Double>>();
	public int times;
	public double d;
	public int index = 0, N = 499;
	public double step = 2 * Math.PI / N;
	public double badp, badmin;
	private Random rand = new Random();
	
	public FlowData(String text, double d, double baddp, double badmin) {
		parse(text);
		pBeginBuses.putAll(pBuses);
		pBeginBranches.putAll(pBranches);
		this.d = d;
		this.badp = baddp;
		this.badmin = badmin;
	}
	
	public FlowData parse(String text) {
		pBuses.clear();
		pBranches.clear();
		String [] ts = text.split("&");
		String [] ps = ts[0].split(";");
		times = Integer.parseInt(ps[0]);
		
		for (int i = 1; i < ps.length; i++) {
			String [] inner = ps[i].split(":");
			String [] ids = inner[0].split(",");
			
			if (ids.length == 1) {
				pBuses.put(Integer.parseInt(ids[0]), Double.parseDouble(inner[1]));
			} else {
				pBranches.put(new IntPair(Integer.parseInt(ids[0]),  Integer.parseInt(ids[1])), Double.parseDouble(inner[1]));
			}
		}
		ps = ts[1].split(";");
		for (int i = 1; i < ps.length; i++) {
			String [] inner = ps[i].split(":");
			pBusesVoltage.put(Integer.parseInt(inner[0]), Double.parseDouble(inner[1]));
		}
		ps = ts[2].split(";");
		for (int i = 1; i < ps.length; i++) {
			String [] inner = ps[i].split(":");
			String [] ids = inner[0].split(",");
			
			if (ids.length == 1) {
				pBusesQ.put(Integer.parseInt(ids[0]), Double.parseDouble(inner[1]));
			} else {
				pBranchesQ.put(new IntPair(Integer.parseInt(ids[0]),  Integer.parseInt(ids[1])), Double.parseDouble(inner[1]));
			}
		}
		return this;
	}
	
	public double gaussian(double u, double d) {
		return rand.nextGaussian() * Math.sqrt(d) + u;
	}
	
	public double cal(int id) {
		double v = pBeginBuses.get(id);
		return v / 2 * Math.sin(index * step) + v;
	}
	
	public double withError(double real) {
		if (Constant.isZero(real)) {
			return 0;
		}
		double r = Math.random();
		if (r < badp) {
			return real + (Math.random() > 0.5 ? 1 : -1) * (badmin + Math.random() * 100);
		} else {
			return real + gaussian(0, d);
		}
	}
	
	public Map<Integer, Double> generateBusPower() {
		Map<Integer, Double> ret = new TreeMap<Integer, Double>();
		
		for (Entry<Integer, Double> bus : pBuses.entrySet()) {
			double v = withError(bus.getValue());
			ret.put(bus.getKey(), v);
			List<Double> list = rawBus.get(bus.getKey());
			
			if (list == null) {
				list = new LinkedList<Double>();
				rawBus.put(bus.getKey(), list);
			}
			list.add(v);
		}
		return ret;
	}
	
	public Map<IntPair, Double> generateBranchPower() {
		Map<IntPair, Double> ret = new TreeMap<IntPair, Double>();
		
		for (Entry<IntPair, Double> branch : pBranches.entrySet()) {
			double v = withError(branch.getValue());
			ret.put(branch.getKey(), v);
			List<Double> list = rawBranch.get(branch.getKey());
			
			if (list == null) {
				list = new LinkedList<Double>();
				rawBranch.put(branch.getKey(), list);
			}
			list.add(v);
		}
		return ret;
	}
	
	public FlowData update(StandardFile sf) {
		for (Entry<Integer, Double> entry : pBeginBuses.entrySet()) {
			int id =  entry.getKey();
			String sid = "P";
			if (id < 10) {
				sid += id;
			} else {
				sid += id;
			}
			List<Double> d = pr.get(sid);
			
			if (d == null) {
				d = new LinkedList<Double>();
				pr.put(sid, d);
			}
			double c = cal(entry.getKey());
			d.add(c);
			sf.setBusPower(entry.getKey(), c);
		}
		return this;
	}
	
	public FlowData afterData(Map<Integer, Double> goodBus, Map<IntPair, Double> goodBranch) {		
		for (Entry<Integer, List<Double>> entry : rawBus.entrySet()) {
			int key = entry.getKey();
			Double v = goodBus.get(key);
			
			if (v == null) {
				v = entry.getValue().get(entry.getValue().size() - 1);
			}
			List<Double> list = afterBus.get(key);
			
			if (list == null) {
				list = new LinkedList<Double>();
				afterBus.put(key, list);
			}
			list.add(v);
		}
		
		for (Entry<IntPair, List<Double>> entry : rawBranch.entrySet()) {
			IntPair key = entry.getKey();
			Double v = goodBranch.get(key);
			
			if (v == null) {
				v = entry.getValue().get(entry.getValue().size() - 1);
			}
			List<Double> list = afterBranch.get(key);
			
			if (list == null) {
				list = new LinkedList<Double>();
				afterBranch.put(key, list);
			}
			list.add(v);
		}
		return this;
	}
	
	public FlowData afterData(ITopologySimulator sim) {
		return afterData(sim.getGoodBuses(), sim.getGoodBranches());
	}
	
	public FlowData drawBus(String path, int index) throws IOException {
		List<Double> raw = rawBus.get(index),
				after = afterBus.get(index);
		Map<String, List<Double>> fs = new TreeMap<String, List<Double>>();
		fs.put("P" + index + "生数据", raw);
		fs.put("P" + index + "熟数据", after);
		new Picture(fs).setNumberTickUnit(8).setSize(280, 200).show(path);
		return this;
	}
	
	public FlowData drawBranch(String path, IntPair index) throws IOException {
		List<Double> raw = rawBranch.get(index),
				after = afterBranch.get(index);
		Map<String, List<Double>> fs = new TreeMap<String, List<Double>>();
		fs.put("P" + index + "生数据", raw);
		fs.put("P" + index + "熟数据", after);
		new Picture(fs).show(path);
		return this;
	}
	
	public FlowData drawOriginBuses(String path) throws IOException {	
		pr.remove("P1");
		new Picture(pr).show(path);
		return this;
	}
}
