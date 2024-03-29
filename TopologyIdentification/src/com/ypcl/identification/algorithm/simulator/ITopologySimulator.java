package com.ypcl.identification.algorithm.simulator;

import java.util.Map;

import com.ypcl.struct.IntPair;

public interface ITopologySimulator {
	public Map<Integer, Double> getBusPowersMap();
	public Map<IntPair, Double> getBranchPowersMap();
	public Map<Integer, Double> getGoodBuses();
	public Map<IntPair, Double> getGoodBranches();
}
