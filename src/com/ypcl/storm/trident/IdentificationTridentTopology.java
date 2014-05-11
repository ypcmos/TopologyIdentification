package com.ypcl.storm.trident;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.ypcl.identification.algorithm.TopologyIdentificationInCloud;
import com.ypcl.identification.algorithm.simulator.TopologySimulatorInCloud;
import com.ypcl.struct.IntPair;
import com.ypcl.struct.tools.Tools;

import storm.trident.Stream;
import storm.trident.TridentTopology;
import storm.trident.operation.BaseFunction;
import storm.trident.operation.CombinerAggregator;
import storm.trident.operation.TridentCollector;
import storm.trident.tuple.TridentTuple;
import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.LocalDRPC;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.StormTopology;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

public class IdentificationTridentTopology {
	static public int boltsNum = 3;
	public static class Distribute extends BaseFunction {
		private static final long serialVersionUID = 5771105058855098264L;

		@SuppressWarnings("unchecked")
		@Override
		public void execute(TridentTuple tuple, TridentCollector collector) {
			String input = tuple.getString(0);
			List<TopologyIdentificationInCloud> list;
			try {
				list = (List<TopologyIdentificationInCloud>)Tools.utf8StringToObject(input);
				for (TopologyIdentificationInCloud t : list) {
					collector.emit(new Values(t));
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}	
			
		}
	}
	
	public static class Identification extends BaseFunction {
		private static final long serialVersionUID = 8408200610363169218L;

		@Override
		public void execute(TridentTuple tuple, TridentCollector collector) {
			TopologyIdentificationInCloud t = (TopologyIdentificationInCloud)tuple.getValue(0);
			t.checkPiece();	
			Map<Integer, Double> bus = t.getGoodBuses();
			Map<IntPair, Double> branch = t.getGoodBranches();
			collector.emit(new Values(bus, branch));		
		}
		
	}
	
	public static class Collect implements CombinerAggregator<List<Object>> {
		private static final long serialVersionUID = -5306969309217158273L;

		@SuppressWarnings("unchecked")
		@Override
		public List<Object> combine(List<Object> arg0, List<Object> arg1) {
			Map<Integer, Double> busMap = (Map<Integer, Double>) arg0.get(0);
			Map<IntPair, Double> branchMap = (Map<IntPair, Double>) arg0.get(1);
			
			Map<Integer, Double> bus = (Map<Integer, Double>) arg1.get(0);
			Map<IntPair, Double> branch = (Map<IntPair, Double>) arg1.get(1);
			busMap.putAll(bus);
			branchMap.putAll(branch);	
			return Arrays.asList((Object)busMap, branchMap);
		}

		@SuppressWarnings("unchecked")
		@Override
		public List<Object> init(TridentTuple tuple) {		
			Map<Integer, Double> bus = (Map<Integer, Double>) tuple.getValue(0);
			Map<IntPair, Double> branch = (Map<IntPair, Double>) tuple.getValue(1);
			return Arrays.asList((Object)bus, branch);
		}

		@Override
		public List<Object> zero() {
			return null;
		}		
	}
	
	public static class ConvertToUtf8Url extends BaseFunction {
		private static final long serialVersionUID = -6719978009465810169L;

		@Override
		public void execute(TridentTuple tuple, TridentCollector collector) {
			try {
				System.out.println(Tools.objectToUTF8String(tuple.getValue(0)));
				collector.emit(new Values(Tools.objectToUTF8String(tuple.getValue(0))));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
	}
	
	public static StormTopology buildTopology(String function) {
		return buildTopology(function, null);
	}
	
	public static StormTopology buildTopology(String function, LocalDRPC drpc) {
		TridentTopology topology = new TridentTopology();
		Stream stream;

		if (drpc != null) {
			stream = topology.newDRPCStream(function, drpc);
		} else {
			stream = topology.newDRPCStream(function);
		}
		
		stream.each(new Fields("args"), new Distribute(), new Fields("son_topology"))
		.parallelismHint(2)
		.shuffle()
		.each(new Fields("son_topology"), new Identification(), new Fields("buses", "branches"))
		.parallelismHint(boltsNum)
		.aggregate(new Fields("buses", "branches"), new Collect(), new Fields("res"))
		.each(new Fields("res"), new ConvertToUtf8Url(), new Fields("ress"))
		.project(new Fields("ress"));
		return topology.build();
	}
	
	public static void main(String[] args) throws Exception {
		Config conf = new Config();
		conf.setDebug(false);
		
		if (args == null || args.length == 0) {
			LocalDRPC drpc = new LocalDRPC();
			LocalCluster cluster = new LocalCluster();

			cluster.submitTopology("drpc-identification", conf, buildTopology("identification", drpc));
			int count = 0;
			for (int i = 0; i < 100; i++) {
				TopologySimulatorInCloud simulator = new TopologySimulatorInCloud("D:/graduate/node_branch_P.txt");
				simulator.createVirtualDataByRandom();
				//simulator.createVirtualDataByFile("D:/graduate/why.txt");
				if (simulator.simulateForTrident(drpc, "identification", boltsNum) != 0) {
					count++;
				}
				simulator.toHtmlFile("D:/graduate/cloud_result.html");
			}
			System.out.println("Count:" + count);
			
			cluster.shutdown();
			drpc.shutdown();
		} else {
			conf.setNumWorkers(2);
			StormSubmitter.submitTopology(args[0], conf, buildTopology("identification"));
		}
	}
}
