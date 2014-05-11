package com.ypcl.identification.algorithm.simulator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.thrift7.TException;

import backtype.storm.LocalDRPC;
import backtype.storm.generated.DRPCExecutionException;
import backtype.storm.utils.DRPCClient;

import com.ypcl.identification.algorithm.TopologyIdentificationInCloud;
import com.ypcl.struct.IntPair;
import com.ypcl.struct.tools.Tools;

public class TopologySimulatorInCloud extends TopologySimulator {
	private int cannotInCloud;
	public TopologySimulatorInCloud(String path) throws IOException {
		super(path);
	}

	@SuppressWarnings("unchecked")
	public int simulate(LocalDRPC drpc, String name, int n) throws IOException, ClassNotFoundException {
		TopologyIdentificationInCloud ti = new TopologyIdentificationInCloud(this);
		List<TopologyIdentificationInCloud> list = ti.piece(n);
		  
        String arg = Tools.objectToUTF8String(list);   
        String ret = drpc.execute(name, arg);
		List<Object> result = (List<Object>)Tools.utf8StringToObject(ret);
        
        ti.merge((Map<Integer, Double>)result.get(0), (Map<IntPair, Double>)result.get(1));
		cannotInCloud = ti.cannotIdentificate();
		cannot = ti.motherCheck();
		pGoodBuses = ti.getGoodBuses();
		pGoodBranches = ti.getGoodBranches();
		return cannot;
	}
	
	@SuppressWarnings("unchecked")
	public int simulateForTrident(LocalDRPC drpc, String name, int n) throws IOException, ClassNotFoundException {
		TopologyIdentificationInCloud ti = new TopologyIdentificationInCloud(this);
		List<TopologyIdentificationInCloud> list = ti.piece(n);
		  
        String arg = Tools.objectToUTF8String(list);   
        String ret = drpc.execute(name, arg);
        ret = ret.substring(3, ret.length() - 3);
		List<Object> result = (List<Object>)Tools.utf8StringToObject(ret);
        
        ti.merge((Map<Integer, Double>)result.get(0), (Map<IntPair, Double>)result.get(1));
		cannotInCloud = ti.cannotIdentificate();
		cannot = ti.motherCheck();
		pGoodBuses = ti.getGoodBuses();
		pGoodBranches = ti.getGoodBranches();
		return cannot;
	}
	
	@SuppressWarnings("unchecked")
	public int simulateForTrident(DRPCClient client, String name, int n) throws IOException, ClassNotFoundException, TException, DRPCExecutionException {
		TopologyIdentificationInCloud ti = new TopologyIdentificationInCloud(this);
		List<TopologyIdentificationInCloud> list = ti.piece(n);
		  
        String arg = Tools.objectToUTF8String(list);   
        String ret = client.execute(name, arg);
        ret = ret.substring(3, ret.length() - 3);
		List<Object> result = (List<Object>)Tools.utf8StringToObject(ret);
        
        ti.merge((Map<Integer, Double>)result.get(0), (Map<IntPair, Double>)result.get(1));
		cannotInCloud = ti.cannotIdentificate();
		cannot = ti.motherCheck();
		pGoodBuses = ti.getGoodBuses();
		pGoodBranches = ti.getGoodBranches();
		return cannot;
	}
	
	@SuppressWarnings("unchecked")
	public int simulate(DRPCClient client, String name, int n) throws IOException, ClassNotFoundException, TException, DRPCExecutionException {
		TopologyIdentificationInCloud ti = new TopologyIdentificationInCloud(this);
		List<TopologyIdentificationInCloud> list = ti.piece(n);
		  
        String arg = Tools.objectToUTF8String(list);   
        String ret = client.execute(name, arg);
		List<Object> result = (List<Object>)Tools.utf8StringToObject(ret);
        
        ti.merge((Map<Integer, Double>)result.get(0), (Map<IntPair, Double>)result.get(1));
		cannotInCloud = ti.cannotIdentificate();
		cannot = ti.motherCheck();
		pGoodBuses = ti.getGoodBuses();
		pGoodBranches = ti.getGoodBranches();
		return cannot;
	}
	
	public void toHtmlFile(String path) throws IOException {
		BufferedWriter br = new BufferedWriter(new FileWriter(path, true));
		br.write("<font size='0.8em'>无法辨识的数目:" + cannot + "</font><br/>");
		br.write("<font size='0.8em'>云中无法辨识的数目:" + cannotInCloud + "</font><br/>");
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
}
