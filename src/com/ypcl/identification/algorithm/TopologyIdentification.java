package com.ypcl.identification.algorithm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ypcl.identification.algorithm.simulator.ITopologySimulator;
import com.ypcl.math.Constant;
import com.ypcl.struct.Graph;
import com.ypcl.struct.IntPair;
import com.ypcl.struct.tools.Tools;

public class TopologyIdentification implements Serializable {
	private static final long serialVersionUID = -5998922401716730508L;
	protected Graph<Integer> topology = new Graph<Integer>();
	protected Map<Integer, Double> pDirtyBuses = null;
	protected Map<IntPair, Double> pDirtyBranches = null;
	
	protected Map<Integer, Double> pGoodBuses = null;
	protected Map<IntPair, Double> pGoodBranches = null;
	
	protected List<IntPair> branchPairs = new LinkedList<IntPair>();
	protected static final double BUSBRASZERO = 1e-4, BRABRAZERO = 0.05;
	
	protected TopologyIdentification() {
		
	}
	public TopologyIdentification(String path) throws IOException {
		pDirtyBuses = new TreeMap<Integer, Double>();
		pDirtyBranches = new HashMap<IntPair, Double>();
		
		pGoodBuses = new TreeMap<Integer, Double>();
		pGoodBranches = new HashMap<IntPair, Double>();
		fromFile(path);
	}
	
	public TopologyIdentification(ITopologySimulator simulator) {
		pDirtyBuses = new TreeMap<Integer, Double>();
		pDirtyBranches = new HashMap<IntPair, Double>();
		
		pGoodBuses = new TreeMap<Integer, Double>();
		pGoodBranches = new HashMap<IntPair, Double>();
		fromSimulator(simulator);
	}
	
	public Map<Integer, Double> getGoodBuses() {
		return pGoodBuses;
	}

	public Map<IntPair, Double> getGoodBranches() {
		return pGoodBranches;
	}

	UnknownValue getBusValue(int id) {
		boolean isGood = false;
		Double busv = pDirtyBuses.get(id);
		if (busv == null) {
			busv = pGoodBuses.get(id);
			
			if (busv == null) {
				return null;
			}
			isGood = true;
		}
		return new UnknownValue(busv, isGood);	
	}
	
	UnknownValue getBranchValue(IntPair key) {
		boolean isGood = false;
		Double branchv = pDirtyBranches.get(key);
		
		if (branchv == null) {
			isGood = true;
			branchv = pGoodBranches.get(key);
			
			if (branchv == null) {
				return null;
			}
		}
		return new UnknownValue(branchv, isGood);	
	}
	
	UnknownValue getBranchValue(int i, int j) {
		return 	getBranchValue(new IntPair(i, j));
	}
	
	public double distanceBetweenNodeAndBranches(int id) {
		Set<Integer> links = topology.getNodeLinks(id);
		double value = getBusValue(id).value;
		double sum = 0;
		for (Integer link : links) {
			sum += getBranchValue(new IntPair(id, link)).value;
		}
		return Math.abs(value - sum);
	}
	
	public double distanceBetweenNodeAndBranchesSuper(int id, List<IntPair> pairs) {
		Set<Integer> links = topology.getNodeLinks(id);
		double value = getBusValue(id).value;
		double sum = 0;
		double basic = 0;
		
		List<IntPair> dirtyBras = new LinkedList<IntPair>();
		for (Integer link : links) {
			IntPair pair = new IntPair(id, link);
			UnknownValue v = getBranchValue(pair);
			
			if (v.isGood) {
				basic += v.value;
			} else {
				dirtyBras.add(pair);
			}
		}
		
		int len = dirtyBras.size();
		
		if (len == 0) {
			return Math.abs(value - basic);
		}
		
		double min = Double.POSITIVE_INFINITY;
		String minSign = null;
		
		int max = (int) Math.pow(2, len);
		
		for (int i = 0; i < max; i++) {
			String sign = Tools.fillZero(Tools.binary(i), len);
			sum = basic;
			for (int k = 0; k < len; k++) {
				if (sign.charAt(k) == '1') {
					sum -= getBranchValue(dirtyBras.get(k).exchange()).value;
				} else {
					sum += getBranchValue(dirtyBras.get(k)).value;
				}
			}
			sum = Math.abs(value - sum);
			
			if (sum < min) {
				min = sum;
				minSign = sign;
			}
		}
		
		for (int k = 0; k < len; k++) {
			if (minSign.charAt(k) == '1') {
				pairs.add(dirtyBras.get(k).exchange());
			} else {
				pairs.add(dirtyBras.get(k));
			}
		}
		return min;
	}
	
