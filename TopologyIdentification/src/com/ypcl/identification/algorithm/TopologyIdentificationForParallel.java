package com.ypcl.identification.algorithm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.ypcl.identification.algorithm.simulator.ITopologySimulator;
import com.ypcl.struct.Graph;
import com.ypcl.struct.IntPair;
import com.ypcl.struct.Pair;

public class TopologyIdentificationForParallel  extends TopologyIdentification2{
	private static final long serialVersionUID = 9177008602715055932L;

	public TopologyIdentificationForParallel(ITopologySimulator simulator) {
		super(simulator);
	}
	
	public TopologyIdentificationForParallel(String path) throws IOException {
		super(path);
	}
	
	public TopologyIdentificationForParallel(Graph<Integer> topology, 
			Map<Integer, Double> pDirtyBuses, 
			Map<IntPair, Double> pDirtyBranches, 
			Map<Integer, Double> pGoodBuses) {
		this.topology = topology;
		this.pDirtyBranches = pDirtyBranches;
		this.pGoodBuses = pGoodBuses;
		this.pDirtyBuses = pDirtyBuses;
		this.pGoodBranches = new TreeMap<IntPair, Double>();
		for (Entry<IntPair, Double> entry : pDirtyBranches.entrySet()) {			
			IntPair pair = entry.getKey();
			
			if (!branchPairs.contains(pair.exchange())) {
				branchPairs.add(pair);
			}
		}
	}
	
	public List<TopologyIdentificationForParallel> piece(int n) {
		List<TopologyIdentificationForParallel> list = new ArrayList<TopologyIdentificationForParallel>(n);
		List<Graph<Integer>> gs = topology.pieces(n);
				
		System.out.println(gs);
		for (Graph<Integer> g : gs) {
			Map<Integer, Double> pDirtyBuses = new TreeMap<Integer, Double>(), 				
					pGoodBuses = new TreeMap<Integer, Double>();
			
			Map<IntPair, Double> pDirtyBranches = new TreeMap<IntPair, Double>();
			
			for (Integer head : g.getHeads()) {
				UnknownValue value = getBusValue(head);				
				if (value.isGood) {
					pGoodBuses.put(head, value.value);		
				} else {
					pDirtyBuses.put(head, value.value);
				}
				
				Set<Integer> links = g.getConnectedNodes(head);
				
				for (Integer link : links) {
					IntPair pair = new IntPair(head, link);
					pDirtyBranches.put(pair, this.pDirtyBranches.get(pair));
					pair = pair.exchange();
					pDirtyBranches.put(pair, this.pDirtyBranches.get(pair));
				}
			}
			
			List<Pair<Integer>> dcs = topology.disconnectLinks(g);
			if (dcs.size() > 0) {
				for (Pair<Integer> pairInt : dcs) {
					int h = pairInt.getI();				
					int virtual = pairInt.getJ();
					IntPair pair = new IntPair(h, virtual);
					if (!g.getHeads().contains(virtual)) {
						g.putHeads(virtual);
					}
					
					g.addConnection(virtual, h);
					pDirtyBranches.put(pair, this.pDirtyBranches.get(pair));
					pair = pair.exchange();
					pDirtyBranches.put(pair, this.pDirtyBranches.get(pair));
				}
			}
			list.add(new TopologyIdentificationForParallel(g, pDirtyBuses, pDirtyBranches, pGoodBuses));
		}
		return list;
	}
	
	public void merge(Map<Integer, Double> pBuses, Map<IntPair, Double> pBranches) {
		for (Entry<Integer, Double> bus : pBuses.entrySet()) {
			correctBus(bus.getKey(), bus.getValue());
		}
		
		for (Entry<IntPair, Double> branch : pBranches.entrySet()) {
			correctBranch(branch.getKey(), branch.getValue());
		}
	}
	
	public int cannotIdentificate() {
		return pDirtyBuses.size() + pDirtyBranches.size();
	}
	
	public int checkPiece() {
		int stillsize = 0;
		checkBranchesByPair();
		
		if (!checkBusesWithBranchesBySum()) {
			stillsize = pDirtyBuses.size() + pDirtyBranches.size();
		}
		return stillsize;
	}
	
	public boolean motherCheckBusesWithBranchesBySum() {		
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
	
	public int motherCheck() {
		int stillsize = 0;
	
		if (!motherCheckBusesWithBranchesBySum()) {
			if (!checkSubBetweenBuses()) {
				recheckBusesWithBranchesBySum();
				stillsize = pDirtyBuses.size() + pDirtyBranches.size();
			}
		}
		return stillsize;
	}
}
