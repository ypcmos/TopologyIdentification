package com.ypcl.estimation.topology.parallel.storm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import backtype.storm.LocalDRPC;
import backtype.storm.generated.StormTopology;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

import com.ypcl.estimation.topology.parallel.TopologyUnit;
import com.ypcl.struct.tools.Tools;

import storm.trident.Stream;
import storm.trident.TridentTopology;
import storm.trident.operation.BaseFunction;
import storm.trident.operation.CombinerAggregator;
import storm.trident.operation.TridentCollector;
import storm.trident.tuple.TridentTuple;

public class StateEstimateTopology {
	static public int boltsNum = 5;
	public static class Distribute extends BaseFunction {
		private static final long serialVersionUID = 1L;

		@SuppressWarnings("unchecked")
		@Override
		public void execute(TridentTuple tuple, TridentCollector collector) {
			String input = tuple.getString(0);
			List<TopologyUnit> list = null;
			try {
				list = (List<TopologyUnit>) Tools.utf8StringToObject(input);
				for (TopologyUnit t : list) {
					collector.emit(new Values(t));
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
	}
	
	public static class StateEstimate extends BaseFunction {
		private static final long serialVersionUID = 1L;

		@Override
		public void execute(TridentTuple tuple, TridentCollector collector) {
			TopologyUnit t = (TopologyUnit) tuple.getValue(0);
			try {
				t.execute();
			} catch (IOException e) {
				e.printStackTrace();
			}
			collector.emit(new Values(t));
		}
	}
	
	public static class Collect implements CombinerAggregator<List<TopologyUnit>> {
		private static final long serialVersionUID = 1L;

		@Override
		public List<TopologyUnit> combine(List<TopologyUnit> arg0,
				List<TopologyUnit> arg1) {
			arg0.addAll(arg1);
			return arg0;
		}

		@Override
		public List<TopologyUnit> init(TridentTuple arg0) {
			List<TopologyUnit> ts = new ArrayList<TopologyUnit>();
			ts.add((TopologyUnit) arg0.getValue(0));
			return ts;
		}

		@Override
		public List<TopologyUnit> zero() {
			return null;
		}
	}
	
	public static class ConvertToUtf8Url extends BaseFunction {
		private static final long serialVersionUID = 1L;

		@Override
		public void execute(TridentTuple tuple, TridentCollector collector) {
			try {
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
		.each(new Fields("son_topology"), new StateEstimate(), new Fields("topologys"))
		.parallelismHint(boltsNum)
		.aggregate(new Fields("topologys"), new Collect(), new Fields("res"))
		.each(new Fields("res"), new ConvertToUtf8Url(), new Fields("ress"))
		.project(new Fields("ress"));
		return topology.build();
	}
}
