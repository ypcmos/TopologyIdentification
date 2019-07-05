package com.ypcl.identification.algorithm.simulator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.ypcl.data.FlowData;
import com.ypcl.data.FlowDataV2;
import com.ypcl.identification.algorithm.TopologyIdentification2;
import com.ypcl.ieee.StandardFile;
import com.ypcl.math.Constant;
import com.ypcl.struct.IntPair;
import com.ypcl.util.TimeState;

public class Simulator implements ITopologySimulator {
	String path;
	protected Map<Integer, Double> pBuses = new TreeMap<Integer, Double>();
	protected Map<IntPair, Double> pBranches = new TreeMap<IntPair, Double>();
	
	protected Map<Integer, Double> pVirtualBuses = new TreeMap<Integer, Double>();
	protected Map<IntPair, Double> pVirtualBranches = new TreeMap<IntPair, Double>();
	
	int cannot = 0;
	protected Map<Integer, Double> pGoodBuses;
	protected Map<IntPair, Double> pGoodBranches;
	
	StandardFile sf = new StandardFile();
	public static final double d = 4, baddp = 0.01, badmin = 10;
	public Simulator(String input, String tmppath) throws IOException {
		sf.readFile(input);
		this.path = tmppath;
	}

	public Simulator() {
	}
	
	public int simulate() {
		TopologyIdentification2 ti = new TopologyIdentification2(this);
		cannot = ti.check();
		pGoodBuses = ti.getGoodBuses();
		pGoodBranches = ti.getGoodBranches();
		return cannot;
	}
	
	protected int output2(BufferedWriter br, StringBuilder sb, String label, Double v1, double v2, double r) {
		int bad = 0;
		double error;
		
		String isAlert = "未辨识";
		if (v1 != null) {		
			isAlert = "";
		} else {
			v1 = v2;
		}
		
		if (Constant.isZero(r)) {
			if (Constant.isZero(v1)) {
				error = 0;
			} else {
				error = 1;
			}
		} else {
			error = Math.abs(v1 - r);
		}
		if (error >= 6 * Math.sqrt(d)) {
			sb.append("<tr style=\"background-color: red\">");
			bad++;
		} else {
			sb.append("<tr style=\"background-color: #7EC0EE\">");
		}
		double fe = v2 - r;
		String in = "";
		if (Math.abs(fe) > 10) {
			in = "<td style='background-color: green'>";
		} else {
			in = "<td>";
		}
		in += String.format("%.2f", fe) + "</td>";
		sb.append("<td>" + label + "</td>" + "<td>" + isAlert + "</td>" + "<td>" + v1 + "</td>" + "<td>" + v2 + "</td>" + "<td>" + String.format("%.2f", error) + "</td>" + "<td>" + r + "</td>" + in + "</tr>");
		return bad;
	}
	
	public int toHtmlFile2(String path) throws IOException {
		int bad = 0;
		BufferedWriter br = new BufferedWriter(new FileWriter(path, true));
		br.write("无法辨识的数目:" + cannot + "\r\n");
		
		StringBuilder sb = new StringBuilder("<table><tr style=\"background-color: #00CED1\"><th>标志</th><th>是否辨识</th><th>修正值</th><th>脏值</th><th>偏差</th><th>真实值</th><th>坏数据</th></tr>");
		for (Entry<Integer, Double> entry : pBuses.entrySet()) {
			Double v1 = pGoodBuses.get(entry.getKey());
					
			double v2 = pVirtualBuses.get(entry.getKey());
			double r = entry.getValue();
			bad += output2(br, sb, entry.getKey().toString(), v1, v2, r);
		}
		
		for (Entry<IntPair, Double> entry : pBranches.entrySet()) {
			Double v1 = pGoodBranches.get(entry.getKey());
			double v2 = pVirtualBranches.get(entry.getKey());
			double r = entry.getValue();
			
			bad += output2(br, sb, entry.getKey().toString(), v1, v2, r);
		}
		sb.append("</table>");
		br.write(sb.toString());
		br.close();
		return bad;
	}
	
	public void writeInjectPower(String path, FlowData data) throws IOException {
		BufferedWriter br = new BufferedWriter(new FileWriter(path, true));
		StringBuilder sb = new StringBuilder();
		Map<String, List<Double>> pr = data.pr;
		for (Entry<String, List<Double>> entry : pr.entrySet()) {
			List<Double> list = entry.getValue();
			sb.append(entry.getKey() + ":");
			for (double v : list) {
				sb.append(v + ",");
			}
			sb.append("\r\n");
		}
		br.write(sb.toString());
		br.close();
	}
	public Simulator work(String output) throws IOException {		
		File file = new File(output);
	    if (file.isFile() && file.exists()) {  
	        file.delete();  
	    }  
		int count = 0, bad = 0;
		sf.writeFlowInput(path);
		FlowDataV2 data = new FlowDataV2(runFlow(path, 0, 100), d, baddp, badmin);
		int iterateTimes = data.N;
		while (data.index++ < iterateTimes) {
			pVirtualBuses = data.generateBusPower();
			pVirtualBranches = data.generateBranchPower();
			pBuses = data.pBuses;
			pBranches = data.pBranches;
			TimeState s = new TimeState();
			if (simulate() != 0)
			{
				count++;
			}
		//	System.out.println("Time:" + s.past());
			data.afterData(this);
			bad += toHtmlFile2(output);
			data.update(sf);
			sf.writeFlowInput(path);
			data.parse(runFlow(path, 0, 100));
		}
		System.out.println("Count:" + count);
		System.out.println("Bad:" + bad);
		//writeInjectPower("E:/graduate_data/test_4_1/ds.dat" , data);
		//data.drawOriginBuses("E:/graduate_data/test_4_1/ds.png");
		data.drawBus("E:/graduate_data/test_4_1/ds_pi.png", 1);
		data.drawBranch("E:/graduate_data/test_4_1/ds_pij.png", new IntPair(1, 2));
		data.drawBranch("E:/graduate_data/test_4_1/ds_pji.png", new IntPair(2, 1));
		return this;
	}
	public static native String runFlow(String path, int type, int times);
	
	public static void main(String[] args) throws IOException {
		System.loadLibrary("PowerFlow");
		Simulator s = new Simulator("E:/graduate_data/test_4_1/014IEEE.DAT" ,"E:/graduate_data/test_4_1/tmp/tmp.txt");
		s.work("E:/graduate_data/test_4_1/result.html");
	}

	
	@Override
	public Map<Integer, Double> getGoodBuses() {
		return pGoodBuses;
	}

	@Override
	public Map<IntPair, Double> getGoodBranches() {
		return pGoodBranches;
	}
	
	@Override
	public Map<Integer, Double> getBusPowersMap() {
		return pVirtualBuses;
	}

	@Override
	public Map<IntPair, Double> getBranchPowersMap() {
		return pVirtualBranches;
	}
}