	public boolean validateBusUsingSimpleRule(int id, List<IntPair> pairs) {
		return distanceBetweenNodeAndBranchesSuper(id, pairs) < BUSBRASZERO;
	}
	
	public boolean validateBranchUsingSimpleRule(IntPair pair) {
		/*boolean numberCheck = false;
		double v1 = getBranchValue(pair).value, v2 = getBranchValue(pair.exchange()).value;
		
		if (v1 <= 0 && v2 >= 0) {
			numberCheck = (-v1 <= v2);
		} else if (v2 <= 0 && v1 >= 0) {
			numberCheck = (-v2 <= v1);
		}*/
		return /*numberCheck && */distanceBetweenBranches(pair) < BRABRAZERO;
	}
	
	public boolean validateBranchUsingSimpleRule(int i, int j) {
		return validateBranchUsingSimpleRule(new IntPair(i, j));
	}
	
	public double distanceBetweenBranches(IntPair pair) {
		double v1 = Math.abs(getBranchValue(pair).value), v2 = Math.abs(getBranchValue(pair.exchange()).value);
		if (Constant.isZero(v1) && Constant.isZero(v2)) {
			return 0;
		}
		return Math.abs((v1 - v2) / Math.min(v1, v2));
	}
	
	public double distanceBetweenBranches(int i, int j) {
		return Math.abs(distanceBetweenBranches(new IntPair(i, j)));
	}
	
	protected void putBusToGoodWithoutDelete(Map.Entry<Integer, Double> entry) {	
		pGoodBuses.put(entry.getKey(), entry.getValue());
	}
	
	protected boolean putBusToGood(int id) {
		Double busv = pDirtyBuses.get(id);
		if (busv != null) {
			pGoodBuses.put(id, busv);
			pDirtyBuses.remove(id);
			return true;
		} 
		return false;
	}
	
	protected boolean putBranchToGood(int i, int j) {
		return putBranchToGood(new IntPair(i, j));
	}
	
	protected boolean putBranchToGood(IntPair id) {
		Double branchv = pDirtyBranches.get(id);
		
		if (branchv != null) {
			pGoodBranches.put(id, branchv);
			pDirtyBranches.remove(id);
			return true;
		}
		
		return false;
	}
	
	protected void putAllBranchToGoodByBusID(int id) {
		Set<Integer> links = topology.getNodeLinks(id);

		for (Integer link : links) {
			putBranchToGood(id, link);		
			correctBranch(link, id, -getBranchValue(id, link).value);
		}
	}
	
	protected void putAllBranchToGood(List<IntPair> pairs) {
		for (IntPair pair : pairs) {
			putBranchToGood(pair);		
			correctBranch(pair.exchange(), -getBranchValue(pair).value);
		}
	}
	
	protected boolean correctBranch(int i, int j, double val) {
		return correctBranch(new IntPair(i, j), val);
	}
	
	protected boolean correctBranch(IntPair id, double val) {
		Double branchv = pDirtyBranches.get(id);
		
		if (branchv != null) {
			branchv = val;
			pGoodBranches.put(id, branchv);
			pDirtyBranches.remove(id);
			return true;
		}
		return false;
	}
	
	protected boolean correctByPrefixBranches(int id) {
		Set<Integer> links = topology.getNodeLinks(id);
		boolean ret = true;
		double sum = 0;
		for (Integer link : links) {
			UnknownValue value = getBranchValue(new IntPair(id, link));
			
			if (!value.isGood) {
				ret = false;
				break;
			} else {
				sum += value.value;
			}
		}
		
		if (ret) {
			correctBus(id, sum);
		}
		return ret;
	}
	
