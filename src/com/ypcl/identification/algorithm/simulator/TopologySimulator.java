package com.ypcl.identification.algorithm.simulator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ypcl.identification.algorithm.TopologyIdentification;
import com.ypcl.math.Constant;
import com.ypcl.struct.IntPair;

public class TopologySimulator implements ITopologySimulator{
	protected Map<Integer, Double> pBuses = new TreeMap<Integer, Double>();
	protected Map<IntPair, Double> pBranches = new HashMap<IntPair, Double>();
	
	protected Map<Integer, Double> pVirtualBuses = new TreeMap<Integer, Double>();
	protected Map<IntPair, Double> pVirtualBranches = new HashMap<IntPair, Double>();
	
	int cannot = 0;
	protected Map<Integer, Double> pGoodBuses;
	protected Map<IntPair, Double> pGoodBranches;
	
	public TopologySimulator(String path) throws IOException {
		fromFile(path, pBuses, pBranches);
	}
	
	private static void fromFile(String path, Map<Integer, Double> buses, Map<IntPair, Double> branches) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(path));
		String str = null;
		Pattern buspat = Pattern.compile("^(\\d+):(-?\\d*\\.?\\d*)$"),
				branchpat = Pattern.compile("^S\\[(\\d+)\\]\\[(\\d+)\\]\\s*=\\s*(-?\\d*\\.?\\d*)$");  
		
		while ((str = br.readLine()) != null) {
			if (!str.isEmpty()) {
				Matcher mat = buspat.matcher(str);
				if (mat.find()) {
					int busid = Integer.parseInt(mat.group(1));
					double busp = Double.parseDouble(mat.group(2));
					buses.put(busid, busp);			
				} else {
					mat = branchpat.matcher(str);
					if (mat.find()) {
						int i = Integer.parseInt(mat.group(1)), j = Integer.parseInt(mat.group(2));
						double v = Double.parseDouble(mat.group(3));								
						IntPair pair = new IntPair(i, j);
						branches.put(pair, v);
					} else {
						continue;
					}
				}
			}
		}
		br.close();
	}

	protected void output(BufferedWriter br, StringBuilder sb, String label, Double v1, double v2, double r) {
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
			error = Math.abs(v1 - r) / Math.abs(r);
		}
		if (error >= 0.05) {
			sb.append("<tr style=\"background-color: red\">");
		} else {
			sb.append("<tr style=\"background-color: #7EC0EE\">");
		}
		sb.append("<td>" + label + "</td>" + "<td>" + isAlert + "</td>" + "<td>" + v1 + "</td>" + "<td>" + v2 + "</td>" + "<td>" + String.format("%.2f", error * 100) + "</td>" + "<td>" + r + "</td></tr>");
	}
	
	public void toHtmlFile(String path) throws IOException {
		BufferedWriter br = new BufferedWriter(new FileWriter(path, true));
		br.write("无法辨识的数目:" + cannot + "\r\n");
		
		StringBuilder sb = new StringBuilder("<table><tr style=\"background-color: #00CED1\"><th>标志</th><th>是否辨识</th><th>修正值</th><th>脏值</th><th>误差(%)</th><th>真实值</th></tr>");
		for (Entry<Integer, Double> entry : pBuses.entrySet()) {
			Double v1 = pGoodBuses.get(entry.getKey());
					
			double v2 = pVirtualBuses.get(entry.getKey());
			double r = entry.getValue();
			output(br, sb, entry.getKey().toString(), v1, v2, r);
		}
		
		for (Entry<IntPair, Double> entry : pBranches.entrySet()) {
			Double v1 = pGoodBranches.get(entry.getKey());
			double v2 = pVirtualBranches.get(entry.getKey());
			double r = entry.getValue();
			
			output(br, sb, entry.getKey().toString(), v1, v2, r);
		}
		sb.append("</table>");
		br.write(sb.toString());
		br.close();
	}
	
	public int simulate() {
		TopologyIdentification ti = new TopologyIdentification(this);
		cannot = ti.check();
		pGoodBuses = ti.getGoodBuses();
		pGoodBranches = ti.getGoodBranches();
		return cannot;
	}
	
	public void createVirtualDataByFile(String path) throws IOException {
		fromFile(path, pVirtualBuses, pVirtualBranches);
	}
	
	public void createVirtualDataByRandom() throws IOException {
		double r = 0;
		for (Entry<Integer, Double> entry : pBuses.entrySet()) {
			if (Constant.isZero(entry.getValue())) {
				pVirtualBuses.put(entry.getKey(), 0.0);
				continue;
			}
			
			r = Math.random();
			if (r < 0.01) {
				pVirtualBuses.put(entry.getKey(), entry.getValue() * 1.1);
				System.out.println("Error:" + entry.getKey());
			} else {
				pVirtualBuses.put(entry.getKey(), entry.getValue() * (1 + 0.0001 * r * ( r > 0.5 ? -1 : 1)));
			}
		}
		
		for (Entry<IntPair, Double> entry : pBranches.entrySet()) {
			r = Math.random();
			if (r < 0.01) {
				pVirtualBranches.put(entry.getKey(), entry.getValue() * 1.1);
				System.out.println("Error:" + entry.getKey());
			} else {
				pVirtualBranches.put(entry.getKey(), entry.getValue() * (1 + 0.0001 * r * ( r > 0.5 ? -1 : 1)));
			}
		}
	}
	
	@Override
	public Map<Integer, Double> getBusPowesMap() {
		return pVirtualBuses;
	}

	@Override
	public Map<IntPair, Double> getBranchPowesMap() {
		return pVirtualBranches;
	}
	
	public static void main(String[] args) throws IOException {
		int count = 0;
		for (int i = 0; i < 500; i++) {
			TopologySimulator simulator = new TopologySimulator("D:/graduate/node_branch_P.txt");	
			//simulator.createVirtualDataByFile("D:/graduate/node_branch_P_dirty.txt");
			simulator.createVirtualDataByRandom();
			if (simulator.simulate() != 0)
			{
				count++;
			}
			simulator.toHtmlFile("D:/graduate/result.html");
		}
		System.out.println("Count:" + count);
	}

	@Override
	public Map<Integer, Double> getGoodBuses() {
		// TODO Auto-generated method stub
		return pGoodBuses;
	}

	@Override
	public Map<IntPair, Double> getGoodBranches() {
		// TODO Auto-generated method stub
		return pGoodBranches;
	}
}
