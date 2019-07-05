package com.ypcl.estimation.simulator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.ypcl.data.FlowData;
import com.ypcl.data.FlowDataV2;
import com.ypcl.estimation.topology.Branch;
import com.ypcl.estimation.topology.Bus;
import com.ypcl.estimation.topology.Topology;
import com.ypcl.estimation.topology.parallel.TopologyManager;
import com.ypcl.identification.algorithm.simulator.ITopologySimulator;
import com.ypcl.identification.algorithm.simulator.Simulator;
import com.ypcl.ieee.StandardFile;
import com.ypcl.math.Constant;
import com.ypcl.struct.IntPair;

public class SimulatorForParallel implements ITopologySimulator{
	String path, path2;
	protected Map<Integer, Double> pBuses = new TreeMap<Integer, Double>();
	protected Map<IntPair, Double> pBranches = new TreeMap<IntPair, Double>();
	
	protected Map<Integer, Double> pVirtualBuses = new TreeMap<Integer, Double>();
	protected Map<IntPair, Double> pVirtualBranches = new TreeMap<IntPair, Double>();
	
	int cannot = 0;
	protected Map<Integer, Double> pGoodBuses = new TreeMap<Integer, Double>();
	protected Map<IntPair, Double> pGoodBranches = new TreeMap<IntPair, Double>();
	
	StandardFile sf = new StandardFile();
	StandardFile esf = new StandardFile();
	public static final double d = 4, baddp = 0.01, badmin = 10;
	
	public SimulatorForParallel(String input, String tmppath, String tmppath2) throws IOException {
		sf.readFile(input);
		esf.readFile(input);
		
		this.path = tmppath;
		this.path2 = tmppath2;
	}
	
	public SimulatorForParallel setData(TopologyManager t) {	
		Map<Integer, Bus> buses = t.getBuses();
		Map<IntPair, Branch> branches = t.getBranches();
		for (Entry<Integer, Bus> entry : buses.entrySet()) {
			Bus bus = entry.getValue();
			pGoodBuses.put(entry.getKey(), bus.getData(Bus.P_CAL) * t.getS());
		}
		
		for (Entry<IntPair, Branch> entry : branches.entrySet()) {
			Branch branch = entry.getValue();
			pGoodBranches.put(entry.getKey(), branch.getData(Branch.PIJ_CAL) * t.getS());
			pGoodBranches.put(entry.getKey().exchange(), branch.getData(Branch.PJI_CAL) * t.getS());
		}
		return this;
	}
	
	public boolean simulate(String blocksFile) throws Exception {
		boolean ret = false;
		TopologyManager t = new TopologyManager();
		t.fromFile(path2);
		t.initialY();

		Topology.initialKaTable("E:/研究生/毕业论文/code_data/k2.txt");
		String conf = "E:/研究生/毕业论文/code_data/distributed_file.txt";
		t.makeBlocksConfigure(blocksFile, conf);
		t.prepareBlock(conf);
		//System.out.println(t.kaTable);
		if (t.execute(1000)) {
			//t.correctPhaseAngle(30 / 180.0 * Math.PI);
			//t.toFile("E:/学习资料/研究生大本营/毕业论文/code_data/ieee_output_" + t.getName() + ".csv");
			ret = true;
		} else {
			System.out.println("不收敛");
		}
		t.showBadData();

		setData(t);
		return ret;
	}
		
