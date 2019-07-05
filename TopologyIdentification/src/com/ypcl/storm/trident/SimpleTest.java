package com.ypcl.storm.trident;

import storm.trident.Stream;
import storm.trident.TridentTopology;
import storm.trident.operation.BaseFunction;
import storm.trident.operation.TridentCollector;
import storm.trident.tuple.TridentTuple;
import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.LocalDRPC;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.StormTopology;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

public class SimpleTest {
	static public int boltsNum = 4;
	public static class Distribute extends BaseFunction {
		private static final long serialVersionUID = 5771105058855098264L;

		@Override
		public void execute(TridentTuple tuple, TridentCollector collector) {
			String input = tuple.getString(0);	
			collector.emit(new Values(input + " Storm"));
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
		
		stream.each(new Fields("args"), new Distribute(), new Fields("ress"));
		//.project(new Fields("ress"));
		return topology.build();
	}
	
	public static void main(String[] args) throws Exception {
		Config conf = new Config();
		conf.setDebug(false);
		
		if (args == null || args.length == 0) {
			LocalDRPC drpc = new LocalDRPC();
			LocalCluster cluster = new LocalCluster();

			cluster.submitTopology("drpc-identification", conf, buildTopology("identification", drpc));
			long begin = System.currentTimeMillis();
			String ret = drpc.execute("identification", "Hello");
			System.out.println("Time:" + (System.currentTimeMillis() - begin));
			System.out.println(ret);
			cluster.shutdown();
			drpc.shutdown();
		} else {
			conf.setNumWorkers(2);
			StormSubmitter.submitTopology(args[0], conf, buildTopology("identification"));
		}
	}
}
