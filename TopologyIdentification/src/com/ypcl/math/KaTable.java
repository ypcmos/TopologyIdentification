package com.ypcl.math;

import java.util.HashMap;
import java.util.Map;

public abstract class KaTable {
	private Map<Integer, Double> table = new HashMap<Integer, Double>(); 
	abstract public KaTable fromFile(String path) throws Exception;
	
	public Double get(int key) {
		return table.get(key);
	}
	
	public KaTable put(int key, double value) {
		table.put(key, value);
		return this;
	}
	
	@Override
	public String toString() {
		return table.toString();
	}
}
