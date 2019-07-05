package com.ypcl.estimation.topology.parallel.storm;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.LocalDRPC;

import com.ypcl.estimation.topology.Branch;
import com.ypcl.estimation.topology.Bus;
import com.ypcl.estimation.topology.Topology;
import com.ypcl.estimation.topology.parallel.TopologyManager;
import com.ypcl.estimation.topology.parallel.TopologyUnit;
import com.ypcl.math.Vector;
import com.ypcl.struct.IntPair;
import com.ypcl.struct.tools.Tools;

public class TridentTopologyManager extends TopologyManager {
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	public boolean execute(LocalDRPC drpc, String name, int times) throws IOException, ClassNotFoundException {
		List<TopologyUnit> ts = makeBlocks();
		for (final TopologyUnit t : ts) {
			t.setTimes(times);
		}
		String arg = Tools.objectToUTF8String(ts);   
        String ret = drpc.execute(name, arg);
        ret = ret.substring(3, ret.length() - 3);
		ts = (List<TopologyUnit>) Tools.utf8StringToObject(ret);
		return stat(ts);
	}
	
	private boolean stat(List<TopologyUnit> ts) {
		buses.clear();
		branches.clear();
		badDataRecords.clear();
		
		int size = ts.size();
		Collections.sort(ts, new Comparator<TopologyUnit>() {
			@Override
			public int compare(TopologyUnit o1, TopologyUnit o2) {
				return o1.getName().compareTo(o2.getName());
			}	
		});
		TopologyUnit tu = ts.get(0);
		if (!tu.isOk) {
			return false;
		}
		for (int i = 1; i < size; i++) {	
			TopologyUnit t = ts.get(i);
			if (t.isOk) {
				tu = mergeBlocks(tu, t);
			} else {
				return false;
			}
		}
		
		buses = tu.getBuses();
		branches = tu.getBranches();
		badDataRecords = tu.getBadDataRecord();
		
		double[] pseux = new double[buses.size() * 2 - 1]; 
		for (Entry<Integer, Bus> entry : buses.entrySet()) {
			int id = entry.getKey();
			Bus bus = entry.getValue();
			if (id != 1) {
				pseux[indexOfCita(id - 1)] = bus.getData(Bus.CITA_CAL);
			}
			pseux[indexOfV(id - 1)] = bus.getData(Bus.V_CAL);
		}
		
		x = new Vector(pseux);
		System.out.println(x.size());
		for (Branch branch : borders) {
			IntPair pair = branch.getId();
			branches.put(pair, branch);
			int i = pair.getI() - 1, j = pair.getJ() - 1;
			branch.putData(Branch.PIJ_CAL, calPij(i, j));
			branch.putData(Branch.QIJ_CAL, calQij(i, j));
			branch.putData(Branch.PJI_CAL, calPij(j, i));
			branch.putData(Branch.QJI_CAL, calQij(j, i));
		}
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		Config conf = new Config();
		conf.setDebug(true);
		
		String topologyName = "StateEstimation";
		if (args == null || args.length == 0) {
			LocalDRPC drpc = new LocalDRPC();
			LocalCluster cluster = new LocalCluster();

			cluster.submitTopology("drpc-StateEstimation", conf, StateEstimateTopology.buildTopology(topologyName, drpc));
			long begin = System.currentTimeMillis();
			
			TridentTopologyManager t = new TridentTopologyManager();
			t.fromFile("E:/学习资料/研究生大本营/毕业论文/code_data/ieee118es.dat");
			t.initialY();
			Topology.initialKaTable("E:/学习资料/研究生大本营/毕业论文/code_data/k2.txt");
			String conffile = "E:/学习资料/研究生大本营/毕业论文/code_data/distributed_file.txt";
			t.makeBlocksConfigure("E:/学习资料/研究生大本营/毕业论文/code_data/distributed_conf_118_1.txt", conffile);
			t.prepareBlock(conffile);
			
			//System.out.println(t.kaTable);
			if (t.execute(drpc, topologyName, 1000)) {
				t.toFile("E:/学习资料/研究生大本营/毕业论文/code_data/ieee118_output_storm_" + t.getName() + ".csv");
			} else {
				System.out.println("不收敛");
			}
			t.showBadData();
			
			System.out.println("Time:" + (System.currentTimeMillis() - begin));
			cluster.shutdown();
			drpc.shutdown();
		}
	}
}