	protected boolean correctHalfByPrefixBranches(int id) {
		Set<Integer> links = topology.getNodeLinks(id);
		boolean ret = true;
		double sum = 0;
		for (Integer link : links) {
			UnknownValue value = getBranchValue(new IntPair(id, link));
			
			if (!value.isGood) {
				ret = false;
				break;
			} else {
				sum += value.value;
			}
		}
		
		if (ret) {
			correctBusWithoutDelete(id, sum);
		}
		return ret;
	}
	
	protected void correctBusWithoutDelete(int id, double val) {	
		pGoodBuses.put(id, val);		
	}
	
	protected boolean correctBus(int id, double val) {
		Double busv = pDirtyBuses.get(id);
		if (busv != null) {
			pGoodBuses.put(id, val);
			pDirtyBuses.remove(id);
			return true;
		} 
		return false;
	}
	
	protected double subBetweenBusAndBranches(int id, IntPair except) {
		Set<Integer> links = topology.getNodeLinks(id);
		double value = getBusValue(id).value;
		double sum = 0;
		int e = except.getI() == id ? except.getJ() : except.getI();
		for (Integer link : links) {
			if (link != e) {
				sum += getBranchValue(new IntPair(id, link)).value;
			}
		}
		return value - sum;
	}
	
	public void checkBranchesByPair() {
		for (IntPair pair : branchPairs) {
			if (validateBranchUsingSimpleRule(pair)) {
				putBranchToGood(pair);
				putBranchToGood(pair.exchange());
			}
		}
	}
	
