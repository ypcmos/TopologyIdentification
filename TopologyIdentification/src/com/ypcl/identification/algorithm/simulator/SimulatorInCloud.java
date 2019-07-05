package com.ypcl.identification.algorithm.simulator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.LocalDRPC;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;

import com.ypcl.data.FlowData;
import com.ypcl.identification.algorithm.TopologyIdentificationForParallel;
import com.ypcl.storm.trident.IdentificationTridentTopology2;
import com.ypcl.struct.IntPair;
import com.ypcl.struct.tools.Tools;
import com.ypcl.util.TimeState;

public class SimulatorInCloud extends Simulator {
	public SimulatorInCloud(String input, String tmppath) throws IOException {
		super(input, tmppath);
	}
	
	@SuppressWarnings("unchecked")
	public int simulateForTrident(LocalDRPC drpc, String name, int n) throws IOException, ClassNotFoundException {
		TopologyIdentificationForParallel ti = new TopologyIdentificationForParallel(this);
		List<TopologyIdentificationForParallel> list = ti.piece(n);
		  
        String arg = Tools.objectToUTF8String(list);   
        String ret = drpc.execute(name, arg);
        ret = ret.substring(3, ret.length() - 3);
		List<Object> result = (List<Object>)Tools.utf8StringToObject(ret);
        
        ti.merge((Map<Integer, Double>)result.get(0), (Map<IntPair, Double>)result.get(1));
        //cannot = ti.cannotIdentificate();
		cannot = ti.motherCheck();
		pGoodBuses = ti.getGoodBuses();
		pGoodBranches = ti.getGoodBranches();
		return cannot;
	}
	
	public SimulatorInCloud work(String output, LocalDRPC drpc, String name, int n) throws IOException, ClassNotFoundException {		
		File file = new File(output);
	    if (file.isFile() && file.exists()) {  
	        file.delete();  
	    }  
		int count = 0, bad = 0;
		sf.writeFlowInput(path);
		FlowData data = new FlowData(runFlow(path, 0, 100), d, baddp, badmin);
		int iterateTimes = 1;//data.N;
		while (data.index++ < iterateTimes) {
			pVirtualBuses = data.generateBusPower();
			pVirtualBranches = data.generateBranchPower();
			pBuses = data.pBuses;
			pBranches = data.pBranches;
			TimeState s = new TimeState();
			if (simulateForTrident(drpc, name, n) != 0)
			{
				count++;
			}
			System.out.println("Time:" + s.past());
			data.afterData(this);
			bad += toHtmlFile2(output);
			data.update(sf);
			sf.writeFlowInput(path);
			data.parse(runFlow(path, 0, 100));
		}
		System.out.println("Count:" + count);
		System.out.println("Bad:" + bad);
		//writeInjectPower("F:/graduate_data/test_4_1/ds.dat" , data);
		//data.drawOriginBuses("F:/graduate_data/test_4_1/ds.png");
		data.drawBus("F:/graduate_data/test_4_1/ds_pi.png", 1);
		data.drawBranch("F:/graduate_data/test_4_1/ds_pij.png", new IntPair(1, 2));
		data.drawBranch("F:/graduate_data/test_4_1/ds_pji.png", new IntPair(2, 1));
		return this;
	}
	

	public static void main(String[] args) {
		Config conf = new Config();
		conf.setDebug(false);
		System.loadLibrary("PowerFlow");
		if (args == null || args.length == 0) {
			LocalDRPC drpc = new LocalDRPC();
			LocalCluster cluster = new LocalCluster();

			cluster.submitTopology("drpc-identification", conf, IdentificationTridentTopology2.buildTopology("identification", drpc));
			int count = 0;
			
			try {
				SimulatorInCloud simulator = new SimulatorInCloud("F:/graduate_data/test_4_1/118IEEE.DAT", "F:/graduate_data/test_4_1/tmp/tmp.txt");
				simulator.work("F:/graduate_data/test_4_1/result.html", drpc, "identification", 4);//IdentificationTridentTopology2.boltsNum);
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Count:" + count);
			cluster.shutdown();
			drpc.shutdown();
		} else {
			conf.setNumWorkers(2);
			try {
				StormSubmitter.submitTopology(args[0], conf, IdentificationTridentTopology2.buildTopology("identification"));
			} catch (AlreadyAliveException | InvalidTopologyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
