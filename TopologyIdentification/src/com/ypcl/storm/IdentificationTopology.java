package com.ypcl.storm;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.ypcl.identification.algorithm.TopologyIdentificationInCloud;
import com.ypcl.identification.algorithm.simulator.TopologySimulatorInCloud;
import com.ypcl.struct.IntPair;
import com.ypcl.struct.tools.Tools;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.LocalDRPC;
import backtype.storm.StormSubmitter;
import backtype.storm.coordination.BatchOutputCollector;
import backtype.storm.drpc.LinearDRPCTopologyBuilder;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.topology.base.BaseBatchBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

@SuppressWarnings("deprecation")
public class IdentificationTopology {
	static public int boltsNum = 3;
	public static class DistributeBolt extends BaseBasicBolt {
		private static final long serialVersionUID = -2377345472061837337L;

		@SuppressWarnings("unchecked")
		@Override
		public void execute(Tuple tuple, BasicOutputCollector collector) {
			String input = tuple.getString(1);
			List<TopologyIdentificationInCloud> list;
			try {
				list = (List<TopologyIdentificationInCloud>)Tools.utf8StringToObject(input);
				for (TopologyIdentificationInCloud t : list) {
					collector.emit(new Values(tuple.getValue(0), t));
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}

		@Override
		public void declareOutputFields(OutputFieldsDeclarer declarer) {
			declarer.declare(new Fields("id", "son_topology"));
		}
	}
	
	public static class IdentificationBolt extends BaseBasicBolt {
		private static final long serialVersionUID = -2377345472061837337L;

		@Override
		public void execute(Tuple tuple, BasicOutputCollector collector) {
			// TODO Auto-generated method stub
			TopologyIdentificationInCloud t = (TopologyIdentificationInCloud)tuple.getValue(1);
			t.checkPiece();
			
			Map<Integer, Double> bus = t.getGoodBuses();
			Map<IntPair, Double> branch = t.getGoodBranches();
			collector.emit(new Values(tuple.getValue(0), bus, branch));
		}

		@Override
		public void declareOutputFields(OutputFieldsDeclarer declarer) {
			declarer.declare(new Fields("id", "buses", "branches"));		
		}	
	}
	
	@SuppressWarnings("rawtypes")
	public static class CollectBolt extends BaseBatchBolt {
		private static final long serialVersionUID = 858798094169926416L;
		BatchOutputCollector collector;
		Object id;
		Map<Integer, Double> busMap = new TreeMap<Integer, Double>();
		Map<IntPair, Double> branchMap = new TreeMap<IntPair, Double>();
		@Override
		public void prepare(Map conf, TopologyContext context, BatchOutputCollector collector, Object id) {
			this.collector = collector;
			this.id = id;
		}

		@Override
		public void declareOutputFields(OutputFieldsDeclarer declarer) {
			declarer.declare(new Fields("id", "res"));
		}

		@SuppressWarnings("unchecked")
		@Override
		public void execute(Tuple tuple) {
			Map<Integer, Double> bus = (Map<Integer, Double>) tuple.getValue(1);
			Map<IntPair, Double> branch = (Map<IntPair, Double>) tuple.getValue(2);
			
			for (Entry<Integer, Double> entry : bus.entrySet()) {
				busMap.put(entry.getKey(), entry.getValue());
			}
			
			for (Entry<IntPair, Double> entry : branch.entrySet()) {
				branchMap.put(entry.getKey(), entry.getValue());
			}
		}

		@Override
		public void finishBatch() {
			try {
				collector.emit(new Values(id, Tools.objectToUTF8String(Arrays.asList(busMap, branchMap))));
			} catch (IOException e) {
				e.printStackTrace();
			}		
		}	
	}

	public static void main(String[] args) throws Exception {
		LinearDRPCTopologyBuilder builder = new LinearDRPCTopologyBuilder("identification");
		builder.addBolt(new DistributeBolt(), 2);
		builder.addBolt(new IdentificationBolt(), boltsNum).shuffleGrouping();
		builder.addBolt(new CollectBolt(), 2).fieldsGrouping(new Fields("id"));

		Config conf = new Config();
		conf.setDebug(false);
		
		if (args == null || args.length == 0) {
			LocalDRPC drpc = new LocalDRPC();
			LocalCluster cluster = new LocalCluster();

			cluster.submitTopology("drpc-identification", conf, builder.createLocalTopology(drpc));

			int count = 0;
			for (int i = 0; i < 100; i++) {
				TopologySimulatorInCloud simulator = new TopologySimulatorInCloud("D:/graduate/node_branch_P.txt");
				simulator.createVirtualDataByRandom();
				//simulator.createVirtualDataByFile("D:/graduate/why.txt");
				if (simulator.simulate(drpc, "identification", boltsNum) != 0) {
					count++;
				}
				simulator.toHtmlFile("D:/graduate/cloud_result.html");
			}
			System.out.println("Count:" + count);
			
			cluster.shutdown();
			drpc.shutdown();
		} else {
			conf.setNumWorkers(3);
			StormSubmitter.submitTopology(args[0], conf, builder.createRemoteTopology());
		}
	}
}