	public boolean checkBusesWithBranchesBySum() {
		//节点正确，支路未定
		for (Entry<Integer, Double> busEntry : pGoodBuses.entrySet()) {
			List<IntPair> pairs = new LinkedList<IntPair>();
			if (validateBusUsingSimpleRule(busEntry.getKey(), pairs)) {
				putAllBranchToGood(pairs);
			}
		}
		
		int previous = 0;
		int now = pDirtyBranches.size();
		do {
			//节点未定，支路未定
			Iterator<Entry<Integer, Double>> it = pDirtyBuses.entrySet().iterator();
			while(it.hasNext()){  
	            Entry<Integer, Double> busEntry = it.next();  
	            List<IntPair> pairs = new LinkedList<IntPair>();
				if (validateBusUsingSimpleRule(busEntry.getKey(), pairs)) {
					putAllBranchToGood(pairs);
					putBusToGoodWithoutDelete(busEntry);
					it.remove();
				} else {
					if (correctHalfByPrefixBranches(busEntry.getKey())) {
						it.remove();
					}
				}
			}
			previous = now;
			now = pDirtyBranches.size();
		} while (now != previous);
		
		if (now == 0) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean checkSubBetweenBuses() {
		Map<IntPair, Double> forLoop = new HashMap<IntPair, Double>();
		forLoop.putAll(pDirtyBranches);
		for (Entry<IntPair, Double> braEntry : forLoop.entrySet()) {
			IntPair pair = braEntry.getKey();
			
			if (pDirtyBranches.get(pair) == null) {
				continue;
			}
			double pis = subBetweenBusAndBranches(pair.getI(), pair),
					pjs = subBetweenBusAndBranches(pair.getJ(), pair);
			
			double thta = Math.min(Math.abs(pis), Math.abs(pjs)) * BRABRAZERO;
			System.out.println(pair.getI() + "->" + pair.getJ() + ":" +  thta + "," + (pis + pjs));
			if (Math.abs(pis + pjs) <= thta) {
				putBusToGood(pair.getI());
				putBusToGood(pair.getJ());
				
				correctBranch(pair, pis);
				correctBranch(pair.exchange(), pjs);
				
				putAllBranchToGoodByBusID(pair.getI());
				putAllBranchToGoodByBusID(pair.getJ());
			}
		}
		
		return pDirtyBuses.size() == 0 && pDirtyBranches.size() == 0;
	}
	
	public void recheckBusesWithBranchesBySum() {
		Iterator<Entry<Integer, Double>> it = pDirtyBuses.entrySet().iterator();
		while(it.hasNext()) {  
            Entry<Integer, Double> busEntry = it.next();  			
			if (correctHalfByPrefixBranches(busEntry.getKey())) {
				it.remove();
			}			
		}
	}
	
	public int check() {
		int stillsize = 0;
		checkBranchesByPair();
		
		if (!checkBusesWithBranchesBySum()) {
			if (!checkSubBetweenBuses()) {
				recheckBusesWithBranchesBySum();
				stillsize = pDirtyBuses.size() + pDirtyBranches.size();
			}
		}
		//System.out.println(pGoodBuses);
		return stillsize;
	}
	
	public void statistics() {
		Set<Integer> bps = topology.getHeads();
		
		for (Integer bp : bps) {
			System.out.println("bus " + bp + ": " + (distanceBetweenNodeAndBranches(bp)));
		}
		
		for (IntPair pair : branchPairs) {
			System.out.println("branch " + pair.getI() + "," + pair.getJ() + ":" + distanceBetweenBranches(pair));
		}
	}
	
	private void fromSimulator(ITopologySimulator simulator) {
		Map<Integer, Double> pBuses = simulator.getBusPowesMap();
		Map<IntPair, Double> pBranches = simulator.getBranchPowesMap();
		for (Entry<Integer, Double> entry : pBuses.entrySet()) {
			int busid = entry.getKey();
			double busp = entry.getValue();
			topology.putHeads(busid);
			
			if (Constant.isZero(busp)) {
				pGoodBuses.put(busid, busp);
			} else {
				pDirtyBuses.put(busid, busp);
			}
		}
		
		for (Entry<IntPair, Double> entry : pBranches.entrySet()) {
			int i = entry.getKey().getI(), j = entry.getKey().getJ();
			double v = entry.getValue();
			
			if (!topology.isConnected(i, j)) {
				topology.addConnection(i, j);
			}
			
			IntPair pair = new IntPair(i, j);
			pDirtyBranches.put(pair, v);
			
			if (!branchPairs.contains(new IntPair(j, i))) {
				branchPairs.add(pair);
			}
		}
	}
	
	@Override
	public String toString() {
		return topology.toString() + "\r\n"
				+ pDirtyBuses.toString() + "\r\n"
				+ pGoodBuses.toString() + "\r\n"
				+ pDirtyBranches.toString() + "\r\n"
				+ pGoodBranches.toString() + "\r\n";
	}
	
	private void fromFile(String path) throws IOException {
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
					topology.putHeads(busid);
					
					if (Constant.isZero(busp)) {
						pGoodBuses.put(busid, busp);
					} else {
						pDirtyBuses.put(busid, busp);
					}
				} else {
					mat = branchpat.matcher(str);
					if (mat.find()) {
						int i = Integer.parseInt(mat.group(1)), j = Integer.parseInt(mat.group(2));
						double v = Double.parseDouble(mat.group(3));
						
						if (!topology.isConnected(i, j)) {
							topology.addConnection(i, j);
						}
						
						IntPair pair = new IntPair(i, j);
						pDirtyBranches.put(pair, v);
						
						if (!branchPairs.contains(new IntPair(j, i))) {
							branchPairs.add(pair);
						}
					} else {
						continue;
					}
				}
			}
		}
		br.close();
	}
	
	public static void main(String[] args) throws IOException {
		TopologyIdentification ti = new TopologyIdentification("D:/graduate/node_branch_P.txt");
		Graph<Integer> g = ti.topology.childGraphKeepConnection(8);
		System.out.println(g);
		System.out.println(ti.topology.separateChileGraph(g));
		System.out.println(ti.topology.pieces(2));
		ti.statistics();
		System.out.println("can not distinguish :" + ti.check());
	}
}

class UnknownValue {
	double value;
	boolean isGood;
	
	UnknownValue() {
		
	}
	
	UnknownValue(double value, boolean isGood) {
		this.value = value;
		this.isGood = isGood;	
	}
}