	protected int output(BufferedWriter br, StringBuilder sb, String label, Double v1, double v2, double r) {
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
	
	public int toHtmlFile(String path) throws IOException {
		int bad = 0;
		BufferedWriter br = new BufferedWriter(new FileWriter(path, true));
		br.write("无法辨识的数目:" + cannot + "\r\n");
		
		StringBuilder sb = new StringBuilder("<table><tr style=\"background-color: #00CED1\"><th>标志</th><th>是否辨识</th><th>修正值</th><th>脏值</th><th>偏差</th><th>真实值</th><th>坏数据</th></tr>");
		for (Entry<Integer, Double> entry : pBuses.entrySet()) {
			Double v1 = pGoodBuses.get(entry.getKey());
					
			double v2 = pVirtualBuses.get(entry.getKey());
			double r = entry.getValue();
			bad += output(br, sb, entry.getKey().toString(), v1, v2, r);
		}
		
		/*for (Entry<IntPair, Double> entry : pBranches.entrySet()) {
			Double v1 = pGoodBranches.get(entry.getKey());
			double v2 = pVirtualBranches.get(entry.getKey());
			double r = entry.getValue();
			
			bad += output(br, sb, entry.getKey().toString(), v1, v2, r);
		}*/
		sb.append("</table>");
		br.write(sb.toString());
		br.close();
		return bad;
	}
	
	public void readyEstimate(FlowData data) throws IOException {
		pVirtualBuses = data.generateBusPower();
		pVirtualBranches = data.generateBranchPower();
		Map<Integer, Double> vs = data.pBusesVoltage;
		Map<Integer, Double> qs = data.pBusesQ;
		
		Map<IntPair, Double> pline = data.pBranches;
		Map<IntPair, Double> qline = data.pBranchesQ;
		for (Entry<Integer, Double> entry : pVirtualBuses.entrySet()) {
			esf.setBusPower(entry.getKey(), entry.getValue());//data.pBuses.get(entry.getKey()));
			esf.setBusVoltage(entry.getKey(), vs.get(entry.getKey()));
			esf.setBusQ(entry.getKey(), qs.get(entry.getKey()));
		}
		
		for (Entry<IntPair, Double> entry : pline.entrySet()) {
			esf.setBranchPower(entry.getKey(), pline.get(entry.getKey()) / 100);//pVirtualBranches.get(entry.getKey()) / 100);
			esf.setBranchPowerQ(entry.getKey(), qline.get(entry.getKey()) / 100);
		}
		esf.writeStateEstimationFullInput(path2);
	}
	
	public SimulatorForParallel work(String output, int n) throws Exception {		
		File file = new File(output);
	    if (file.isFile() && file.exists()) {  
	        file.delete();  
	    }  
	    file = new File("E:/graduate_data/test_4_1/result2.html");
	    if (file.isFile() && file.exists()) {  
	        file.delete();  
	    }  
		int count = 0, count2 = 0, bad = 0, bad2 = 0;
		sf.writeFlowInput(path);
		FlowDataV2 data = new FlowDataV2(Simulator.runFlow(path, 0, 100), d, baddp, badmin);
		FlowDataV2 data2 = new FlowDataV2(Simulator.runFlow(path, 0, 100), d, baddp, badmin);
		int iterateTimes = data.N;
		while (data.index++ < iterateTimes) {		
			readyEstimate(data);
			data2.rawBus.putAll(data.rawBus);
			pBuses = data.pBuses;
			pBranches = data.pBranches;
			//TimeState s = new TimeState();
			if (!simulate("E:/研究生/毕业论文/code_data/distributed_conf_14_3.txt"))
			{
				count++;
			}
			
			//System.out.println("Time:" + s.past());
			data.afterData(this);
			bad += toHtmlFile(output);
			if (!simulate("E:/研究生/毕业论文/code_data/distributed_conf_14_1.txt"))
			{
				count2++;
			}
			data2.afterData(this);
			bad2 += toHtmlFile("E:/graduate_data/test_4_1/result2.html");
			data.update(sf);
			sf.writeFlowInput(path);		
			data.parse(Simulator.runFlow(path, 0, 100));
		}
		System.out.println("Count:" + count);
		System.out.println("Count2:" + count2);
		System.out.println("Bad:" + bad);
		System.out.println("Bad2:" + bad2);
		//writeInjectPower("E:/graduate_data/test_4_1/ds.dat" , data);
		//data.drawOriginBuses("E:/graduate_data/test_4_1/ds.png");
		data.drawBus("E:/graduate_data/test_4_1/ds_pi11.png", 1);
		data.drawBus("E:/graduate_data/test_4_1/ds_pi12.png", 2);
		data.drawBus("E:/graduate_data/test_4_1/ds_pi13.png", 3);
		data.drawBus("E:/graduate_data/test_4_1/ds_pi14.png", 4);
		data.drawBus("E:/graduate_data/test_4_1/ds_pi15.png", 5);
		data.drawBus("E:/graduate_data/test_4_1/ds_pi16.png", 6);
		data.drawBus("E:/graduate_data/test_4_1/ds_pi17.png", 7);
		data.drawBus("E:/graduate_data/test_4_1/ds_pi18.png", 8);
		data.drawBus("E:/graduate_data/test_4_1/ds_pi19.png", 9);
		data.drawBus("E:/graduate_data/test_4_1/ds_pi110.png", 10);
		data.drawBus("E:/graduate_data/test_4_1/ds_pi111.png", 11);
		data.drawBus("E:/graduate_data/test_4_1/ds_pi112.png", 12);
		data.drawBus("E:/graduate_data/test_4_1/ds_pi113.png", 13);
		data.drawBus("E:/graduate_data/test_4_1/ds_pi114.png", 14);

		data2.drawBus("E:/graduate_data/test_4_1/ds_pi21.png", 1);
		data2.drawBus("E:/graduate_data/test_4_1/ds_pi22.png", 2);
		data2.drawBus("E:/graduate_data/test_4_1/ds_pi23.png", 3);
		data2.drawBus("E:/graduate_data/test_4_1/ds_pi24.png", 4);
		data2.drawBus("E:/graduate_data/test_4_1/ds_pi25.png", 5);
		data2.drawBus("E:/graduate_data/test_4_1/ds_pi26.png", 6);
		data2.drawBus("E:/graduate_data/test_4_1/ds_pi27.png", 7);
		data2.drawBus("E:/graduate_data/test_4_1/ds_pi28.png", 8);
		data2.drawBus("E:/graduate_data/test_4_1/ds_pi29.png", 9);
		data2.drawBus("E:/graduate_data/test_4_1/ds_pi210.png", 10);
		data2.drawBus("E:/graduate_data/test_4_1/ds_pi211.png", 11);
		data2.drawBus("E:/graduate_data/test_4_1/ds_pi212.png", 12);
		data2.drawBus("E:/graduate_data/test_4_1/ds_pi213.png", 13);
		data2.drawBus("E:/graduate_data/test_4_1/ds_pi214.png", 14);
		
		data.drawBranch("E:/graduate_data/test_4_1/ds_pij.png", new IntPair(1, 2));
		data.drawBranch("E:/graduate_data/test_4_1/ds_pji.png", new IntPair(2, 1));
		return this;
	}
	
	public static void main(String[] args) throws Exception {
		System.loadLibrary("PowerFlow");
		SimulatorForParallel s = new SimulatorForParallel("E:/graduate_data/test_4_1/014IEEE.DAT" ,"E:/graduate_data/test_4_1/tmp/tmp.txt", "E:/graduate_data/test_4_1/tmp/tmp2.txt");
		
		s.work("E:/graduate_data/test_4_1/result.html", 3);
		
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
