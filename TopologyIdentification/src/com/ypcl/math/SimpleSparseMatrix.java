package com.ypcl.math;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.ypcl.struct.IntPair;

public class SimpleSparseMatrix {
	private Map<IntPair, Double> data = new TreeMap<IntPair, Double>();
	
	public SimpleSparseMatrix set(int i, int j, double v) {
		return set(new IntPair(i, j), v);
	}
	
	public SimpleSparseMatrix set(IntPair pair, double v) {
		data.put(pair, v);
		return this;
	}
	
	public double get(IntPair pair) {
		return data.get(pair);
	}
	
	public double get(int i, int j) {
		return data.get(new IntPair(i, j));
	}
	
	public Matrix toMatrix(int row, int col) {
		Matrix matrix = new Matrix(row, col).setValue(0);
		
		for (Entry<IntPair, Double> entry : data.entrySet()) {
			matrix.set(entry.getKey().getI(), entry.getKey().getJ(), entry.getValue());
		}
		return matrix;
	}
	
	@Override 
	public String toString() {
		return data.toString();
	}
}
