package com.ypcl.estimation.topology.parallel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.ypcl.estimation.topology.Branch;
import com.ypcl.estimation.topology.Bus;
import com.ypcl.estimation.topology.Topology;
import com.ypcl.struct.IntPair;

public class TopologyUnit extends Topology {
	private static final long serialVersionUID = 1L;
	private List<Integer> nodeMap = new ArrayList<Integer>();
	private int times = 100;
	public boolean isOk = false;
	private int virtualId(int realid) {
		for (int i = 0; i < nodeMap.size(); i++) {
			if (nodeMap.get(i) == realid) {
				return i + 1;
			}
		}
		return -1;
	}
	
	public TopologyUnit setTimes(int times) {
		this.times = times;
		return this;
	}
	
	public int getTimes() {
		return times;
	}
	
	private int realId(int virtualId) {
		return nodeMap.get(virtualId - 1);
	}
	
	private void createNodeMap(Map<Integer, Bus> buses) {
		for (int key : buses.keySet()) {
			nodeMap.add(key);
		}
	}
	
	public void fromMemory(Map<Integer, Bus> buses, Map<IntPair, Branch> branches, double s) {
		createNodeMap(buses);
		this.setBuses(buses);
		this.branches = branches;
		this.s = s;
	}
	
	public void restore() {
		Map<Integer, Bus> busesCopy = new TreeMap<Integer, Bus>();
		Map<IntPair, Branch> branchesCopy = new TreeMap<IntPair, Branch>();
		Iterator<Entry<Integer, Bus>> it = getBuses().entrySet().iterator();
		
		while (it.hasNext()) {
			Entry<Integer, Bus> entry = it.next();
			int id = realId(entry.getKey());
			busesCopy.put(id, entry.getValue().setId(id));
			it.remove();
			graph.putHeads(id);
		}
		
		Iterator<Entry<IntPair, Branch>> it2 = branches.entrySet().iterator();
		
		while (it2.hasNext()) {
			Entry<IntPair, Branch> entry = it2.next();
			IntPair id = new IntPair(realId(entry.getKey().getI()), realId(entry.getKey().getJ()));
			it2.remove();
			branchesCopy.put(id, entry.getValue().setId(id));
			graph.addConnection(id.getI(), id.getJ());
		}
		setBuses(busesCopy);
		branches = branchesCopy;
		
		for (Record record : badDataRecords) {
			if (record.deviceType == Record.BUS) {
				record.id = realId((int)record.id);
			} else if (record.deviceType == Record.BRANCH) {
				IntPair pair = (IntPair)record.id;
				record.id = new IntPair(realId(pair.getI()), realId(pair.getJ()));
			}
		}
	}
	
	public void regularize() {
		Map<Integer, Bus> busesCopy = new TreeMap<Integer, Bus>();
		Map<IntPair, Branch> branchesCopy = new TreeMap<IntPair, Branch>();
		Iterator<Entry<Integer, Bus>> it = getBuses().entrySet().iterator();
		
		while (it.hasNext()) {
			Entry<Integer, Bus> entry = it.next();
			int id = virtualId(entry.getKey());
			busesCopy.put(id, entry.getValue().setId(id));
			it.remove();
			graph.putHeads(id);
		}
		
		Iterator<Entry<IntPair, Branch>> it2 = branches.entrySet().iterator();
		
		while (it2.hasNext()) {
			Entry<IntPair, Branch> entry = it2.next();
			IntPair id = new IntPair(virtualId(entry.getKey().getI()), virtualId(entry.getKey().getJ()));
			it2.remove();
			branchesCopy.put(id, entry.getValue().setId(id));
			graph.addConnection(id.getI(), id.getJ());
		}
		setBuses(busesCopy);
		branches = branchesCopy;
	}
		
	public void showBadData() {
		System.out.println(name + " Bad Data:");
		for (Record r : badDataRecords) {
			System.out.println(r.toString() + "[" + (r.id instanceof Integer ? nodeMap.get((int)r.id - 1) : 
				(r.id instanceof IntPair ? (nodeMap.get(((IntPair)(r.id)).getI() - 1)) + "," + nodeMap.get(((IntPair)(r.id)).getJ() - 1) : "") + "]"));
		}
	}
	
	public boolean execute() throws IOException {
		return execute(times);
	}
	
	public boolean execute(int times) throws IOException {
		regularize();
		System.out.println(name + "映射：" + nodeMap);
		initialY();
		if (work(times)) {
			fillBus().fillBranch();
			restore();
			toFile("E:/研究生/毕业论文/code_data/ieee_t_output_" + name + ".csv");
			System.out.println(x);	
			isOk = true;
			return true;
		} 
		showBadData();
		return false;
	}
	
	public TopologyUnit removeBranch(int id) {
		Iterator<Entry<IntPair, Branch>> it = branches.entrySet().iterator();
		
		while (it.hasNext()) {
			IntPair pair = it.next().getKey();
			
			if (pair.getI() == id || pair.getJ() == id) {
				it.remove();
			}
		}
		return this;
	}
	
	public int anotherBranch(int id) {
		Iterator<Entry<IntPair, Branch>> it = branches.entrySet().iterator();
		int ret = -1;
		while (it.hasNext()) {
			IntPair pair = it.next().getKey();
			
			if (pair.getI() == id) {
				ret = pair.getJ();
				break;
			} else if (pair.getJ() == id) {
				ret = pair.getI();
				break;
			}
		}
		return ret;
	}
	
	@Override
	protected void dealRMatrix() {
		int index = 0;
		for (Record r : mRecords) {
			if (r.deviceType == Record.BUS && r.pointType == Bus.P) {
				int id = (int)r.id;
				Set<Integer> set = graph.getNodeLinks(id);
				boolean sign = true;
				for (int bid : set) {
					if (buses.get(bid).getType() == Bus.VIRTUAL) {
						sign = false;
						break;
					}
				}
				if (!sign) {
					R.set(index, index, 10e-4);
					//System.out.println("----------------------------------" + realId(id));
				}
				
			}
			if (r.deviceType == Record.BRANCH && (r.pointType == Branch.PIJ || 
					r.pointType == Branch.PJI)) {
				IntPair id = (IntPair)r.id;
				
				if (buses.get(id.getI()).getType() == Bus.VIRTUAL || buses.get(id.getJ()).getType() == Bus.VIRTUAL ) {
					//R.set(index, index, 2e-4);
					//System.out.println("----------------------------------" + realId(id.getI()) + "," +  realId(id.getJ()));
				}
			}
			index++;
		}
	}
}
